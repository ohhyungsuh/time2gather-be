package com.cover.time2gather.domain.chat;

import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.meeting.SelectionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingQueryToolsTest {

    @Mock
    private MeetingQueryService meetingQueryService;

    private MeetingQueryTools meetingQueryTools;

    @BeforeEach
    void setUp() {
        meetingQueryTools = new MeetingQueryTools(meetingQueryService);
    }

    @Nested
    @DisplayName("getAllMeetings")
    class GetAllMeetings {

        @Test
        @DisplayName("사용자의 모든 미팅을 조회하여 요약 정보를 반환한다")
        void shouldReturnAllMeetingsSummary() {
            // Given
            Long userId = 1L;
            Meeting meeting1 = Meeting.create(
                "mtg_1", "팀 회식", "설명1", userId, "Asia/Seoul",
                SelectionType.TIME, 60, Map.of("2025-01-20", new int[]{18, 19})
            );
            Meeting meeting2 = Meeting.create(
                "mtg_2", "프로젝트 킥오프", "설명2", userId, "Asia/Seoul",
                SelectionType.ALL_DAY, 60, Map.of("2025-01-22", new int[]{})
            );

            when(meetingQueryService.findAllMeetingsByUser(userId))
                .thenReturn(List.of(meeting1, meeting2));

            // When
            String result = meetingQueryTools.getAllMeetings(userId);

            // Then
            assertThat(result).contains("팀 회식");
            assertThat(result).contains("프로젝트 킥오프");
            assertThat(result).contains("2025-01-20");
            assertThat(result).contains("2025-01-22");
        }

        @Test
        @DisplayName("미팅이 없으면 없다는 메시지를 반환한다")
        void shouldReturnEmptyMessageWhenNoMeetings() {
            // Given
            Long userId = 1L;
            when(meetingQueryService.findAllMeetingsByUser(userId))
                .thenReturn(List.of());

            // When
            String result = meetingQueryTools.getAllMeetings(userId);

            // Then
            assertThat(result).contains("미팅이 없습니다");
        }
    }

    @Nested
    @DisplayName("searchMeetingsByTitle")
    class SearchMeetingsByTitle {

        @Test
        @DisplayName("제목으로 미팅을 검색하여 결과를 반환한다")
        void shouldSearchMeetingsByTitle() {
            // Given
            Long userId = 1L;
            String keyword = "회식";
            Meeting meeting = Meeting.create(
                "mtg_1", "팀 회식", "맛있는 저녁", userId, "Asia/Seoul",
                SelectionType.TIME, 60, Map.of("2025-01-20", new int[]{18})
            );

            when(meetingQueryService.findMeetingsByTitle(userId, keyword))
                .thenReturn(List.of(meeting));

            // When
            String result = meetingQueryTools.searchMeetingsByTitle(userId, keyword);

            // Then
            assertThat(result).contains("팀 회식");
        }

        @Test
        @DisplayName("검색 결과가 없으면 없다는 메시지를 반환한다")
        void shouldReturnEmptyMessageWhenNoResults() {
            // Given
            Long userId = 1L;
            String keyword = "존재하지않는키워드";

            when(meetingQueryService.findMeetingsByTitle(userId, keyword))
                .thenReturn(List.of());

            // When
            String result = meetingQueryTools.searchMeetingsByTitle(userId, keyword);

            // Then
            assertThat(result).contains("검색 결과가 없습니다");
        }
    }

    @Nested
    @DisplayName("getMeetingCount")
    class GetMeetingCount {

        @Test
        @DisplayName("사용자의 미팅 개수를 반환한다")
        void shouldReturnMeetingCount() {
            // Given
            Long userId = 1L;
            Meeting meeting1 = Meeting.create(
                "mtg_1", "미팅1", null, userId, "Asia/Seoul",
                SelectionType.TIME, 60, Map.of("2025-01-20", new int[]{18})
            );
            Meeting meeting2 = Meeting.create(
                "mtg_2", "미팅2", null, userId, "Asia/Seoul",
                SelectionType.TIME, 60, Map.of("2025-01-21", new int[]{14})
            );

            when(meetingQueryService.findAllMeetingsByUser(userId))
                .thenReturn(List.of(meeting1, meeting2));

            // When
            String result = meetingQueryTools.getMeetingCount(userId);

            // Then
            assertThat(result).contains("2");
        }
    }
}
