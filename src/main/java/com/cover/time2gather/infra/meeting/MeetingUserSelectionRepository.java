package com.cover.time2gather.infra.meeting;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.cover.time2gather.domain.meeting.MeetingUserSelection;

/** Persistence port implemented by JPA locally and Firestore in production. */
public interface MeetingUserSelectionRepository {

    <S extends MeetingUserSelection> S save(S entity);

    default <S extends MeetingUserSelection> S saveForCandidates(S entity, com.cover.time2gather.domain.meeting.Meeting expected) {
        return save(entity);
    }

    void delete(MeetingUserSelection entity);

    Optional<MeetingUserSelection> findByMeetingIdAndUserId(Long meetingId, Long userId);

    List<MeetingUserSelection> findAllByMeetingId(Long meetingId);

    List<MeetingUserSelection> findAllByUserId(Long userId);

    List<MeetingUserSelection> findAllByUserIdIn(Collection<Long> userIds);

    List<MeetingUserSelection> findAllByMeetingIdIn(Collection<Long> meetingIds);
}
