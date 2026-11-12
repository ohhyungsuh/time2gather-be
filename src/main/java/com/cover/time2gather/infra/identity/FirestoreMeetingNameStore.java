package com.cover.time2gather.infra.identity;

import com.google.cloud.firestore.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository @Profile("firestore") @RequiredArgsConstructor
public class FirestoreMeetingNameStore implements MeetingNameStore {
    private final Firestore firestore;
    private DocumentReference scope(Long id) { return firestore.collection("meetingNames").document(id.toString()); }
    @Override public Optional<String> name(Long meetingId, Long userId) {
        try {
            var doc = scope(meetingId).collection("users").document(userId.toString()).get().get(15, TimeUnit.SECONDS);
            return doc.exists() ? Optional.ofNullable(doc.getString("name")) : Optional.empty();
        } catch (Exception e) { throw failure(e); }
    }
    @Override public Map<Long, String> names(Long meetingId) {
        try {
            Map<Long, String> result = new HashMap<>();
            for (var doc : scope(meetingId).collection("users").get().get(15, TimeUnit.SECONDS).getDocuments())
                result.put(Long.valueOf(doc.getId()), doc.getString("name"));
            return result;
        } catch (Exception e) { throw failure(e); }
    }
    @Override public long version(Long meetingId) {
        try {
            var root = scope(meetingId).get().get(15, TimeUnit.SECONDS);
            Long version = root.exists() ? root.getLong("version") : null;
            return version == null ? 0 : version;
        } catch (Exception e) { throw failure(e); }
    }
    @Override public String save(Long meetingId, String code, Long userId, String name, boolean onlyIfAbsent) {
        try {
            return firestore.runTransaction(tx -> {
                var meeting = tx.get(firestore.collection("meetings").document(meetingId.toString())).get();
                if (!meeting.exists() || !code.equals(meeting.getString("meetingCode"))) throw new IllegalStateException("Meeting missing");
                var root = scope(meetingId);
                var ref = root.collection("users").document(userId.toString());
                var existing = tx.get(ref).get();
                if (existing.exists() && (onlyIfAbsent || name.equals(existing.getString("name")))) return existing.getString("name");
                var state = tx.get(root).get();
                Long previous = state.exists() ? state.getLong("version") : null;
                long next = Math.max(System.currentTimeMillis(), previous == null ? 1 : previous + 1);
                tx.set(ref, Map.of("name", name));
                tx.set(root, Map.of("version", next));
                return name;
            }).get(30, TimeUnit.SECONDS);
        } catch (Exception e) { throw failure(e); }
    }
    private RuntimeException failure(Exception e) {
        if (e instanceof InterruptedException) Thread.currentThread().interrupt();
        return new IllegalStateException("Meeting name persistence failed", e);
    }
}
