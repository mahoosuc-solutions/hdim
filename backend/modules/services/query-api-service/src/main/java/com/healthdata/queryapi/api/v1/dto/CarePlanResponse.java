package com.healthdata.queryapi.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Response DTO for CarePlan queries
 * Maps CarePlanProjection to REST response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CarePlanResponse {
    private String patientId;
    private String title;
    private String status;
    private String coordinatorId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer goalCount;
}
