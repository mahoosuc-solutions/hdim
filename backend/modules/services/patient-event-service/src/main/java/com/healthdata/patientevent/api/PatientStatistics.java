package com.healthdata.patientevent.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Patient Statistics DTO
 * Aggregated statistics for a tenant's patient population
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientStatistics {
    private long totalPatients;
    private long highRiskCount;
    private long patientsWithUrgentGaps;
    private long patientsWithActiveAlerts;
}
