package com.example.orderservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 스레드 풀 설정
        executor.setCorePoolSize(5);  // 기본 스레드 수
        executor.setMaxPoolSize(10);  // 최대 스레드 수
        executor.setQueueCapacity(500);  // 큐 용량
        executor.setThreadNamePrefix("Async-");  // 스레드 이름 접두사

        // graceful shutdown 설정
        executor.setWaitForTasksToCompleteOnShutdown(true);  // 종료 시 대기 설정
        executor.setAwaitTerminationSeconds(60);  // 종료 대기 시간 (초)

        executor.initialize();
        return executor;
    }
}
