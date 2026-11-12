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

    @Test
    void timeChangesKeepOnlyVotesForRetainedCandidates() {
        Meeting meeting = createMeeting();
        MeetingUserSelection retained = createSelection(2L, Map.of("2026-10-16", new int[]{9, 10}));
        MeetingUserSelection removed = createSelection(3L, Map.of("2026-10-17", new int[]{9}));
        when(meetingRepository.findByMeetingCode("mtg_dates")).thenReturn(Optional.of(meeting));
        when(selectionRepository.findAllByMeetingId(10L)).thenReturn(List.of(retained, removed));

        meetingService.updateAvailableDates("mtg_dates", 1L,
                List.of("2026-10-16", "2026-10-17", "2026-10-18"), List.of("10:00", "11:00"));

        meeting.getAvailableDates().values().forEach(slots -> assertThat(slots).containsExactly(10, 11));
        assertThat(retained.getSelections().get("2026-10-16")).containsExactly(10);
        verify(selectionRepository).save(retained);
        verify(selectionRepository).delete(removed);
    }

    @Test
    void expandingTimesRequiresUnavailableParticipantsToRespondAgain() {
        Meeting meeting = createMeeting();
        MeetingUserSelection unavailable = MeetingUserSelection.unavailable(meeting, 2L,
                List.of("2026-10-16", "2026-10-17"));
        when(meetingRepository.findByMeetingCode("mtg_dates")).thenReturn(Optional.of(meeting));
        when(selectionRepository.findAllByMeetingId(10L)).thenReturn(List.of(unavailable));

        meetingService.updateAvailableDates("mtg_dates", 1L,
                List.of("2026-10-16", "2026-10-17"), List.of("09:00", "10:00", "11:00"));

        assertThat(unavailable.availabilityResponse(meeting)).isEqualTo("NEEDS_RESPONSE");
        verify(selectionRepository).save(unavailable);
    }

    @Test
    void narrowingTimesPreservesUnavailableResponse() {
        Meeting meeting = createMeeting();
        MeetingUserSelection unavailable = MeetingUserSelection.unavailable(meeting, 2L,
                List.of("2026-10-16", "2026-10-17"));
        when(meetingRepository.findByMeetingCode("mtg_dates")).thenReturn(Optional.of(meeting));
        when(selectionRepository.findAllByMeetingId(10L)).thenReturn(List.of(unavailable));
        meetingService.updateAvailableDates("mtg_dates", 1L,
                List.of("2026-10-16"), List.of("10:00"));
        assertThat(unavailable.availabilityResponse(meeting)).isEqualTo("NONE");
    }

    @Test
    void rejectsInvalidTimesWithoutChangingCandidates() {
        for (List<String> times : List.of(List.<String>of(), List.of("24:00"), List.of("09:30"), List.of("invalid"))) {
            Meeting meeting = createMeeting();
            assertThatThrownBy(() -> meeting.updateAvailableDates(List.of("2026-10-16"), times))
                    .isInstanceOf(BusinessException.class);
            assertThat(meeting.getAvailableDates()).containsOnlyKeys("2026-10-16", "2026-10-17");
            assertThat(meeting.getAvailableDates().get("2026-10-16")).containsExactly(9, 10);
        }
    }

    @Test
    void rejectsTimeChangesForConfirmedOrDateOnlyMeetings() {
        Meeting meeting = createMeeting();
        meeting.confirm(java.time.LocalDate.parse("2026-10-16"), 9);
        assertThatThrownBy(() -> meeting.updateAvailableDates(List.of("2026-10-16"), List.of("10:00")))
                .isInstanceOf(BusinessException.class);
        Meeting allDay = Meeting.create("day", "Day", null, 1L, "Asia/Seoul", SelectionType.ALL_DAY,
                60, Map.of("2026-10-16", new int[0]));
        assertThatThrownBy(() -> allDay.updateAvailableDates(List.of("2026-10-16"), List.of("10:00")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void preservesIrregularLegacyTimesWhenTimesAreOmitted() {
        Meeting meeting = Meeting.create("half", "Half hour", null, 1L, "Asia/Seoul", SelectionType.TIME,
                30, Map.of("2026-10-16", new int[]{19, 21}, "2026-10-17", new int[]{22}));
        meeting.updateAvailableDates(List.of("2026-10-16", "2026-10-17"));
        assertThat(meeting.getAvailableDates().get("2026-10-16")).containsExactly(19, 21);
        assertThat(meeting.getAvailableDates().get("2026-10-17")).containsExactly(22);
        meeting.updateAvailableDates(List.of("2026-10-16"), List.of("09:30", "10:00"));
        assertThat(meeting.getAvailableDates().get("2026-10-16")).containsExactly(19, 20);
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
