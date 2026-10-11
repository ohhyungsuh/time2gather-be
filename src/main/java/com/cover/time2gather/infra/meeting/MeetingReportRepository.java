package com.cover.time2gather.infra.meeting;

import com.cover.time2gather.domain.meeting.MeetingReport;
import java.util.Optional;

/** Persistence port implemented by JPA locally and Firestore in production. */
public interface MeetingReportRepository {

    <S extends MeetingReport> S save(S entity);

    Optional<MeetingReport> findByMeetingId(Long meetingId);
}
