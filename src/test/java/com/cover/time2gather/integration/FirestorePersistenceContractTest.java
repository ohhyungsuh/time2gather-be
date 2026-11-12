package com.cover.time2gather.integration;

import com.cover.time2gather.domain.meeting.*;
import com.cover.time2gather.domain.user.*;
import com.cover.time2gather.infra.meeting.*;
import com.cover.time2gather.support.FirestoreIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class FirestorePersistenceContractTest extends FirestoreIntegrationTest {
    @Autowired UserRepository users;
    @Autowired MeetingRepository meetings;
    @Autowired MeetingUserSelectionRepository votes;

    @Test void usersRoundTripExactIdsNullableFieldsProviderKeysAndAggregateQueries() {
        var first = User.builder().username("Same name").provider(User.AuthProvider.ANONYMOUS).providerId("first").build();
        ReflectionTestUtils.setField(first, "id", 9007199254740993L);
        users.save(first);
        var second = users.save(User.builder().username("Same name").email("Second@example.com")
                .provider(User.AuthProvider.GOOGLE).providerId("second").role(UserRole.ADMIN).build());
        assertThat(users.findById(first.getId()).orElseThrow().getId()).isEqualTo(9007199254740993L);
        assertThat(users.findById(first.getId()).orElseThrow().getEmail()).isNull();
        assertThat(users.existsById(first.getId())).isTrue();
        assertThat(users.existsById(Long.MAX_VALUE)).isFalse();
        assertThat(users.findById(null)).isEmpty();
        assertThat(users.findByProviderAndProviderId(User.AuthProvider.GOOGLE, "second").orElseThrow().getId()).isEqualTo(second.getId());
        assertThat(users.findByProviderAndProviderId(User.AuthProvider.ANONYMOUS, "second")).isEmpty();
        assertThat(users.findByEmail("Second@example.com").orElseThrow().getId()).isEqualTo(second.getId());
        assertThat(users.findByUsername("Same name")).isPresent();
        assertThat(users.findByUsername("missing")).isEmpty();
        assertThat(users.findAll()).hasSize(2);
        assertThat(users.count()).isEqualTo(2);
        assertThat(users.countByProvider(User.AuthProvider.ANONYMOUS)).isEqualTo(1);
        assertThat(users.countByProviderNot(User.AuthProvider.ANONYMOUS)).isEqualTo(1);
        assertThat(users.countByRole(UserRole.ADMIN)).isEqualTo(1);
        assertThat(users.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase("second", "second", PageRequest.of(0, 10)).getContent())
                .extracting(User::getId).containsExactly(second.getId());
        assertThat(users.findAll(PageRequest.of(0, 1)).getTotalElements()).isEqualTo(2);
        users.deleteAll();
        assertThat(users.findAll()).isEmpty();
    }

    @Test void batchReadsKeepOrderIgnoreMissingIdsAndSupportMoreThanTenParticipants() {
        var meeting = meetings.save(meeting(1L));
        List<Long> ids = new ArrayList<>();
        for (int index = 0; index < 16; index++) {
            var user = users.save(User.builder().username("Person " + index).provider(User.AuthProvider.ANONYMOUS)
                    .providerId("person-" + index).build());
            ids.add(user.getId());
            votes.save(MeetingUserSelection.create(meeting.getId(), user.getId(), Map.of("2027-01-04", new int[]{9})));
        }
        var requested = new ArrayList<>(ids.reversed()); requested.add(ids.getFirst()); requested.add(Long.MAX_VALUE); requested.add(null);
        assertThat(users.findAllById(requested)).extracting(User::getId).containsExactlyElementsOf(ids.reversed());
        assertThat(users.findAllById(List.of())).isEmpty();
        assertThat(votes.findAllByUserIdIn(requested)).hasSize(16);
        assertThat(votes.findAllByUserIdIn(List.of())).isEmpty();
        assertThat(votes.findAllByMeetingIdIn(List.of(meeting.getId(), Long.MAX_VALUE))).hasSize(16);
    }

    @Test void meetingQueriesPreserveHostOwnershipActiveStateAndLookupBoundaries() {
        var active = meetings.save(meeting(1L));
        var inactiveValue = meeting(1L); ReflectionTestUtils.setField(inactiveValue, "isActive", false);
        var inactive = meetings.save(inactiveValue);
        meetings.save(meeting(2L));
        assertThat(meetings.existsByMeetingCode(active.getMeetingCode())).isTrue();
        assertThat(meetings.existsByMeetingCode("mtg_missing")).isFalse();
        assertThat(meetings.findByHostUserId(1L)).hasSize(2);
        assertThat(meetings.findByHostUserIdOrderByCreatedAtDesc(1L)).extracting(Meeting::getId).containsExactly(inactive.getId(), active.getId());
        assertThat(meetings.findByHostUserIdAndIsActiveTrue(1L)).extracting(Meeting::getId).containsExactly(active.getId());
        assertThat(meetings.findAllByIdIn(List.of(inactive.getId(), active.getId(), Long.MAX_VALUE)))
                .extracting(Meeting::getId).containsExactly(inactive.getId(), active.getId());
        assertThat(meetings.findAllByIdInAndIsActiveTrue(List.of(active.getId(), inactive.getId())))
                .extracting(Meeting::getId).containsExactly(active.getId());
        assertThat(meetings.findByTitleContainingIgnoreCaseOrMeetingCodeContainingIgnoreCase("contract", "contract", PageRequest.of(0, 10)).getTotalElements()).isEqualTo(3);
        assertThat(meetings.findAll(PageRequest.of(0, 2)).getTotalElements()).isEqualTo(3);
        assertThat(meetings.countByIsActiveTrue()).isEqualTo(2);
    }

    Meeting meeting(Long host) {
        return Meeting.create("mtg_" + UUID.randomUUID().toString().replace("-", ""), "Contract", "", host,
                "UTC", SelectionType.TIME, 60, Map.of("2027-01-04", new int[]{9, 10}), false);
    }
}
