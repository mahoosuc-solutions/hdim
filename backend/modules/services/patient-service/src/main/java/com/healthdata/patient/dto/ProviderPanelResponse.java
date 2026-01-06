package com.healthdata.patient.dto;

import com.healthdata.patient.entity.ProviderPanelAssignmentEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for provider panel assignment.
 * Issue #135: Create Provider Panel Assignment API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderPanelResponse {

    private UUID assignmentId;
    private UUID patientId;
    private String assignmentType;
    private Instant assignedDate;
    private String notes;

    // Patient summary fields (populated from FHIR/demographics)
    private String patientName;
    private String mrn;
    private String dateOfBirth;
    private String gender;
    private String riskLevel;
    private Integer openCareGaps;
    private Instant lastVisitDate;

    /**
     * Create from entity (without patient details)
     */
    public static ProviderPanelResponse fromEntity(ProviderPanelAssignmentEntity entity) {
        return ProviderPanelResponse.builder()
                .assignmentId(entity.getId())
                .patientId(entity.getPatientId())
                .assignmentType(entity.getAssignmentType().name())
                .assignedDate(entity.getAssignedDate())
                .notes(entity.getNotes())
                .build();
    }

    /**
     * Create from entity with patient details
     */
    public static ProviderPanelResponse fromEntityWithPatient(
            ProviderPanelAssignmentEntity entity,
            String patientName,
            String mrn,
            String dateOfBirth,
            String gender,
            String riskLevel,
            Integer openCareGaps,
            Instant lastVisitDate) {

        return ProviderPanelResponse.builder()
                .assignmentId(entity.getId())
                .patientId(entity.getPatientId())
                .assignmentType(entity.getAssignmentType().name())
                .assignedDate(entity.getAssignedDate())
                .notes(entity.getNotes())
                .patientName(patientName)
                .mrn(mrn)
                .dateOfBirth(dateOfBirth)
                .gender(gender)
                .riskLevel(riskLevel)
                .openCareGaps(openCareGaps)
                .lastVisitDate(lastVisitDate)
                .build();
    }
}
