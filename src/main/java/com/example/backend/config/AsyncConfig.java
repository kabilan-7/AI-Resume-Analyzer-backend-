package com.example.backend.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Bounded pool — kept small deliberately.
     *
     * Groq's free tier allows 30 requests/minute. With 5 concurrent
     * threads, a batch of 20 resumes finishes in ~4 rounds rather
     * than firing 20 requests simultaneously and tripping rate limits.
     */
    @Bean(name = "screeningExecutor")
    public Executor screeningExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("screen-async-");
        executor.initialize();
        return executor;
    }
}
