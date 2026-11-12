package com.cover.time2gather.infra.firestore;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.cloud.firestore.CollectionReference;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.AggregateQuerySnapshot;
import com.google.cloud.firestore.QuerySnapshot;
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
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import com.cover.time2gather.domain.common.CursorPage;
import com.google.cloud.firestore.FieldPath;
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
import java.util.function.BiConsumer;

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
        return saveEntity(entity, (batch, saved) -> { });
    }

    protected Firestore firestore() { return collection.getFirestore(); }

    protected <S extends T> S saveEntity(S entity, BiConsumer<WriteBatch, S> additionalWrites) {
        return saveEntityAt(entity, null, additionalWrites);
    }

    /** Keep the numeric entity ID while allowing a natural key to enforce uniqueness. */
    protected <S extends T> S saveEntityAt(S entity, String documentId, BiConsumer<WriteBatch, S> additionalWrites) {
        Long id = readLongField(entity, "id");
        if (id == null) {
            id = newId();
            writeField(entity, "id", id);
        }
        touchTimestamps(entity);

        try {
            Map<String, Object> data = mapper.convertValue(entity, MAP_TYPE);
            WriteBatch batch = firestore().batch();
            batch.set(collection.document(documentId == null ? id.toString() : documentId), data);
            additionalWrites.accept(batch, entity);
            batch.commit().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
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

    protected List<T> queryIn(String field, Collection<?> values) {
        List<?> distinctValues = values.stream().filter(Objects::nonNull).distinct().toList();
        List<T> results = new ArrayList<>();
        for (int start = 0; start < distinctValues.size(); start += 10) {
            results.addAll(query(collection.whereIn(field,
                    distinctValues.subList(start, Math.min(start + 10, distinctValues.size())))));
        }
        return results;
    }

    protected List<T> queryEquals(String field, Object value) {
        return query(collection.whereEqualTo(field, value));
    }

    protected List<T> queryNotNull(String field) {
        return query(collection.whereNotEqualTo(field, null));
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

    protected void deleteEntityDocuments(T entity, Collection<String> documentIds) {
        try {
            WriteBatch batch = firestore().batch();
            for (String id : documentIds) batch.delete(collection.document(id));
            batch.commit().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
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
        return countQuery(collection);
    }

    protected long countEqualTo(String field, Object value) {
        return countQuery(collection.whereEqualTo(field, value));
    }

    protected long countNotNull(String field) {
        // The SDK maps a null inequality to Firestore's IS_NOT_NULL unary filter.
        return countQuery(collection.whereNotEqualTo(field, null));
    }

    private long countQuery(Query query) {
        ApiFuture<AggregateQuerySnapshot> read = query.count().get();
        try {
            return read.get(TIMEOUT_SECONDS, TimeUnit.SECONDS).getCount();
        } catch (InterruptedException e) {
            read.cancel(true);
            Thread.currentThread().interrupt();
            throw persistenceFailure("count", e);
        } catch (Exception e) {
            read.cancel(true);
            throw persistenceFailure("count", e);
        }
    }

    protected Page<T> newestPage(Pageable pageable) {
        return newestPage(pageable, null);
    }

    protected Page<T> newestPage(Pageable pageable, String cursor) {
        String[] anchor = cursor == null ? null : decodeCursor(cursor, pageable);
        long total = countAll();
        if (anchor == null && pageable.getOffset() >= total) {
            return new PageImpl<>(List.of(), pageable, total);
        }
        // Include the document ID so equal creation times have a stable boundary.
        Query query = collection.orderBy("createdAt", Query.Direction.DESCENDING)
                .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING);
        if (anchor != null) {
            // Values survive deletion of the boundary document; no extra document read.
            query = query.startAfter(anchor[4], collection.document(anchor[5]));
        } else if (pageable.getOffset() > 0) {
            // Preserve existing clients and random page-number access.
            query = query.offset(Math.toIntExact(pageable.getOffset()));
        }
        List<QueryDocumentSnapshot> documents = readDocuments(query.limit(pageable.getPageSize()));
        List<T> content = documents.stream().map(this::fromDocument).toList();
        var page = new PageImpl<>(content, pageable, total);
        String nextCursor = null;
        if (page.hasNext() && !documents.isEmpty()) {
            var last = documents.getLast();
            String payload = String.join("\n", "1", collection.getId(),
                    Integer.toString(pageable.getPageNumber() + 1), Integer.toString(pageable.getPageSize()),
                    (String) last.getData().get("createdAt"), last.getId());
            nextCursor = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        }
        return new CursorPage<>(content, pageable, page.getTotalElements(), nextCursor);
    }

    private String[] decodeCursor(String cursor, Pageable pageable) {
        try {
            if (cursor.length() > 512) throw new IllegalArgumentException();
            String[] fields = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8).split("\n", -1);
            if (fields.length != 6 || !fields[0].equals("1") || !fields[1].equals(collection.getId())
                    || Integer.parseInt(fields[2]) != pageable.getPageNumber()
                    || Integer.parseInt(fields[3]) != pageable.getPageSize() || pageable.getPageNumber() == 0
                    || !fields[5].matches("[1-9][0-9]{0,18}") || Long.parseLong(fields[5]) <= 0) {
                throw new IllegalArgumentException();
            }
            LocalDateTime.parse(fields[4]);
            return fields;
        } catch (RuntimeException invalid) {
            throw new IllegalArgumentException("Invalid pagination cursor");
        }
    }

    protected Page<T> searchNewest(Predicate<T> predicate, Pageable pageable) {
        // Arbitrary case-insensitive substring search needs a scan, but retain only
        // one bounded batch and the requested page, rather than every entity.
        Query ordered = collection.orderBy("createdAt", Query.Direction.DESCENDING);
        Query next = ordered.limit(100);
        List<T> page = new ArrayList<>();
        long matches = 0;
        while (true) {
            List<QueryDocumentSnapshot> documents = readDocuments(next);
            for (QueryDocumentSnapshot document : documents) {
                T entity = fromDocument(document);
                if (predicate.test(entity)) {
                    if (matches >= pageable.getOffset() && page.size() < pageable.getPageSize()) {
                        page.add(entity);
                    }
                    matches++;
                }
            }
            if (documents.size() < 100) break;
            next = ordered.startAfter(documents.getLast()).limit(100);
        }
        return new PageImpl<>(page, pageable, matches);
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
        return readDocuments(query).stream().map(this::fromDocument).toList();
    }

    private List<QueryDocumentSnapshot> readDocuments(Query query) {
        ApiFuture<QuerySnapshot> read = query.get();
        try {
            return read.get(TIMEOUT_SECONDS, TimeUnit.SECONDS).getDocuments();
        } catch (InterruptedException e) {
            read.cancel(true);
            Thread.currentThread().interrupt();
            throw persistenceFailure("query", e);
        } catch (Exception e) {
            read.cancel(true);
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

    protected <E> E decodeDocument(DocumentSnapshot document, Class<E> type) {
        return mapper.convertValue(document.getData(), type);
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
