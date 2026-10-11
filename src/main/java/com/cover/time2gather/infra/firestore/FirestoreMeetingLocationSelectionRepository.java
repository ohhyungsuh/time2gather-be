package com.cover.time2gather.infra.firestore;

import com.cover.time2gather.domain.meeting.MeetingLocationSelection;
import com.cover.time2gather.infra.meeting.MeetingLocationSelectionRepository;
import com.google.cloud.firestore.Firestore;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;

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
