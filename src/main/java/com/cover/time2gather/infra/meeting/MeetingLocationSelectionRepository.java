package com.cover.time2gather.infra.meeting;

import com.cover.time2gather.domain.meeting.MeetingLocationSelection;
import java.util.List;

/** Persistence port implemented by JPA locally and Firestore in production. */
public interface MeetingLocationSelectionRepository {

    <S extends MeetingLocationSelection> S save(S entity);

    List<MeetingLocationSelection> selectByMeetingIdAndUserId(Long meetingId, Long userId);

    List<MeetingLocationSelection> selectByMeetingId(Long meetingId);

    List<MeetingLocationSelection> selectByLocationId(Long locationId);

    int countByLocationId(Long locationId);

    void deleteByMeetingIdAndUserId(Long meetingId, Long userId);

    void deleteByLocationId(Long locationId);

    boolean existsByMeetingIdAndLocationIdAndUserId(Long meetingId, Long locationId, Long userId);
}
