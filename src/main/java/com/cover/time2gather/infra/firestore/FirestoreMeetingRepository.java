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
    public <S extends Meeting> S saveWithHostName(S entity, String hostName) {
        if (entity.getId() != null) throw new IllegalArgumentException("Initial host name requires a new meeting");
        return saveEntity(entity, (batch, meeting) -> {
            var root = firestore().collection("meetingNames").document(meeting.getId().toString());
            batch.set(root, java.util.Map.of("version", System.currentTimeMillis()));
            batch.set(root.collection("users").document(meeting.getHostUserId().toString()), java.util.Map.of("name", hostName));
        });
    }

    @Override
    public Optional<Meeting> findById(Long id) {
        return findEntityById(id);
    }

    @Override
    public Page<Meeting> findAll(Pageable pageable) {
        return newestPage(pageable);
    }

    @Override
    public Page<Meeting> findAll(Pageable pageable, String cursor) {
        return newestPage(pageable, cursor);
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
        return countEqualTo("isActive", true);
    }

    @Override
    public long countByConfirmedDateIsNotNull() {
        return countNotNull("confirmedDate");
    }

    @Override
    public Page<Meeting> findConfirmed(String query, Pageable pageable) {
        String keyword = normalize(query);
        // Read only confirmed documents using the existing single-field index.
        // Sort locally to preserve creation order without requiring a composite index.
        var matches = queryNotNull("confirmedDate").stream()
                .filter(meeting -> normalize(meeting.getTitle()).contains(keyword)
                        || normalize(meeting.getMeetingCode()).contains(keyword))
                .sorted(Comparator.comparing(Meeting::getCreatedAt,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Meeting::getId, Comparator.reverseOrder()))
                .toList();
        return page(matches, pageable);
    }

    @Override
    public Page<Meeting> findByTitleContainingIgnoreCaseOrMeetingCodeContainingIgnoreCase(
            String title, String meetingCode, Pageable pageable) {
        String titleQuery = normalize(title);
        String codeQuery = normalize(meetingCode);
        return searchNewest(meeting -> normalize(meeting.getTitle()).contains(titleQuery)
                || normalize(meeting.getMeetingCode()).contains(codeQuery), pageable);
    }

    private List<Meeting> newestFirst(List<Meeting> meetings) {
        return sorted(meetings, Comparator.comparing(Meeting::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
