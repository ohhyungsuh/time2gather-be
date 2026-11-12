package com.cover.time2gather.infra.firestore;

import com.cover.time2gather.domain.admin.AdminPage;
import com.cover.time2gather.domain.user.User;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Uses only a unique demo-project database on the local emulator. */
@org.junit.jupiter.api.Tag("firestore")
class FirestoreAdminCursorEmulatorTest {
    @Test
    void equalTimestampsAndDeletedAnchorDoNotDuplicateOrSkipRemainingDocuments() throws Exception {
        try (Firestore db = FirestoreOptions.newBuilder()
                .setProjectId("demo-cursor-" + UUID.randomUUID().toString().substring(0, 8))
                .setEmulatorHost(com.cover.time2gather.support.FirestoreTestEnvironment.endpoint())
                .setHost(com.cover.time2gather.support.FirestoreTestEnvironment.endpoint()).build().getService()) {
            for (long id : List.of(10L, 20L, 30L, 40L, 50L)) {
                db.collection("users").document(Long.toString(id)).set(Map.of(
                        "id", id, "username", "Fixture " + id, "createdAt", "2026-11-09T12:00:00")).get();
            }
            var repository = new FirestoreUserRepository(db);
            var first = repository.findAll(PageRequest.of(0, 2));
            assertThat(first.getContent()).extracting(User::getId).containsExactly(50L, 40L);
            var firstCursor = AdminPage.from(first).nextCursor();
            db.collection("users").document("40").delete().get();
            db.collection("users").document("60").set(Map.of(
                    "id", 60L, "username", "New fixture", "createdAt", "2026-11-10T12:00:00")).get();
            var second = repository.findAll(PageRequest.of(1, 2), firstCursor);
            assertThat(second.getContent()).extracting(User::getId).containsExactly(30L, 20L);
            assertThat(second.getTotalElements()).isEqualTo(5);
            var third = repository.findAll(PageRequest.of(2, 2), AdminPage.from(second).nextCursor());
            assertThat(third.getContent()).extracting(User::getId).containsExactly(10L);
            assertThat(AdminPage.from(third).nextCursor()).isNull();
        }
    }
}
