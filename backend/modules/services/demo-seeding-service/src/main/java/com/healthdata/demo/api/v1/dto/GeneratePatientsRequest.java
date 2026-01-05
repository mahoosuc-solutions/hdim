package com.healthdata.demo.api.v1.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for generating synthetic patients.
 */
public class GeneratePatientsRequest {

    @NotNull(message = "Patient count is required")
    @Min(value = 1, message = "Count must be at least 1")
    @Max(value = 50000, message = "Count cannot exceed 50,000")
    private Integer count;

    @NotBlank(message = "Tenant ID is required")
    private String tenantId;

    @Min(value = 0, message = "Care gap percentage must be at least 0")
    @Max(value = 100, message = "Care gap percentage cannot exceed 100")
    private Integer careGapPercentage;

    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public Integer getCareGapPercentage() { return careGapPercentage; }
    public void setCareGapPercentage(Integer careGapPercentage) { this.careGapPercentage = careGapPercentage; }
}
