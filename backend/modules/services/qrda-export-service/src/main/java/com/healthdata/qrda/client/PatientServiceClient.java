package com.healthdata.qrda.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Feign client for communicating with the patient-service.
 *
 * Fetches patient demographics and lists for QRDA document generation.
 */
@FeignClient(
    name = "patient-service",
    url = "${backend.services.patient.url:http://localhost:8084}"
)
public interface PatientServiceClient {

    /**
     * Get patient demographics by ID.
     */
    @GetMapping("/api/v1/patients/{patientId}")
    PatientDTO getPatient(
        @RequestHeader("X-Tenant-Id") String tenantId,
        @PathVariable("patientId") UUID patientId
    );

    /**
     * Get list of patients for the tenant.
     */
    @GetMapping("/api/v1/patients")
    List<PatientDTO> getPatients(
        @RequestHeader("X-Tenant-Id") String tenantId,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "100") int size
    );

    /**
     * Get patients by IDs.
     */
    @PostMapping("/api/v1/patients/batch")
    List<PatientDTO> getPatientsByIds(
        @RequestHeader("X-Tenant-Id") String tenantId,
        @RequestBody List<UUID> patientIds
    );

    /**
     * Patient demographics DTO.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    class PatientDTO {
        private UUID id;
        private String tenantId;
        private String mrn;
        private String firstName;
        private String lastName;
        private String middleName;
        private LocalDate birthDate;
        private String gender;
        private String race;
        private String ethnicity;
        private String language;

        // Address
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String state;
        private String postalCode;
        private String country;

        // Contact
        private String phoneNumber;
        private String email;

        // Insurance
        private String payerName;
        private String payerCode;
        private String subscriberId;

        // Clinical identifiers
        private String fhirId;
        private String ssnLast4;
    }
}
