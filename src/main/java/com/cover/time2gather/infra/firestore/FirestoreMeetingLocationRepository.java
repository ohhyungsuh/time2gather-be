package com.cover.time2gather.infra.firestore;

import com.cover.time2gather.domain.meeting.MeetingLocation;
import com.cover.time2gather.infra.meeting.MeetingLocationRepository;
import com.google.cloud.firestore.Firestore;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("firestore")
public class FirestoreMeetingLocationRepository extends FirestoreRepositorySupport<MeetingLocation>
        implements MeetingLocationRepository {

    public FirestoreMeetingLocationRepository(Firestore firestore) {
        super(firestore, "meetingLocations", MeetingLocation.class);
    }

    @Override
    public <S extends MeetingLocation> S save(S entity) {
        return saveEntity(entity);
    }

    @Override
    public <S extends MeetingLocation> List<S> saveAll(Iterable<S> entities) {
        List<S> saved = new ArrayList<>();
        entities.forEach(entity -> saved.add(saveEntity(entity)));
        return saved;
    }

    @Override
    public Optional<MeetingLocation> findById(Long id) {
        return findEntityById(id);
    }

    @Override
    public void delete(MeetingLocation entity) {
        deleteEntity(entity);
    }

    @Override
    public List<MeetingLocation> selectByMeetingId(Long meetingId) {
        return queryEquals("meetingId", meetingId);
    }

    @Override
    public List<MeetingLocation> selectByMeetingIdOrderByDisplayOrderAsc(Long meetingId) {
        return sorted(selectByMeetingId(meetingId), Comparator.comparing(MeetingLocation::getDisplayOrder));
    }

    @Override
    public int countByMeetingId(Long meetingId) {
        return selectByMeetingId(meetingId).size();
    }

    @Override
    public void deleteByMeetingId(Long meetingId) {
        deleteWhere("meetingId", meetingId);
    }
}
