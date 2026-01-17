package com.cover.time2gather.api.meeting;

import com.cover.time2gather.api.meeting.dto.response.MeetingDetailResponse;
import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.meeting.MeetingDetailData;
import com.cover.time2gather.domain.meeting.SelectionType;
import com.cover.time2gather.domain.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MeetingDetailResponse 장소 투표 정보 테스트")
class MeetingDetailResponseLocationTest {

    @Nested
    @DisplayName("장소 투표 활성화된 미팅")
    class LocationVoteEnabled {

        @Test
        @DisplayName("장소 목록과 투표 정보가 응답에 포함된다")
        void shouldIncludeLocationDataInResponse() {
            // Given
            User host = createUser(1L, "host");
            Meeting meeting = createMeeting(true);

            // 장소 목록 생성
            User voter1 = createUser(2L, "voter1");
            User voter2 = createUser(3L, "voter2");

            List<MeetingDetailData.LocationInfo> locations = List.of(
                new MeetingDetailData.LocationInfo(1L, "강남역 스타벅스", 0, 2, "67%", List.of(voter1, voter2)),
                new MeetingDetailData.LocationInfo(2L, "홍대 투썸", 1, 1, "33%", List.of(voter1))
            );

            MeetingDetailData.LocationData locationData = new MeetingDetailData.LocationData(
                true,
                locations,
                null  // 아직 확정 안됨
            );

            MeetingDetailData detailData = new MeetingDetailData(
                meeting,
                host,
                new ArrayList<>(),
                new ArrayList<>(),
                new MeetingDetailData.ScheduleData(new HashMap<>()),
                new MeetingDetailData.SummaryData(3, new ArrayList<>()),
                false,
                locationData
            );

            // When
            MeetingDetailResponse response = MeetingDetailResponse.from(detailData);

            // Then
            assertThat(response.getLocationVote()).isNotNull();
            assertThat(response.getLocationVote().isEnabled()).isTrue();
            assertThat(response.getLocationVote().getLocations()).hasSize(2);

            // 첫 번째 장소 검증
            MeetingDetailResponse.LocationInfo firstLocation = response.getLocationVote().getLocations().get(0);
            assertThat(firstLocation.getId()).isEqualTo(1L);
            assertThat(firstLocation.getName()).isEqualTo("강남역 스타벅스");
            assertThat(firstLocation.getVoteCount()).isEqualTo(2);
            assertThat(firstLocation.getPercentage()).isEqualTo("67%");
            assertThat(firstLocation.getVoters()).hasSize(2);

            // 확정된 장소 없음
            assertThat(response.getLocationVote().getConfirmedLocation()).isNull();
        }

        @Test
        @DisplayName("확정된 장소가 있으면 confirmedLocation에 포함된다")
        void shouldIncludeConfirmedLocationInResponse() {
            // Given
            User host = createUser(1L, "host");
            Meeting meeting = createMeeting(true);

            MeetingDetailData.LocationInfo confirmedLocation = new MeetingDetailData.LocationInfo(
                1L, "강남역 스타벅스", 0, 2, "100%", new ArrayList<>()
            );

            List<MeetingDetailData.LocationInfo> locations = List.of(confirmedLocation);

            MeetingDetailData.LocationData locationData = new MeetingDetailData.LocationData(
                true,
                locations,
                confirmedLocation  // 확정됨
            );

            MeetingDetailData detailData = new MeetingDetailData(
                meeting,
                host,
                new ArrayList<>(),
                new ArrayList<>(),
                new MeetingDetailData.ScheduleData(new HashMap<>()),
                new MeetingDetailData.SummaryData(2, new ArrayList<>()),
                false,
                locationData
            );

            // When
            MeetingDetailResponse response = MeetingDetailResponse.from(detailData);

            // Then
            assertThat(response.getLocationVote().getConfirmedLocation()).isNotNull();
            assertThat(response.getLocationVote().getConfirmedLocation().getId()).isEqualTo(1L);
            assertThat(response.getLocationVote().getConfirmedLocation().getName()).isEqualTo("강남역 스타벅스");
        }
    }

    @Nested
    @DisplayName("장소 투표 비활성화된 미팅")
    class LocationVoteDisabled {

        @Test
        @DisplayName("locationVote가 null로 반환된다")
        void shouldReturnNullLocationVote() {
            // Given
            User host = createUser(1L, "host");
            Meeting meeting = createMeeting(false);  // 장소 투표 비활성화

            MeetingDetailData detailData = new MeetingDetailData(
                meeting,
                host,
                new ArrayList<>(),
                new ArrayList<>(),
                new MeetingDetailData.ScheduleData(new HashMap<>()),
                new MeetingDetailData.SummaryData(0, new ArrayList<>()),
                false,
                null  // locationData가 null
            );

            // When
            MeetingDetailResponse response = MeetingDetailResponse.from(detailData);

            // Then
            assertThat(response.getLocationVote()).isNull();
        }
    }

    private User createUser(Long id, String username) {
        return User.builder()
            .username(username)
            .email(username + "@test.com")
            .profileImageUrl("https://profile.url")
            .provider(User.AuthProvider.ANONYMOUS)
            .providerId("providerId" + id)
            .build();
    }

    private Meeting createMeeting(boolean locationVoteEnabled) {
        return Meeting.create(
            "mtg_test123",
            "테스트 미팅",
            "설명",
            1L,
            "Asia/Seoul",
            SelectionType.TIME,
            60,
            Map.of("2024-02-15", new int[]{9, 10}),
            locationVoteEnabled
        );
    }
}
