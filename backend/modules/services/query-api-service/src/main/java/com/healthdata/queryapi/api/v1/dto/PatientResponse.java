package com.healthdata.queryapi.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Response DTO for Patient queries
 * Maps PatientProjection to REST response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PatientResponse {
    private String patientId;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String mrn;
    private String insuranceMemberId;
}
