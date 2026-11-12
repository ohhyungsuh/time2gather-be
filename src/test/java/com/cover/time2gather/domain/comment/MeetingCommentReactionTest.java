package com.cover.time2gather.domain.comment;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class MeetingCommentReactionTest {
    final ObjectMapper json = new ObjectMapper().setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

    @Test void legacyCommentsWithoutReactionFieldsKeepTheirActivityTimeAndRoundTripExactUserIds() throws Exception {
        MeetingComment legacy=json.readValue("""
                {"id":"legacy","meetingId":1,"authorId":2,"body":"Hello","mentions":[],
                 "createdAt":100,"updatedAt":200,"edited":true,"status":"ACTIVE","requestId":"legacy"}
                """,MeetingComment.class);
        assertThat(legacy.getReactions()).isEmpty();assertThat(legacy.getActivityAt()).isEqualTo(200);
        legacy.react("👍",9007199254740993L,true,300);
        legacy.react("❤️",4L,true,400);
        MeetingComment stored=json.readValue(json.writeValueAsBytes(legacy),MeetingComment.class);
        assertThat(stored.getReactions().get("👍")).containsExactly(9007199254740993L);
        assertThat(stored.getReactions().get("❤️")).containsExactly(4L);
        assertThat(stored.getActivityAt()).isEqualTo(200);assertThat(stored.getUpdatedAt()).isEqualTo(400);
        stored.react("👍",9007199254740993L,true,500);
        assertThat(stored.getUpdatedAt()).isEqualTo(400);
        stored.edit("Changed",List.of(),600);
        assertThat(stored.getActivityAt()).isEqualTo(600);
        stored.remove(true,700);assertThat(stored.getReactions()).isEmpty();
    }
}
