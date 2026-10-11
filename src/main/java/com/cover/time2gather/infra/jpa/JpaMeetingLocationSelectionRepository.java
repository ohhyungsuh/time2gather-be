package com.cover.time2gather.infra.jpa;

import com.cover.time2gather.domain.meeting.MeetingLocationSelection;
import com.cover.time2gather.infra.meeting.MeetingLocationSelectionRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Profile("!firestore")
public interface JpaMeetingLocationSelectionRepository extends MeetingLocationSelectionRepository, JpaRepository<MeetingLocationSelection, Long> {

    @Override
    @Query("SELECT s FROM MeetingLocationSelection s WHERE s.meetingId = :meetingId AND s.userId = :userId")
    List<MeetingLocationSelection> selectByMeetingIdAndUserId(@Param("meetingId") Long meetingId, @Param("userId") Long userId);

    @Override
    @Query("SELECT s FROM MeetingLocationSelection s WHERE s.meetingId = :meetingId")
    List<MeetingLocationSelection> selectByMeetingId(@Param("meetingId") Long meetingId);

    @Override
    @Query("SELECT s FROM MeetingLocationSelection s WHERE s.locationId = :locationId")
    List<MeetingLocationSelection> selectByLocationId(@Param("locationId") Long locationId);

    @Override
    @Modifying
    @Query("DELETE FROM MeetingLocationSelection s WHERE s.meetingId = :meetingId AND s.userId = :userId")
    void deleteByMeetingIdAndUserId(@Param("meetingId") Long meetingId, @Param("userId") Long userId);

    @Override
    @Modifying
    @Query("DELETE FROM MeetingLocationSelection s WHERE s.locationId = :locationId")
    void deleteByLocationId(@Param("locationId") Long locationId);
}
