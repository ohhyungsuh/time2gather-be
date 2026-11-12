package com.cover.time2gather.domain.identity;

import com.cover.time2gather.domain.exception.BusinessException;
import com.cover.time2gather.domain.exception.ErrorCode;
import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.user.User;
import com.cover.time2gather.infra.identity.MeetingNameStore;
import com.cover.time2gather.infra.meeting.MeetingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service @RequiredArgsConstructor
public class MeetingNameService {
    private final MeetingNameStore names;
    private final MeetingRepository meetings;
    private Meeting meeting(String code) {
        Meeting meeting = meetings.findByMeetingCode(code).orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND));
        if (!Boolean.TRUE.equals(meeting.getIsActive())) throw new BusinessException(ErrorCode.MEETING_NOT_FOUND);
        return meeting;
    }
    public String get(String code, User user) {
        return names.name(meeting(code).getId(), user.getId()).orElse(user.getUsername());
    }
    public String save(String code, User user, String name, boolean onlyIfAbsent) {
        Meeting meeting = meeting(code);
        return names.save(meeting.getId(), code, user.getId(), validate(name), onlyIfAbsent);
    }
    public static String validate(String name) {
        if (name == null || name.isBlank() || name.length() > 100) throw new BusinessException(ErrorCode.VALIDATION_USERNAME_REQUIRED);
        return name.trim();
    }
}
