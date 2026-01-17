package com.cover.time2gather.util;

import com.cover.time2gather.domain.meeting.*;
import com.cover.time2gather.domain.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReportInputTextBuilder - 장소 정보 포함 테스트")
class ReportInputTextBuilderLocationTest {

    @Test
    @DisplayName("장소 투표가 활성화된 미팅의 장소 정보가 요약 텍스트에 포함된다")
    void shouldIncludeLocationInfoWhenLocationVoteEnabled() {
        // given
        Meeting meeting = createMeeting(true);
        User host = createUser(1L, "host");
        User user1 = createUser(2L, "user1");
        User user2 = createUser(3L, "user2");
        Map<Long, User> userMap = Map.of(1L, host, 2L, user1, 3L, user2);

        List<MeetingUserSelection> selections = Collections.emptyList();

        MeetingLocation location1 = createLocation(1L, 1L, "강남역 스타벅스", 0);
        MeetingLocation location2 = createLocation(2L, 1L, "홍대입구역 카페", 1);
        List<MeetingLocation> locations = List.of(location1, location2);

        // location1에 2명 투표, location2에 1명 투표
        MeetingLocationSelection sel1 = createLocationSelection(1L, 1L, 2L); // user1 -> location1
        MeetingLocationSelection sel2 = createLocationSelection(1L, 1L, 3L); // user2 -> location1
        MeetingLocationSelection sel3 = createLocationSelection(1L, 2L, 2L); // user1 -> location2
        List<MeetingLocationSelection> locationSelections = List.of(sel1, sel2, sel3);

        // when
        String result = ReportInputTextBuilder.build(meeting, selections, userMap, locations, locationSelections);

        // then
        assertThat(result).contains("장소 투표");
        assertThat(result).contains("강남역 스타벅스");
        assertThat(result).contains("홍대입구역 카페");
        assertThat(result).contains("2명"); // location1 투표 수
    }

    @Test
    @DisplayName("장소 투표가 비활성화된 미팅에서는 장소 정보가 포함되지 않는다")
    void shouldNotIncludeLocationInfoWhenLocationVoteDisabled() {
        // given
        Meeting meeting = createMeeting(false);
        User host = createUser(1L, "host");
        Map<Long, User> userMap = Map.of(1L, host);
        List<MeetingUserSelection> selections = Collections.emptyList();
        List<MeetingLocation> locations = Collections.emptyList();
        List<MeetingLocationSelection> locationSelections = Collections.emptyList();

        // when
        String result = ReportInputTextBuilder.build(meeting, selections, userMap, locations, locationSelections);

        // then
        assertThat(result).doesNotContain("장소 투표");
    }

    @Test
    @DisplayName("확정된 장소가 있을 때 확정 정보가 포함된다")
    void shouldIncludeConfirmedLocationInfo() {
        // given
        Meeting meeting = createMeeting(true);
        ReflectionTestUtils.setField(meeting, "confirmedLocationId", 1L);

        User host = createUser(1L, "host");
        Map<Long, User> userMap = Map.of(1L, host);
        List<MeetingUserSelection> selections = Collections.emptyList();

        MeetingLocation location1 = createLocation(1L, 1L, "강남역 스타벅스", 0);
        List<MeetingLocation> locations = List.of(location1);
        List<MeetingLocationSelection> locationSelections = Collections.emptyList();

        // when
        String result = ReportInputTextBuilder.build(meeting, selections, userMap, locations, locationSelections);

        // then
        assertThat(result).contains("확정된 장소");
        assertThat(result).contains("강남역 스타벅스");
    }

    @Test
    @DisplayName("장소별 투표 현황이 정확하게 집계된다")
    void shouldAggregateLocationVotesCorrectly() {
        // given
        Meeting meeting = createMeeting(true);
        User host = createUser(1L, "host");
        User user1 = createUser(2L, "user1");
        User user2 = createUser(3L, "user2");
        User user3 = createUser(4L, "user3");
        Map<Long, User> userMap = Map.of(1L, host, 2L, user1, 3L, user2, 4L, user3);
        List<MeetingUserSelection> selections = Collections.emptyList();

        MeetingLocation location1 = createLocation(1L, 1L, "장소A", 0);
        MeetingLocation location2 = createLocation(2L, 1L, "장소B", 1);
        List<MeetingLocation> locations = List.of(location1, location2);

        // location1: user1, user2, user3 (3명)
        // location2: user1 (1명)
        MeetingLocationSelection sel1 = createLocationSelection(1L, 1L, 2L);
        MeetingLocationSelection sel2 = createLocationSelection(1L, 1L, 3L);
        MeetingLocationSelection sel3 = createLocationSelection(1L, 1L, 4L);
        MeetingLocationSelection sel4 = createLocationSelection(1L, 2L, 2L);
        List<MeetingLocationSelection> locationSelections = List.of(sel1, sel2, sel3, sel4);

        // when
        String result = ReportInputTextBuilder.build(meeting, selections, userMap, locations, locationSelections);

        // then
        assertThat(result).contains("장소A");
        assertThat(result).contains("3명");
        assertThat(result).contains("장소B");
        assertThat(result).contains("1명");
    }

    @Test
    @DisplayName("시간이 확정된 미팅에서는 확정된 시간 정보가 표시된다")
    void shouldIncludeConfirmedTimeInfo() {
        // given
        Meeting meeting = createMeeting(false);
        ReflectionTestUtils.setField(meeting, "confirmedDate", LocalDate.of(2025, 1, 20));
        ReflectionTestUtils.setField(meeting, "confirmedSlotIndex", 36); // 18:00 (36 * 30분 = 1080분 = 18시간)

        User host = createUser(1L, "host");
        Map<Long, User> userMap = Map.of(1L, host);
        List<MeetingUserSelection> selections = Collections.emptyList();

        // when
        String result = ReportInputTextBuilder.build(meeting, selections, userMap);

        // then
        assertThat(result).contains("확정된 시간");
        assertThat(result).contains("2025-01-20");
        assertThat(result).contains("18:00");
    }

    @Test
    @DisplayName("ALL_DAY 타입에서 확정된 날짜가 표시된다")
    void shouldIncludeConfirmedDateForAllDayType() {
        // given
        Map<String, int[]> availableDates = Map.of(
                "2025-01-20", new int[]{},
                "2025-01-21", new int[]{}
        );
        Meeting meeting = Meeting.create(
                "TEST002",
                "All Day Meeting",
                "Test description",
                1L,
                "Asia/Seoul",
                SelectionType.ALL_DAY,
                60,
                availableDates,
                false
        );
        ReflectionTestUtils.setField(meeting, "id", 2L);
        ReflectionTestUtils.setField(meeting, "confirmedDate", LocalDate.of(2025, 1, 20));

        User host = createUser(1L, "host");
        Map<Long, User> userMap = Map.of(1L, host);
        List<MeetingUserSelection> selections = Collections.emptyList();

        // when
        String result = ReportInputTextBuilder.build(meeting, selections, userMap);

        // then
        assertThat(result).contains("확정된 날짜");
        assertThat(result).contains("2025-01-20");
    }

    private Meeting createMeeting(boolean locationVoteEnabled) {
        Map<String, int[]> availableDates = Map.of(
                "2025-01-20", new int[]{18, 19, 20},
                "2025-01-21", new int[]{18, 19, 20}
        );
        Meeting meeting = Meeting.create(
                "TEST001",
                "Test Meeting",
                "Test description",
                1L,
                "Asia/Seoul",
                SelectionType.TIME,
                60,
                availableDates,
                locationVoteEnabled
        );
        ReflectionTestUtils.setField(meeting, "id", 1L);
        return meeting;
    }

    private User createUser(Long id, String username) {
        User user = User.builder()
                .username(username)
                .provider(User.AuthProvider.ANONYMOUS)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private MeetingLocation createLocation(Long id, Long meetingId, String name, Integer displayOrder) {
        MeetingLocation location = MeetingLocation.create(meetingId, name, displayOrder);
        ReflectionTestUtils.setField(location, "id", id);
        return location;
    }

    private MeetingLocationSelection createLocationSelection(Long meetingId, Long locationId, Long userId) {
        return MeetingLocationSelection.create(meetingId, locationId, userId);
    }
}
