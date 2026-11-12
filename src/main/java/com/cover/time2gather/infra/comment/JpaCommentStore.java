package com.cover.time2gather.infra.comment;

import com.cover.time2gather.domain.comment.MeetingComment;
import com.cover.time2gather.domain.meeting.Meeting;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.function.Function;

@Repository @Profile("!firestore") @RequiredArgsConstructor
public class JpaCommentStore implements CommentStore {
    private final EntityManager em;
    @Override @Transactional(readOnly = true)
    public Stats stats(Long meetingId) {
        Object[] row = em.createQuery("select coalesce(max(c.updatedAt), 0), sum(case when c.status = 'ACTIVE' then 1 else 0 end) from MeetingComment c where c.meetingId = :meetingId", Object[].class)
                .setParameter("meetingId", meetingId).getSingleResult();
        return new Stats(((Number) row[0]).longValue(), row[1] == null ? 0 : ((Number) row[1]).longValue());
    }
    @Override @Transactional(readOnly = true)
    public List<MeetingComment> list(Long meetingId) {
        return em.createQuery("select c from MeetingComment c where c.meetingId = :meetingId", MeetingComment.class)
                .setParameter("meetingId", meetingId).getResultList();
    }
    @Override @Transactional
    public MeetingComment mutate(Long meetingId, Function<List<MeetingComment>, MeetingComment> mutation) {
        em.find(Meeting.class, meetingId, LockModeType.PESSIMISTIC_WRITE);
        MeetingComment result = mutation.apply(list(meetingId));
        return em.merge(result);
    }
}
