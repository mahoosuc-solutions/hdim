package com.healthdata.approval;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Human-in-the-Loop Approval Service.
 * Provides approval workflow management for HITL operations across HDIM platform.
 */
@SpringBootApplication
@EnableFeignClients
@EnableScheduling
public class ApprovalServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApprovalServiceApplication.class, args);
    }
}
