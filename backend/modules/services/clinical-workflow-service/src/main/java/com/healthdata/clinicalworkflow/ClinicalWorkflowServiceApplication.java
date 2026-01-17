package com.healthdata.clinicalworkflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

/**
 * Clinical Workflow Service Application
 *
 * Microservice for managing clinical workflows across Medical Assistants (MA),
 * Nurses, and Providers in primary care and hospital settings:
 * - Patient check-in and pre-visit preparation
 * - Vital signs recording with abnormal value alerts
 * - Room management and assignment
 * - Waiting room queue management with priority-based triage
 *
 * Key Features:
 * - Real-time WebSocket updates for dashboards
 * - FHIR R4 integration (Observation, Encounter, Task resources)
 * - Multi-tenant isolation with strict access control
 * - HIPAA-compliant caching (5-minute TTL for PHI)
 *
 * Port: 8110
 * Context Path: /clinical-workflow
 *
 * Authentication: Gateway-trust authentication (no direct JWT validation)
 * Multi-tenant: Yes (tenant_id in all queries)
 * HIPAA Compliant: Yes (audit logging, PHI cache TTL 5 minutes)
 * Realtime: Yes (WebSocket via STOMP/SockJS with Redis pub/sub)
 */
@SpringBootApplication
@EnableFeignClients(basePackages = "com.healthdata")
@EnableCaching
@EnableAsync
@EnableWebSocket
public class ClinicalWorkflowServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClinicalWorkflowServiceApplication.class, args);
    }
}
