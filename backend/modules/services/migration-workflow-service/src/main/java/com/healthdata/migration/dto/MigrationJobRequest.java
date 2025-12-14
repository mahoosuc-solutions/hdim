package com.healthdata.migration.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to create a new migration job
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MigrationJobRequest {

    @NotBlank(message = "Tenant ID is required")
    private String tenantId;

    @NotBlank(message = "Job name is required")
    private String jobName;

    private String description;

    @NotNull(message = "Source type is required")
    private SourceType sourceType;

    @Valid
    @NotNull(message = "Source configuration is required")
    private SourceConfig sourceConfig;

    @NotNull(message = "Data type is required")
    private DataType dataType;

    @Builder.Default
    private boolean convertToFhir = true;

    @Builder.Default
    private boolean continueOnError = true;

    @Builder.Default
    @Min(value = 1, message = "Batch size must be at least 1")
    private int batchSize = 100;

    @Builder.Default
    private boolean resumable = true;

    @Builder.Default
    private int maxRetries = 3;

    // Optional: target FHIR server if not using internal service
    private String targetFhirUrl;

    // Optional: callback URL for completion notification
    private String callbackUrl;
}
