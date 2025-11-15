package com.cover.time2gather.domain.meeting;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MeetingUserSelectionRepository extends JpaRepository<MeetingUserSelection, Long> {

    Optional<MeetingUserSelection> findByMeetingIdAndUserId(Long meetingId, Long userId);

    List<MeetingUserSelection> findAllByMeetingId(Long meetingId);
}
