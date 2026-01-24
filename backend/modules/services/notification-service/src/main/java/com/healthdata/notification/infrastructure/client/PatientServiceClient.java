package com.healthdata.notification.infrastructure.client;

import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

/**
 * Feign client for Patient Service
 *
 * Queries patient contact information for SMS reminders.
 */
@FeignClient(
    name = "patient-service",
    url = "${patient.service.url:http://localhost:8084}",
    path = "/patient"
)
public interface PatientServiceClient {

    /**
     * Get patient contact information
     *
     * @param tenantId  Tenant ID (HIPAA §164.312(d))
     * @param patientId Patient ID
     * @return Patient contact details
     */
    @GetMapping("/api/v1/patients/{patientId}/contact")
    PatientContactDto getPatientContact(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable("patientId") UUID patientId
    );

    /**
     * Patient Contact DTO
     */
    @Data
    class PatientContactDto {
        private UUID id;
        private String firstName;
        private String lastName;
        private String phoneNumber;
        private String email;
        private Boolean smsOptIn;
    }
}
