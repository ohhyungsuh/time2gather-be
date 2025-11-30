package com.cover.time2gather.infra.meeting;

import com.cover.time2gather.domain.meeting.MeetingReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MeetingReportRepository extends JpaRepository<MeetingReport, Long> {

    Optional<MeetingReport> findByMeetingId(Long meetingId);
}
