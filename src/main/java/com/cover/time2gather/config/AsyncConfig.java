package com.cover.time2gather.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 비동기 작업 처리를 위한 설정
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "reportTaskExecutor")
    public ThreadPoolTaskExecutor reportTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(30);
        executor.setQueueCapacity(300);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("report-");

        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.setRejectedExecutionHandler((runnable, exec) -> {
            log.warn("Thread pool is full. Running task synchronously in caller thread. Queue: {}, Active: {}",
                    exec.getQueue().size(), exec.getActiveCount());
            if (!exec.isShutdown()) {
                runnable.run();
            }
        });

        executor.initialize();
        return executor;
    }

    @Bean(name = "reportRetryScheduler", destroyMethod = "shutdown")
    public ScheduledExecutorService reportRetryScheduler() {
        return Executors.newScheduledThreadPool(5, runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("report-retry-" + thread.getName());
            thread.setDaemon(true);
            return thread;
        });
    }
}
