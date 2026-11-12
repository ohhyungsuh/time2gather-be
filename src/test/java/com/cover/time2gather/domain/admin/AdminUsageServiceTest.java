package com.cover.time2gather.domain.admin;

import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.meeting.MeetingUserSelection;
import com.cover.time2gather.domain.meeting.SelectionType;
import com.cover.time2gather.infra.meeting.MeetingRepository;
import com.cover.time2gather.infra.meeting.MeetingUserSelectionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminUsageServiceTest {
    private final MeetingRepository meetings = mock(MeetingRepository.class);
    private final MeetingUserSelectionRepository selections = mock(MeetingUserSelectionRepository.class);
    private final AdminUsageService service = new AdminUsageService(meetings, selections);

    @Test
    void countsDistinctExternalResponsesIncludingUnavailableAndRepeatHosts() {
        Meeting first = meeting(1L, 10L, SelectionType.ALL_DAY);
        Meeting second = meeting(2L, 10L, SelectionType.ALL_DAY);
        Meeting third = meeting(3L, 20L, SelectionType.ALL_DAY);
        ReflectionTestUtils.setField(second, "confirmedDate", LocalDate.of(2026, 11, 10));
        ReflectionTestUtils.setField(third, "isActive", false);
        when(meetings.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of(first, second, third)));
        MeetingUserSelection none = MeetingUserSelection.unavailable(first, 12L, List.of("2026-11-10"));
        when(selections.findAllByMeetingIdIn(List.of(1L, 2L, 3L))).thenReturn(List.of(
                response(1L, 10L), response(1L, 11L), response(1L, 11L), none, response(2L, 11L)));

        AdminUsageStats result = service.getUsage(Set.of());

        assertThat(result.totalMeetings()).isEqualTo(3);
        assertThat(result.activeMeetings()).isEqualTo(2);
        assertThat(result.respondedMeetings()).isEqualTo(2);
        assertThat(result.activatedMeetings()).isEqualTo(1);
        assertThat(result.confirmedMeetings()).isEqualTo(1);
        assertThat(result.externalResponses()).isEqualTo(3);
        assertThat(result.hosts()).isEqualTo(2);
        assertThat(result.repeatHosts()).isEqualTo(1);
        assertThat(result.activationRate()).isEqualTo(1.0 / 3);
        assertThat(result.confirmationRate()).isEqualTo(1.0 / 3);
        assertThat(result.repeatHostRate()).isEqualTo(0.5);
        assertThat(result.activationResponseThreshold()).isEqualTo(2);
        assertThat(result.basis()).isEqualTo("CURRENT_STORED_STATE_ALL_TIME");
        assertThat(result.generatedAt()).isNotNull();
    }

    @Test
    void ignoresEmptyStaleAndWrongTypeResponses() {
        Meeting meeting = meeting(1L, 10L, SelectionType.TIME);
        MeetingUserSelection removedDate = MeetingUserSelection.create(1L, 11L, SelectionType.TIME, 60,
                Map.of("2026-11-09", new int[]{9}));
        MeetingUserSelection removedTime = MeetingUserSelection.create(1L, 12L, SelectionType.TIME, 60,
                Map.of("2026-11-10", new int[]{11}));
        MeetingUserSelection staleNone = MeetingUserSelection.unavailable(meeting, 13L, List.of("2026-11-09"));
        MeetingUserSelection valid = MeetingUserSelection.create(1L, 14L, SelectionType.TIME, 60,
                Map.of("2026-11-10", new int[]{9}));
        MeetingUserSelection empty = response(1L, 15L);
        ReflectionTestUtils.setField(empty, "selectionType", SelectionType.TIME);
        ReflectionTestUtils.setField(empty, "selections", Map.of());
        when(meetings.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of(meeting)));
        when(selections.findAllByMeetingIdIn(List.of(1L))).thenReturn(List.of(
                removedDate, removedTime, staleNone, valid, empty, response(1L, 16L)));

        AdminUsageStats result = service.getUsage(Set.of());

        assertThat(result.externalResponses()).isEqualTo(1);
        assertThat(result.respondedMeetings()).isEqualTo(1);
        assertThat(result.activatedMeetings()).isZero();
    }

    @Test
    void readsEveryPageAndExcludesKnownTestHostsBeforeReadingResponses() {
        when(meetings.findAll(any(PageRequest.class))).thenAnswer(invocation -> {
            PageRequest request = invocation.getArgument(0);
            return new PageImpl<>(List.of(meeting(request.getPageNumber() == 0 ? 1L : 2L,
                    request.getPageNumber() == 0 ? 99L : 10L, SelectionType.ALL_DAY)), request, 101);
        });
        when(selections.findAllByMeetingIdIn(List.of(2L))).thenReturn(List.of(response(2L, 11L), response(2L, 12L)));

        AdminUsageStats result = service.getUsage(Set.of(99L));

        assertThat(result.totalMeetings()).isEqualTo(1);
        assertThat(result.activatedMeetings()).isEqualTo(1);
        assertThat(result.activationRate()).isEqualTo(1);
        verify(selections, never()).findAllByMeetingIdIn(List.of(1L));
    }

    @Test
    void handlesEmptyDataWithoutNanAndRejectsInvalidExclusions() {
        when(meetings.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of()));
        AdminUsageStats result = service.getUsage(Set.of());
        assertThat(result.totalMeetings()).isZero();
        assertThat(result.activationRate()).isZero();
        assertThat(result.confirmationRate()).isZero();
        assertThat(result.repeatHostRate()).isZero();
        verifyNoInteractions(selections);
        assertThatThrownBy(() -> service.getUsage(Set.of(0L))).isInstanceOf(IllegalArgumentException.class);
    }

    private Meeting meeting(long id, long host, SelectionType type) {
        Meeting meeting = Meeting.create("private-code-" + id, "private title", null, host, "Asia/Seoul",
                type, 60, Map.of("2026-11-10", type == SelectionType.ALL_DAY ? new int[0] : new int[]{9, 10}));
        ReflectionTestUtils.setField(meeting, "id", id);
        return meeting;
    }

    private MeetingUserSelection response(long meetingId, long userId) {
        return MeetingUserSelection.create(meetingId, userId, SelectionType.ALL_DAY, 60,
                Map.of("2026-11-10", new int[0]));
    }
}
