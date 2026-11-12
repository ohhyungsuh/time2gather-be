package com.cover.time2gather.config.monitoring;

import jakarta.annotation.PreDestroy;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.management.ObjectName;
import java.io.IOException;
import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/** Request-driven diagnostics: no background CPU, new endpoint or user data. */
@Slf4j
@Component
@ConditionalOnProperty(name = "memory.diagnostics.enabled", havingValue = "true")
public class MemoryDiagnosticsFilter extends OncePerRequestFilter {
    private static final Pattern NMT_COMMITTED = Pattern.compile("Total: reserved=\\d+KB, committed=(\\d+)KB");
    private final AtomicLong nextSample = new AtomicLong();
    private final long intervalNanos;

    public MemoryDiagnosticsFilter(@Value("${memory.diagnostics.interval-seconds:300}") long intervalSeconds) {
        this.intervalNanos = TimeUnit.SECONDS.toNanos(Math.max(60, intervalSeconds));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ready() {
        sampleIfDue("startup");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            chain.doFilter(request, response);
        } finally {
            sampleIfDue("request");
        }
    }

    void sampleIfDue(String reason) {
        long now = System.nanoTime();
        long next = nextSample.get();
        if ((next == 0 || now - next >= 0) && nextSample.compareAndSet(next, now + intervalNanos)) {
            sample(reason);
        }
    }

    @PreDestroy
    public void shutdown() {
        sample("shutdown");
    }

    void sample(String reason) {
        try {
            var memory = ManagementFactory.getMemoryMXBean();
            var heap = memory.getHeapMemoryUsage();
            var nonHeap = memory.getNonHeapMemoryUsage();
            long directBytes = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class).stream()
                    .filter(pool -> pool.getName().equals("direct")).mapToLong(BufferPoolMXBean::getMemoryUsed).sum();
            long gcCount = ManagementFactory.getGarbageCollectorMXBeans().stream()
                    .mapToLong(bean -> Math.max(0, bean.getCollectionCount())).sum();
            long gcMillis = ManagementFactory.getGarbageCollectorMXBeans().stream()
                    .mapToLong(bean -> Math.max(0, bean.getCollectionTime())).sum();
            log.info("MEMORY_SAMPLE reason={} heapUsedBytes={} heapCommittedBytes={} heapMaxBytes={} "
                            + "nonHeapUsedBytes={} directBufferBytes={} threads={} gcCount={} gcMillis={} "
                            + "processRssBytes={} containerBytes={} containerLimitBytes={} nmtCommittedBytes={}",
                    reason, heap.getUsed(), heap.getCommitted(), heap.getMax(), nonHeap.getUsed(), directBytes,
                    ManagementFactory.getThreadMXBean().getThreadCount(), gcCount, gcMillis,
                    processRss(), readLong("/sys/fs/cgroup/memory.current", "/sys/fs/cgroup/memory/memory.usage_in_bytes"),
                    readLong("/sys/fs/cgroup/memory.max", "/sys/fs/cgroup/memory/memory.limit_in_bytes"), nmtCommitted());
        } catch (Exception exception) {
            // Diagnostics must never fail a request or log request content.
            log.debug("Memory diagnostics unavailable: {}", exception.getClass().getSimpleName());
        }
    }

    private static long readLong(String... paths) {
        for (String path : paths) {
            try {
                return Long.parseLong(Files.readString(Path.of(path)).trim());
            } catch (IOException | RuntimeException ignored) {
                // A missing cgroup file or the literal 'max' means unavailable.
            }
        }
        return -1;
    }

    private static long processRss() {
        try (var lines = Files.lines(Path.of("/proc/self/status"))) {
            return lines.filter(line -> line.startsWith("VmRSS:"))
                    .mapToLong(line -> Long.parseLong(line.trim().split("\\s+")[1]) * 1024).findFirst().orElse(-1);
        } catch (IOException | RuntimeException ignored) {
            return -1;
        }
    }

    private static long nmtCommitted() {
        try {
            String summary = (String) ManagementFactory.getPlatformMBeanServer().invoke(
                    new ObjectName("com.sun.management:type=DiagnosticCommand"), "vmNativeMemory",
                    new Object[]{new String[]{"summary", "scale=KB"}}, new String[]{"[Ljava.lang.String;"});
            var matcher = NMT_COMMITTED.matcher(summary);
            return matcher.find() ? Long.parseLong(matcher.group(1)) * 1024 : -1;
        } catch (Exception ignored) {
            return -1;
        }
    }
}
