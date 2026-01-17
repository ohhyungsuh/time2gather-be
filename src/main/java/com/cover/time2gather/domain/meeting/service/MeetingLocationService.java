package com.cover.time2gather.domain.meeting.service;

import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.meeting.MeetingLocation;
import com.cover.time2gather.domain.meeting.MeetingLocationSelection;
import com.cover.time2gather.infra.meeting.MeetingLocationRepository;
import com.cover.time2gather.infra.meeting.MeetingLocationSelectionRepository;
import com.cover.time2gather.infra.meeting.MeetingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingLocationService {

    private static final int MAX_LOCATIONS = 5;
    private static final int MIN_LOCATIONS = 2;

    private final MeetingRepository meetingRepository;
    private final MeetingLocationRepository locationRepository;
    private final MeetingLocationSelectionRepository locationSelectionRepository;

    @Transactional
    public MeetingLocation addLocation(String meetingCode, Long userId, String locationName) {
        Meeting meeting = getMeetingByCode(meetingCode);

        // 호스트 권한 확인
        validateHost(meeting, userId);

        // 장소 투표 활성화 여부 확인
        validateLocationVoteEnabled(meeting);

        // 장소 이름 검증
        validateLocationName(locationName);

        // 최대 개수 확인
        int currentCount = locationRepository.countByMeetingId(meeting.getId());
        if (currentCount >= MAX_LOCATIONS) {
            throw new IllegalArgumentException("장소는 최대 " + MAX_LOCATIONS + "개까지 추가할 수 있습니다.");
        }

        // 다음 displayOrder 계산
        int nextOrder = currentCount;

        MeetingLocation location = MeetingLocation.create(meeting.getId(), locationName, nextOrder);
        return locationRepository.save(location);
    }

    @Transactional
    public void deleteLocation(String meetingCode, Long userId, Long locationId) {
        Meeting meeting = getMeetingByCode(meetingCode);

        // 호스트 권한 확인
        validateHost(meeting, userId);

        // 장소 투표 활성화 여부 확인
        validateLocationVoteEnabled(meeting);

        // 장소 존재 확인
        MeetingLocation location = locationRepository.findById(locationId)
                .orElseThrow(() -> new IllegalArgumentException("장소를 찾을 수 없습니다."));

        // 해당 미팅의 장소인지 확인
        if (!location.getMeetingId().equals(meeting.getId())) {
            throw new IllegalArgumentException("해당 미팅의 장소가 아닙니다.");
        }

        // 최소 개수 확인
        int currentCount = locationRepository.countByMeetingId(meeting.getId());
        if (currentCount <= MIN_LOCATIONS) {
            throw new IllegalArgumentException("장소는 최소 " + MIN_LOCATIONS + "개 이상이어야 합니다.");
        }

        // 해당 장소에 대한 투표 삭제
        locationSelectionRepository.deleteByLocationId(locationId);

        // 장소 삭제
        locationRepository.delete(location);

        // displayOrder 재정렬
        reorderLocations(meeting.getId());
    }

    public List<MeetingLocation> selectLocationsByMeetingCode(String meetingCode) {
        Meeting meeting = getMeetingByCode(meetingCode);
        return locationRepository.selectByMeetingIdOrderByDisplayOrderAsc(meeting.getId());
    }

    /**
     * 사용자의 장소 투표를 저장합니다.
     * 기존 투표를 삭제하고 새로운 투표로 대체합니다.
     */
    @Transactional
    public void voteLocations(String meetingCode, Long userId, List<Long> locationIds) {
        Meeting meeting = getMeetingByCode(meetingCode);

        // 장소 투표 활성화 여부 확인
        validateLocationVoteEnabled(meeting);

        // 기존 투표 삭제
        locationSelectionRepository.deleteByMeetingIdAndUserId(meeting.getId(), userId);

        // 빈 배열이면 투표 스킵 (기존 투표만 삭제)
        if (locationIds == null || locationIds.isEmpty()) {
            return;
        }

        // 유효한 장소 ID 목록 조회
        List<MeetingLocation> validLocations = locationRepository.selectByMeetingId(meeting.getId());
        Set<Long> validLocationIds = validLocations.stream()
                .map(MeetingLocation::getId)
                .collect(Collectors.toSet());

        // 새로운 투표 저장
        for (Long locationId : locationIds) {
            if (!validLocationIds.contains(locationId)) {
                throw new IllegalArgumentException("존재하지 않는 장소입니다: " + locationId);
            }

            MeetingLocationSelection selection = MeetingLocationSelection.create(
                    meeting.getId(),
                    locationId,
                    userId
            );
            locationSelectionRepository.save(selection);
        }
    }

    /**
     * 사용자의 장소 투표 목록을 조회합니다.
     */
    public List<Long> selectUserLocationIds(String meetingCode, Long userId) {
        Meeting meeting = getMeetingByCode(meetingCode);

        // 장소 투표 활성화 여부 확인
        validateLocationVoteEnabled(meeting);

        List<MeetingLocationSelection> selections = locationSelectionRepository
                .selectByMeetingIdAndUserId(meeting.getId(), userId);

        return selections.stream()
                .map(MeetingLocationSelection::getLocationId)
                .collect(Collectors.toList());
    }

    /**
     * 장소를 확정합니다. (호스트만 가능)
     */
    @Transactional
    public void confirmLocation(String meetingCode, Long userId, Long locationId) {
        Meeting meeting = getMeetingByCode(meetingCode);

        // 호스트 권한 확인
        validateHost(meeting, userId);

        // 장소 투표 활성화 여부 확인
        validateLocationVoteEnabled(meeting);

        // 이미 확정되었는지 확인
        if (meeting.isLocationConfirmed()) {
            throw new IllegalStateException("이미 장소가 확정되었습니다.");
        }

        // 장소 존재 확인
        MeetingLocation location = locationRepository.findById(locationId)
                .orElseThrow(() -> new IllegalArgumentException("장소를 찾을 수 없습니다."));

        // 해당 미팅의 장소인지 확인
        if (!location.getMeetingId().equals(meeting.getId())) {
            throw new IllegalArgumentException("해당 미팅의 장소가 아닙니다.");
        }

        // 장소 확정
        meeting.confirmLocation(locationId);
        meetingRepository.save(meeting);
    }

    /**
     * 장소 확정을 취소합니다. (호스트만 가능)
     */
    @Transactional
    public void cancelLocationConfirmation(String meetingCode, Long userId) {
        Meeting meeting = getMeetingByCode(meetingCode);

        // 호스트 권한 확인
        validateHost(meeting, userId);

        // 장소 투표 활성화 여부 확인
        validateLocationVoteEnabled(meeting);

        // 확정되지 않은 경우 에러
        if (!meeting.isLocationConfirmed()) {
            throw new IllegalStateException("장소가 확정되지 않았습니다.");
        }

        // 확정 취소
        meeting.cancelLocationConfirmation();
        meetingRepository.save(meeting);
    }

    private Meeting getMeetingByCode(String meetingCode) {
        return meetingRepository.findByMeetingCode(meetingCode)
                .orElseThrow(() -> new IllegalArgumentException("미팅을 찾을 수 없습니다."));
    }

    private void validateHost(Meeting meeting, Long userId) {
        if (!meeting.getHostUserId().equals(userId)) {
            throw new AccessDeniedException("호스트만 장소를 관리할 수 있습니다.");
        }
    }

    private void validateLocationVoteEnabled(Meeting meeting) {
        if (!meeting.isLocationVoteEnabled()) {
            throw new IllegalArgumentException("장소 투표가 활성화되지 않은 미팅입니다.");
        }
    }

    private void validateLocationName(String locationName) {
        if (locationName == null || locationName.trim().isEmpty()) {
            throw new IllegalArgumentException("장소 이름은 비어있을 수 없습니다.");
        }
    }

    private void reorderLocations(Long meetingId) {
        List<MeetingLocation> locations = locationRepository.selectByMeetingIdOrderByDisplayOrderAsc(meetingId);
        for (int i = 0; i < locations.size(); i++) {
            locations.get(i).updateDisplayOrder(i);
        }
        locationRepository.saveAll(locations);
    }
}
