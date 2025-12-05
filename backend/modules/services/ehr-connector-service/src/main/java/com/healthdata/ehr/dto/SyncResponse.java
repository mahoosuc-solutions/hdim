package com.healthdata.ehr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for data synchronization results.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncResponse {

    private String patientId;
    private int encountersRetrieved;
    private int observationsRetrieved;
    private LocalDateTime syncStartTime;
    private LocalDateTime syncEndTime;
    private boolean success;
    private String errorMessage;
}
