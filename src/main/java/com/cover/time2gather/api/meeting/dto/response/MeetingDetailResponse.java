package com.cover.time2gather.api.meeting.dto.response;

import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.meeting.MeetingDetailData;
import com.cover.time2gather.domain.user.User;
import com.cover.time2gather.domain.meeting.vo.TimeSlot;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.*;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
@Schema(description = "모임 상세 응답")
public class MeetingDetailResponse {

	@Schema(description = "모임 정보")
	private MeetingInfo meeting;

	@Schema(description = "참여자 목록")
	private List<ParticipantInfo> participants;

	@Schema(description = "날짜/시간별 참여 가능한 사용자 목록 및 카운트")
	private Map<String, Map<String, TimeSlotDetail>> schedule;

	@Schema(description = "요약 정보")
	private SummaryInfo summary;

	@Schema(description = "현재 사용자의 참여 여부 (미인증 시 false)")
	private boolean isParticipated;

	@Schema(description = "장소 투표 정보 (장소 투표 비활성화 시 null)")
	private LocationVoteInfo locationVote;

	/**
	 * 도메인 모델로부터 DTO 생성
	 */
	public static MeetingDetailResponse from(MeetingDetailData detailData) {
		Meeting meeting = detailData.getMeeting();
		User host = detailData.getHost();
		int intervalMinutes = meeting.getIntervalMinutes();

		// Meeting 정보 변환
		String confirmedDate = meeting.getConfirmedDate() != null
			? meeting.getConfirmedDate().toString()
			: null;
		String confirmedTime = null;
		if (meeting.getConfirmedDate() != null) {
			Integer confirmedSlotIndex = meeting.getConfirmedSlotIndex();
			if (confirmedSlotIndex == null || confirmedSlotIndex == -1) {
				confirmedTime = "ALL_DAY";
			} else {
				confirmedTime = TimeSlot.fromIndex(confirmedSlotIndex, intervalMinutes).toTimeString();
			}
		}

		MeetingInfo meetingInfo = new MeetingInfo(
			meeting.getId(),
			meeting.getMeetingCode(),
			meeting.getTitle(),
			meeting.getDescription(),
			new HostInfo(
				host.getId(),
				host.getUsername(),
				host.getProfileImageUrl()
			),
			meeting.getTimezone(),
			meeting.getSelectionType().name(),
			intervalMinutes,
			convertSlotIndexesToTimeStrings(meeting.getAvailableDates(), intervalMinutes),
			confirmedDate,
			confirmedTime
		);

		// 참여자 정보 변환
		List<ParticipantInfo> participants = detailData.getParticipants().stream()
			.map(user -> new ParticipantInfo(
				user.getId(),
				user.getUsername(),
				user.getProfileImageUrl()
			))
			.collect(Collectors.toList());

		// Schedule 정보 변환
		Map<String, Map<String, TimeSlotDetail>> schedule = convertScheduleToResponse(
			detailData.getSchedule(),
			intervalMinutes
		);

		// Summary 정보 변환
		SummaryInfo summary = convertSummaryToResponse(
			detailData.getSummary(),
			intervalMinutes
		);

		// 장소 투표 정보 변환
		LocationVoteInfo locationVote = convertLocationDataToResponse(detailData.getLocationData());

		return new MeetingDetailResponse(meetingInfo, participants, schedule, summary, detailData.isParticipated(), locationVote);
	}

	/**
	 * Schedule 도메인 모델 → DTO 변환
	 */
	private static Map<String, Map<String, TimeSlotDetail>> convertScheduleToResponse(
		MeetingDetailData.ScheduleData scheduleData,
		int intervalMinutes
	) {
		Map<String, Map<String, TimeSlotDetail>> result = new HashMap<>();

		Map<String, Map<Integer, List<User>>> dateTimeUserMap = scheduleData.getDateTimeUserMap();
		for (Map.Entry<String, Map<Integer, List<User>>> dateEntry : dateTimeUserMap.entrySet()) {
			String date = dateEntry.getKey();
			Map<Integer, List<User>> slotUserMap = dateEntry.getValue();

			result.putIfAbsent(date, new HashMap<>());
			Map<String, TimeSlotDetail> timeDetailMap = result.get(date);

			for (Map.Entry<Integer, List<User>> slotEntry : slotUserMap.entrySet()) {
				int slot = slotEntry.getKey();
				List<User> users = slotEntry.getValue();

				// ALL_DAY 타입인 경우 (slotIndex = -1)
				String time = (slot == -1) ? "ALL_DAY" : TimeSlot.fromIndex(slot, intervalMinutes).toTimeString();

				List<ParticipantInfo> participantInfos = users.stream()
					.map(user -> new ParticipantInfo(
						user.getId(),
						user.getUsername(),
						user.getProfileImageUrl()
					))
					.collect(Collectors.toList());

				timeDetailMap.put(time, new TimeSlotDetail(users.size(), participantInfos));
			}
		}

		return result;
	}

	/**
	 * Summary 도메인 모델 → DTO 변환
	 */
	private static SummaryInfo convertSummaryToResponse(
		MeetingDetailData.SummaryData summaryData,
		int intervalMinutes
	) {
		// bestSlots가 비어있으면 빈 리스트 반환
		if (summaryData.getBestSlots().isEmpty()) {
			return new SummaryInfo(summaryData.getTotalParticipants(), new ArrayList<>());
		}

		List<BestSlot> bestSlots = summaryData.getBestSlots().stream()
			.map(slot -> {
				String timeString = formatTimeRange(slot, intervalMinutes);

				List<ParticipantInfo> participants = slot.getParticipants().stream()
					.map(user -> new ParticipantInfo(
						user.getId(),
						user.getUsername(),
						user.getProfileImageUrl()
					))
					.collect(Collectors.toList());

				return new BestSlot(
					slot.getDate(),
					timeString,
					slot.getStartSlotIndex(),
					slot.getEndSlotIndex(),
					slot.getCount(),
					slot.getPercentage(),
					participants
				);
			})
			.collect(Collectors.toList());

		return new SummaryInfo(summaryData.getTotalParticipants(), bestSlots);
	}

	/**
	 * BestSlot의 시간 범위를 문자열로 변환
	 * - ALL_DAY: "ALL_DAY"
	 * - 단일 슬롯: "09:00"
	 * - 연속 슬롯: "09:00 ~ 12:00" (endSlotIndex + 1의 시작 시간)
	 */
	private static String formatTimeRange(MeetingDetailData.BestSlot slot, int intervalMinutes) {
		// ALL_DAY 타입인 경우
		if (slot.getStartSlotIndex() == -1) {
			return "ALL_DAY";
		}

		String startTime = TimeSlot.fromIndex(slot.getStartSlotIndex(), intervalMinutes).toTimeString();

		// 단일 슬롯인 경우
		if (!slot.isRange()) {
			return startTime;
		}

		// 연속 슬롯인 경우: 종료 시간은 endSlotIndex + 1의 시작 시간
		String endTime = TimeSlot.fromIndex(slot.getEndSlotIndex() + 1, intervalMinutes).toTimeString();
		return startTime + " ~ " + endTime;
	}

	/**
	 * slotIndex → API "HH:mm" 변환
	 * ALL_DAY 타입의 경우 빈 배열로 반환
	 */
	private static Map<String, String[]> convertSlotIndexesToTimeStrings(Map<String, int[]> slotIndexes, int intervalMinutes) {
		Map<String, String[]> result = new HashMap<>();
		for (Map.Entry<String, int[]> entry : slotIndexes.entrySet()) {
			String date = entry.getKey();
			int[] slots = entry.getValue();

			// ALL_DAY 타입(빈 배열)인 경우 빈 배열 반환
			if (slots.length == 0) {
				result.put(date, new String[0]);
			} else {
				// TIME 타입인 경우 시간 문자열로 변환
				String[] times = Arrays.stream(slots)
					.mapToObj(slotIndex -> TimeSlot.fromIndex(slotIndex, intervalMinutes).toTimeString())
					.toArray(String[]::new);
				result.put(date, times);
			}
		}
		return result;
	}

	@Getter
	@AllArgsConstructor
	@Schema(description = "모임 기본 정보")
	public static class MeetingInfo {
		@Schema(description = "모임 ID", example = "1")
		private Long id;

		@Schema(description = "모임 코드", example = "mtg_a3f8k2md9x")
		private String code;

		@Schema(description = "모임 제목", example = "프로젝트 킥오프 미팅")
		private String title;

		@Schema(description = "모임 설명", example = "2월 신규 프로젝트 시작 회의")
		private String description;

		@Schema(description = "방장 정보")
		private HostInfo host;

		@Schema(description = "타임존", example = "Asia/Seoul")
		private String timezone;

		@Schema(description = "선택 타입 (TIME: 시간 단위, ALL_DAY: 일 단위)", example = "TIME")
		private String selectionType;

		@Schema(description = "시간 간격 (분 단위)", example = "60")
		private int intervalMinutes;

		@Schema(description = "가능한 날짜/시간대. ALL_DAY인 경우 빈 배열",
			example = "{\"2024-02-15\": [\"09:00\", \"10:00\", \"11:00\"], \"2024-02-16\": [\"14:00\", \"15:00\"]}")
		private Map<String, String[]> availableDates;

		@Schema(description = "확정된 날짜 (없으면 null)", example = "2024-02-15")
		private String confirmedDate;

		@Schema(description = "확정된 시간 (없으면 null, ALL_DAY인 경우 'ALL_DAY')", example = "09:00")
		private String confirmedTime;
	}

	@Getter
	@AllArgsConstructor
	@Schema(description = "방장 정보")
	public static class HostInfo {
		@Schema(description = "사용자 ID", example = "1")
		private Long id;

		@Schema(description = "사용자명", example = "jinwoo")
		private String username;

		@Schema(description = "프로필 이미지 URL", example = "https://...")
		private String profileImageUrl;
	}

	@Getter
	@AllArgsConstructor
	@Schema(description = "참여자 정보")
	public static class ParticipantInfo {
		@Schema(description = "사용자 ID", example = "1")
		private Long userId;

		@Schema(description = "사용자명", example = "jinwoo")
		private String username;

		@Schema(description = "프로필 이미지 URL", example = "https://...")
		private String profileImageUrl;
	}

	@Getter
	@AllArgsConstructor
	@Schema(description = "시간대별 상세 정보")
	public static class TimeSlotDetail {
		@Schema(description = "해당 시간대를 선택한 참여자 수", example = "3")
		private int count;

		@Schema(description = "해당 시간대를 선택한 참여자 목록")
		private List<ParticipantInfo> participants;
	}

	@Getter
	@AllArgsConstructor
	@Schema(description = "요약 정보")
	public static class SummaryInfo {
		@Schema(description = "총 참여자 수", example = "5")
		private int totalParticipants;

		@Schema(description = "베스트 시간대 (가장 많은 사람이 가능한 시간대)")
		private List<BestSlot> bestSlots;
	}

	@Getter
	@AllArgsConstructor
	@Schema(description = "베스트 시간대")
	public static class BestSlot {
		@Schema(description = "날짜", example = "2024-02-15")
		private String date;

		@Schema(description = "시간 (연속 시간대인 경우 범위 형식)", example = "09:00 ~ 12:00")
		private String time;

		@Schema(description = "시작 슬롯 인덱스 (약속 확정 시 사용, ALL_DAY는 -1)", example = "9")
		private int startSlotIndex;

		@Schema(description = "종료 슬롯 인덱스 (연속 범위의 끝, 단일 슬롯이면 startSlotIndex와 동일)", example = "11")
		private int endSlotIndex;

		@Schema(description = "가능한 인원 수", example = "4")
		private int count;

		@Schema(description = "가능 비율 (% 포함)", example = "80%")
		private String percentage;

		@Schema(description = "해당 시간대에 참여 가능한 참여자 목록")
		private List<ParticipantInfo> participants;
	}

	@Getter
	@AllArgsConstructor
	@Schema(description = "장소 투표 정보")
	public static class LocationVoteInfo {
		@Schema(description = "장소 투표 활성화 여부", example = "true")
		private boolean enabled;

		@Schema(description = "장소 목록")
		private List<LocationInfo> locations;

		@Schema(description = "확정된 장소 (없으면 null)")
		private LocationInfo confirmedLocation;
	}

	@Getter
	@AllArgsConstructor
	@Schema(description = "장소 정보")
	public static class LocationInfo {
		@Schema(description = "장소 ID", example = "1")
		private Long id;

		@Schema(description = "장소 이름", example = "강남역 스타벅스")
		private String name;

		@Schema(description = "표시 순서", example = "0")
		private int displayOrder;

		@Schema(description = "투표 수", example = "3")
		private int voteCount;

		@Schema(description = "투표 비율", example = "60%")
		private String percentage;

		@Schema(description = "투표한 참여자 목록")
		private List<ParticipantInfo> voters;
	}

	/**
	 * 장소 투표 데이터 → DTO 변환
	 */
	private static LocationVoteInfo convertLocationDataToResponse(MeetingDetailData.LocationData locationData) {
		if (locationData == null) {
			return null;
		}

		List<LocationInfo> locations = locationData.getLocations().stream()
			.map(loc -> new LocationInfo(
				loc.getId(),
				loc.getName(),
				loc.getDisplayOrder(),
				loc.getVoteCount(),
				loc.getPercentage(),
				loc.getVoters().stream()
					.map(user -> new ParticipantInfo(
						user.getId(),
						user.getUsername(),
						user.getProfileImageUrl()
					))
					.collect(Collectors.toList())
			))
			.collect(Collectors.toList());

		LocationInfo confirmedLocation = null;
		if (locationData.getConfirmedLocation() != null) {
			MeetingDetailData.LocationInfo confirmed = locationData.getConfirmedLocation();
			confirmedLocation = new LocationInfo(
				confirmed.getId(),
				confirmed.getName(),
				confirmed.getDisplayOrder(),
				confirmed.getVoteCount(),
				confirmed.getPercentage(),
				confirmed.getVoters().stream()
					.map(user -> new ParticipantInfo(
						user.getId(),
						user.getUsername(),
						user.getProfileImageUrl()
					))
					.collect(Collectors.toList())
			);
		}

		return new LocationVoteInfo(locationData.isEnabled(), locations, confirmedLocation);
	}
}
