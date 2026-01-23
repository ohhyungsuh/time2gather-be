// package com.cover.time2gather.domain.chat;
//
// import com.cover.time2gather.domain.meeting.Meeting;
// import com.cover.time2gather.domain.meeting.SelectionType;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Nested;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;
//
// import java.time.LocalDate;
// import java.util.List;
// import java.util.Map;
//
// import static org.assertj.core.api.Assertions.assertThat;
// import static org.mockito.Mockito.when;
//
// @ExtendWith(MockitoExtension.class)
// class MeetingQueryToolsTest {
//
//     @Mock
//     private MeetingQueryService meetingQueryService;
//
//     private MeetingQueryTools meetingQueryTools;
//
//     @BeforeEach
//     void setUp() {
//         meetingQueryTools = new MeetingQueryTools(meetingQueryService);
//     }
//
//     @Nested
//     @DisplayName("getConfirmedMeetings")
//     class GetConfirmedMeetings {
//
//         @Test
//         @DisplayName("확정된 미팅만 조회하여 반환한다")
//         void shouldReturnOnlyConfirmedMeetings() {
//             // Given
//             Long userId = 1L;
//             Meeting confirmedMeeting = Meeting.create(
//                 "mtg_1", "팀 회식", "설명1", userId, "Asia/Seoul",
//                 SelectionType.TIME, 60, Map.of("2025-01-20", new int[]{18, 19})
//             );
//             confirmedMeeting.confirm(LocalDate.of(2025, 1, 20), 18);
//
//             Meeting unconfirmedMeeting = Meeting.create(
//                 "mtg_2", "프로젝트 킥오프", "설명2", userId, "Asia/Seoul",
//                 SelectionType.ALL_DAY, 60, Map.of("2025-01-22", new int[]{})
//             );
//
//             when(meetingQueryService.findAllMeetingsByUser(userId))
//                 .thenReturn(List.of(confirmedMeeting, unconfirmedMeeting));
//
//             // When
//             String result = meetingQueryTools.getConfirmedMeetings(userId);
//
//             // Then
//             assertThat(result).contains("팀 회식");
//             assertThat(result).contains("확정된 미팅");
//             assertThat(result).doesNotContain("프로젝트 킥오프");
//         }
//
//         @Test
//         @DisplayName("확정된 미팅이 없으면 없다는 메시지를 반환한다")
//         void shouldReturnEmptyMessageWhenNoConfirmedMeetings() {
//             // Given
//             Long userId = 1L;
//             Meeting unconfirmedMeeting = Meeting.create(
//                 "mtg_1", "미팅1", null, userId, "Asia/Seoul",
//                 SelectionType.TIME, 60, Map.of("2025-01-20", new int[]{18})
//             );
//
//             when(meetingQueryService.findAllMeetingsByUser(userId))
//                 .thenReturn(List.of(unconfirmedMeeting));
//
//             // When
//             String result = meetingQueryTools.getConfirmedMeetings(userId);
//
//             // Then
//             assertThat(result).contains("확정된 미팅이 없습니다");
//         }
//     }
//
//     @Nested
//     @DisplayName("searchConfirmedMeetingsByTitle")
//     class SearchConfirmedMeetingsByTitle {
//
//         @Test
//         @DisplayName("제목으로 확정된 미팅을 검색하여 결과를 반환한다")
//         void shouldSearchConfirmedMeetingsByTitle() {
//             // Given
//             Long userId = 1L;
//             String keyword = "회식";
//             Meeting confirmedMeeting = Meeting.create(
//                 "mtg_1", "팀 회식", "맛있는 저녁", userId, "Asia/Seoul",
//                 SelectionType.TIME, 60, Map.of("2025-01-20", new int[]{18})
//             );
//             confirmedMeeting.confirm(LocalDate.of(2025, 1, 20), 18);
//
//             when(meetingQueryService.findMeetingsByTitle(userId, keyword))
//                 .thenReturn(List.of(confirmedMeeting));
//
//             // When
//             String result = meetingQueryTools.searchConfirmedMeetingsByTitle(userId, keyword);
//
//             // Then
//             assertThat(result).contains("팀 회식");
//             assertThat(result).contains("2025-01-20");
//         }
//
//         @Test
//         @DisplayName("확정된 검색 결과가 없으면 없다는 메시지를 반환한다")
//         void shouldReturnEmptyMessageWhenNoConfirmedResults() {
//             // Given
//             Long userId = 1L;
//             String keyword = "회식";
//             Meeting unconfirmedMeeting = Meeting.create(
//                 "mtg_1", "팀 회식", "맛있는 저녁", userId, "Asia/Seoul",
//                 SelectionType.TIME, 60, Map.of("2025-01-20", new int[]{18})
//             );
//
//             when(meetingQueryService.findMeetingsByTitle(userId, keyword))
//                 .thenReturn(List.of(unconfirmedMeeting));
//
//             // When
//             String result = meetingQueryTools.searchConfirmedMeetingsByTitle(userId, keyword);
//
//             // Then
//             assertThat(result).contains("확정된 미팅이 없습니다");
//         }
//     }
//
//     @Nested
//     @DisplayName("getConfirmedMeetingDetail")
//     class GetConfirmedMeetingDetail {
//
//         @Test
//         @DisplayName("확정된 미팅의 상세 정보를 반환한다")
//         void shouldReturnConfirmedMeetingDetail() {
//             // Given
//             Long userId = 1L;
//             String meetingCode = "mtg_1";
//             Meeting confirmedMeeting = Meeting.create(
//                 meetingCode, "팀 회식", "맛있는 저녁", userId, "Asia/Seoul",
//                 SelectionType.TIME, 60, Map.of("2025-01-20", new int[]{18})
//             );
//             confirmedMeeting.confirm(LocalDate.of(2025, 1, 20), 18);
//
//             when(meetingQueryService.findMeetingByCode(userId, meetingCode))
//                 .thenReturn(confirmedMeeting);
//
//             // When
//             String result = meetingQueryTools.getConfirmedMeetingDetail(userId, meetingCode);
//
//             // Then
//             assertThat(result).contains("팀 회식");
//             assertThat(result).contains("확정된 일정");
//             assertThat(result).contains("2025-01-20");
//         }
//
//         @Test
//         @DisplayName("확정되지 않은 미팅은 확정되지 않았다는 메시지를 반환한다")
//         void shouldReturnNotConfirmedMessageForUnconfirmedMeeting() {
//             // Given
//             Long userId = 1L;
//             String meetingCode = "mtg_1";
//             Meeting unconfirmedMeeting = Meeting.create(
//                 meetingCode, "팀 회식", "맛있는 저녁", userId, "Asia/Seoul",
//                 SelectionType.TIME, 60, Map.of("2025-01-20", new int[]{18})
//             );
//
//             when(meetingQueryService.findMeetingByCode(userId, meetingCode))
//                 .thenReturn(unconfirmedMeeting);
//
//             // When
//             String result = meetingQueryTools.getConfirmedMeetingDetail(userId, meetingCode);
//
//             // Then
//             assertThat(result).contains("확정되지 않았습니다");
//         }
//     }
//
//     @Nested
//     @DisplayName("getConfirmedMeetingStats")
//     class GetConfirmedMeetingStats {
//
//         @Test
//         @DisplayName("확정된 미팅 통계를 반환한다")
//         void shouldReturnConfirmedMeetingStats() {
//             // Given
//             Long userId = 1L;
//             Meeting confirmedMeeting = Meeting.create(
//                 "mtg_1", "미팅1", null, userId, "Asia/Seoul",
//                 SelectionType.TIME, 60, Map.of("2025-01-20", new int[]{18})
//             );
//             confirmedMeeting.confirm(LocalDate.now().plusDays(1), 18);
//
//             Meeting pastConfirmedMeeting = Meeting.create(
//                 "mtg_2", "미팅2", null, userId, "Asia/Seoul",
//                 SelectionType.TIME, 60, Map.of("2024-01-20", new int[]{14})
//             );
//             pastConfirmedMeeting.confirm(LocalDate.of(2024, 1, 20), 14);
//
//             when(meetingQueryService.findAllMeetingsByUser(userId))
//                 .thenReturn(List.of(confirmedMeeting, pastConfirmedMeeting));
//
//             // When
//             String result = meetingQueryTools.getConfirmedMeetingStats(userId);
//
//             // Then
//             assertThat(result).contains("확정된 미팅 통계");
//             assertThat(result).contains("전체 확정된 미팅: 2개");
//             assertThat(result).contains("다가오는 일정: 1개");
//             assertThat(result).contains("지난 일정: 1개");
//         }
//     }
// }
