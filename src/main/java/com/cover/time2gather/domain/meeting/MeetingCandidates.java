package com.cover.time2gather.domain.meeting;

import com.cover.time2gather.domain.exception.BusinessException;
import com.cover.time2gather.domain.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Shared candidate-edit rules; persistence adapters commit the resulting plan atomically. */
public final class MeetingCandidates {
    private MeetingCandidates() { }
    public record Location(Long id, String name) { }
    public record Edit(List<String> dates, List<String> times, List<Location> locations, String expectedVersion) { }
    public record Plan(Meeting meeting, List<MeetingLocation> locations, Set<Long> removedLocationIds,
                       List<MeetingUserSelection> savedSelections, List<MeetingUserSelection> deletedSelections) { }

    public static String version(Meeting meeting, List<Location> locations) {
        var value = new StringBuilder(meeting.getSelectionType().name()).append('|').append(meeting.getIntervalMinutes())
                .append('|').append(meeting.getTimezone()).append('|').append(meeting.isLocationVoteEnabled());
        new TreeMap<>(meeting.getAvailableDates()).forEach((date, slots) -> value.append('|').append(date).append(':')
                .append(Arrays.toString(slots == null ? new int[0] : Arrays.stream(slots).sorted().toArray())));
        for (var place : locations) value.append('|').append(place.id()).append(':').append(place.name().length()).append(':').append(place.name());
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.toString().getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    public static List<Location> values(List<MeetingLocation> locations) {
        return locations.stream().sorted(Comparator.comparing(MeetingLocation::getDisplayOrder).thenComparing(MeetingLocation::getId))
                .map(p -> new Location(p.getId(), p.getName())).toList();
    }
    public static Plan plan(Meeting meeting, Long user, Edit edit, List<MeetingLocation> original,
                            List<MeetingUserSelection> selections) {
        if (!meeting.getHostUserId().equals(user)) throw new BusinessException(ErrorCode.MEETING_HOST_ONLY);
        if (meeting.isConfirmed() || meeting.isLocationConfirmed()) throw new BusinessException(ErrorCode.MEETING_ALREADY_CONFIRMED);
        if (!Objects.equals(version(meeting, values(original)), edit.expectedVersion())) {
            throw new BusinessException(ErrorCode.SELECTION_CANDIDATES_CHANGED);
        }
        var places = new ArrayList<MeetingLocation>();
        var removed = new HashSet<Long>();
        var originals = new HashMap<Long,MeetingLocation>();
        original.forEach(p -> { originals.put(p.getId(),p); removed.add(p.getId()); });
        if (edit.locations() == null) {
            places.addAll(original); removed.clear();
        } else {
            if (!meeting.isLocationVoteEnabled()) throw new BusinessException(ErrorCode.LOCATION_VOTE_NOT_ENABLED);
            if (edit.locations().size() < 2) throw new BusinessException(ErrorCode.LOCATION_MIN_REQUIRED,2);
            if (edit.locations().size() > 5) throw new BusinessException(ErrorCode.LOCATION_MAX_EXCEEDED,5);
            var seen = new HashSet<Long>();
            for (int i=0;i<edit.locations().size();i++) {
                var input=edit.locations().get(i);
                if (input == null) throw new BusinessException(ErrorCode.INVALID_INPUT);
                var validated = MeetingLocation.create(meeting.getId(),input.name(),i);
                if (input.id() != null) {
                    if (!seen.add(input.id())) throw new BusinessException(ErrorCode.INVALID_INPUT);
                    var old=originals.get(input.id());
                    if (old == null) throw new BusinessException(ErrorCode.LOCATION_NOT_BELONG_TO_MEETING);
                    if (old.getName().equals(validated.getName())) {
                        old.updateDisplayOrder(i); places.add(old); removed.remove(old.getId()); continue;
                    }
                }
                // Renaming proposes a new option: old ballots and stale option IDs cannot carry over.
                places.add(validated);
            }
        }
        var previous = meeting.getAvailableDates();
        meeting.updateAvailableDates(edit.dates(),edit.times());
        var candidates = meeting.getAvailableDates();
        var saved = new ArrayList<MeetingUserSelection>();
        var deleted = new ArrayList<MeetingUserSelection>();
        for (var selection : selections) {
            if (selection.getUnavailableDates()!=null) {
                selection.markUnavailable(selection.getUnavailableDates().stream()
                        .filter(d -> previous.containsKey(d) && candidates.containsKey(d))
                        .filter(d -> Arrays.stream(candidates.get(d)).allMatch(slot -> Arrays.stream(previous.get(d)).anyMatch(old -> old==slot))).toList());
                saved.add(selection); continue;
            }
            var retained = new LinkedHashMap<String,int[]>();
            selection.getSelections().forEach((date,slots) -> {
                if (!candidates.containsKey(date)) return;
                int[] kept = meeting.getSelectionType()==SelectionType.ALL_DAY ? new int[0]
                        : Arrays.stream(slots).filter(slot -> Arrays.stream(candidates.get(date)).anyMatch(v -> v==slot)).toArray();
                if (meeting.getSelectionType()==SelectionType.ALL_DAY || kept.length>0) retained.put(date,kept);
            });
            if (retained.isEmpty()) deleted.add(selection);
            else { selection.updateSelections(retained); saved.add(selection); }
        }
        return new Plan(meeting,places,removed,saved,deleted);
    }
}
