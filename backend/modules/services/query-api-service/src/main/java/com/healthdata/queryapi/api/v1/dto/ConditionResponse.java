package com.healthdata.queryapi.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Response DTO for Condition queries
 * Maps ConditionProjection to REST response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConditionResponse {
    private String patientId;
    private String icdCode;
    private String description;
    private String status;
    private String verificationStatus;
    private LocalDate onsetDate;
}
