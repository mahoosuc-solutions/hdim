package com.healthdata.queryapi.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO for Observation queries
 * Maps ObservationProjection to REST response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ObservationResponse {
    private String patientId;
    private String loincCode;
    private BigDecimal value;
    private String unit;
    private Instant observationDate;
    private String notes;
}
