package com.cover.time2gather.infra.firestore;

import com.cover.time2gather.domain.exception.*;
import com.cover.time2gather.domain.meeting.*;
import com.cover.time2gather.infra.meeting.MeetingCandidateStore;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.cloud.firestore.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Repository @Profile("firestore")
public class FirestoreMeetingCandidateStore implements MeetingCandidateStore {
    private final Firestore db;
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .setVisibility(PropertyAccessor.ALL,JsonAutoDetect.Visibility.NONE).setVisibility(PropertyAccessor.FIELD,JsonAutoDetect.Visibility.ANY);
    public FirestoreMeetingCandidateStore(Firestore db) { this.db=db; }
    private <T> T decode(DocumentSnapshot doc,Class<T> type) { return mapper.convertValue(doc.getData(),type); }
    private Map<String,Object> encode(Object value) { return mapper.convertValue(value,new TypeReference<Map<String,Object>>(){}); }
    @Override public void update(String code,Long host,MeetingCandidates.Edit edit) {
        try {
            db.runTransaction(tx -> {
                var matches=tx.get(db.collection("meetings").whereEqualTo("meetingCode",code).limit(1)).get();
                if (matches.isEmpty()) throw new BusinessException(ErrorCode.MEETING_NOT_FOUND);
                var document=matches.getDocuments().getFirst(); var meeting=decode(document,Meeting.class);
                var locationDocs=tx.get(db.collection("meetingLocations").whereEqualTo("meetingId",meeting.getId())).get().getDocuments();
                var voteDocs=tx.get(db.collection("meetingUserSelections").whereEqualTo("meetingId",meeting.getId())).get().getDocuments();
                var locationVotes=tx.get(db.collection("meetingLocationSelections").whereEqualTo("meetingId",meeting.getId())).get().getDocuments();
                var latest=new HashMap<Long,MeetingUserSelection>();
                Comparator<MeetingUserSelection> order=Comparator.comparing((MeetingUserSelection s) -> s.getUpdatedAt()==null?s.getCreatedAt():s.getUpdatedAt(),Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(MeetingUserSelection::getId,Comparator.nullsFirst(Comparator.naturalOrder()));
                for(var doc:voteDocs) { var selection=decode(doc,MeetingUserSelection.class); latest.merge(selection.getUserId(),selection,(a,b)->order.compare(a,b)>=0?a:b); }
                var plan=MeetingCandidates.plan(meeting,host,edit,locationDocs.stream().map(d->decode(d,MeetingLocation.class)).toList(),new ArrayList<>(latest.values()));
                var now=LocalDateTime.now().toString();
                // All candidate, retained-vote, deletion and signal writes share this transaction.
                tx.update(document.getReference(),Map.of("availableDates",encode(meeting).get("availableDates"),"updatedAt",now));
                for(var place:plan.locations()) {
                    var fields=encode(place);
                    if(place.getId()==null)fields.put("id",UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE);
                    tx.set(db.collection("meetingLocations").document(fields.get("id").toString()),fields);
                }
                for(Long id:plan.removedLocationIds())tx.delete(db.collection("meetingLocations").document(id.toString()));
                for(var vote:locationVotes)if(plan.removedLocationIds().contains(vote.getLong("locationId")))tx.delete(vote.getReference());
                var retainedKeys=new HashSet<String>();
                for(var selection:plan.savedSelections()) {
                    String key=FirestoreMeetingUserSelectionRepository.documentId(meeting.getId(),selection.getUserId());retainedKeys.add(key);
                    var fields=encode(selection);fields.put("updatedAt",now);
                    tx.set(db.collection("meetingUserSelections").document(key),fields);
                }
                for(var vote:voteDocs)if(!retainedKeys.contains(vote.getId()))tx.delete(vote.getReference());
                tx.set(db.collection("meetingSignals").document(code),Map.of("meetingVersion",FieldValue.increment(1)),SetOptions.merge());
                return null;
            }).get(45,TimeUnit.SECONDS);
        } catch(Exception failure) {
            if(failure instanceof InterruptedException)Thread.currentThread().interrupt();
            for(Throwable cause=failure;cause!=null;cause=cause.getCause())if(cause instanceof BusinessException business)throw business;
            throw new IllegalStateException("Firestore candidate edit failed",failure);
        }
    }
}
