package com.cover.time2gather.infra.firestore;

import com.cover.time2gather.domain.meeting.MeetingLocationSelection;
import com.cover.time2gather.domain.exception.BusinessException;
import com.cover.time2gather.domain.exception.ErrorCode;
import com.cover.time2gather.infra.meeting.MeetingLocationSelectionRepository;
import com.google.cloud.firestore.Firestore;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Repository
@Profile("firestore")
public class FirestoreMeetingLocationSelectionRepository
        extends FirestoreRepositorySupport<MeetingLocationSelection>
        implements MeetingLocationSelectionRepository {

    public FirestoreMeetingLocationSelectionRepository(Firestore firestore) {
        super(firestore, "meetingLocationSelections", MeetingLocationSelection.class);
    }

    @Override
    public <S extends MeetingLocationSelection> S save(S entity) {
        return saveEntity(entity);
    }

    @Override
    public void replaceSelections(Long meetingId, Long userId, List<Long> locationIds) {
        var requested = List.copyOf(new LinkedHashSet<>(locationIds));
        var collection = firestore().collection("meetingLocationSelections");
        var guard = firestore().collection("meetingLocationVoteGuards").document(meetingId + "-" + userId);
        try {
            for (int attempt = 0; attempt < 16; attempt++) {
                // Read the per-person version before discovering existing records.
                // Its precondition fences the entire replacement, including first saves.
                var state = guard.get().get(15, TimeUnit.SECONDS);
                var meeting = firestore().collection("meetings").document(meetingId.toString()).get().get(15, TimeUnit.SECONDS);
                if (!meeting.exists()) throw new BusinessException(ErrorCode.MEETING_NOT_FOUND);
                if (!Boolean.TRUE.equals(meeting.getBoolean("locationVoteEnabled"))) {
                    throw new BusinessException(ErrorCode.LOCATION_VOTE_NOT_ENABLED);
                }
                for (Long id : requested) {
                    var location = firestore().collection("meetingLocations").document(id.toString()).get().get(15, TimeUnit.SECONDS);
                    if (!location.exists() || !meetingId.equals(location.getLong("meetingId"))) {
                        throw new BusinessException(ErrorCode.LOCATION_NOT_EXIST, id);
                    }
                }
                var existing = collection.whereEqualTo("meetingId", meetingId).whereEqualTo("userId", userId)
                        .get().get(15, TimeUnit.SECONDS).getDocuments();
                var batch = firestore().batch();
                var keys = requested.stream().map(id -> documentId(meetingId, userId, id)).collect(java.util.stream.Collectors.toSet());
                for (var document : existing) if (!keys.contains(document.getId())) batch.delete(document.getReference());
                for (Long id : requested) {
                    batch.set(collection.document(documentId(meetingId, userId, id)), Map.of(
                            "id", UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE,
                            "meetingId", meetingId, "locationId", id, "userId", userId,
                            "createdAt", LocalDateTime.now().toString()));
                }
                Map<String, Object> version = Map.of("meetingId", meetingId, "userId", userId,
                        "version", state.exists() ? state.getLong("version") + 1 : 1L);
                if (state.exists()) batch.update(guard, version, com.google.cloud.firestore.Precondition.updatedAt(state.getUpdateTime()));
                else batch.create(guard, version);
                // Candidate edits also update this document. A stale vote must retry
                // validation before it can write references to removed or renamed places.
                batch.update(meeting.getReference(), Map.of("meetingCode", meeting.getString("meetingCode")),
                        com.google.cloud.firestore.Precondition.updatedAt(meeting.getUpdateTime()));
                var pending = batch.commit();
                try {
                    pending.get(15, TimeUnit.SECONDS);
                    return;
                } catch (java.util.concurrent.TimeoutException | InterruptedException failure) {
                    pending.cancel(true);
                    throw failure;
                } catch (java.util.concurrent.ExecutionException conflict) {
                    String reason = io.grpc.Status.fromThrowable(conflict.getCause()).getCode().name();
                    boolean changed = !java.util.Objects.equals(state.getUpdateTime(), guard.get().get(15, TimeUnit.SECONDS).getUpdateTime())
                            || !java.util.Objects.equals(meeting.getUpdateTime(), meeting.getReference().get().get(15, TimeUnit.SECONDS).getUpdateTime());
                    if (attempt == 15 || !changed || !java.util.Set.of("ABORTED", "ALREADY_EXISTS", "FAILED_PRECONDITION").contains(reason)) throw conflict;
                    Thread.sleep(java.util.concurrent.ThreadLocalRandom.current().nextInt(10, 51));
                }
            }
        } catch (Exception failure) {
            if (failure instanceof InterruptedException) Thread.currentThread().interrupt();
            for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
                if (cause instanceof BusinessException business) throw business;
            }
            throw new IllegalStateException("Firestore location vote replacement failed", failure);
        }
    }

    private static String documentId(Long meetingId, Long userId, Long locationId) {
        return "selection-" + meetingId + "-" + userId + "-" + locationId;
    }

    @Override
    public List<MeetingLocationSelection> selectByMeetingIdAndUserId(Long meetingId, Long userId) {
        return queryEquals("meetingId", meetingId, "userId", userId);
    }

    @Override
    public List<MeetingLocationSelection> selectByMeetingId(Long meetingId) {
        return queryEquals("meetingId", meetingId);
    }

    @Override
    public List<MeetingLocationSelection> selectByLocationId(Long locationId) {
        return queryEquals("locationId", locationId);
    }

    @Override
    public int countByLocationId(Long locationId) {
        return selectByLocationId(locationId).size();
    }

    @Override
    public void deleteByMeetingIdAndUserId(Long meetingId, Long userId) {
        deleteWhere("meetingId", meetingId, "userId", userId);
    }

    @Override
    public void deleteByLocationId(Long locationId) {
        deleteWhere("locationId", locationId);
    }

    @Override
    public boolean existsByMeetingIdAndLocationIdAndUserId(Long meetingId, Long locationId, Long userId) {
        return selectByMeetingIdAndUserId(meetingId, userId).stream()
                .anyMatch(selection -> locationId.equals(selection.getLocationId()));
    }
}
