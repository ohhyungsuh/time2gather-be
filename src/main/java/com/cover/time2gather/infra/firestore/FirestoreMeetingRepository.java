package com.cover.time2gather.infra.firestore;

import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.infra.meeting.MeetingRepository;
import com.google.cloud.firestore.Firestore;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Repository
@Profile("firestore")
public class FirestoreMeetingRepository extends FirestoreRepositorySupport<Meeting> implements MeetingRepository {

    public FirestoreMeetingRepository(Firestore firestore) {
        super(firestore, "meetings", Meeting.class);
    }

    @Override
    public <S extends Meeting> S save(S entity) {
        return saveEntity(entity);
    }

    @Override
    public Optional<Meeting> findById(Long id) {
        return findEntityById(id);
    }

    @Override
    public Page<Meeting> findAll(Pageable pageable) {
        return page(newestFirst(allEntities()), pageable);
    }

    @Override
    public long count() {
        return countAll();
    }

    @Override
    public Optional<Meeting> findByMeetingCode(String meetingCode) {
        return firstEquals("meetingCode", meetingCode);
    }

    @Override
    public boolean existsByMeetingCode(String meetingCode) {
        return findByMeetingCode(meetingCode).isPresent();
    }

    @Override
    public List<Meeting> findByHostUserId(Long hostUserId) {
        return queryEquals("hostUserId", hostUserId);
    }

    @Override
    public List<Meeting> findByHostUserIdOrderByCreatedAtDesc(Long hostUserId) {
        return newestFirst(findByHostUserId(hostUserId));
    }

    @Override
    public List<Meeting> findByHostUserIdAndIsActiveTrue(Long hostUserId) {
        return findByHostUserId(hostUserId).stream().filter(meeting -> Boolean.TRUE.equals(meeting.getIsActive())).toList();
    }

    @Override
    public List<Meeting> findAllByIdIn(Collection<Long> ids) {
        return findEntitiesByIds(ids);
    }

    @Override
    public List<Meeting> findAllByIdInAndIsActiveTrue(Collection<Long> ids) {
        return findAllByIdIn(ids).stream().filter(meeting -> Boolean.TRUE.equals(meeting.getIsActive())).toList();
    }

    @Override
    public long countByIsActiveTrue() {
        return countMatching(meeting -> Boolean.TRUE.equals(meeting.getIsActive()));
    }

    @Override
    public long countByConfirmedDateIsNotNull() {
        return countMatching(meeting -> meeting.getConfirmedDate() != null);
    }

    @Override
    public Page<Meeting> findByTitleContainingIgnoreCaseOrMeetingCodeContainingIgnoreCase(
            String title, String meetingCode, Pageable pageable) {
        String titleQuery = normalize(title);
        String codeQuery = normalize(meetingCode);
        List<Meeting> matches = newestFirst(allEntities().stream()
                .filter(meeting -> normalize(meeting.getTitle()).contains(titleQuery)
                        || normalize(meeting.getMeetingCode()).contains(codeQuery))
                .toList());
        return page(matches, pageable);
    }

    private List<Meeting> newestFirst(List<Meeting> meetings) {
        return sorted(meetings, Comparator.comparing(Meeting::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
