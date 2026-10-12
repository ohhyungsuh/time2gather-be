package com.cover.time2gather.domain.meeting.service;

import com.cover.time2gather.domain.exception.BusinessException;
import com.cover.time2gather.domain.exception.ErrorCode;
import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.meeting.MeetingUserSelection;
import com.cover.time2gather.domain.meeting.SelectionType;
import com.cover.time2gather.domain.user.UserRepository;
import com.cover.time2gather.infra.meeting.MeetingLocationRepository;
import com.cover.time2gather.infra.meeting.MeetingLocationSelectionRepository;
import com.cover.time2gather.infra.meeting.MeetingRepository;
import com.cover.time2gather.infra.meeting.MeetingUserSelectionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingAvailableDatesUpdateTest {

    @Mock
    private MeetingRepository meetingRepository;
    @Mock
    private MeetingLocationRepository locationRepository;
    @Mock
    private MeetingLocationSelectionRepository locationSelectionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MeetingUserSelectionRepository selectionRepository;
    @Mock
    private BestSlotBuilder bestSlotBuilder;

    @InjectMocks
    private MeetingService meetingService;

    @Test
    void shouldKeepVotesForRetainedDatesAndDeleteEmptyVotes() {
        Meeting meeting = createMeeting();
        MeetingUserSelection retained = createSelection(2L, Map.of(
                "2026-10-16", new int[]{9},
                "2026-10-17", new int[]{10}
        ));
        MeetingUserSelection removed = createSelection(3L, Map.of(
                "2026-10-16", new int[]{9}
        ));
        when(meetingRepository.findByMeetingCode("mtg_dates")).thenReturn(Optional.of(meeting));
        when(selectionRepository.findAllByMeetingId(10L)).thenReturn(List.of(retained, removed));

        meetingService.updateAvailableDates(
                "mtg_dates",
                1L,
                List.of("2026-10-17", "2026-10-18")
        );

        assertThat(meeting.getAvailableDates()).containsOnlyKeys("2026-10-17", "2026-10-18");
        assertThat(meeting.getAvailableDates().get("2026-10-18")).containsExactly(9, 10);
        assertThat(retained.getSelections()).containsOnlyKeys("2026-10-17");
        verify(selectionRepository).save(retained);
        verify(selectionRepository).delete(removed);
        verify(meetingRepository).save(meeting);
    }

    @Test
    void shouldRejectUpdateFromNonHost() {
        Meeting meeting = createMeeting();
        when(meetingRepository.findByMeetingCode("mtg_dates")).thenReturn(Optional.of(meeting));

        assertThatThrownBy(() -> meetingService.updateAvailableDates(
                "mtg_dates",
                999L,
                List.of("2026-10-18")
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getErrorCode())
                        .isEqualTo(ErrorCode.MEETING_HOST_ONLY));

        verify(meetingRepository, never()).save(meeting);
    }

    private Meeting createMeeting() {
        Meeting meeting = Meeting.create(
                "mtg_dates",
                "날짜 수정 테스트",
                null,
                1L,
                "Asia/Seoul",
                SelectionType.TIME,
                60,
                Map.of(
                        "2026-10-16", new int[]{9, 10},
                        "2026-10-17", new int[]{9, 10}
                )
        );
        ReflectionTestUtils.setField(meeting, "id", 10L);
        return meeting;
    }

    private MeetingUserSelection createSelection(Long userId, Map<String, int[]> selections) {
        return MeetingUserSelection.create(10L, userId, SelectionType.TIME, 60, selections);
    }
}
