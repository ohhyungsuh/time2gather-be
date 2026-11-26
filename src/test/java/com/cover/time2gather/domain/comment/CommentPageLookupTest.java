package com.cover.time2gather.domain.comment;

import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.user.UserRepository;
import com.cover.time2gather.infra.comment.CommentStore;
import com.cover.time2gather.infra.identity.MeetingNameStore;
import com.cover.time2gather.infra.meeting.MeetingLocationSelectionRepository;
import com.cover.time2gather.infra.meeting.MeetingRepository;
import com.cover.time2gather.infra.meeting.MeetingUserSelectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CommentPageLookupTest {
    private final CommentStore store = mock(CommentStore.class);
    private final MeetingNameStore names = mock(MeetingNameStore.class);
    private final MeetingRepository meetings = mock(MeetingRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final CommentService service = new CommentService(store, names, meetings, users,
            mock(MeetingUserSelectionRepository.class), mock(MeetingLocationSelectionRepository.class));

    @BeforeEach
    void setUp() {
        Meeting meeting = mock(Meeting.class);
        when(meeting.getId()).thenReturn(1L);
        when(meeting.getIsActive()).thenReturn(true);
        when(meetings.findByMeetingCode("test")).thenReturn(Optional.of(meeting));
    }

    @Test
    void firstPageReadsProfilesOnlyForReturnedCommentsAndTheirReactions() {
        List<MeetingComment> comments = LongStream.rangeClosed(1, 12).mapToObj(id -> comment(id, null, null)).toList();
        comments.getLast().react("👍", 20L, true, 13);
        when(store.list(1L)).thenReturn(comments);

        var page = service.page("test", null, null, null);

        assertThat(page.items()).extracting(CommentService.View::id)
                .containsExactlyElementsOf(comments.reversed().subList(0, 10).stream().map(MeetingComment::getId).toList());
        assertThat(page.nextCursor()).isEqualTo(comments.get(2).getId());
        verify(users).findAllById(argThat(ids -> {
            assertThat(ids).containsExactlyInAnyOrder(3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L, 20L);
            return true;
        }));
    }

    @Test
    void replyPageReadsTargetAuthorEvenWhenTargetIsOutsidePage() {
        MeetingComment root = comment(1, null, null);
        MeetingComment earlyReply = comment(2, root.getId(), root.getId());
        MeetingComment later = comment(3, root.getId(), earlyReply.getId());
        when(store.list(1L)).thenReturn(List.of(root, earlyReply, later));

        var page = service.page("test", null, root.getId(), earlyReply.getId());

        assertThat(page.items()).extracting(CommentService.View::id).containsExactly(later.getId());
        verify(users).findAllById(argThat(ids -> {
            assertThat(ids).containsExactlyInAnyOrder(2L, 3L);
            return true;
        }));
    }

    @Test
    void emptyPageSkipsNameAndProfileReads() {
        when(store.list(1L)).thenReturn(List.of());

        assertThat(service.page("test", null, null, null).items()).isEmpty();

        verifyNoInteractions(names, users);
    }

    private MeetingComment comment(long authorId, String root, String replyTo) {
        return MeetingComment.create(1L, authorId, root, replyTo, "Hello", List.of(), "request", authorId);
    }
}
