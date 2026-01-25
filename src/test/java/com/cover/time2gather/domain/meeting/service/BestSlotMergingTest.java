package com.cover.time2gather.domain.meeting.service;

import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.meeting.MeetingDetailData;
import com.cover.time2gather.domain.meeting.MeetingUserSelection;
import com.cover.time2gather.domain.meeting.SelectionType;
import com.cover.time2gather.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BestSlot 연속 시간 병합 테스트
 * 
 * 요구사항:
 * - 연속된 2개 이상의 슬롯은 하나로 병합
 * - 병합된 슬롯의 count는 모든 슬롯에 참여한 인원만 카운트 (엄격)
 * - 각 BestSlot에 참여자 명단 포함
 */
class BestSlotMergingTest {

    private Meeting timeMeeting;
    private Meeting allDayMeeting;
    
    private User user1;
    private User user2;
    private User user3;
    
    private BestSlotBuilder bestSlotBuilder;

    @BeforeEach
    void setUp() {
        // TIME 타입 미팅 (60분 간격)
        timeMeeting = Meeting.create(
            "mtg_time",
            "시간 미팅",
            "설명",
            1L,
            "Asia/Seoul",
            SelectionType.TIME,
            60,
            Map.of("2024-02-15", new int[]{14, 15, 16, 17, 18})
        );

        // ALL_DAY 타입 미팅
        allDayMeeting = Meeting.create(
            "mtg_allday",
            "종일 미팅",
            "설명",
            1L,
            "Asia/Seoul",
            SelectionType.ALL_DAY,
            null,
            Map.of(
                "2024-02-15", new int[]{},
                "2024-02-16", new int[]{},
                "2024-02-17", new int[]{}
            )
        );

        // 테스트 사용자 생성
        user1 = User.builder()
            .username("김철수")
            .provider(User.AuthProvider.KAKAO)
            .providerId("kakao_1")
            .build();

        user2 = User.builder()
            .username("이영희")
            .provider(User.AuthProvider.KAKAO)
            .providerId("kakao_2")
            .build();

        user3 = User.builder()
            .username("박민수")
            .provider(User.AuthProvider.KAKAO)
            .providerId("kakao_3")
            .build();
        
        bestSlotBuilder = new BestSlotBuilder();
    }

    @Nested
    @DisplayName("연속 슬롯 병합")
    class ConsecutiveSlotMerging {

        @Test
        @DisplayName("연속 슬롯 2개는 가장 넓은 범위가 Top1에 위치한다 (14:00, 15:00 → 14:00~15:00)")
        void shouldMergeTwoConsecutiveSlots() {
            // Given: 3명의 사용자가 14:00, 15:00 둘 다 선택
            Map<Long, User> userMap = Map.of(1L, user1, 2L, user2, 3L, user3);
            List<MeetingUserSelection> selections = List.of(
                createSelection(1L, 1L, Map.of("2024-02-15", new int[]{14, 15})),
                createSelection(2L, 2L, Map.of("2024-02-15", new int[]{14, 15})),
                createSelection(3L, 3L, Map.of("2024-02-15", new int[]{14, 15}))
            );

            // When
            MeetingDetailData.SummaryData summary = bestSlotBuilder.buildSummaryData(
                timeMeeting, selections, userMap, 3);

            // Then: 모든 조합 중 가장 넓은 범위(14~15)가 Top1
            assertThat(summary.getBestSlots()).isNotEmpty();
            
            MeetingDetailData.BestSlot bestSlot = summary.getBestSlots().get(0);
            assertThat(bestSlot.getDate()).isEqualTo("2024-02-15");
            assertThat(bestSlot.getStartSlotIndex()).isEqualTo(14);
            assertThat(bestSlot.getEndSlotIndex()).isEqualTo(15);
            assertThat(bestSlot.getCount()).isEqualTo(3);
            assertThat(bestSlot.getParticipants()).hasSize(3);
        }

        @Test
        @DisplayName("연속 슬롯 3개는 가장 넓은 범위가 Top1에 위치한다 (14:00, 15:00, 16:00 → 14:00~16:00)")
        void shouldMergeThreeConsecutiveSlots() {
            // Given: 2명의 사용자가 14:00, 15:00, 16:00 모두 선택
            Map<Long, User> userMap = Map.of(1L, user1, 2L, user2);
            List<MeetingUserSelection> selections = List.of(
                createSelection(1L, 1L, Map.of("2024-02-15", new int[]{14, 15, 16})),
                createSelection(2L, 2L, Map.of("2024-02-15", new int[]{14, 15, 16}))
            );

            // When
            MeetingDetailData.SummaryData summary = bestSlotBuilder.buildSummaryData(
                timeMeeting, selections, userMap, 2);

            // Then: 모든 조합 중 가장 넓은 범위(14~16)가 Top1
            assertThat(summary.getBestSlots()).isNotEmpty();
            
            MeetingDetailData.BestSlot bestSlot = summary.getBestSlots().get(0);
            assertThat(bestSlot.getDate()).isEqualTo("2024-02-15");
            assertThat(bestSlot.getStartSlotIndex()).isEqualTo(14);
            assertThat(bestSlot.getEndSlotIndex()).isEqualTo(16);
            assertThat(bestSlot.getCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("비연속 슬롯은 병합되지 않는다 (14:00, 16:00 → 개별 유지)")
        void shouldNotMergeNonConsecutiveSlots() {
            // Given: 3명의 사용자가 14:00, 16:00 선택 (15:00은 없음)
            Map<Long, User> userMap = Map.of(1L, user1, 2L, user2, 3L, user3);
            List<MeetingUserSelection> selections = List.of(
                createSelection(1L, 1L, Map.of("2024-02-15", new int[]{14, 16})),
                createSelection(2L, 2L, Map.of("2024-02-15", new int[]{14, 16})),
                createSelection(3L, 3L, Map.of("2024-02-15", new int[]{14, 16}))
            );

            // When
            MeetingDetailData.SummaryData summary = bestSlotBuilder.buildSummaryData(
                timeMeeting, selections, userMap, 3);

            // Then: 개별 슬롯으로 유지
            assertThat(summary.getBestSlots()).hasSize(2);
            
            MeetingDetailData.BestSlot slot1 = summary.getBestSlots().get(0);
            assertThat(slot1.getStartSlotIndex()).isEqualTo(slot1.getEndSlotIndex());
            
            MeetingDetailData.BestSlot slot2 = summary.getBestSlots().get(1);
            assertThat(slot2.getStartSlotIndex()).isEqualTo(slot2.getEndSlotIndex());
        }

        @Test
        @DisplayName("다른 날짜의 슬롯은 병합되지 않는다")
        void shouldNotMergeSlotsFromDifferentDates() {
            // Given: 2/15 14:00과 2/16 15:00은 병합되지 않음
            Map<Long, User> userMap = Map.of(1L, user1, 2L, user2);
            List<MeetingUserSelection> selections = List.of(
                createSelection(1L, 1L, Map.of(
                    "2024-02-15", new int[]{14},
                    "2024-02-16", new int[]{15}
                )),
                createSelection(2L, 2L, Map.of(
                    "2024-02-15", new int[]{14},
                    "2024-02-16", new int[]{15}
                ))
            );

            // When
            MeetingDetailData.SummaryData summary = bestSlotBuilder.buildSummaryData(
                timeMeeting, selections, userMap, 2);

            // Then: 각 날짜별로 개별 슬롯
            assertThat(summary.getBestSlots()).hasSize(2);
            
            List<String> dates = summary.getBestSlots().stream()
                .map(MeetingDetailData.BestSlot::getDate)
                .toList();
            assertThat(dates).containsExactlyInAnyOrder("2024-02-15", "2024-02-16");
        }
    }

    @Nested
    @DisplayName("엄격한 카운트 계산")
    class StrictCountCalculation {

        @Test
        @DisplayName("사용자별로 다른 범위를 선택한 경우 공통 참여자가 가장 많은 범위가 Top1")
        void shouldCountOnlyUsersWhoSelectedAllSlots() {
            // Given: 
            // user1: 14:00, 15:00, 16:00 모두 선택
            // user2: 14:00, 15:00만 선택 (16:00 없음)
            // user3: 14:00만 선택
            Map<Long, User> userMap = Map.of(1L, user1, 2L, user2, 3L, user3);
            List<MeetingUserSelection> selections = List.of(
                createSelection(1L, 1L, Map.of("2024-02-15", new int[]{14, 15, 16})),
                createSelection(2L, 2L, Map.of("2024-02-15", new int[]{14, 15})),
                createSelection(3L, 3L, Map.of("2024-02-15", new int[]{14}))
            );

            // When
            MeetingDetailData.SummaryData summary = bestSlotBuilder.buildSummaryData(
                timeMeeting, selections, userMap, 3);

            // Then: 
            // 14 단독: 3명 → Top1
            // 14~15: 2명 (user1, user2)
            // 14~16: 1명 (user1만)
            assertThat(summary.getBestSlots()).isNotEmpty();
            
            MeetingDetailData.BestSlot topSlot = summary.getBestSlots().get(0);
            
            // 14 단독이 count=3으로 Top1
            assertThat(topSlot.getStartSlotIndex()).isEqualTo(14);
            assertThat(topSlot.getEndSlotIndex()).isEqualTo(14);
            assertThat(topSlot.getCount()).isEqualTo(3);
            assertThat(topSlot.getParticipants()).containsExactlyInAnyOrder(user1, user2, user3);
        }

        @Test
        @DisplayName("연속 범위 중 더 넓은 범위가 카운트와 함께 우선")
        void shouldPreferWiderRangeWithHigherCount() {
            // Given: 2명의 사용자가 동일한 연속 슬롯 선택
            Map<Long, User> userMap = Map.of(1L, user1, 2L, user2);
            List<MeetingUserSelection> selections = List.of(
                createSelection(1L, 1L, Map.of("2024-02-15", new int[]{14, 15, 16})),
                createSelection(2L, 2L, Map.of("2024-02-15", new int[]{14, 15, 16}))
            );

            // When
            MeetingDetailData.SummaryData summary = bestSlotBuilder.buildSummaryData(
                timeMeeting, selections, userMap, 2);

            // Then: 14~16 범위가 가장 넓고 count=2, Top1에 위치
            assertThat(summary.getBestSlots()).isNotEmpty();
            MeetingDetailData.BestSlot slot = summary.getBestSlots().get(0);
            assertThat(slot.getStartSlotIndex()).isEqualTo(14);
            assertThat(slot.getEndSlotIndex()).isEqualTo(16);
            assertThat(slot.getCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("참여자 명단")
    class ParticipantsList {

        @Test
        @DisplayName("BestSlot에 참여자 목록이 포함된다")
        void shouldIncludeParticipantsInBestSlot() {
            // Given
            Map<Long, User> userMap = Map.of(1L, user1, 2L, user2);
            List<MeetingUserSelection> selections = List.of(
                createSelection(1L, 1L, Map.of("2024-02-15", new int[]{14, 15})),
                createSelection(2L, 2L, Map.of("2024-02-15", new int[]{14, 15}))
            );

            // When
            MeetingDetailData.SummaryData summary = bestSlotBuilder.buildSummaryData(
                timeMeeting, selections, userMap, 2);

            // Then
            assertThat(summary.getBestSlots()).isNotEmpty();
            
            MeetingDetailData.BestSlot bestSlot = summary.getBestSlots().get(0);
            assertThat(bestSlot.getParticipants()).isNotNull();
            assertThat(bestSlot.getParticipants()).hasSize(2);
            assertThat(bestSlot.getParticipants()).containsExactlyInAnyOrder(user1, user2);
        }

        @Test
        @DisplayName("공통 참여자가 가장 많은 범위가 Top1이고, 해당 범위 전체 참여 사용자만 포함")
        void shouldIncludeOnlyUsersWhoSelectedAllSlotsInRange() {
            // Given:
            // user1: 14, 15, 16 모두 선택
            // user2: 14, 15만 선택
            Map<Long, User> userMap = Map.of(1L, user1, 2L, user2);
            List<MeetingUserSelection> selections = List.of(
                createSelection(1L, 1L, Map.of("2024-02-15", new int[]{14, 15, 16})),
                createSelection(2L, 2L, Map.of("2024-02-15", new int[]{14, 15}))
            );

            // When
            MeetingDetailData.SummaryData summary = bestSlotBuilder.buildSummaryData(
                timeMeeting, selections, userMap, 2);

            // Then: 
            // 14~15: count=2 (user1, user2) → Top1
            // 16: count=1 (user1) → Top2 (14~15와 겹치지 않음)
            // 14~16: count=1 (user1) → 14~15와 겹쳐서 제외됨
            assertThat(summary.getBestSlots()).isNotEmpty();
            
            MeetingDetailData.BestSlot topSlot = summary.getBestSlots().get(0);
            assertThat(topSlot.getStartSlotIndex()).isEqualTo(14);
            assertThat(topSlot.getEndSlotIndex()).isEqualTo(15);
            assertThat(topSlot.getCount()).isEqualTo(2);
            assertThat(topSlot.getParticipants()).containsExactlyInAnyOrder(user1, user2);
            
            // Top2는 16 단독 (14~15와 겹치지 않음)
            assertThat(summary.getBestSlots()).hasSizeGreaterThanOrEqualTo(2);
            MeetingDetailData.BestSlot secondSlot = summary.getBestSlots().get(1);
            assertThat(secondSlot.getStartSlotIndex()).isEqualTo(16);
            assertThat(secondSlot.getEndSlotIndex()).isEqualTo(16);
            assertThat(secondSlot.getCount()).isEqualTo(1);
            assertThat(secondSlot.getParticipants()).containsExactly(user1);
        }
    }

    @Nested
    @DisplayName("ALL_DAY 타입")
    class AllDayType {

        @Test
        @DisplayName("ALL_DAY 타입은 병합 없이 날짜별로 표시된다")
        void shouldNotMergeAllDaySlots() {
            // Given
            Map<Long, User> userMap = Map.of(1L, user1, 2L, user2);
            List<MeetingUserSelection> selections = List.of(
                createSelection(1L, 1L, SelectionType.ALL_DAY, Map.of(
                    "2024-02-15", new int[]{},
                    "2024-02-16", new int[]{}
                )),
                createSelection(2L, 2L, SelectionType.ALL_DAY, Map.of(
                    "2024-02-15", new int[]{},
                    "2024-02-16", new int[]{}
                ))
            );

            // When
            MeetingDetailData.SummaryData summary = bestSlotBuilder.buildSummaryData(
                allDayMeeting, selections, userMap, 2);

            // Then: 날짜별로 개별 표시
            assertThat(summary.getBestSlots()).hasSize(2);
            
            for (MeetingDetailData.BestSlot slot : summary.getBestSlots()) {
                // ALL_DAY는 slotIndex가 -1
                assertThat(slot.getStartSlotIndex()).isEqualTo(-1);
                assertThat(slot.getEndSlotIndex()).isEqualTo(-1);
            }
        }
    }

    @Nested
    @DisplayName("Top3 제한")
    class Top3Limit {

        @Test
        @DisplayName("Top3만 반환된다")
        void shouldReturnOnlyTop3() {
            // Given: 5개의 다른 슬롯
            Map<Long, User> userMap = Map.of(1L, user1);
            List<MeetingUserSelection> selections = List.of(
                createSelection(1L, 1L, Map.of("2024-02-15", new int[]{10, 12, 14, 16, 18}))
            );

            // When
            MeetingDetailData.SummaryData summary = bestSlotBuilder.buildSummaryData(
                timeMeeting, selections, userMap, 1);

            // Then
            assertThat(summary.getBestSlots()).hasSizeLessThanOrEqualTo(3);
        }
    }

    // Helper methods
    
    private MeetingUserSelection createSelection(Long id, Long userId, Map<String, int[]> selections) {
        return createSelection(id, userId, SelectionType.TIME, selections);
    }

    private MeetingUserSelection createSelection(Long id, Long userId, SelectionType type, Map<String, int[]> selections) {
        return MeetingUserSelection.create(1L, userId, type, 60, selections);
    }

}
