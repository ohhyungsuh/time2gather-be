package com.cover.time2gather.infra.meeting;
import com.cover.time2gather.domain.meeting.MeetingCandidates;
public interface MeetingCandidateStore {
    void update(String code, Long hostUserId, MeetingCandidates.Edit edit);
}
