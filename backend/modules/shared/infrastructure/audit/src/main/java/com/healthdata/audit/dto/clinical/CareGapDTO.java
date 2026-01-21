package com.healthdata.audit.dto.clinical;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Care gap information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareGapDTO {
    
    private String gapId;
    private String patientId;
    private String gapType;
    private String serviceDescription;
    private LocalDate dueDate;
    private Integer daysPastDue;
    private String priority;
    private String guidelineReference;
    private String status; // OPEN, IN_PROGRESS, CLOSED, DISMISSED
    private String evidenceGrade;
}
