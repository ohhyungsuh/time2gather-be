package com.cover.time2gather.infra.firestore;

import com.cover.time2gather.domain.meeting.MeetingReport;
import com.cover.time2gather.infra.meeting.MeetingReportRepository;
import com.google.cloud.firestore.Firestore;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Profile("firestore")
public class FirestoreMeetingReportRepository extends FirestoreRepositorySupport<MeetingReport>
        implements MeetingReportRepository {

    public FirestoreMeetingReportRepository(Firestore firestore) {
        super(firestore, "meetingReports", MeetingReport.class);
    }

    @Override
    public <S extends MeetingReport> S save(S entity) {
        return saveEntity(entity);
    }

    @Override
    public Optional<MeetingReport> findByMeetingId(Long meetingId) {
        return firstEquals("meetingId", meetingId);
    }
}
