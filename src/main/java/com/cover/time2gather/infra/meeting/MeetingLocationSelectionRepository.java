package com.cover.time2gather.infra.meeting;

import com.cover.time2gather.domain.meeting.MeetingLocationSelection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MeetingLocationSelectionRepository extends JpaRepository<MeetingLocationSelection, Long> {

    @Query("SELECT s FROM MeetingLocationSelection s WHERE s.meetingId = :meetingId AND s.userId = :userId")
    List<MeetingLocationSelection> selectByMeetingIdAndUserId(@Param("meetingId") Long meetingId, @Param("userId") Long userId);

    @Query("SELECT s FROM MeetingLocationSelection s WHERE s.meetingId = :meetingId")
    List<MeetingLocationSelection> selectByMeetingId(@Param("meetingId") Long meetingId);

    @Query("SELECT s FROM MeetingLocationSelection s WHERE s.locationId = :locationId")
    List<MeetingLocationSelection> selectByLocationId(@Param("locationId") Long locationId);

    int countByLocationId(Long locationId);

    @Modifying
    @Query("DELETE FROM MeetingLocationSelection s WHERE s.meetingId = :meetingId AND s.userId = :userId")
    void deleteByMeetingIdAndUserId(@Param("meetingId") Long meetingId, @Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM MeetingLocationSelection s WHERE s.locationId = :locationId")
    void deleteByLocationId(@Param("locationId") Long locationId);

    boolean existsByMeetingIdAndLocationIdAndUserId(Long meetingId, Long locationId, Long userId);
}
