package com.cover.time2gather.infra.firestore;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.NoCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("firestore")
public class FirestoreConfig {

    @Bean(destroyMethod = "close")
    Firestore firestore(@Value("${gcp.project-id}") String projectId,
                        @Value("${FIRESTORE_EMULATOR_HOST:}") String emulatorHost) {
        var builder = FirestoreOptions.getDefaultInstance()
                .toBuilder()
                .setProjectId(projectId);
        if (!emulatorHost.isBlank()) {
            if (!projectId.startsWith("demo-")) throw new IllegalArgumentException("Emulator requires a demo project");
            builder.setHost(emulatorHost).setEmulatorHost(emulatorHost).setCredentials(NoCredentials.getInstance());
        }
        return builder.build().getService();
    }
}
