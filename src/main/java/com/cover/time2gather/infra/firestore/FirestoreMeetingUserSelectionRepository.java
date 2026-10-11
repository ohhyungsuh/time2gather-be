package com.cover.time2gather.infra.firestore;

import com.cover.time2gather.domain.meeting.MeetingUserSelection;
import com.cover.time2gather.infra.meeting.MeetingUserSelectionRepository;
import com.google.cloud.firestore.Firestore;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Profile("firestore")
public class FirestoreMeetingUserSelectionRepository extends FirestoreRepositorySupport<MeetingUserSelection>
        implements MeetingUserSelectionRepository {

    public FirestoreMeetingUserSelectionRepository(Firestore firestore) {
        super(firestore, "meetingUserSelections", MeetingUserSelection.class);
    }

    @Override
    public <S extends MeetingUserSelection> S save(S entity) {
        return saveEntity(entity);
    }

    @Override
    public Optional<MeetingUserSelection> findByMeetingIdAndUserId(Long meetingId, Long userId) {
        return queryEquals("meetingId", meetingId, "userId", userId).stream().findFirst();
    }

    @Override
    public List<MeetingUserSelection> findAllByMeetingId(Long meetingId) {
        return queryEquals("meetingId", meetingId);
    }

    @Override
    public List<MeetingUserSelection> findAllByUserId(Long userId) {
        return queryEquals("userId", userId);
    }
}
