package com.healthdata.patient.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event published when a new patient is created
 * Other modules can listen to this event for their own processing
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientCreatedEvent {
    private String patientId;
    private String mrn;
    private String tenantId;
    private LocalDateTime timestamp = LocalDateTime.now();

    public PatientCreatedEvent(String patientId, String mrn, String tenantId) {
        this.patientId = patientId;
        this.mrn = mrn;
        this.tenantId = tenantId;
        this.timestamp = LocalDateTime.now();
    }
}