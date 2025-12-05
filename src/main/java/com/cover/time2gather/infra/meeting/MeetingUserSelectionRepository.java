package com.cover.time2gather.infra.meeting;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

import com.cover.time2gather.domain.meeting.MeetingUserSelection;

public interface MeetingUserSelectionRepository extends JpaRepository<MeetingUserSelection, Long> {

    Optional<MeetingUserSelection> findByMeetingIdAndUserId(Long meetingId, Long userId);

    List<MeetingUserSelection> findAllByMeetingId(Long meetingId);

    List<MeetingUserSelection> findAllByUserId(Long userId);
}
