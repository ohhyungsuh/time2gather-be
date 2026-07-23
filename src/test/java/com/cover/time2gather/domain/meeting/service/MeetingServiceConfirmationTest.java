package com.cover.time2gather.domain.meeting.service;

import com.cover.time2gather.domain.exception.BusinessException;
import com.cover.time2gather.domain.exception.ErrorCode;
import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.meeting.MeetingLocation;
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

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingServiceConfirmationTest {

    private static final LocalDate CONFIRMED_DATE = LocalDate.of(2024, 2, 15);
    private static final int CONFIRMED_SLOT = 9;

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
    void shouldConfirmScheduleAndLocationAtomically() {
        Meeting meeting = createMeeting(true);
        MeetingLocation location = createLocation(meeting.getId(), 12L);
        when(locationRepository.findById(12L)).thenReturn(Optional.of(location));

        meetingService.confirmMeeting(meeting, CONFIRMED_DATE, CONFIRMED_SLOT, 12L);

        assertThat(meeting.getConfirmedDate()).isEqualTo(CONFIRMED_DATE);
        assertThat(meeting.getConfirmedSlotIndex()).isEqualTo(CONFIRMED_SLOT);
        assertThat(meeting.getConfirmedLocationId()).isEqualTo(12L);
        verify(meetingRepository).save(meeting);
    }

    @Test
    void shouldRequireLocationBeforeMutatingSchedule() {
        Meeting meeting = createMeeting(true);

        assertThatThrownBy(() ->
                meetingService.confirmMeeting(meeting, CONFIRMED_DATE, CONFIRMED_SLOT, null)
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getErrorCode())
                        .isEqualTo(ErrorCode.LOCATION_ID_REQUIRED));

        assertThat(meeting.isConfirmed()).isFalse();
        assertThat(meeting.isLocationConfirmed()).isFalse();
        verify(meetingRepository, never()).save(meeting);
    }

    @Test
    void shouldRejectLocationFromAnotherMeetingBeforeMutatingSchedule() {
        Meeting meeting = createMeeting(true);
        MeetingLocation anotherMeetingLocation = createLocation(999L, 12L);
        when(locationRepository.findById(12L)).thenReturn(Optional.of(anotherMeetingLocation));

        assertThatThrownBy(() ->
                meetingService.confirmMeeting(meeting, CONFIRMED_DATE, CONFIRMED_SLOT, 12L)
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getErrorCode())
                        .isEqualTo(ErrorCode.LOCATION_NOT_BELONG_TO_MEETING));

        assertThat(meeting.isConfirmed()).isFalse();
        assertThat(meeting.isLocationConfirmed()).isFalse();
        verify(meetingRepository, never()).save(meeting);
    }

    @Test
    void shouldKeepDateOnlyConfirmationCompatibleWhenLocationVoteIsDisabled() {
        Meeting meeting = createMeeting(false);

        meetingService.confirmMeeting(meeting, CONFIRMED_DATE, CONFIRMED_SLOT);

        assertThat(meeting.getConfirmedDate()).isEqualTo(CONFIRMED_DATE);
        assertThat(meeting.isLocationConfirmed()).isFalse();
        verify(meetingRepository).save(meeting);
    }

    @Test
    void shouldCancelScheduleAndLocationTogether() {
        Meeting meeting = createMeeting(true);
        MeetingLocation location = createLocation(meeting.getId(), 12L);
        when(locationRepository.findById(12L)).thenReturn(Optional.of(location));
        meetingService.confirmMeeting(meeting, CONFIRMED_DATE, CONFIRMED_SLOT, 12L);

        meetingService.cancelConfirmation(meeting);

        assertThat(meeting.isConfirmed()).isFalse();
        assertThat(meeting.isLocationConfirmed()).isFalse();
        verify(meetingRepository, org.mockito.Mockito.times(2)).save(meeting);
    }

    private Meeting createMeeting(boolean locationVoteEnabled) {
        Meeting meeting = Meeting.create(
                "mtg_abc123",
                "테스트 미팅",
                "테스트 설명",
                1L,
                "Asia/Seoul",
                SelectionType.TIME,
                60,
                Map.of("2024-02-15", new int[]{9, 10, 11}),
                locationVoteEnabled
        );
        ReflectionTestUtils.setField(meeting, "id", 1L);
        return meeting;
    }

    private MeetingLocation createLocation(Long meetingId, Long locationId) {
        MeetingLocation location = MeetingLocation.create(meetingId, "회의실 A", 0);
        ReflectionTestUtils.setField(location, "id", locationId);
        return location;
    }
}
