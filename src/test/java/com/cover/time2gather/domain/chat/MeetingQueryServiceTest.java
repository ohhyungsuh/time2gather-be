// package com.cover.time2gather.domain.chat;
//
// import com.cover.time2gather.domain.meeting.Meeting;
// import com.cover.time2gather.domain.meeting.MeetingUserSelection;
// import com.cover.time2gather.domain.meeting.SelectionType;
// import com.cover.time2gather.domain.user.User;
// import com.cover.time2gather.infra.meeting.MeetingRepository;
// import com.cover.time2gather.infra.meeting.MeetingUserSelectionRepository;
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
// import static org.mockito.Mockito.mock;
// import static org.mockito.Mockito.when;
//
// @ExtendWith(MockitoExtension.class)
// class MeetingQueryServiceTest {
//
//     @Mock
//     private MeetingRepository meetingRepository;
//
//     @Mock
//     private MeetingUserSelectionRepository meetingUserSelectionRepository;
//
//     private MeetingQueryService meetingQueryService;
//
//     private User testUser;
//     private Meeting hostedMeeting;
//     private Meeting participatedMeeting;
//
//     @BeforeEach
//     void setUp() {
//         meetingQueryService = new MeetingQueryService(meetingRepository, meetingUserSelectionRepository);
//
//         testUser = User.builder()
//             .username("testUser")
//             .provider(User.AuthProvider.KAKAO)
//             .providerId("kakao_123")
//             .build();
//
//         hostedMeeting = Meeting.create(
//             "mtg_hosted",
//             "내가 만든 미팅",
//             "설명",
//             1L,
//             "Asia/Seoul",
//             SelectionType.TIME,
//             60,
//             Map.of("2025-01-20", new int[]{18, 19, 20})
//         );
//
//         participatedMeeting = Meeting.create(
//             "mtg_participated",
//             "참여한 미팅",
//             "설명",
//             2L,
//             "Asia/Seoul",
//             SelectionType.TIME,
//             60,
//             Map.of("2025-01-21", new int[]{14, 15, 16})
//         );
//     }
//
//     @Nested
//     @DisplayName("findAllMeetingsByUser")
//     class FindAllMeetingsByUser {
//
//         @Test
//         @DisplayName("호스트로 만든 미팅과 참여한 미팅 모두 조회한다")
//         void shouldReturnBothHostedAndParticipatedMeetings() {
//             // Given
//             Long userId = 1L;
//
//             when(meetingRepository.findByHostUserIdAndIsActiveTrue(userId))
//                 .thenReturn(List.of(hostedMeeting));
//
//             MeetingUserSelection selection = mock(MeetingUserSelection.class);
//             when(selection.getMeetingId()).thenReturn(2L);
//             when(meetingUserSelectionRepository.findAllByUserId(userId))
//                 .thenReturn(List.of(selection));
//
//             when(meetingRepository.findAllByIdInAndIsActiveTrue(List.of(2L)))
//                 .thenReturn(List.of(participatedMeeting));
//
//             // When
//             List<Meeting> result = meetingQueryService.findAllMeetingsByUser(userId);
//
//             // Then
//             assertThat(result).hasSize(2);
//             assertThat(result).extracting(Meeting::getTitle)
//                 .containsExactlyInAnyOrder("내가 만든 미팅", "참여한 미팅");
//         }
//
//         @Test
//         @DisplayName("호스트이자 참여자인 미팅은 중복 없이 반환한다")
//         void shouldNotDuplicateWhenBothHostAndParticipant() {
//             // Given
//             Long userId = 1L;
//             Long meetingId = 100L;  // 가상의 meeting ID
//
//             when(meetingRepository.findByHostUserIdAndIsActiveTrue(userId))
//                 .thenReturn(List.of(hostedMeeting));
//
//             // 자신이 만든 미팅에도 참여한 경우 (같은 미팅이 반환됨)
//             MeetingUserSelection selection = mock(MeetingUserSelection.class);
//             when(selection.getMeetingId()).thenReturn(meetingId);
//             when(meetingUserSelectionRepository.findAllByUserId(userId))
//                 .thenReturn(List.of(selection));
//
//             // 같은 hostedMeeting 객체를 반환하면 Set에서 중복 제거됨
//             when(meetingRepository.findAllByIdInAndIsActiveTrue(List.of(meetingId)))
//                 .thenReturn(List.of(hostedMeeting));
//
//             // When
//             List<Meeting> result = meetingQueryService.findAllMeetingsByUser(userId);
//
//             // Then
//             assertThat(result).hasSize(1);
//         }
//
//         @Test
//         @DisplayName("미팅이 없으면 빈 리스트를 반환한다")
//         void shouldReturnEmptyListWhenNoMeetings() {
//             // Given
//             Long userId = 1L;
//
//             when(meetingRepository.findByHostUserIdAndIsActiveTrue(userId))
//                 .thenReturn(List.of());
//
//             when(meetingUserSelectionRepository.findAllByUserId(userId))
//                 .thenReturn(List.of());
//
//             // When
//             List<Meeting> result = meetingQueryService.findAllMeetingsByUser(userId);
//
//             // Then
//             assertThat(result).isEmpty();
//         }
//     }
//
//     @Nested
//     @DisplayName("findMeetingsByTitle")
//     class FindMeetingsByTitle {
//
//         @Test
//         @DisplayName("제목으로 미팅을 검색한다")
//         void shouldFindMeetingsByTitle() {
//             // Given
//             Long userId = 1L;
//             String keyword = "회식";
//
//             Meeting meeting1 = Meeting.create(
//                 "mtg_1", "팀 회식", null, userId, "Asia/Seoul",
//                 SelectionType.TIME, 60, Map.of("2025-01-20", new int[]{18})
//             );
//             Meeting meeting2 = Meeting.create(
//                 "mtg_2", "프로젝트 미팅", null, userId, "Asia/Seoul",
//                 SelectionType.TIME, 60, Map.of("2025-01-21", new int[]{14})
//             );
//
//             when(meetingRepository.findByHostUserIdAndIsActiveTrue(userId))
//                 .thenReturn(List.of(meeting1, meeting2));
//             when(meetingUserSelectionRepository.findAllByUserId(userId))
//                 .thenReturn(List.of());
//
//             // When
//             List<Meeting> result = meetingQueryService.findMeetingsByTitle(userId, keyword);
//
//             // Then
//             assertThat(result).hasSize(1);
//             assertThat(result.get(0).getTitle()).isEqualTo("팀 회식");
//         }
//
//         @Test
//         @DisplayName("대소문자 구분 없이 검색한다")
//         void shouldSearchCaseInsensitive() {
//             // Given
//             Long userId = 1L;
//
//             Meeting meeting = Meeting.create(
//                 "mtg_1", "Project Meeting", null, userId, "Asia/Seoul",
//                 SelectionType.TIME, 60, Map.of("2025-01-20", new int[]{18})
//             );
//
//             when(meetingRepository.findByHostUserIdAndIsActiveTrue(userId))
//                 .thenReturn(List.of(meeting));
//             when(meetingUserSelectionRepository.findAllByUserId(userId))
//                 .thenReturn(List.of());
//
//             // When
//             List<Meeting> result = meetingQueryService.findMeetingsByTitle(userId, "project");
//
//             // Then
//             assertThat(result).hasSize(1);
//         }
//     }
//
//     @Nested
//     @DisplayName("findUpcomingMeetings")
//     class FindUpcomingMeetings {
//
//         @Test
//         @DisplayName("오늘 이후 날짜가 포함된 미팅만 반환한다")
//         void shouldReturnOnlyUpcomingMeetings() {
//             // Given
//             Long userId = 1L;
//             LocalDate today = LocalDate.now();
//             String futureDate = today.plusDays(7).toString();
//             String pastDate = today.minusDays(7).toString();
//
//             Meeting upcomingMeeting = Meeting.create(
//                 "mtg_upcoming", "다가오는 미팅", null, userId, "Asia/Seoul",
//                 SelectionType.TIME, 60, Map.of(futureDate, new int[]{18, 19})
//             );
//             Meeting pastMeeting = Meeting.create(
//                 "mtg_past", "지난 미팅", null, userId, "Asia/Seoul",
//                 SelectionType.TIME, 60, Map.of(pastDate, new int[]{18, 19})
//             );
//
//             when(meetingRepository.findByHostUserIdAndIsActiveTrue(userId))
//                 .thenReturn(List.of(upcomingMeeting, pastMeeting));
//             when(meetingUserSelectionRepository.findAllByUserId(userId))
//                 .thenReturn(List.of());
//
//             // When
//             List<Meeting> result = meetingQueryService.findUpcomingMeetings(userId);
//
//             // Then
//             assertThat(result).hasSize(1);
//             assertThat(result.get(0).getTitle()).isEqualTo("다가오는 미팅");
//         }
//
//         @Test
//         @DisplayName("오늘 날짜도 다가오는 미팅에 포함된다")
//         void shouldIncludeTodayAsUpcoming() {
//             // Given
//             Long userId = 1L;
//             String today = LocalDate.now().toString();
//
//             Meeting todayMeeting = Meeting.create(
//                 "mtg_today", "오늘 미팅", null, userId, "Asia/Seoul",
//                 SelectionType.TIME, 60, Map.of(today, new int[]{18})
//             );
//
//             when(meetingRepository.findByHostUserIdAndIsActiveTrue(userId))
//                 .thenReturn(List.of(todayMeeting));
//             when(meetingUserSelectionRepository.findAllByUserId(userId))
//                 .thenReturn(List.of());
//
//             // When
//             List<Meeting> result = meetingQueryService.findUpcomingMeetings(userId);
//
//             // Then
//             assertThat(result).hasSize(1);
//         }
//     }
//
//     @Nested
//     @DisplayName("findPastMeetings")
//     class FindPastMeetings {
//
//         @Test
//         @DisplayName("모든 날짜가 오늘 이전인 미팅만 반환한다")
//         void shouldReturnOnlyPastMeetings() {
//             // Given
//             Long userId = 1L;
//             LocalDate today = LocalDate.now();
//             String futureDate = today.plusDays(7).toString();
//             String pastDate = today.minusDays(7).toString();
//
//             Meeting upcomingMeeting = Meeting.create(
//                 "mtg_upcoming", "다가오는 미팅", null, userId, "Asia/Seoul",
//                 SelectionType.TIME, 60, Map.of(futureDate, new int[]{18, 19})
//             );
//             Meeting pastMeeting = Meeting.create(
//                 "mtg_past", "지난 미팅", null, userId, "Asia/Seoul",
//                 SelectionType.TIME, 60, Map.of(pastDate, new int[]{18, 19})
//             );
//
//             when(meetingRepository.findByHostUserIdAndIsActiveTrue(userId))
//                 .thenReturn(List.of(upcomingMeeting, pastMeeting));
//             when(meetingUserSelectionRepository.findAllByUserId(userId))
//                 .thenReturn(List.of());
//
//             // When
//             List<Meeting> result = meetingQueryService.findPastMeetings(userId);
//
//             // Then
//             assertThat(result).hasSize(1);
//             assertThat(result.get(0).getTitle()).isEqualTo("지난 미팅");
//         }
//     }
//
//     @Nested
//     @DisplayName("findMeetingByCode")
//     class FindMeetingByCode {
//
//         @Test
//         @DisplayName("미팅 코드로 사용자의 미팅을 조회한다")
//         void shouldFindMeetingByCode() {
//             // Given
//             Long userId = 1L;
//             String meetingCode = "mtg_hosted";
//
//             when(meetingRepository.findByHostUserIdAndIsActiveTrue(userId))
//                 .thenReturn(List.of(hostedMeeting));
//             when(meetingUserSelectionRepository.findAllByUserId(userId))
//                 .thenReturn(List.of());
//
//             // When
//             Meeting result = meetingQueryService.findMeetingByCode(userId, meetingCode);
//
//             // Then
//             assertThat(result).isNotNull();
//             assertThat(result.getMeetingCode()).isEqualTo(meetingCode);
//         }
//
//         @Test
//         @DisplayName("존재하지 않는 미팅 코드는 null을 반환한다")
//         void shouldReturnNullForNonExistentCode() {
//             // Given
//             Long userId = 1L;
//             String meetingCode = "non_existent";
//
//             when(meetingRepository.findByHostUserIdAndIsActiveTrue(userId))
//                 .thenReturn(List.of(hostedMeeting));
//             when(meetingUserSelectionRepository.findAllByUserId(userId))
//                 .thenReturn(List.of());
//
//             // When
//             Meeting result = meetingQueryService.findMeetingByCode(userId, meetingCode);
//
//             // Then
//             assertThat(result).isNull();
//         }
//     }
// }
