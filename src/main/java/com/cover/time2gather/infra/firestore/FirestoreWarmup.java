package com.cover.time2gather.infra.firestore;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/** Initializes the Firestore transport and credentials before accepting requests. */
@Slf4j
@Component
@Profile("firestore")
@ConditionalOnProperty(name = "firestore.warmup.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class FirestoreWarmup {

    private static final long TIMEOUT_SECONDS = 5;
    private final Firestore firestore;

    @PostConstruct
    void warmUp() {
        long started = System.nanoTime();
        ApiFuture<DocumentSnapshot> read = null;
        try {
            // An absent document is sufficient: never create users, meetings or votes.
            read = firestore.collection("meetings").document("startup-warmup-probe").get();
            read.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            log.info("Firestore startup warmup completed in {} ms", elapsedMillis(started));
        } catch (InterruptedException e) {
            if (read != null) read.cancel(true);
            Thread.currentThread().interrupt();
            log.warn("Firestore startup warmup interrupted after {} ms", elapsedMillis(started));
        } catch (Exception e) {
            if (read != null) read.cancel(true);
            // A best-effort optimization must not prevent the service from starting.
            log.warn("Firestore startup warmup skipped after {} ms", elapsedMillis(started), e);
        }
    }

    private long elapsedMillis(long started) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
    }
}
