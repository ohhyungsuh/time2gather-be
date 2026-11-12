package com.cover.time2gather.integration;

import com.cover.time2gather.domain.exception.BusinessException;
import com.cover.time2gather.domain.meeting.*;
import com.cover.time2gather.domain.meeting.service.MeetingLocationService;
import com.cover.time2gather.infra.jpa.JpaMeetingLocationSelectionRepository;
import com.cover.time2gather.infra.meeting.*;
import com.cover.time2gather.support.TestDatabases;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.*;
import org.springframework.transaction.annotation.*;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

@Tag("mysql")
@SpringBootTest
@ActiveProfiles({"test", "mysql-test"})
@Transactional
class JpaMysqlIntegrationTest {
    @Autowired MeetingRepository meetings;
    @Autowired MeetingUserSelectionRepository selections;
    @Autowired MeetingLocationRepository locations;
    @Autowired MeetingLocationSelectionRepository votes;
    @Autowired MeetingLocationService service;
    @Autowired EntityManager entityManager;
    Meeting meeting;
    MeetingLocation a, b;

    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> TestDatabases.mysql().getJdbcUrl());
        registry.add("spring.datasource.username", () -> TestDatabases.mysql().getUsername());
        registry.add("spring.datasource.password", () -> TestDatabases.mysql().getPassword());
    }

    @BeforeEach void createMeeting() {
        meeting = meetings.save(Meeting.create("mtg_" + UUID.randomUUID().toString().replace("-", ""),
                "MySQL integration", "", 1L, "Asia/Kathmandu", SelectionType.TIME, 60,
                Map.of("2027-01-04", new int[]{9, 10}), true));
        a = locations.save(MeetingLocation.create(meeting.getId(), "A", 0));
        b = locations.save(MeetingLocation.create(meeting.getId(), "B", 1));
    }

    @Test void mysqlJsonRoundTripsVotesAndUnavailableResponsesAfterClearingPersistenceContext() {
        selections.save(MeetingUserSelection.create(meeting.getId(), 9007199254740993L,
                Map.of("2027-01-04", new int[]{9, 10})));
        entityManager.flush(); entityManager.clear();
        var stored = selections.findByMeetingIdAndUserId(meeting.getId(), 9007199254740993L).orElseThrow();
        assertThat(stored.getSelections().get("2027-01-04")).containsExactly(9, 10);
        stored.markUnavailable(List.of("2027-01-04")); selections.save(stored);
        entityManager.flush(); entityManager.clear();
        var none = selections.findByMeetingIdAndUserId(meeting.getId(), 9007199254740993L).orElseThrow();
        assertThat(none.getSelections()).isEmpty();
        assertThat(none.availabilityResponse(meeting)).isEqualTo("NONE");
        assertThat(meetings.findById(meeting.getId()).orElseThrow().getTimezone()).isEqualTo("Asia/Kathmandu");
    }

    @Test void mysqlEnforcesTheLocationVoteUniqueConstraint() {
        var jpa = (JpaMeetingLocationSelectionRepository) votes;
        jpa.saveAndFlush(MeetingLocationSelection.create(meeting.getId(), a.getId(), 2L));
        assertThatThrownBy(() -> jpa.saveAndFlush(MeetingLocationSelection.create(meeting.getId(), a.getId(), 2L)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test void serviceReplacementDeduplicatesIdsAndPersistsTheExactRequestedSet() {
        service.voteLocations(meeting.getMeetingCode(), 2L, List.of(a.getId()));
        service.voteLocations(meeting.getMeetingCode(), 2L, List.of(b.getId(), b.getId()));
        entityManager.flush(); entityManager.clear();
        assertThat(service.selectUserLocationIds(meeting.getMeetingCode(), 2L)).containsExactly(b.getId());
        service.voteLocations(meeting.getMeetingCode(), 2L, List.of());
        entityManager.flush(); entityManager.clear();
        assertThat(votes.selectByMeetingIdAndUserId(meeting.getId(), 2L)).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void rejectedServiceRequestRollsBackBeforeTheNextDatabaseRead() {
        service.voteLocations(meeting.getMeetingCode(), 2L, List.of(a.getId()));
        assertThatThrownBy(() -> service.voteLocations(meeting.getMeetingCode(), 2L, List.of(b.getId(), Long.MAX_VALUE)))
                .isInstanceOf(BusinessException.class);
        assertThat(votes.selectByMeetingIdAndUserId(meeting.getId(), 2L))
                .extracting(MeetingLocationSelection::getLocationId).containsExactly(a.getId());
    }
}
