package com.cover.time2gather.infra.meeting;

import com.cover.time2gather.domain.meeting.MeetingLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MeetingLocationRepository extends JpaRepository<MeetingLocation, Long> {

    @Query("SELECT l FROM MeetingLocation l WHERE l.meetingId = :meetingId")
    List<MeetingLocation> selectByMeetingId(@Param("meetingId") Long meetingId);

    @Query("SELECT l FROM MeetingLocation l WHERE l.meetingId = :meetingId ORDER BY l.displayOrder ASC")
    List<MeetingLocation> selectByMeetingIdOrderByDisplayOrderAsc(@Param("meetingId") Long meetingId);

    int countByMeetingId(Long meetingId);

    void deleteByMeetingId(Long meetingId);
}
