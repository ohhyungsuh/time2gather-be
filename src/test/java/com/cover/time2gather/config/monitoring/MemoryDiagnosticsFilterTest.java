package com.cover.time2gather.config.monitoring;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class MemoryDiagnosticsFilterTest {
    @Test
    void concurrentRequestsProduceOnlyOneSamplePerInterval() throws Exception {
        var filter = spy(new MemoryDiagnosticsFilter(300));
        doNothing().when(filter).sample(anyString());
        try (var executor = Executors.newFixedThreadPool(8)) {
            for (int i = 0; i < 100; i++) executor.submit(() -> filter.sampleIfDue("request"));
            executor.shutdown();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
        verify(filter, times(1)).sample("request");
    }

    @Test
    void preservesResponseAndSamplesAfterDownstreamWork() throws Exception {
        var filter = spy(new MemoryDiagnosticsFilter(300));
        doNothing().when(filter).sample(anyString());
        var response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest(), response, (req, res) -> {
            verify(filter, never()).sample(anyString());
            ((jakarta.servlet.http.HttpServletResponse) res).setStatus(401);
        });
        assertThat(response.getStatus()).isEqualTo(401);
        verify(filter).sample("request");
    }

    @Test
    void unsupportedPlatformOrDisabledNmtDoesNotBreakRequests() {
        assertThatCode(() -> new MemoryDiagnosticsFilter(300).sample("test")).doesNotThrowAnyException();
    }
}
