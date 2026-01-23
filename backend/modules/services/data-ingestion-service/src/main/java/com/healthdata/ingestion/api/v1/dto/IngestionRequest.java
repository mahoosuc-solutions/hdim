package com.healthdata.ingestion.api.v1.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for initiating data ingestion.
 *
 * <p>This request configures the volume and type of data to be ingested into the HDIM platform.
 * The service supports configurable patient counts from 10 to 10,000 for flexible demonstration
 * and testing scenarios.
 *
 * <p><strong>Scenarios:</strong>
 *
 * <ul>
 *   <li><strong>basic:</strong> Simple patient demographics and encounters (minimal data)
 *   <li><strong>hedis:</strong> HEDIS quality measure evaluation focus (default)
 *   <li><strong>complex:</strong> Comprehensive clinical data with procedures, medications, labs
 *   <li><strong>risk-stratification:</strong> Data optimized for HCC risk scoring
 * </ul>
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestionRequest {

  /**
   * Tenant identifier for multi-tenant isolation.
   *
   * <p>All ingested data will be tagged with this tenantId to ensure proper data isolation.
   */
  @NotNull(message = "Tenant ID is required")
  @Pattern(
      regexp = "^[a-z0-9-]{3,63}$",
      message = "Tenant ID must be 3-63 lowercase alphanumeric characters or hyphens")
  private String tenantId;

  /**
   * Number of patients to generate and ingest.
   *
   * <p>Supported range: 10-10,000 patients. Larger volumes take longer to process and should be
   * used for performance testing scenarios.
   */
  @NotNull(message = "Patient count is required")
  @Min(value = 10, message = "Minimum 10 patients required")
  @Max(value = 10000, message = "Maximum 10,000 patients allowed")
  private Integer patientCount;

  /**
   * Whether to create care gaps during ingestion.
   *
   * <p>When enabled, the service will identify and create care gaps based on HEDIS quality measures
   * (e.g., BCS, COL, CBP, CDC, CCS, EED, SPC).
   */
  @Builder.Default private Boolean includeCareGaps = true;

  /**
   * Whether to seed quality measures during ingestion.
   *
   * <p>When enabled, the service will seed HEDIS quality measures and generate evaluation results.
   */
  @Builder.Default private Boolean includeQualityMeasures = true;

  /**
   * Data generation scenario.
   *
   * <p>Supported scenarios:
   *
   * <ul>
   *   <li><strong>basic:</strong> Minimal patient data
   *   <li><strong>hedis:</strong> HEDIS quality measure focus (default)
   *   <li><strong>complex:</strong> Comprehensive clinical data
   *   <li><strong>risk-stratification:</strong> HCC risk scoring focus
   * </ul>
   */
  @Pattern(
      regexp = "^(basic|hedis|complex|risk-stratification)$",
      message = "Scenario must be: basic, hedis, complex, or risk-stratification")
  @Builder.Default
  private String scenario = "hedis";
}
