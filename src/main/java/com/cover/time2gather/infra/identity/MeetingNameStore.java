package com.cover.time2gather.infra.identity;

import java.util.Map;
import java.util.Optional;

/** Display names never grant membership or permissions. Keys are stable user IDs. */
public interface MeetingNameStore {
    Optional<String> name(Long meetingId, Long userId);
    Map<Long, String> names(Long meetingId);
    long version(Long meetingId);
    String save(Long meetingId, String meetingCode, Long userId, String name, boolean onlyIfAbsent);
}
