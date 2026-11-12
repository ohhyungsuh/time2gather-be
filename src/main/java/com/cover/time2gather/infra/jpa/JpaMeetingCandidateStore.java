package com.cover.time2gather.infra.jpa;
import com.cover.time2gather.domain.exception.*;
import com.cover.time2gather.domain.meeting.*;
import com.cover.time2gather.infra.meeting.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository @Profile("!firestore") @RequiredArgsConstructor
public class JpaMeetingCandidateStore implements MeetingCandidateStore {
    private final EntityManager entityManager;
    private final MeetingRepository meetings;
    private final MeetingLocationRepository locations;
    private final MeetingLocationSelectionRepository locationVotes;
    private final MeetingUserSelectionRepository selections;
    @Override @Transactional public void update(String code,Long host,MeetingCandidates.Edit edit) {
        var found=entityManager.createQuery("select m from Meeting m where m.meetingCode=:code",Meeting.class)
                .setParameter("code",code).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
        if(found.isEmpty())throw new BusinessException(ErrorCode.MEETING_NOT_FOUND);
        var meeting=found.getFirst();
        var plan=MeetingCandidates.plan(meeting,host,edit,locations.selectByMeetingId(meeting.getId()),selections.findAllByMeetingId(meeting.getId()));
        for(Long id:plan.removedLocationIds()) {
            locationVotes.deleteByLocationId(id);locations.findById(id).ifPresent(locations::delete);
        }
        locations.saveAll(plan.locations());
        plan.deletedSelections().forEach(selections::delete);plan.savedSelections().forEach(selections::save);
        meetings.save(meeting);
    }
}
