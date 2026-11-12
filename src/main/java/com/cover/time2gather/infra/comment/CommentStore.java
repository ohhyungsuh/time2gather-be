package com.cover.time2gather.infra.comment;

import com.cover.time2gather.domain.comment.MeetingComment;
import java.util.List;
import java.util.function.Function;

/** Writes serialize per meeting, including request deduplication and throttling. */
public interface CommentStore {
    record Stats(long version, long count) { }
    Stats stats(Long meetingId);
    List<MeetingComment> list(Long meetingId);
    MeetingComment mutate(Long meetingId, Function<List<MeetingComment>, MeetingComment> mutation);
}
