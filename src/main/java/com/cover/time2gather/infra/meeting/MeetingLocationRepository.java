package com.cover.time2gather.infra.meeting;

import com.cover.time2gather.domain.meeting.MeetingLocation;
import java.util.List;
import java.util.Optional;

/** Persistence port implemented by JPA locally and Firestore in production. */
public interface MeetingLocationRepository {

    <S extends MeetingLocation> S save(S entity);

    <S extends MeetingLocation> List<S> saveAll(Iterable<S> entities);

    Optional<MeetingLocation> findById(Long id);

    void delete(MeetingLocation entity);

    List<MeetingLocation> selectByMeetingId(Long meetingId);

    List<MeetingLocation> selectByMeetingIdOrderByDisplayOrderAsc(Long meetingId);

    int countByMeetingId(Long meetingId);

    void deleteByMeetingId(Long meetingId);
}
