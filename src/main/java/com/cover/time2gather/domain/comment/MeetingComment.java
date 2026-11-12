package com.cover.time2gather.domain.comment;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "meeting_comments", indexes = @Index(name = "idx_comment_meeting", columnList = "meeting_id"))
@Getter
@NoArgsConstructor
public class MeetingComment {
    @Id @Column(length = 36) private String id;
    @Column(name = "meeting_id", nullable = false) private Long meetingId;
    @Column(nullable = false) private Long authorId;
    @Column(length = 36) private String rootId;
    @Column(length = 36) private String replyToId;
    @Column(nullable = false, length = 1000) private String body;
    @Type(JsonType.class) @Column(columnDefinition = "json") private List<Mention> mentions;
    @Type(JsonType.class) @Column(columnDefinition = "json") private Map<String, List<Long>> reactions = Map.of();
    private Long activityAt;
    @Column(nullable = false) private long createdAt;
    @Column(nullable = false) private long updatedAt;
    @Column(nullable = false) private boolean edited;
    @Column(nullable = false, length = 12) private String status;
    @Column(nullable = false, length = 36) private String requestId;
    public record Mention(String userId, int start, int end) { }

    public static MeetingComment create(Long meetingId, Long authorId, String rootId, String replyToId,
                                         String body, List<Mention> mentions, String requestId, long now) {
        MeetingComment c = new MeetingComment();
        c.id = UUID.randomUUID().toString(); c.meetingId = meetingId; c.authorId = authorId;
        c.rootId = rootId; c.replyToId = replyToId; c.body = body; c.mentions = List.copyOf(mentions);
        c.requestId = requestId; c.createdAt = now; c.updatedAt = now; c.activityAt = now; c.status = "ACTIVE";
        return c;
    }
    public boolean active() { return "ACTIVE".equals(status); }
    public Map<String, List<Long>> getReactions() { return reactions == null ? Map.of() : reactions; }
    public long getActivityAt() { return activityAt == null ? updatedAt : activityAt; }
    public void react(String emoji, Long actor, boolean selected, long now) {
        List<Long> existing = getReactions().getOrDefault(emoji, List.of());
        if (existing.contains(actor) == selected) return;
        Map<String, List<Long>> next = new LinkedHashMap<>(getReactions());
        List<Long> actors = new ArrayList<>(existing);
        if (selected) actors.add(actor); else actors.remove(actor);
        if (actors.isEmpty()) next.remove(emoji); else next.put(emoji, List.copyOf(actors));
        // Reactions refresh the conversation without resurfacing old mentions/replies.
        activityAt = getActivityAt();
        reactions = Map.copyOf(next); updatedAt = now;
    }
    public void edit(String body, List<Mention> mentions, long now) {
        this.body = body; this.mentions = List.copyOf(mentions); this.updatedAt = now; this.activityAt = now; this.edited = true;
    }
    public void remove(boolean hidden, long now) {
        status = hidden ? "HIDDEN" : "DELETED"; body = ""; mentions = List.of(); reactions = Map.of(); updatedAt = now; activityAt = now;
    }
}
