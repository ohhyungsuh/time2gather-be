package com.cover.time2gather.infra.comment;

import com.cover.time2gather.domain.comment.MeetingComment;
import com.cover.time2gather.domain.exception.BusinessException;
import com.cover.time2gather.domain.exception.ErrorCode;
import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FirestoreCommentStoreTest {
    @Test @SuppressWarnings({"unchecked", "rawtypes"})
    void transactionRoundTripsExactIdsMentionsAndCountsAndPropagatesDomainErrors() throws Exception {
        Firestore firestore = mock(Firestore.class);
        CollectionReference scopes = mock(CollectionReference.class), items = mock(CollectionReference.class);
        DocumentReference guard = mock(DocumentReference.class), item = mock(DocumentReference.class);
        Transaction tx = mock(Transaction.class);
        DocumentSnapshot guardSnapshot = mock(DocumentSnapshot.class);
        QuerySnapshot query = mock(QuerySnapshot.class);
        when(firestore.collection("meetingComments")).thenReturn(scopes);
        when(scopes.document("1")).thenReturn(guard);
        when(guard.collection("items")).thenReturn(items);
        when(items.document(anyString())).thenReturn(item);
        when(tx.get(guard)).thenReturn(ApiFutures.immediateFuture(guardSnapshot));
        when(tx.get(items)).thenReturn(ApiFutures.immediateFuture(query));
        when(query.getDocuments()).thenReturn(List.of());
        when(firestore.runTransaction(any(Transaction.Function.class))).thenAnswer(invocation -> {
            try {
                Transaction.Function<MeetingComment> fn = invocation.getArgument(0);
                return ApiFutures.immediateFuture(fn.updateCallback(tx));
            } catch (Exception e) { return ApiFutures.immediateFailedFuture(e); }
        });
        FirestoreCommentStore store = new FirestoreCommentStore(firestore);
        MeetingComment c = store.mutate(1L, all -> MeetingComment.create(1L, 9007199254740993L, null, null, "@민수 확인",
                List.of(new MeetingComment.Mention("9007199254740995", 0, 3)), "request", 100));
        ArgumentCaptor<Map<String,Object>> saved = ArgumentCaptor.forClass(Map.class);
        verify(tx).set(eq(item), saved.capture());
        verify(tx).set(eq(guard), eq(Map.of("updatedAt", 100L, "count", 1L)));
        QueryDocumentSnapshot document = mock(QueryDocumentSnapshot.class);
        when(document.getData()).thenReturn(saved.getValue());
        when(query.getDocuments()).thenReturn(List.of(document));
        when(items.get()).thenReturn(ApiFutures.immediateFuture(query));
        MeetingComment read = store.list(1L).getFirst();
        assertThat(read.getId()).isEqualTo(c.getId());
        assertThat(read.getAuthorId()).isEqualTo(9007199254740993L);
        assertThat(read.getMentions()).containsExactly(new MeetingComment.Mention("9007199254740995", 0, 3));
        assertThatThrownBy(() -> store.mutate(1L, all -> { throw new BusinessException(ErrorCode.ACCESS_DENIED); }))
                .isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED));
    }
}
