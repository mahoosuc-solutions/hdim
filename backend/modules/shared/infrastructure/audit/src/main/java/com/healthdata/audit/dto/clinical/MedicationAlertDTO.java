package com.healthdata.audit.dto.clinical;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Medication alert information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicationAlertDTO {
    
    private String alertId;
    private String patientId;
    private String alertType; // INTERACTION, ALLERGY, CONTRAINDICATION, DOSE_RANGE
    private String severity;
    private List<String> involvedMedications;
    private String alertMessage;
    private String clinicalRecommendation;
    private String evidenceGrade;
    private Boolean acknowledged;
    private String acknowledgedBy;
}
