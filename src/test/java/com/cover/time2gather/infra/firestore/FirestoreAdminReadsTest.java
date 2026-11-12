package com.cover.time2gather.infra.firestore;

import com.cover.time2gather.domain.user.User;
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class FirestoreAdminReadsTest {
    private final Firestore firestore = mock(Firestore.class);
    private final CollectionReference collection = mock(CollectionReference.class);
    private final Query ordered = mock(Query.class, RETURNS_SELF);
    private FirestoreUserRepository users;
    private FirestoreMeetingRepository meetings;

    @BeforeEach
    void setUp() {
        when(firestore.collection(anyString())).thenReturn(collection);
        when(collection.getId()).thenReturn("users");
        when(collection.orderBy("createdAt", Query.Direction.DESCENDING)).thenReturn(ordered);
        users = new FirestoreUserRepository(firestore);
        meetings = new FirestoreMeetingRepository(firestore);
    }

    private void count(Query query, long value) {
        AggregateQuery aggregate = mock(AggregateQuery.class);
        AggregateQuerySnapshot snapshot = mock(AggregateQuerySnapshot.class);
        when(query.count()).thenReturn(aggregate);
        when(aggregate.get()).thenReturn(ApiFutures.immediateFuture(snapshot));
        when(snapshot.getCount()).thenReturn(value);
    }

    private QueryDocumentSnapshot user(int id) {
        var document = mock(QueryDocumentSnapshot.class);
        when(document.getId()).thenReturn(Integer.toString(id + 1));
        when(document.getData()).thenReturn(Map.of("id", (long) id + 1,
                "username", id % 3 == 0 ? "MATCH " + id : "other " + id,
                "createdAt", "2026-10-31T12:00:00"));
        return document;
    }

    private void documents(Query query, List<QueryDocumentSnapshot> documents) {
        QuerySnapshot snapshot = mock(QuerySnapshot.class);
        when(snapshot.getDocuments()).thenReturn(documents);
        when(query.get()).thenReturn(ApiFutures.immediateFuture(snapshot));
    }

    @Test
    void aggregateCountsDoNotDownloadCollectionsAndNotProviderIncludesMissingValues() {
        count(collection, 7);
        Query anonymous = mock(Query.class);
        when(collection.whereEqualTo("provider", "ANONYMOUS")).thenReturn(anonymous);
        count(anonymous, 5);
        assertThat(users.count()).isEqualTo(7);
        assertThat(users.countByProvider(User.AuthProvider.ANONYMOUS)).isEqualTo(5);
        assertThat(users.countByProviderNot(User.AuthProvider.ANONYMOUS)).isEqualTo(2);
        verify(collection, never()).get();
        verify(anonymous, never()).get();
    }

    @Test
    void meetingCountersUseServerFilters() {
        Query active = mock(Query.class);
        Query confirmed = mock(Query.class);
        when(collection.whereEqualTo("isActive", true)).thenReturn(active);
        when(collection.whereNotEqualTo("confirmedDate", null)).thenReturn(confirmed);
        count(active, 38);
        count(confirmed, 2);
        assertThat(meetings.countByIsActiveTrue()).isEqualTo(38);
        assertThat(meetings.countByConfirmedDateIsNotNull()).isEqualTo(2);
        verify(collection, never()).get();
    }

    @Test
    void normalPageDownloadsOnlyRequestedDocumentsAndRetainsTotal() {
        count(collection, 75);
        documents(ordered, List.of(user(21), user(22)));
        var page = users.findAll(PageRequest.of(1, 20));
        assertThat(page.getTotalElements()).isEqualTo(75);
        assertThat(page.getContent()).extracting(User::getId).containsExactly(22L, 23L);
        verify(ordered).offset(20);
        verify(ordered).limit(20);
        verify(collection, never()).get();
    }

    @Test
    void confirmedMeetingsUseServerFilterThenSearchSortAndPaginate() {
        Query confirmed = mock(Query.class);
        when(collection.whereNotEqualTo("confirmedDate", null)).thenReturn(confirmed);
        var documents = IntStream.rangeClosed(1, 5).mapToObj(id -> {
            var document = mock(QueryDocumentSnapshot.class);
            when(document.getId()).thenReturn(Integer.toString(id));
            when(document.getData()).thenReturn(Map.of("id", (long) id,
                    "title", id == 5 ? "Other" : "Dinner",
                    "meetingCode", "code_" + id, "confirmedDate", "2026-11-15",
                    "createdAt", "2026-11-01T12:00:00"));
            return document;
        }).toList();
        documents(confirmed, documents);
        var page = meetings.findConfirmed("dInNeR", PageRequest.of(1, 2));
        assertThat(page.getTotalElements()).isEqualTo(4);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent()).extracting(com.cover.time2gather.domain.meeting.Meeting::getId)
                .containsExactly(2L, 1L);
        assertThat(meetings.findConfirmed("CODE_5", PageRequest.of(0, 20)).getTotalElements()).isEqualTo(1);
        assertThat(meetings.findConfirmed("", PageRequest.of(0, 20)).getTotalElements()).isEqualTo(5);
        verify(collection, never()).get();
        verify(collection, never()).orderBy(anyString(), any(Query.Direction.class));
    }

    @Test
    void pageBeyondEndReturnsEmptyWithoutReadingDocuments() {
        count(collection, 2);
        var page = users.findAll(PageRequest.of(Integer.MAX_VALUE, 50));
        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isEqualTo(2);
        verify(collection, never()).orderBy(anyString(), any(Query.Direction.class));
    }

    @Test
    void cursorPageUsesStableValuesWithoutOffsetOrBoundaryReadAndSurvivesDtoMapping() {
        count(collection, 80);
        documents(ordered, IntStream.range(0, 20).mapToObj(this::user).toList());
        var first = users.findAll(PageRequest.of(0, 20));
        var response = com.cover.time2gather.domain.admin.AdminPage.from(first.map(User::getId));
        assertThat(response.nextCursor()).isNotBlank();
        clearInvocations(ordered);
        DocumentReference boundary = mock(DocumentReference.class);
        when(collection.document("20")).thenReturn(boundary);
        documents(ordered, List.of(user(20), user(21)));
        var second = users.findAll(PageRequest.of(1, 20), response.nextCursor());
        assertThat(second.getContent()).extracting(User::getId).containsExactly(21L, 22L);
        verify(ordered).startAfter("2026-10-31T12:00:00", boundary);
        verify(ordered, never()).offset(anyInt());
        verify(boundary, never()).get();
        verify(ordered).limit(20);
    }

    @Test
    void cursorCannotBeReusedWithDifferentSizeOrPageAndMalformedCursorDoesNotReadFirestore() {
        count(collection, 80);
        documents(ordered, List.of(user(0)));
        var first = com.cover.time2gather.domain.admin.AdminPage.from(users.findAll(PageRequest.of(0, 20)));
        clearInvocations(collection, ordered);
        assertThatThrownBy(() -> users.findAll(PageRequest.of(1, 10), first.nextCursor()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> users.findAll(PageRequest.of(2, 20), first.nextCursor()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> users.findAll(PageRequest.of(1, 20), "not a cursor"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(collection, never()).count();
        verify(ordered, never()).get();
    }

    @Test
    void substringSearchWalksBoundedCursorBatchesAndCountsMatchesAfterRequestedPage() {
        List<QueryDocumentSnapshot> first = IntStream.range(0, 100).mapToObj(this::user).toList();
        List<QueryDocumentSnapshot> second = IntStream.range(100, 103).mapToObj(this::user).toList();
        Query next = mock(Query.class, RETURNS_SELF);
        when(ordered.startAfter(first.getLast())).thenReturn(next);
        documents(ordered, first);
        documents(next, second);
        var page = users.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase("mAtCh", "mAtCh", PageRequest.of(1, 5));
        assertThat(page.getTotalElements()).isEqualTo(35);
        assertThat(page.getContent()).extracting(User::getId).containsExactly(16L, 19L, 22L, 25L, 28L);
        verify(ordered).limit(100);
        verify(ordered).startAfter(first.getLast());
        verify(next).limit(100);
        verify(collection, never()).get();
    }

    @Test
    @SuppressWarnings("unchecked")
    void timedOutAggregateIsCancelled() throws Exception {
        AggregateQuery aggregate = mock(AggregateQuery.class);
        ApiFuture<AggregateQuerySnapshot> future = mock(ApiFuture.class);
        when(collection.count()).thenReturn(aggregate);
        when(aggregate.get()).thenReturn(future);
        when(future.get(15, TimeUnit.SECONDS)).thenThrow(new TimeoutException());
        assertThatThrownBy(users::count).isInstanceOf(IllegalStateException.class).hasMessageContaining("count");
        verify(future).cancel(true);
    }
}
