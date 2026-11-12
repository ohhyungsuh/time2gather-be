package com.cover.time2gather.integration;

import com.cover.time2gather.domain.comment.MeetingComment;
import com.cover.time2gather.infra.comment.CommentStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class CommentIntegrationTest extends com.cover.time2gather.support.FirestoreIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired CommentStore store;
    record Session(Cookie cookie, String id) { }
    record Fixture(Session host, String code, long meetingId) { }
    Session login(String name) throws Exception {
        var result = mvc.perform(post("/api/v1/auth/anonymous").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("username", name)))).andExpect(status().isOk()).andReturn();
        return new Session(result.getResponse().getCookie("__session"), data(result).get("userId").asText());
    }
    Fixture fixture() throws Exception {
        Session host = login("민수");
        JsonNode m = data(mvc.perform(post("/api/v1/meetings").cookie(host.cookie()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"코멘트 테스트","timezone":"Asia/Seoul","selectionType":"ALL_DAY","availableDates":{"2026-12-01":[]}}
                        """)).andExpect(status().isOk()).andReturn());
        return new Fixture(host, m.get("meetingCode").asText(), m.get("meetingId").asLong());
    }
    String path(Fixture f) { return "/api/v1/meetings/" + f.code() + "/comments"; }
    Map<String,Object> body(String text) {
        var value = new HashMap<String,Object>(); value.put("body", text); value.put("mentions", List.of()); value.put("requestId", UUID.randomUUID().toString()); return value;
    }
    JsonNode create(Fixture f, Session s, Map<String,Object> body) throws Exception {
        return data(mvc.perform(post(path(f)).cookie(s.cookie()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body))).andExpect(status().isOk()).andReturn());
    }
    JsonNode data(MvcResult result) throws Exception { return json.readTree(result.getResponse().getContentAsByteArray()).get("data"); }

    @Test void anonymousReadAuthenticatedWriteAndIdempotency() throws Exception {
        Fixture f = fixture();
        mvc.perform(get(path(f))).andExpect(status().isOk());
        mvc.perform(post(path(f)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body("hello"))))
                .andExpect(status().isUnauthorized());
        Map<String,Object> request = body("hello");
        JsonNode first = create(f, f.host(), request);
        assertThat(create(f, f.host(), request).get("id")).isEqualTo(first.get("id"));
        mvc.perform(post(path(f)).cookie(f.host().cookie()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body("again"))))
                .andExpect(status().isTooManyRequests());
        mvc.perform(get(path(f)+"/summary")).andExpect(jsonPath("$.data.count").value(1));
    }

    @Test void repliesStayAtOneLevelAndDeletedRootPreservesConversation() throws Exception {
        Fixture f = fixture(); Session guest = login("지수"); Session third = login("지수");
        JsonNode root = create(f, f.host(), body("주차?"));
        var replyBody = body("가능해요"); replyBody.put("replyToId", root.get("id").asText());
        JsonNode reply = create(f, guest, replyBody);
        var nested = body("무료인가요?"); nested.put("replyToId", reply.get("id").asText());
        JsonNode reply2 = create(f, third, nested);
        assertThat(reply2.get("rootId").asText()).isEqualTo(root.get("id").asText());
        assertThat(reply2.get("replyTo").get("id").asText()).isEqualTo(guest.id());
        mvc.perform(delete(path(f)+"/"+root.get("id").asText()).cookie(f.host().cookie())).andExpect(status().isOk());
        mvc.perform(get(path(f))).andExpect(jsonPath("$.data.items[0].body").value(""))
                .andExpect(jsonPath("$.data.items[0].status").value("DELETED"))
                .andExpect(jsonPath("$.data.items[0].replyCount").value(2));
        mvc.perform(get(path(f)).param("rootId", root.get("id").asText())).andExpect(jsonPath("$.data.items.length()").value(2));
        mvc.perform(post(path(f)).cookie(login("fourth").cookie()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(replyBody)))
                .andExpect(status().isBadRequest());
    }

    @Test void identityPermissionsAndHostModeration() throws Exception {
        Fixture f = fixture(); Session guest = login("민수");
        JsonNode root = create(f, guest, body("sensitive text")); String id = root.get("id").asText();
        mvc.perform(put(path(f)+"/"+id).cookie(f.host().cookie()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body("overwrite"))))
                .andExpect(status().isForbidden());
        mvc.perform(delete(path(f)+"/"+id).cookie(f.host().cookie())).andExpect(status().isForbidden());
        mvc.perform(put(path(f)+"/"+id).cookie(guest.cookie()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body("edited"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.edited").value(true));
        mvc.perform(post(path(f)+"/"+id+"/hide").cookie(f.host().cookie())).andExpect(status().isOk());
        mvc.perform(get(path(f)+"/"+id)).andExpect(jsonPath("$.data.body").value(""))
                .andExpect(jsonPath("$.data.mentions.length()").value(0)).andExpect(jsonPath("$.data.status").value("HIDDEN"));
        mvc.perform(get(path(f))).andExpect(jsonPath("$.data.items.length()").value(0));
    }

    @Test void mentionsValidateMembershipRangesAndNotifyOnlyTheRecipient() throws Exception {
        Fixture f = fixture(); Session guest = login("지수"); Session outsider = login("외부인");
        var invalid = body("@외부인 확인");
        invalid.put("mentions", List.of(Map.of("userId", outsider.id(), "start", 0, "end", 4)));
        mvc.perform(post(path(f)).cookie(guest.cookie()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
        var valid = body("@민수 확인"); valid.put("mentions", List.of(Map.of("userId", f.host().id(), "start", 0, "end", 3)));
        JsonNode c = create(f, guest, valid);
        mvc.perform(get(path(f)+"/activity").cookie(f.host().cookie())).andExpect(jsonPath("$.data[0].mentioned").value(true));
        mvc.perform(get(path(f)+"/activity").cookie(guest.cookie())).andExpect(jsonPath("$.data.length()").value(0));
        mvc.perform(get(path(f)+"/activity")).andExpect(jsonPath("$.data[0].mentioned").value(false));
        var invalidEdit = body("@민수"); invalidEdit.put("mentions", List.of(Map.of("userId", f.host().id(), "start", -1, "end", 3)));
        mvc.perform(put(path(f)+"/"+c.get("id").asText()).cookie(guest.cookie()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(invalidEdit)))
                .andExpect(status().isBadRequest());
        mvc.perform(get(path(f)+"/people")).andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test void crossMeetingReferencesRejectedAndPaginationDoesNotSkipEqualTimestamps() throws Exception {
        Fixture f = fixture(); Fixture other = fixture(); Session guest = login("지수");
        JsonNode foreign = create(other, other.host(), body("other meeting"));
        var invalid = body("reply"); invalid.put("replyToId", foreign.get("id").asText());
        mvc.perform(post(path(f)).cookie(guest.cookie()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(invalid)))
                .andExpect(status().isNotFound());
        for (int i=0; i<23; i++) {
            store.mutate(f.meetingId(), all -> MeetingComment.create(f.meetingId(), Long.valueOf(f.host().id()), null, null,
                    "page", List.of(), UUID.randomUUID().toString(), 1234));
        }
        Set<String> ids = new HashSet<>(); String cursor = null;
        do {
            var request = get(path(f)); if (cursor != null) request.param("cursor", cursor);
            JsonNode page = data(mvc.perform(request).andExpect(status().isOk()).andReturn());
            page.get("items").forEach(c -> { assertThat(ids.add(c.get("id").asText())).isTrue(); });
            cursor = page.get("nextCursor").isNull() ? null : page.get("nextCursor").asText();
        } while (cursor != null);
        assertThat(ids).hasSize(23);
    }

    JsonNode react(Fixture f, Session session, String id, String emoji, boolean active) throws Exception {
        return data(mvc.perform(put(path(f)+"/"+id+"/reactions").cookie(session.cookie()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("emoji", emoji, "active", active))))
                .andExpect(status().isOk()).andReturn());
    }

    @Test void reactionsAreIdempotentPerPersonAndExposeNamesWithoutPrivateAccountData() throws Exception {
        Fixture f=fixture();Session guest=login("지수");String id=create(f,f.host(),body("See you there")).get("id").asText();
        JsonNode first=react(f,guest,id,"👍",true);
        assertThat(first.get("reactions").get(0).get("count").asInt()).isEqualTo(1);
        assertThat(first.get("reactions").get(0).get("reacted").asBoolean()).isTrue();
        JsonNode person=first.get("reactions").get(0).get("people").get(0);
        assertThat(person.get("id").asText()).isEqualTo(guest.id());
        assertThat(person.get("name").asText()).isEqualTo("지수");
        assertThat(person.has("email")).isFalse();
        long version=store.stats(f.meetingId()).version();
        react(f,guest,id,"👍",true);
        assertThat(store.stats(f.meetingId()).version()).isEqualTo(version);
        react(f,f.host(),id,"👍",true);react(f,guest,id,"❤️",true);
        JsonNode visible=data(mvc.perform(get(path(f)+"/"+id)).andExpect(status().isOk()).andReturn());
        assertThat(visible.get("reactions").get(0).get("count").asInt()).isEqualTo(2);
        assertThat(visible.get("reactions").get(0).get("reacted").asBoolean()).isFalse();
        assertThat(visible.get("reactions").get(0).get("people")).hasSize(2);
        JsonNode cancelled=react(f,guest,id,"👍",false);
        assertThat(cancelled.get("reactions").get(0).get("count").asInt()).isEqualTo(1);
        assertThat(cancelled.get("reactions").get(0).get("people").get(0).get("id").asText()).isEqualTo(f.host().id());
        assertThat(cancelled.get("reactions").get(1).get("reacted").asBoolean()).isTrue();
        react(f,guest,id,"❤️",false);react(f,f.host(),id,"👍",false);
        assertThat(store.stats(f.meetingId()).count()).isEqualTo(1);
        mvc.perform(get(path(f)+"/"+id)).andExpect(jsonPath("$.data.reactions.length()").value(0));
        react(f,guest,id,"👀",true);
        react(f,f.host(),id,"💯",true);
        mvc.perform(get(path(f)+"/"+id).cookie(guest.cookie()))
                .andExpect(jsonPath("$.data.reactions[0].emoji").value("👀"))
                .andExpect(jsonPath("$.data.reactions[0].reacted").value(true))
                .andExpect(jsonPath("$.data.reactions[0].people[0].id").value(guest.id()))
                .andExpect(jsonPath("$.data.reactions[1].emoji").value("💯"))
                .andExpect(jsonPath("$.data.reactions[1].reacted").value(false))
                .andExpect(jsonPath("$.data.reactions[1].people[0].id").value(f.host().id()));
        react(f,guest,id,"👀",false);react(f,f.host(),id,"💯",false);
        mvc.perform(get(path(f)+"/"+id)).andExpect(jsonPath("$.data.reactions.length()").value(0));
    }

    @Test void reactionsRequireAnAuthenticatedActorAndAValidActiveCommentInTheSameMeeting() throws Exception {
        Fixture f=fixture();Fixture other=fixture();String id=create(f,f.host(),body("hello")).get("id").asText();
        String endpoint=path(f)+"/"+id+"/reactions";
        mvc.perform(put(endpoint).contentType(MediaType.APPLICATION_JSON).content("{\"emoji\":\"👍\",\"active\":true}"))
                .andExpect(status().isUnauthorized());
        for(String invalid:List.of("{\"emoji\":\"🔥\",\"active\":true}","{\"emoji\":\"👍\"}","{\"emoji\":null,\"active\":true}")) {
            mvc.perform(put(endpoint).cookie(f.host().cookie()).contentType(MediaType.APPLICATION_JSON).content(invalid))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(put(path(other)+"/"+id+"/reactions").cookie(f.host().cookie()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"emoji\":\"👍\",\"active\":true}")).andExpect(status().isNotFound());
        react(f,f.host(),id,"👍",true);
        mvc.perform(delete(path(f)+"/"+id).cookie(f.host().cookie())).andExpect(status().isOk());
        mvc.perform(get(path(f)+"/"+id)).andExpect(jsonPath("$.data.reactions.length()").value(0));
        mvc.perform(put(endpoint).cookie(f.host().cookie()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"emoji\":\"👍\",\"active\":true}")).andExpect(status().isBadRequest());
    }

    @Test void reactingDoesNotMarkTheTextEditedOrResurfaceAnOldMentionAndRepliesSupportReactions() throws Exception {
        Fixture f=fixture();Session guest=login("지수");var text=body("@민수 hello");
        text.put("mentions",List.of(Map.of("userId",f.host().id(),"start",0,"end",3)));
        JsonNode root=create(f,guest,text);String id=root.get("id").asText();long seen=store.stats(f.meetingId()).version();
        react(f,f.host(),id,"🙏",true);
        assertThat(store.stats(f.meetingId()).version()).isGreaterThan(seen);
        mvc.perform(get(path(f)+"/activity").cookie(f.host().cookie()).param("after",Long.toString(seen)))
                .andExpect(jsonPath("$.data.length()").value(0));
        mvc.perform(get(path(f)+"/"+id)).andExpect(jsonPath("$.data.edited").value(false))
                .andExpect(jsonPath("$.data.body").value("@민수 hello"));
        var replyBody=body("Thanks");replyBody.put("replyToId",id);
        String reply=create(f,f.host(),replyBody).get("id").asText();
        JsonNode reaction=react(f,guest,reply,"🎉",true);
        assertThat(reaction.get("rootId").asText()).isEqualTo(id);
        mvc.perform(get(path(f)).param("rootId",id)).andExpect(jsonPath("$.data.items[0].reactions[0].emoji").value("🎉"));
        mvc.perform(post(path(f)+"/"+id+"/hide").cookie(f.host().cookie())).andExpect(status().isOk());
        mvc.perform(get(path(f)+"/"+id)).andExpect(jsonPath("$.data.reactions.length()").value(0));
        assertThat(react(f,guest,reply,"🤔",true).get("reactions")).hasSize(2);
    }
}
