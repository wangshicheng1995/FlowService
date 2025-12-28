package com.flowservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步任务配置
 * 配置异步任务执行的线程池
 */
@Configuration
public class AsyncConfig {

    /**
     * 自定义异步任务执行器
     * 用于执行 @Async 标注的方法
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数
        executor.setCorePoolSize(5);

        // 最大线程数
        executor.setMaxPoolSize(10);

        // 队列容量
        executor.setQueueCapacity(25);

        // 线程名前缀
        executor.setThreadNamePrefix("AsyncTask-");

        // 初始化
        executor.initialize();

        return executor;
    }
}
