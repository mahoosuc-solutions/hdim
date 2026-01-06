package com.healthdata.quality.dto.versioning;

import com.healthdata.quality.persistence.MeasureVersionEntity.VersionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request to create a new measure version.
 *
 * Issue #152: Measure Versioning and Audit Trail
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateVersionRequest {

    @NotNull(message = "Measure ID is required")
    private UUID measureId;

    @NotNull(message = "Version type is required")
    private VersionType versionType;

    @NotBlank(message = "CQL text is required")
    private String cqlText;

    private String valueSets;

    private String changeSummary;

    private String metadata;
}
