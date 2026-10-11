package com.cover.time2gather.infra.firestore;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("firestore")
public class FirestoreConfig {

    @Bean(destroyMethod = "close")
    Firestore firestore(@Value("${gcp.project-id}") String projectId) {
        return FirestoreOptions.getDefaultInstance()
                .toBuilder()
                .setProjectId(projectId)
                .build()
                .getService();
    }
}
