package com.cover.time2gather.infra.identity;

import com.cover.time2gather.domain.identity.MeetingName;
import com.cover.time2gather.domain.meeting.Meeting;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository @Profile("!firestore") @RequiredArgsConstructor
public class JpaMeetingNameStore implements MeetingNameStore {
    private final EntityManager em;
    @Override @Transactional(readOnly = true)
    public Optional<String> name(Long meetingId, Long userId) {
        return Optional.ofNullable(em.find(MeetingName.class, meetingId + ":" + userId)).map(MeetingName::getName);
    }
    @Override @Transactional(readOnly = true)
    public Map<Long, String> names(Long meetingId) {
        return em.createQuery("select n from MeetingName n where n.meetingId = :meeting", MeetingName.class)
                .setParameter("meeting", meetingId).getResultStream()
                .collect(Collectors.toMap(MeetingName::getUserId, MeetingName::getName));
    }
    @Override @Transactional(readOnly = true)
    public long version(Long meetingId) {
        return em.createQuery("select coalesce(max(n.version), 0) from MeetingName n where n.meetingId = :meeting", Long.class)
                .setParameter("meeting", meetingId).getSingleResult();
    }
    @Override @Transactional
    public String save(Long meetingId, String code, Long userId, String name, boolean onlyIfAbsent) {
        if (em.find(Meeting.class, meetingId, LockModeType.PESSIMISTIC_WRITE) == null)
            throw new IllegalStateException("Meeting missing");
        MeetingName existing = em.find(MeetingName.class, meetingId + ":" + userId);
        if (existing != null && (onlyIfAbsent || existing.getName().equals(name))) return existing.getName();
        long version = Math.max(System.currentTimeMillis(), version(meetingId) + 1);
        if (existing == null) em.persist(new MeetingName(meetingId, userId, name, version));
        else existing.rename(name, version);
        return name;
    }
}
