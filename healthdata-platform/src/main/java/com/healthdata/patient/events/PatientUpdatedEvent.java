package com.healthdata.patient.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event published when a patient is updated
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientUpdatedEvent {
    private String patientId;
    private String mrn;
    private String tenantId;
    private LocalDateTime timestamp = LocalDateTime.now();

    public PatientUpdatedEvent(String patientId, String mrn, String tenantId) {
        this.patientId = patientId;
        this.mrn = mrn;
        this.tenantId = tenantId;
        this.timestamp = LocalDateTime.now();
    }
}