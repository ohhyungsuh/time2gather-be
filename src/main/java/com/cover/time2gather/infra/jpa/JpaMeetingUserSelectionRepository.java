package com.cover.time2gather.infra.jpa;

import com.cover.time2gather.domain.meeting.MeetingUserSelection;
import com.cover.time2gather.infra.meeting.MeetingUserSelectionRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

@Profile("!firestore")
public interface JpaMeetingUserSelectionRepository extends MeetingUserSelectionRepository, JpaRepository<MeetingUserSelection, Long> {
}
