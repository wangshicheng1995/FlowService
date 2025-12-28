package com.flowservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EntityScan("com.flowservice.entity")
@EnableJpaRepositories("com.flowservice.repository")
@EnableAsync
@EnableScheduling
public class FlowServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(FlowServiceApplication.class, args);
    }
}