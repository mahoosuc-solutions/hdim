package com.healthdata.patientevent.api.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientEventResponse {

    private String patientId;
    private String status;
    private Instant timestamp;
    private String message;
}
