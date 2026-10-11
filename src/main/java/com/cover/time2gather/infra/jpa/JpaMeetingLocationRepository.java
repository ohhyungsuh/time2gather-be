package com.cover.time2gather.infra.jpa;

import com.cover.time2gather.domain.meeting.MeetingLocation;
import com.cover.time2gather.infra.meeting.MeetingLocationRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Profile("!firestore")
public interface JpaMeetingLocationRepository extends MeetingLocationRepository, JpaRepository<MeetingLocation, Long> {

    @Override
    @Query("SELECT l FROM MeetingLocation l WHERE l.meetingId = :meetingId")
    List<MeetingLocation> selectByMeetingId(@Param("meetingId") Long meetingId);

    @Override
    @Query("SELECT l FROM MeetingLocation l WHERE l.meetingId = :meetingId ORDER BY l.displayOrder ASC")
    List<MeetingLocation> selectByMeetingIdOrderByDisplayOrderAsc(@Param("meetingId") Long meetingId);
}
