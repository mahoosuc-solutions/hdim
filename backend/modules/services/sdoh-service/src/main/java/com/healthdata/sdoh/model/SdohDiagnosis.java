package com.healthdata.sdoh.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * SDOH-related diagnosis entry with Z-code
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SdohDiagnosis {
    private String diagnosisId;
    private String patientId;
    private String tenantId;
    private String zCode;
    private String zCodeDescription;
    private SdohCategory category;
    private String clinicalNote;
    private DiagnosisStatus status;
    private LocalDateTime diagnosisDate;
    private String diagnosedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum DiagnosisStatus {
        ACTIVE,
        RESOLVED,
        INACTIVE
    }
}
