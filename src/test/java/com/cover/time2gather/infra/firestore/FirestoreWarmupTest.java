package com.cover.time2gather.infra.firestore;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

class FirestoreWarmupTest {
    private final Firestore firestore = mock(Firestore.class);
    private final CollectionReference collection = mock(CollectionReference.class);
    private final DocumentReference document = mock(DocumentReference.class);
    @SuppressWarnings("unchecked")
    private final ApiFuture<DocumentSnapshot> read = mock(ApiFuture.class);
    private final FirestoreWarmup warmup = new FirestoreWarmup(firestore);

    @BeforeEach
    void setUp() {
        when(firestore.collection("meetings")).thenReturn(collection);
        when(collection.document("startup-warmup-probe")).thenReturn(document);
        when(document.get()).thenReturn(read);
    }

    @Test
    void warmsConnectionWithOnlyOneReadEvenWhenDocumentDoesNotExist() throws Exception {
        DocumentSnapshot missing = mock(DocumentSnapshot.class);
        when(read.get(5, TimeUnit.SECONDS)).thenReturn(missing);

        warmup.warmUp();

        verify(firestore).collection("meetings");
        verify(collection).document("startup-warmup-probe");
        verify(document).get();
        verify(read).get(5, TimeUnit.SECONDS);
        verifyNoMoreInteractions(firestore, collection, document, read);
        verifyNoInteractions(missing);
    }

    @Test
    void cancelsSlowReadAndAllowsStartupToContinue() throws Exception {
        when(read.get(5, TimeUnit.SECONDS)).thenThrow(new TimeoutException());

        assertThatCode(warmup::warmUp).doesNotThrowAnyException();

        verify(read).cancel(true);
    }

    @Test
    void allowsStartupWhenFirestoreIsUnavailable() throws Exception {
        when(read.get(5, TimeUnit.SECONDS)).thenThrow(new ExecutionException(new IllegalStateException()));

        assertThatCode(warmup::warmUp).doesNotThrowAnyException();

        verify(read).cancel(true);
    }

    @Test
    void preservesInterruptAndCancelsRead() throws Exception {
        when(read.get(5, TimeUnit.SECONDS)).thenThrow(new InterruptedException());
        try {
            warmup.warmUp();

            assertThat(Thread.currentThread().isInterrupted()).isTrue();
            verify(read).cancel(true);
        } finally {
            Thread.interrupted();
        }
    }
}
