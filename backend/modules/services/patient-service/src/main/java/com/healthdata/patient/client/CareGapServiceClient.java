package com.healthdata.patient.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Feign client for Care Gap Service
 *
 * Issue #6: Provides access to patient care gaps for pre-visit planning.
 */
@FeignClient(
    name = "care-gap-service",
    url = "${care-gap.server.url:http://care-gap-service:8086}",
    configuration = CareGapServiceClientConfiguration.class
)
public interface CareGapServiceClient {

    /**
     * Get care gaps for a specific patient.
     */
    @GetMapping("/api/v1/patients/{patientId}/gaps")
    List<CareGapDto> getPatientCareGaps(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @PathVariable("patientId") String patientId
    );

    /**
     * Get open care gaps for a specific patient.
     */
    @GetMapping("/api/v1/patients/{patientId}/gaps/open")
    List<CareGapDto> getOpenCareGaps(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @PathVariable("patientId") String patientId
    );

    /**
     * Get care gap count for a patient.
     */
    @GetMapping("/api/v1/patients/{patientId}/gaps/count")
    CareGapCountResponse getCareGapCount(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @PathVariable("patientId") String patientId
    );

    /**
     * Health check endpoint.
     */
    @GetMapping("/actuator/health")
    String healthCheck();

    // ==================== DTOs ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class CareGapDto {
        private String id;
        private String patientId;
        private String measureId;
        private String measureName;
        private String gapCategory;
        private String priority;
        private String severity;
        private String status;
        private LocalDate dueDate;
        private LocalDate identifiedDate;
        private String recommendation;
        private String recommendationType;
        private String gapDescription;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class CareGapCountResponse {
        private long total;
        private long open;
        private long highPriority;
        private long overdue;
    }
}
