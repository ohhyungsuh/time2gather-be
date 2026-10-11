package com.cover.time2gather.infra.meeting;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.cover.time2gather.domain.meeting.Meeting;

/** Persistence port implemented by JPA locally and Firestore in production. */
public interface MeetingRepository {

    <S extends Meeting> S save(S entity);

    Optional<Meeting> findById(Long id);

    Page<Meeting> findAll(Pageable pageable);

    long count();

    Optional<Meeting> findByMeetingCode(String meetingCode);

    boolean existsByMeetingCode(String meetingCode);

    List<Meeting> findByHostUserId(Long hostUserId);

    List<Meeting> findByHostUserIdOrderByCreatedAtDesc(Long hostUserId);

    List<Meeting> findByHostUserIdAndIsActiveTrue(Long hostUserId);

    List<Meeting> findAllByIdIn(Collection<Long> ids);

    List<Meeting> findAllByIdInAndIsActiveTrue(Collection<Long> ids);

    long countByIsActiveTrue();

    long countByConfirmedDateIsNotNull();

    Page<Meeting> findByTitleContainingIgnoreCaseOrMeetingCodeContainingIgnoreCase(
            String title,
            String meetingCode,
            Pageable pageable
    );
}
