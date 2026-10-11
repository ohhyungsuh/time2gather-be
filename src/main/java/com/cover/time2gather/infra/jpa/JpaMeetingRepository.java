package com.cover.time2gather.infra.jpa;

import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.infra.meeting.MeetingRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

@Profile("!firestore")
public interface JpaMeetingRepository extends MeetingRepository, JpaRepository<Meeting, Long> {
}
