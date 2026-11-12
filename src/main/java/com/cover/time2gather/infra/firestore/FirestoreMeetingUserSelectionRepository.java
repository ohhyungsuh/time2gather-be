package com.cover.time2gather.infra.firestore;

import com.cover.time2gather.domain.meeting.MeetingUserSelection;
import com.cover.time2gather.infra.meeting.MeetingUserSelectionRepository;
import com.google.cloud.firestore.Firestore;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository
@Profile("firestore")
public class FirestoreMeetingUserSelectionRepository extends FirestoreRepositorySupport<MeetingUserSelection>
        implements MeetingUserSelectionRepository {

    public FirestoreMeetingUserSelectionRepository(Firestore firestore) {
        super(firestore, "meetingUserSelections", MeetingUserSelection.class);
    }

    @Override
    public <S extends MeetingUserSelection> S save(S entity) {
        String key = documentId(entity.getMeetingId(), entity.getUserId());
        List<String> legacy = legacyDocumentIds(entity.getMeetingId(), entity.getUserId());
        // Concurrent first saves target the same document. Replacing it and removing
        // legacy rows is one atomic commit; no process-local lock is required.
        return saveEntityAt(entity, key, (batch, saved) -> {
            for (String id : legacy) batch.delete(firestore().collection("meetingUserSelections").document(id));
        });
    }

    @Override
    public <S extends MeetingUserSelection> S saveForCandidates(S entity, com.cover.time2gather.domain.meeting.Meeting expected) {
        var reference = firestore().collection("meetings").document(expected.getId().toString());
        try {
            for (int attempt = 0; attempt < 16; attempt++) {
                var snapshot = reference.get().get(15, TimeUnit.SECONDS);
                if (!snapshot.exists()) throw new com.cover.time2gather.domain.exception.BusinessException(com.cover.time2gather.domain.exception.ErrorCode.MEETING_NOT_FOUND);
                var current = decodeDocument(snapshot, com.cover.time2gather.domain.meeting.Meeting.class);
                if (current.isConfirmed()) throw new com.cover.time2gather.domain.exception.BusinessException(com.cover.time2gather.domain.exception.ErrorCode.MEETING_ALREADY_CONFIRMED);
                if (!com.cover.time2gather.domain.meeting.MeetingCandidates.version(expected, List.of()).equals(
                        com.cover.time2gather.domain.meeting.MeetingCandidates.version(current, List.of()))) {
                    throw new com.cover.time2gather.domain.exception.BusinessException(com.cover.time2gather.domain.exception.ErrorCode.SELECTION_CANDIDATES_CHANGED);
                }
                var legacy = legacyDocumentIds(entity.getMeetingId(), entity.getUserId());
                try {
                    return saveEntityAt(entity, documentId(entity.getMeetingId(), entity.getUserId()), (batch, saved) -> {
                        batch.update(reference, java.util.Map.of("meetingCode", expected.getMeetingCode()),
                                com.google.cloud.firestore.Precondition.updatedAt(snapshot.getUpdateTime()));
                        for (String id : legacy) batch.delete(firestore().collection("meetingUserSelections").document(id));
                    });
                } catch (IllegalStateException conflict) {
                    var code = io.grpc.Status.fromThrowable(conflict).getCode();
                    if (attempt == 15 || !java.util.Set.of(io.grpc.Status.Code.ABORTED, io.grpc.Status.Code.FAILED_PRECONDITION).contains(code)) throw conflict;
                    Thread.sleep(java.util.concurrent.ThreadLocalRandom.current().nextInt(10, 51));
                }
            }
            throw new IllegalStateException("Vote retry limit reached");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while saving vote", interrupted);
        } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException failure) {
            throw new IllegalStateException("Unable to read vote candidates", failure);
        }
    }

    @Override
    public void delete(MeetingUserSelection entity) {
        List<String> ids = new ArrayList<>(legacyDocumentIds(entity.getMeetingId(), entity.getUserId()));
        ids.add(documentId(entity.getMeetingId(), entity.getUserId()));
        deleteEntityDocuments(entity, ids);
    }

    @Override
    public Optional<MeetingUserSelection> findByMeetingIdAndUserId(Long meetingId, Long userId) {
        return queryEquals("meetingId", meetingId, "userId", userId).stream().max(LATEST);
    }

    @Override
    public List<MeetingUserSelection> findAllByMeetingId(Long meetingId) {
        return latestPerParticipant(queryEquals("meetingId", meetingId));
    }

    @Override
    public List<MeetingUserSelection> findAllByUserIdIn(Collection<Long> userIds) {
        return latestPerParticipant(queryIn("userId", userIds));
    }

    @Override
    public List<MeetingUserSelection> findAllByMeetingIdIn(Collection<Long> meetingIds) {
        return latestPerParticipant(queryIn("meetingId", meetingIds));
    }

    @Override
    public List<MeetingUserSelection> findAllByUserId(Long userId) {
        return latestPerParticipant(queryEquals("userId", userId));
    }

    static String documentId(Long meetingId, Long userId) {
        return "selection-" + meetingId + "-" + userId;
    }

    private static final Comparator<MeetingUserSelection> LATEST = Comparator
            .comparing((MeetingUserSelection value) -> value.getUpdatedAt() == null ? value.getCreatedAt() : value.getUpdatedAt(),
                    Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(MeetingUserSelection::getId, Comparator.nullsFirst(Comparator.naturalOrder()));

    private static List<MeetingUserSelection> latestPerParticipant(List<MeetingUserSelection> values) {
        var latest = new LinkedHashMap<String, MeetingUserSelection>();
        for (var value : values) latest.merge(documentId(value.getMeetingId(), value.getUserId()), value,
                (first, second) -> LATEST.compare(first, second) >= 0 ? first : second);
        return List.copyOf(latest.values());
    }

    private List<String> legacyDocumentIds(Long meetingId, Long userId) {
        try {
            String key = documentId(meetingId, userId);
            return firestore().collection("meetingUserSelections").whereEqualTo("meetingId", meetingId)
                    .whereEqualTo("userId", userId).get().get(15, TimeUnit.SECONDS).getDocuments().stream()
                    .map(document -> document.getId()).filter(id -> !id.equals(key)).toList();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while reading legacy selections", e);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to read legacy selections", e);
        }
    }
}
