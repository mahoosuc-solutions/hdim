package com.healthdata.nurseworkflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Nurse Workflow Service Application
 *
 * Microservice for managing nurse-specific clinical workflows:
 * - Patient outreach and follow-up management
 * - Medication reconciliation tracking
 * - Patient education delivery and assessment
 * - Referral coordination and closed-loop tracking
 *
 * This service implements industry-standard nursing workflows including:
 * - Joint Commission NPSG (National Patient Safety Goals)
 * - Meaningful Use quality measures
 * - NANDA-I nursing diagnoses
 * - NIC (Nursing Interventions Classification)
 * - NOC (Nursing Outcomes Classification)
 * - FHIR R4 resource integration
 *
 * Port: 8093
 * Context Path: /nurse-workflow
 *
 * Authentication: Gateway-trust authentication
 * Multi-tenant: Yes (tenant_id in all queries)
 * HIPAA Compliant: Yes (audit logging, PHI cache TTL 5 minutes)
 */
@SpringBootApplication
@EnableFeignClients(basePackages = "com.healthdata")
@EnableCaching
@EnableAsync
public class NurseWorkflowServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NurseWorkflowServiceApplication.class, args);
    }
}
