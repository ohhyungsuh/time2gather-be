package com.cover.time2gather.infra.comment;

import com.cover.time2gather.domain.comment.MeetingComment;
import com.cover.time2gather.domain.exception.BusinessException;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.firestore.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Repository @Profile("firestore") @RequiredArgsConstructor
public class FirestoreCommentStore implements CommentStore {
    private final Firestore firestore;
    private final ObjectMapper mapper = new ObjectMapper()
            .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    private DocumentReference scope(Long meetingId) { return firestore.collection("meetingComments").document(meetingId.toString()); }
    private List<MeetingComment> decode(QuerySnapshot snapshot) {
        return snapshot.getDocuments().stream().map(d -> mapper.convertValue(d.getData(), MeetingComment.class)).toList();
    }
    @Override public Stats stats(Long meetingId) {
        try {
            DocumentSnapshot d = scope(meetingId).get().get(15, TimeUnit.SECONDS);
            return new Stats(d.exists() ? d.getLong("updatedAt") : 0, d.exists() ? d.getLong("count") : 0);
        } catch (Exception e) { throw failure(e); }
    }
    @Override public List<MeetingComment> list(Long meetingId) {
        try { return decode(scope(meetingId).collection("items").get().get(15, TimeUnit.SECONDS)); }
        catch (Exception e) { throw failure(e); }
    }
    @Override public MeetingComment mutate(Long meetingId, Function<List<MeetingComment>, MeetingComment> mutation) {
        try {
            return firestore.runTransaction(tx -> {
                DocumentReference guard = scope(meetingId);
                // Reading and writing this guard serializes writes even when the collection is empty.
                tx.get(guard).get();
                List<MeetingComment> all = decode(tx.get(guard.collection("items")).get());
                MeetingComment result = mutation.apply(all);
                long count = all.stream().filter(c -> !c.getId().equals(result.getId()) && c.active()).count() + (result.active() ? 1 : 0);
                long version = Math.max(result.getUpdatedAt(), all.stream().mapToLong(MeetingComment::getUpdatedAt).max().orElse(0));
                tx.set(guard.collection("items").document(result.getId()), mapper.convertValue(result, new TypeReference<Map<String, Object>>() {}));
                tx.set(guard, Map.of("updatedAt", version, "count", count));
                return result;
            }).get(30, TimeUnit.SECONDS);
        } catch (Exception e) { throw failure(e); }
    }
    private RuntimeException failure(Exception e) {
        for (Throwable t = e; t != null; t = t.getCause()) if (t instanceof BusinessException b) return b;
        return new IllegalStateException("Comment persistence failed", e);
    }
}
