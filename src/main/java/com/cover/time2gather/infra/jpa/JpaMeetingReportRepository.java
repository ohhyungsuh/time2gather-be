package com.cover.time2gather.infra.jpa;

import com.cover.time2gather.domain.meeting.MeetingReport;
import com.cover.time2gather.infra.meeting.MeetingReportRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

@Profile("!firestore")
public interface JpaMeetingReportRepository extends MeetingReportRepository, JpaRepository<MeetingReport, Long> {
}
