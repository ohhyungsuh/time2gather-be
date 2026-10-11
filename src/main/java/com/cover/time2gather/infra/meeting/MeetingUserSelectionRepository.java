package com.cover.time2gather.infra.meeting;

import java.util.List;
import java.util.Optional;

import com.cover.time2gather.domain.meeting.MeetingUserSelection;

/** Persistence port implemented by JPA locally and Firestore in production. */
public interface MeetingUserSelectionRepository {

    <S extends MeetingUserSelection> S save(S entity);

    Optional<MeetingUserSelection> findByMeetingIdAndUserId(Long meetingId, Long userId);

    List<MeetingUserSelection> findAllByMeetingId(Long meetingId);

    List<MeetingUserSelection> findAllByUserId(Long userId);
}
