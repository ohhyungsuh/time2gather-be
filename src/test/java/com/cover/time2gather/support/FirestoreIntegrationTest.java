package com.cover.time2gather.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Tag("firestore")
@ActiveProfiles({"test", "firestore"})
public abstract class FirestoreIntegrationTest {
    private static final String PROJECT = "demo-time2gather-integration";

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("gcp.project-id", () -> PROJECT);
        properties.add("FIRESTORE_EMULATOR_HOST", FirestoreTestEnvironment::endpoint);
        properties.add("spring.flyway.enabled", () -> false);
    }

    @BeforeEach
    void clearTestDatabase() throws Exception {
        var uri = URI.create("http://" + FirestoreTestEnvironment.endpoint()
                + "/emulator/v1/projects/" + PROJECT + "/databases/(default)/documents");
        var response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(uri).DELETE().build(),
                HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() != 200) throw new IllegalStateException("Test database reset failed: " + response.statusCode());
    }
}
