package com.cover.time2gather.integration;

import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.meeting.SelectionType;
import com.cover.time2gather.infra.meeting.MeetingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest


class AdminConfirmedMeetingsIntegrationTest extends com.cover.time2gather.support.FirestoreIntegrationTest {
    @Autowired MeetingRepository meetings;
    private long nextId = 100;

    @Test
    void filtersBeforeCountingAndPaginationAndExcludesCancelledConfirmation() {
        save("mtg_admin_filter_unconfirmed", "Dinner", false, true);
        save("mtg_admin_filter_first", "Dinner", true, true);
        save("mtg_admin_filter_second", "Dinner", true, false);
        var cancelled = save("mtg_admin_filter_cancelled", "Dinner", true, true);
        cancelled.cancelConfirmation();
        meetings.save(cancelled);
        save("mtg_admin_filter_other", "Lunch", true, true);
        var sort = Sort.by("createdAt").descending().and(Sort.by("id").descending());
        var first = meetings.findConfirmed("dInNeR", PageRequest.of(0, 1, sort));
        var second = meetings.findConfirmed("dInNeR", PageRequest.of(1, 1, sort));
        assertThat(first.getTotalElements()).isEqualTo(2);
        assertThat(first.getTotalPages()).isEqualTo(2);
        assertThat(first.getContent()).extracting(Meeting::getMeetingCode).containsExactly("mtg_admin_filter_second");
        assertThat(second.getContent()).extracting(Meeting::getMeetingCode).containsExactly("mtg_admin_filter_first");
        assertThat(meetings.findConfirmed("ADMIN_FILTER_OTHER", PageRequest.of(0, 20)).getTotalElements()).isEqualTo(1);
        assertThat(meetings.findConfirmed("", PageRequest.of(0, 20)).getTotalElements()).isEqualTo(3);
        assertThat(meetings.findConfirmed("no result", PageRequest.of(0, 20)).getContent()).isEmpty();
    }

    private Meeting save(String code, String title, boolean confirmed, boolean active) {
        var meeting = Meeting.create(code, title, null, 1L, "Asia/Seoul", SelectionType.ALL_DAY,
                60, Map.of("2026-11-15", new int[0]));
        ReflectionTestUtils.setField(meeting, "id", nextId++);
        if (confirmed) ReflectionTestUtils.setField(meeting, "confirmedDate", LocalDate.of(2026, 11, 15));
        ReflectionTestUtils.setField(meeting, "isActive", active);
        // Equal timestamps also exercise the stable ID tie-break.
        ReflectionTestUtils.setField(meeting, "createdAt", LocalDateTime.of(2026, 11, 10, 12, 0));
        return meetings.save(meeting);
    }
}
