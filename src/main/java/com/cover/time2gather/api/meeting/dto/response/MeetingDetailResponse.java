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
@Schema(description = "Meeting detail response")
public class MeetingDetailResponse {

	@Schema(description = "Meeting information")
	private MeetingInfo meeting;

	@Schema(description = "Participant list")
	private List<ParticipantInfo> participants;

	@Schema(description = "Available users list and count by date/time")
	private Map<String, Map<String, TimeSlotDetail>> schedule;

	@Schema(description = "Summary information")
	private SummaryInfo summary;

	@Schema(description = "Current user's participation status (false if not authenticated)")
	private boolean isParticipated;

	@Schema(description = "Location vote information (null if location voting is disabled)")
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
	@Schema(description = "Meeting basic information")
	public static class MeetingInfo {
		@Schema(description = "Meeting ID", example = "1")
		private Long id;

		@Schema(description = "Meeting code", example = "mtg_a3f8k2md9x")
		private String code;

		@Schema(description = "Meeting title", example = "Project Kickoff Meeting")
		private String title;

		@Schema(description = "Meeting description", example = "February new project start meeting")
		private String description;

		@Schema(description = "Host information")
		private HostInfo host;

		@Schema(description = "Timezone", example = "Asia/Seoul")
		private String timezone;

		@Schema(description = "Selection type (TIME: time-slot based, ALL_DAY: full-day based)", example = "TIME")
		private String selectionType;

		@Schema(description = "Time interval in minutes", example = "60")
		private int intervalMinutes;

		@Schema(description = "Available dates/times. Empty array for ALL_DAY type",
			example = "{\"2024-02-15\": [\"09:00\", \"10:00\", \"11:00\"], \"2024-02-16\": [\"14:00\", \"15:00\"]}")
		private Map<String, String[]> availableDates;

		@Schema(description = "Confirmed date (null if not confirmed)", example = "2024-02-15")
		private String confirmedDate;

		@Schema(description = "Confirmed time (null if not confirmed, 'ALL_DAY' for ALL_DAY type)", example = "09:00")
		private String confirmedTime;
	}

	@Getter
	@AllArgsConstructor
	@Schema(description = "Host information")
	public static class HostInfo {
		@Schema(description = "User ID", example = "1")
		private Long id;

		@Schema(description = "Username", example = "jinwoo")
		private String username;

		@Schema(description = "Profile image URL", example = "https://...")
		private String profileImageUrl;
	}

	@Getter
	@AllArgsConstructor
	@Schema(description = "Participant information")
	public static class ParticipantInfo {
		@Schema(description = "User ID", example = "1")
		private Long userId;

		@Schema(description = "Username", example = "jinwoo")
		private String username;

		@Schema(description = "Profile image URL", example = "https://...")
		private String profileImageUrl;
	}

	@Getter
	@AllArgsConstructor
	@Schema(description = "Time slot detail information")
	public static class TimeSlotDetail {
		@Schema(description = "Number of participants who selected this time slot", example = "3")
		private int count;

		@Schema(description = "List of participants who selected this time slot")
		private List<ParticipantInfo> participants;
	}

	@Getter
	@AllArgsConstructor
	@Schema(description = "Summary information")
	public static class SummaryInfo {
		@Schema(description = "Total number of participants", example = "5")
		private int totalParticipants;

		@Schema(description = "Best time slots (time slots with most available participants)")
		private List<BestSlot> bestSlots;
	}

	@Getter
	@AllArgsConstructor
	@Schema(description = "Best time slot")
	public static class BestSlot {
		@Schema(description = "Date", example = "2024-02-15")
		private String date;

		@Schema(description = "Time (range format for consecutive slots)", example = "09:00 ~ 12:00")
		private String time;

		@Schema(description = "Start slot index (used for confirmation, -1 for ALL_DAY)", example = "9")
		private int startSlotIndex;

		@Schema(description = "End slot index (end of consecutive range, same as startSlotIndex for single slot)", example = "11")
		private int endSlotIndex;

		@Schema(description = "Number of available participants", example = "4")
		private int count;

		@Schema(description = "Availability percentage (includes %)", example = "80%")
		private String percentage;

		@Schema(description = "List of participants available at this time slot")
		private List<ParticipantInfo> participants;
	}

	@Getter
	@AllArgsConstructor
	@Schema(description = "Location vote information")
	public static class LocationVoteInfo {
		@Schema(description = "Location voting enabled status", example = "true")
		private boolean enabled;

		@Schema(description = "Location list")
		private List<LocationInfo> locations;

		@Schema(description = "Confirmed location (null if not confirmed)")
		private LocationInfo confirmedLocation;
	}

	@Getter
	@AllArgsConstructor
	@Schema(description = "Location information")
	public static class LocationInfo {
		@Schema(description = "Location ID", example = "1")
		private Long id;

		@Schema(description = "Location name", example = "Gangnam Station Starbucks")
		private String name;

		@Schema(description = "Display order", example = "0")
		private int displayOrder;

		@Schema(description = "Vote count", example = "3")
		private int voteCount;

		@Schema(description = "Vote percentage", example = "60%")
		private String percentage;

		@Schema(description = "List of participants who voted for this location")
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
