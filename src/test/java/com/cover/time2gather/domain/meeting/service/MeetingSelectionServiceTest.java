package com.cover.time2gather.domain.meeting.service;

import com.cover.time2gather.domain.exception.BusinessException;
import com.cover.time2gather.domain.exception.ErrorCode;
import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.meeting.MeetingUserSelection;
import com.cover.time2gather.domain.meeting.SelectionType;
import com.cover.time2gather.domain.user.UserRepository;
import com.cover.time2gather.infra.meeting.MeetingReportRepository;
import com.cover.time2gather.infra.meeting.MeetingRepository;
import com.cover.time2gather.infra.meeting.MeetingUserSelectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingSelectionServiceTest {

    @Mock
    private MeetingUserSelectionRepository selectionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private MeetingReportRepository reportRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private MeetingSelectionService selectionService;

    @Nested
    @DisplayName("upsertUserSelections")
    class UpsertUserSelections {

        @Test
        @DisplayName("확정된 미팅에서 투표 수정 시 MEETING_ALREADY_CONFIRMED 예외 발생")
        void shouldThrowExceptionWhenMeetingAlreadyConfirmed() {
            // Given
            Long meetingId = 1L;
            Long userId = 2L;
            Map<String, int[]> selections = Map.of("2024-02-15", new int[]{9, 10});

            Meeting confirmedMeeting = createConfirmedMeeting();

            when(userRepository.existsById(userId)).thenReturn(true);
            when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(confirmedMeeting));

            // When & Then
            assertThatThrownBy(() -> selectionService.upsertUserSelections(meetingId, userId, selections))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException = (BusinessException) exception;
                    assert businessException.getErrorCode() == ErrorCode.MEETING_ALREADY_CONFIRMED;
                });
        }

        @Test
        @DisplayName("미확정 미팅에서 투표 수정 성공")
        void shouldSucceedWhenMeetingNotConfirmed() {
            // Given
            Long meetingId = 1L;
            Long userId = 2L;
            Map<String, int[]> selections = Map.of("2024-02-15", new int[]{9, 10});

            Meeting meeting = createMeeting();

            when(userRepository.existsById(userId)).thenReturn(true);
            when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(meeting));
            when(selectionRepository.findByMeetingIdAndUserId(meetingId, userId)).thenReturn(Optional.empty());

            // When & Then - 예외 없이 실행되어야 함
            selectionService.upsertUserSelections(meetingId, userId, selections);
        }

        private Meeting createMeeting() {
            Map<String, int[]> availableDates = Map.of(
                "2024-02-15", new int[]{9, 10, 11},
                "2024-02-16", new int[]{14, 15}
            );

            return Meeting.create(
                "mtg_test123",
                "테스트 미팅",
                "테스트 설명",
                1L,
                "Asia/Seoul",
                SelectionType.TIME,
                60,
                availableDates
            );
        }

        private Meeting createConfirmedMeeting() {
            Meeting meeting = createMeeting();
            // 미팅을 확정 상태로 만듦
            meeting.confirm(LocalDate.of(2024, 2, 15), 9);
            return meeting;
        }
    }
}
