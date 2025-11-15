package com.cover.time2gather.domain.meeting.service;

import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.meeting.MeetingRepository;
import com.cover.time2gather.domain.user.User;
import com.cover.time2gather.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingService {

    private static final String CODE_PREFIX = "mtg_";
    private static final int CODE_LENGTH = 10;
    private static final String CODE_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";

    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public Meeting createMeeting(
            Long hostUserId,
            String title,
            String description,
            String timezone,
            Map<String, int[]> availableDates
    ) {
        User host = userRepository.findById(hostUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String meetingCode = generateUniqueMeetingCode();

        Meeting meeting = Meeting.create(
                meetingCode,
                title,
                description,
                host,
                timezone,
                availableDates
        );

        return meetingRepository.save(meeting);
    }

    public Meeting getMeetingByCode(String meetingCode) {
        return meetingRepository.findByMeetingCode(meetingCode)
                .orElseThrow(() -> new IllegalArgumentException("Meeting not found"));
    }

    private String generateUniqueMeetingCode() {
        String code;
        do {
            code = CODE_PREFIX + generateRandomString(CODE_LENGTH);
        } while (meetingRepository.existsByMeetingCode(code));
        return code;
    }

    private String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CODE_CHARS.charAt(secureRandom.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }
}

