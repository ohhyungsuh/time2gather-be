package com.cover.time2gather.infra.jpa;

import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.infra.meeting.MeetingRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Profile("!firestore")
public interface JpaMeetingRepository extends MeetingRepository, JpaRepository<Meeting, Long> {
    @Override
    @Query("select m from Meeting m where m.confirmedDate is not null and "
            + "(locate(lower(:query), lower(m.title)) > 0 or locate(lower(:query), lower(m.meetingCode)) > 0)")
    Page<Meeting> findConfirmed(@Param("query") String query, Pageable pageable);
}
