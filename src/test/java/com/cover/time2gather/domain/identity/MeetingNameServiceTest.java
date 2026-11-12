package com.cover.time2gather.domain.identity;

import com.cover.time2gather.domain.exception.*;
import com.cover.time2gather.domain.meeting.*;
import com.cover.time2gather.domain.user.User;
import com.cover.time2gather.infra.identity.MeetingNameStore;
import com.cover.time2gather.infra.meeting.MeetingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingNameServiceTest {
    @Mock MeetingNameStore names;
    @Mock MeetingRepository meetings;
    @InjectMocks MeetingNameService service;

    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {" ", "\t\n"})
    void rejectsMissingNames(String value) {
        assertThatThrownBy(() -> MeetingNameService.validate(value)).isInstanceOf(BusinessException.class);
    }
    @Test void validatesTheMaximumAndPreservesUnicodeWhenTrimming() {
        assertThat(MeetingNameService.validate("  민수  ")).isEqualTo("민수");
        assertThat(MeetingNameService.validate("가".repeat(100))).hasSize(100);
        assertThatThrownBy(() -> MeetingNameService.validate("가".repeat(101))).isInstanceOf(BusinessException.class);
    }
    @Test void missingAndInactiveMeetingsNeverPersistNames() {
        User user = mock(User.class);
        assertThatThrownBy(() -> service.save("mtg_missing", user, "name", false)).isInstanceOf(BusinessException.class);
        var inactive = Meeting.create("mtg_inactive", "Inactive", "", 1L, "UTC", SelectionType.ALL_DAY,
                60, Map.of("2027-01-04", new int[0]), false);
        ReflectionTestUtils.setField(inactive, "isActive", false);
        when(meetings.findByMeetingCode("mtg_inactive")).thenReturn(Optional.of(inactive));
        assertThatThrownBy(() -> service.get("mtg_inactive", user)).isInstanceOf(BusinessException.class);
        ReflectionTestUtils.setField(inactive, "isActive", null);
        assertThatThrownBy(() -> service.save("mtg_inactive", user, "name", false)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(names);
    }
}
