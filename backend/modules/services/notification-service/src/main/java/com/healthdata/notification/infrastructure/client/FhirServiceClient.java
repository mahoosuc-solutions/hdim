package com.healthdata.notification.infrastructure.client;

import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Feign client for FHIR Service
 *
 * Queries appointments for reminder scheduler.
 */
@FeignClient(
    name = "fhir-service",
    url = "${fhir.service.url:http://localhost:8085}",
    path = "/fhir"
)
public interface FhirServiceClient {

    /**
     * Get upcoming appointments for reminder processing
     *
     * @param tenantId Tenant ID (HIPAA §164.312(d))
     * @param startTime Start of time range
     * @param endTime   End of time range
     * @param status    Appointment status (e.g., "booked")
     * @return List of appointments
     */
    @GetMapping("/Appointment")
    List<AppointmentDto> getAppointments(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("start-time-ge") LocalDateTime startTime,
            @RequestParam("start-time-le") LocalDateTime endTime,
            @RequestParam(value = "status", required = false) String status
    );

    /**
     * Appointment DTO
     */
    @Data
    class AppointmentDto {
        private UUID id;
        private UUID patientId;
        private String status;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String appointmentTypeCode;
        private String appointmentTypeDisplay;
        private String description;
        private String locationId;
        private String practitionerId;
    }
}
