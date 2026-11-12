package com.cover.time2gather.domain.meeting;

import com.cover.time2gather.domain.exception.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class MeetingCandidatesTest {
    static final String DAY="2027-01-04";
    Meeting meeting(boolean enabled) { return Meeting.create("mtg_candidates", "Candidates", "", 1L,"Asia/Seoul",SelectionType.TIME,60,Map.of(DAY,new int[]{9,10}),enabled); }
    MeetingLocation place(long id,String name,int order) {
        var p=MeetingLocation.create(1L,name,order);ReflectionTestUtils.setField(p,"id",id);return p;
    }
    MeetingCandidates.Edit edit(Meeting m,List<MeetingLocation> old,List<MeetingCandidates.Location> places,List<String> times) {
        return new MeetingCandidates.Edit(List.of(DAY),times,places,MeetingCandidates.version(m,MeetingCandidates.values(old)));
    }
    void error(Runnable action,ErrorCode code) {assertThatThrownBy(action::run).isInstanceOfSatisfying(BusinessException.class,e->assertThat(e.getErrorCode()).isEqualTo(code));}
    @Test void guardsHostConfirmationAndStaleVersions() {
        var m=meeting(true);var command=edit(m,List.of(),null,null);
        error(()->MeetingCandidates.plan(m,2L,command,List.of(),List.of()),ErrorCode.MEETING_HOST_ONLY);
        error(()->MeetingCandidates.plan(m,1L,new MeetingCandidates.Edit(List.of(DAY),null,null,"stale"),List.of(),List.of()),ErrorCode.SELECTION_CANDIDATES_CHANGED);
        m.confirm(LocalDate.parse(DAY),9);
        error(()->MeetingCandidates.plan(m,1L,command,List.of(),List.of()),ErrorCode.MEETING_ALREADY_CONFIRMED);
        m.cancelConfirmation();m.confirmLocation(1L);
        error(()->MeetingCandidates.plan(m,1L,command,List.of(),List.of()),ErrorCode.MEETING_ALREADY_CONFIRMED);
    }
    @Test void locationValidationDoesNotAcceptDisabledOrInvalidCandidateSets() {
        var disabled=meeting(false);var two=List.of(new MeetingCandidates.Location(null,"A"),new MeetingCandidates.Location(null,"B"));
        error(()->MeetingCandidates.plan(disabled,1L,edit(disabled,List.of(),two,null),List.of(),List.of()),ErrorCode.LOCATION_VOTE_NOT_ENABLED);
        var m=meeting(true);var old=List.of(place(1,"A",0),place(2,"B",1));
        error(()->MeetingCandidates.plan(m,1L,edit(m,old,List.of(two.getFirst()),null),old,List.of()),ErrorCode.LOCATION_MIN_REQUIRED);
        error(()->MeetingCandidates.plan(m,1L,edit(m,old,Collections.nCopies(6,two.getFirst()),null),old,List.of()),ErrorCode.LOCATION_MAX_EXCEEDED);
        var duplicate=List.of(new MeetingCandidates.Location(1L,"A"),new MeetingCandidates.Location(1L,"A"));
        error(()->MeetingCandidates.plan(m,1L,edit(m,old,duplicate,null),old,List.of()),ErrorCode.INVALID_INPUT);
        var unknown=List.of(new MeetingCandidates.Location(99L,"X"),two.getFirst());
        error(()->MeetingCandidates.plan(m,1L,edit(m,old,unknown,null),old,List.of()),ErrorCode.LOCATION_NOT_BELONG_TO_MEETING);
        error(()->MeetingCandidates.plan(m,1L,edit(m,old,Arrays.asList(null,two.getFirst()),null),old,List.of()),ErrorCode.INVALID_INPUT);
        error(()->MeetingCandidates.plan(m,1L,edit(m,old,List.of(new MeetingCandidates.Location(1L," "),two.getFirst()),null),old,List.of()),ErrorCode.LOCATION_NAME_REQUIRED);
    }
    @Test void reorderingAndWhitespacePreserveIdentityButRenamingReplacesIt() {
        var m=meeting(true);var old=List.of(place(1,"A",0),place(2,"B",1),place(3,"C",2));
        var next=List.of(new MeetingCandidates.Location(2L," B "),new MeetingCandidates.Location(1L,"A renamed"));
        var p=MeetingCandidates.plan(m,1L,edit(m,old,next,null),old,List.of());
        assertThat(p.locations().getFirst().getId()).isEqualTo(2L);assertThat(p.locations().getFirst().getDisplayOrder()).isZero();
        assertThat(p.locations().getLast().getId()).isNull();assertThat(p.removedLocationIds()).containsExactlyInAnyOrder(1L,3L);
    }
    @Test void negativeResponsesNeedRenewedConsentWhenCandidatesExpand() {
        var m=meeting(false);var none=MeetingUserSelection.unavailable(m,2L,List.of(DAY));
        var plan=MeetingCandidates.plan(m,1L,edit(m,List.of(),null,List.of("09:00","10:00","11:00")),List.of(),List.of(none));
        assertThat(plan.savedSelections()).containsExactly(none);assertThat(none.availabilityResponse(m)).isEqualTo("NEEDS_RESPONSE");
        var m2=meeting(false);var none2=MeetingUserSelection.unavailable(m2,2L,List.of(DAY,"2027-01-05"));
        MeetingCandidates.plan(m2,1L,edit(m2,List.of(),null,List.of("10:00")),List.of(),List.of(none2));
        assertThat(none2.getUnavailableDates()).containsExactly(DAY);assertThat(none2.availabilityResponse(m2)).isEqualTo("NONE");
    }
    @Test void ballotsWithOnlyRemovedSlotsAreDeletedAndPerDateSparseSlotsRemain() {
        var m=meeting(false);var ballot=MeetingUserSelection.create(1L,2L,Map.of(DAY,new int[]{9}));
        var plan=MeetingCandidates.plan(m,1L,edit(m,List.of(),null,List.of("10:00")),List.of(),List.of(ballot));
        assertThat(plan.deletedSelections()).containsExactly(ballot);assertThat(plan.savedSelections()).isEmpty();
        var sparse=meeting(false);var before=MeetingCandidates.version(sparse,List.of());
        var unchanged=MeetingCandidates.plan(sparse,1L,edit(sparse,List.of(),null,null),List.of(),List.of());
        assertThat(unchanged.meeting().getAvailableDates().get(DAY)).containsExactly(9,10);
        assertThat(MeetingCandidates.version(sparse,List.of())).isEqualTo(before);
    }
}
