package com.healthdata.demo.api.v1.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Optional overrides for scenario loading.
 */
public class LoadScenarioRequest {

    @Min(value = 1, message = "patientsPerTenant must be at least 1")
    @Max(value = 50000, message = "patientsPerTenant cannot exceed 50,000")
    private Integer patientsPerTenant;

    @Min(value = 0, message = "careGapPercentage must be at least 0")
    @Max(value = 100, message = "careGapPercentage cannot exceed 100")
    private Integer careGapPercentage;

    public Integer getPatientsPerTenant() { return patientsPerTenant; }
    public void setPatientsPerTenant(Integer patientsPerTenant) { this.patientsPerTenant = patientsPerTenant; }

    public Integer getCareGapPercentage() { return careGapPercentage; }
    public void setCareGapPercentage(Integer careGapPercentage) { this.careGapPercentage = careGapPercentage; }
}
