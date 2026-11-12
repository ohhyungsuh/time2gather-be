package com.cover.time2gather.domain.comment;

import com.cover.time2gather.domain.exception.BusinessException;
import com.cover.time2gather.domain.exception.ErrorCode;
import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.user.User;
import com.cover.time2gather.domain.user.UserRepository;
import com.cover.time2gather.infra.comment.CommentStore;
import com.cover.time2gather.infra.meeting.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;

@Service @RequiredArgsConstructor
public class CommentService {
    private static final List<String> REACTIONS = List.of("👍", "❤️", "😂", "🎉", "🙏", "🤔", "👀", "💯");
    private final CommentStore store;
    private final com.cover.time2gather.infra.identity.MeetingNameStore names;
    private final MeetingRepository meetings;
    private final UserRepository users;
    private final MeetingUserSelectionRepository selections;
    private final MeetingLocationSelectionRepository locationSelections;
    public record Person(String id, String name, String avatar, boolean host) { }
    public record View(String id, Person author, String rootId, String replyToId, Person replyTo,
                       String body, List<MeetingComment.Mention> mentions, long createdAt, boolean edited,
                       String status, long replyCount, boolean canEdit, boolean canDelete, boolean canHide,
                       List<Reaction> reactions) { }
    public record Reaction(String emoji, int count, boolean reacted, List<Person> people) { }
    public record ReactionWrite(String emoji, Boolean active) { }
    public record Page(List<View> items, String nextCursor) { }
    public record Activity(String id, String rootId, boolean mentioned) { }
    public record Summary(long version, long count, String viewerId, boolean writable, long namesVersion) { }
    public record Write(String body, List<MeetingComment.Mention> mentions, String replyToId, String requestId) { }

    private Meeting meeting(String code) {
        Meeting m = meetings.findByMeetingCode(code).orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND));
        if (!Boolean.TRUE.equals(m.getIsActive())) throw new BusinessException(ErrorCode.MEETING_NOT_FOUND);
        return m;
    }
    private void authenticated(Long actor) {
        if (actor == null) throw new BusinessException(ErrorCode.AUTH_REQUIRED);
        if (!users.existsById(actor)) throw new BusinessException(ErrorCode.AUTH_REQUIRED);
    }
    private MeetingComment find(List<MeetingComment> all, String id) {
        return all.stream().filter(c -> c.getId().equals(id)).findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }
    private Set<Long> peopleIds(Meeting m, List<MeetingComment> all) {
        Set<Long> ids = new LinkedHashSet<>(); ids.add(m.getHostUserId());
        selections.findAllByMeetingId(m.getId()).forEach(s -> ids.add(s.getUserId()));
        locationSelections.selectByMeetingId(m.getId()).forEach(s -> ids.add(s.getUserId()));
        all.forEach(c -> ids.add(c.getAuthorId()));
        return ids;
    }
    private Map<Long, Person> people(Meeting m, Collection<Long> ids) {
        Map<Long, Person> result = new LinkedHashMap<>();
        Map<Long, String> displayNames = names.names(m.getId());
        for (User u : users.findAllById(ids)) result.put(u.getId(), new Person(u.getId().toString(), displayNames.getOrDefault(u.getId(), u.getUsername()),
                u.getProfileImageUrl(), u.getId().equals(m.getHostUserId())));
        return result;
    }
    private Map<Long, Person> commentPeople(Meeting m, List<MeetingComment> all) {
        Set<Long> ids = new LinkedHashSet<>();
        all.forEach(c -> {
            ids.add(c.getAuthorId());
            if (c.active()) c.getReactions().values().forEach(ids::addAll);
        });
        return people(m, ids);
    }
    public List<Person> candidates(String code) {
        Meeting m = meeting(code);
        return List.copyOf(people(m, peopleIds(m, store.list(m.getId()))).values());
    }
    public Summary summary(String code, Long actor) {
        Meeting m = meeting(code);
        CommentStore.Stats stats = store.stats(m.getId());
        return new Summary(stats.version(), stats.count(), actor == null ? null : actor.toString(), true, names.version(m.getId()));
    }
    public List<Activity> activity(String code, Long actor, long after) {
        Meeting m = meeting(code);
        List<MeetingComment> all = store.list(m.getId());
        return all.stream().filter(c -> c.active() && c.getActivityAt() > after && !c.getAuthorId().equals(actor))
                .sorted(Comparator.comparingLong(MeetingComment::getActivityAt).reversed())
                .map(c -> new Activity(c.getId(), c.getRootId(), actor != null &&
                        (c.getMentions().stream().anyMatch(v -> v.userId().equals(actor.toString())) ||
                        c.getReplyToId() != null && find(all, c.getReplyToId()).getAuthorId().equals(actor))))
                .toList();
    }
    private View view(MeetingComment c, List<MeetingComment> all, Map<Long, Person> people, Meeting m, Long actor) {
        long replies = all.stream().filter(r -> c.getId().equals(r.getRootId()) && r.active()).count();
        Person target = c.getReplyToId() == null ? null : people.get(find(all, c.getReplyToId()).getAuthorId());
        boolean owner = c.getAuthorId().equals(actor);
        List<Reaction> reactions = c.active() ? REACTIONS.stream().filter(c.getReactions()::containsKey).map(emoji -> {
            List<Person> reactors = c.getReactions().get(emoji).stream().distinct().map(people::get).filter(Objects::nonNull).toList();
            return new Reaction(emoji, reactors.size(), reactors.stream().anyMatch(p -> p.id().equals(String.valueOf(actor))), reactors);
        }).filter(r -> r.count() > 0).toList() : List.of();
        return new View(c.getId(), people.get(c.getAuthorId()), c.getRootId(), c.getReplyToId(), target,
                c.active() ? c.getBody() : "", c.active() ? c.getMentions() : List.of(), c.getCreatedAt(), c.isEdited(),
                c.getStatus(), replies, c.active() && owner, c.active() && owner,
                c.active() && !owner && m.getHostUserId().equals(actor), reactions);
    }
    public Page page(String code, Long actor, String rootId, String cursor) {
        Meeting m = meeting(code); List<MeetingComment> all = store.list(m.getId());
        if (rootId != null && find(all, rootId).getRootId() != null) throw new BusinessException(ErrorCode.INVALID_INPUT);
        Set<String> liveRoots = new HashSet<>();
        all.stream().filter(MeetingComment::active).map(MeetingComment::getRootId).filter(Objects::nonNull).forEach(liveRoots::add);
        Comparator<MeetingComment> order = Comparator.comparingLong(MeetingComment::getCreatedAt).thenComparing(MeetingComment::getId);
        if (rootId == null) order = order.reversed();
        final Comparator<MeetingComment> comparator = order;
        MeetingComment boundary = cursor == null ? null : find(all, cursor);
        if (boundary != null && !Objects.equals(boundary.getRootId(), rootId)) throw new BusinessException(ErrorCode.INVALID_INPUT);
        List<MeetingComment> selected = all.stream().filter(c -> Objects.equals(c.getRootId(), rootId))
                .filter(c -> c.active() || rootId != null || liveRoots.contains(c.getId()))
                .filter(c -> boundary == null || comparator.compare(c, boundary) > 0).sorted(order).limit(rootId == null ? 11 : 21).toList();
        int limit = rootId == null ? 10 : 20;
        List<MeetingComment> slice = selected.subList(0, Math.min(limit, selected.size()));
        Map<Long, Person> people = commentPeople(m, all);
        return new Page(slice.stream().map(c -> view(c, all, people, m, actor)).toList(),
                selected.size() > limit ? slice.getLast().getId() : null);
    }
    public View one(String code, Long actor, String id) {
        Meeting m = meeting(code); List<MeetingComment> all = store.list(m.getId());
        return view(find(all, id), all, commentPeople(m, all), m, actor);
    }
    public View react(String code, Long actor, String id, ReactionWrite input) {
        authenticated(actor); Meeting m = meeting(code);
        if (input == null || input.emoji() == null || !REACTIONS.contains(input.emoji()) || input.active() == null)
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        store.mutate(m.getId(), all -> {
            MeetingComment c = find(all, id);
            if (!c.active()) throw new BusinessException(ErrorCode.INVALID_STATE);
            c.react(input.emoji(), actor, input.active(), nextTime(all));
            return c;
        });
        User user = users.findById(actor).orElseThrow(() -> new BusinessException(ErrorCode.AUTH_REQUIRED));
        names.save(m.getId(), code, actor, user.getUsername(), true);
        return one(code, actor, id);
    }
    private List<MeetingComment.Mention> validate(Write input, Set<Long> allowed) {
        if (input.body() == null || input.body().isBlank() || input.body().length() > 1000)
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        List<MeetingComment.Mention> mentions = input.mentions() == null ? List.of() : input.mentions();
        if (mentions.size() > 20) throw new BusinessException(ErrorCode.INVALID_INPUT);
        int end = 0;
        Set<String> allowedStrings = new HashSet<>(); allowed.forEach(id -> allowedStrings.add(id.toString()));
        for (var mention : mentions) {
            if (mention == null || !allowedStrings.contains(mention.userId()) || mention.start() < end ||
                    mention.end() <= mention.start() + 1 || mention.end() > input.body().length() ||
                    input.body().charAt(mention.start()) != '@') throw new BusinessException(ErrorCode.INVALID_INPUT);
            end = mention.end();
        }
        return mentions;
    }
    private long nextTime(List<MeetingComment> all) {
        return Math.max(System.currentTimeMillis(), all.stream().mapToLong(MeetingComment::getUpdatedAt).max().orElse(0) + 1);
    }
    public View create(String code, Long actor, Write input) {
        authenticated(actor); Meeting m = meeting(code);
        if (input.requestId() == null || !input.requestId().matches("[a-fA-F0-9-]{36}")) throw new BusinessException(ErrorCode.INVALID_INPUT);
        // Participant queries are outside the storage transaction; comment authors are added inside it.
        Set<Long> allowed = peopleIds(m, List.of());
        MeetingComment result = store.mutate(m.getId(), all -> {
            var existing = all.stream().filter(c -> c.getAuthorId().equals(actor) && c.getRequestId().equals(input.requestId())).findFirst();
            if (existing.isPresent()) return existing.get();
            long now = nextTime(all);
            if (all.stream().anyMatch(c -> c.getAuthorId().equals(actor) && c.getCreatedAt() > now - 3000))
                throw new BusinessException(ErrorCode.COMMENT_RATE_LIMIT);
            Set<Long> ids = new HashSet<>(allowed); all.forEach(c -> ids.add(c.getAuthorId()));
            List<MeetingComment.Mention> mentions = validate(input, ids);
            MeetingComment target = input.replyToId() == null ? null : find(all, input.replyToId());
            if (target != null && !target.active()) throw new BusinessException(ErrorCode.INVALID_STATE);
            String root = target == null ? null : target.getRootId() == null ? target.getId() : target.getRootId();
            return MeetingComment.create(m.getId(), actor, root, input.replyToId(), input.body(), mentions, input.requestId(), now);
        });
        User author = users.findById(actor).orElseThrow(() -> new BusinessException(ErrorCode.AUTH_REQUIRED));
        names.save(m.getId(), code, actor, author.getUsername(), true);
        return one(code, actor, result.getId());
    }
    public View edit(String code, Long actor, String id, Write input) {
        authenticated(actor); Meeting m = meeting(code); Set<Long> allowed = peopleIds(m, List.of());
        store.mutate(m.getId(), all -> {
            MeetingComment c = find(all, id);
            if (!c.getAuthorId().equals(actor)) throw new BusinessException(ErrorCode.ACCESS_DENIED);
            if (!c.active()) throw new BusinessException(ErrorCode.INVALID_STATE);
            Set<Long> ids = new HashSet<>(allowed); all.forEach(v -> ids.add(v.getAuthorId()));
            c.edit(input.body(), validate(input, ids), nextTime(all)); return c;
        });
        return one(code, actor, id);
    }
    public void remove(String code, Long actor, String id, boolean hidden) {
        authenticated(actor); Meeting m = meeting(code);
        store.mutate(m.getId(), all -> {
            MeetingComment c = find(all, id);
            if (hidden ? !m.getHostUserId().equals(actor) : !c.getAuthorId().equals(actor))
                throw new BusinessException(ErrorCode.ACCESS_DENIED);
            if (c.active()) c.remove(hidden, nextTime(all)); return c;
        });
    }
}
