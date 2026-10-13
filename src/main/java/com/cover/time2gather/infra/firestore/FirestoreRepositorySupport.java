package com.cover.time2gather.infra.firestore;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.WriteBatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

abstract class FirestoreRepositorySupport<T> {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() { };
    private static final long TIMEOUT_SECONDS = 15;

    private final CollectionReference collection;
    private final Class<T> entityType;
    private final ObjectMapper mapper;

    protected FirestoreRepositorySupport(Firestore firestore, String collectionName, Class<T> entityType) {
        this.collection = firestore.collection(collectionName);
        this.entityType = entityType;
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }

    protected <S extends T> S saveEntity(S entity) {
        Long id = readLongField(entity, "id");
        if (id == null) {
            id = newId();
            writeField(entity, "id", id);
        }
        touchTimestamps(entity);

        try {
            Map<String, Object> data = mapper.convertValue(entity, MAP_TYPE);
            collection.document(id.toString()).set(data).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return entity;
        } catch (Exception e) {
            throw persistenceFailure("save", e);
        }
    }

    protected <S extends T> List<S> saveEntities(Iterable<S> entities) {
        List<S> saved = new ArrayList<>();
        entities.forEach(saved::add);
        if (saved.isEmpty()) {
            return saved;
        }

        try {
            WriteBatch batch = collection.getFirestore().batch();
            for (S entity : saved) {
                Long id = readLongField(entity, "id");
                if (id == null) {
                    id = newId();
                    writeField(entity, "id", id);
                }
                touchTimestamps(entity);
                batch.set(collection.document(id.toString()), mapper.convertValue(entity, MAP_TYPE));
            }
            batch.commit().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return saved;
        } catch (Exception e) {
            throw persistenceFailure("batch save", e);
        }
    }

    protected Optional<T> findEntityById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        try {
            DocumentSnapshot document = collection.document(id.toString())
                    .get().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return document.exists() ? Optional.of(fromDocument(document)) : Optional.empty();
        } catch (Exception e) {
            throw persistenceFailure("read", e);
        }
    }

    protected boolean entityExists(Long id) {
        return findEntityById(id).isPresent();
    }

    protected List<T> findEntitiesByIds(Iterable<Long> ids) {
        LinkedHashSet<Long> requestedIds = new LinkedHashSet<>();
        for (Long id : ids) {
            if (id != null) {
                requestedIds.add(id);
            }
        }
        if (requestedIds.isEmpty()) {
            return List.of();
        }

        try {
            DocumentReference[] references = requestedIds.stream()
                    .map(id -> collection.document(id.toString()))
                    .toArray(DocumentReference[]::new);
            Map<Long, T> entitiesById = new HashMap<>();
            for (DocumentSnapshot document : collection.getFirestore().getAll(references)
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                if (document.exists()) {
                    T entity = fromDocument(document);
                    entitiesById.put(readLongField(entity, "id"), entity);
                }
            }

            return requestedIds.stream()
                    .map(entitiesById::get)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            throw persistenceFailure("batch read", e);
        }
    }

    protected List<T> allEntities() {
        try {
            return collection.get().get(TIMEOUT_SECONDS, TimeUnit.SECONDS).getDocuments().stream()
                    .map(this::fromDocument)
                    .toList();
        } catch (Exception e) {
            throw persistenceFailure("list", e);
        }
    }

    protected List<T> queryEquals(String field, Object value) {
        return query(collection.whereEqualTo(field, value));
    }

    protected List<T> queryEquals(String firstField, Object firstValue, String secondField, Object secondValue) {
        return query(collection.whereEqualTo(firstField, firstValue).whereEqualTo(secondField, secondValue));
    }

    protected Optional<T> firstEquals(String field, Object value) {
        List<T> matches = query(collection.whereEqualTo(field, value).limit(1));
        return matches.isEmpty() ? Optional.empty() : Optional.of(matches.getFirst());
    }

    protected void deleteEntity(T entity) {
        Long id = readLongField(entity, "id");
        if (id == null) {
            return;
        }
        try {
            collection.document(id.toString()).delete().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw persistenceFailure("delete", e);
        }
    }

    protected void deleteWhere(String field, Object value) {
        deleteDocuments(collection.whereEqualTo(field, value));
    }

    protected void deleteWhere(String firstField, Object firstValue, String secondField, Object secondValue) {
        deleteDocuments(collection.whereEqualTo(firstField, firstValue).whereEqualTo(secondField, secondValue));
    }

    protected void deleteAllEntities() {
        deleteDocuments(collection);
    }

    protected long countAll() {
        return allEntities().size();
    }

    protected long countMatching(Predicate<T> predicate) {
        return allEntities().stream().filter(predicate).count();
    }

    protected Page<T> page(List<T> values, Pageable pageable) {
        int start = Math.toIntExact(Math.min(pageable.getOffset(), values.size()));
        int end = Math.min(start + pageable.getPageSize(), values.size());
        return new PageImpl<>(values.subList(start, end), pageable, values.size());
    }

    protected static <E> List<E> sorted(Collection<E> values, Comparator<E> comparator) {
        return values.stream().sorted(comparator).toList();
    }

    private List<T> query(Query query) {
        try {
            return query.get().get(TIMEOUT_SECONDS, TimeUnit.SECONDS).getDocuments().stream()
                    .map(this::fromDocument)
                    .toList();
        } catch (Exception e) {
            throw persistenceFailure("query", e);
        }
    }

    private void deleteDocuments(Query query) {
        try {
            for (QueryDocumentSnapshot document : query.get().get(TIMEOUT_SECONDS, TimeUnit.SECONDS).getDocuments()) {
                document.getReference().delete().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            throw persistenceFailure("delete query", e);
        }
    }

    private T fromDocument(DocumentSnapshot document) {
        T entity = mapper.convertValue(document.getData(), entityType);
        if (readLongField(entity, "id") == null) {
            writeField(entity, "id", Long.valueOf(document.getId()));
        }
        return entity;
    }

    private void touchTimestamps(Object entity) {
        LocalDateTime now = LocalDateTime.now();
        Field createdAt = findField(entity.getClass(), "createdAt");
        if (createdAt != null && readField(entity, createdAt) == null) {
            writeField(entity, createdAt, now);
        }
        Field updatedAt = findField(entity.getClass(), "updatedAt");
        if (updatedAt != null) {
            writeField(entity, updatedAt, now);
        }
    }

    private long newId() {
        long id;
        do {
            id = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
        } while (id == 0);
        return id;
    }

    private Long readLongField(Object target, String name) {
        Field field = findField(target.getClass(), name);
        return field == null ? null : (Long) readField(target, field);
    }

    private Object readField(Object target, Field field) {
        try {
            field.setAccessible(true);
            return field.get(target);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to read persistence field " + field.getName(), e);
        }
    }

    private void writeField(Object target, String name, Object value) {
        Field field = findField(target.getClass(), name);
        if (field == null) {
            throw new IllegalStateException("Missing persistence field " + name + " on " + target.getClass().getName());
        }
        writeField(target, field, value);
    }

    private void writeField(Object target, Field field, Object value) {
        try {
            field.setAccessible(true);
            field.set(target, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to write persistence field " + field.getName(), e);
        }
    }

    private Field findField(Class<?> type, String name) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                // Continue through mapped superclasses.
            }
        }
        return null;
    }

    private IllegalStateException persistenceFailure(String operation, Exception cause) {
        return new IllegalStateException("Firestore " + operation + " failed for " + collection.getId(), cause);
    }
}
