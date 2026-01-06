package com.healthdata.quality.dto.versioning;

import com.healthdata.quality.service.MeasureVersionService.VersionComparisonResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * DTO for version comparison results.
 *
 * Issue #152: Measure Versioning and Audit Trail
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionComparisonDTO {

    private String version1;
    private String version2;
    private boolean cqlChanged;
    private boolean valueSetsChanged;
    private String cql1;
    private String cql2;
    private String valueSets1;
    private String valueSets2;
    private OffsetDateTime createdAt1;
    private OffsetDateTime createdAt2;

    public static VersionComparisonDTO fromResult(VersionComparisonResult result) {
        if (result == null) {
            return null;
        }
        return VersionComparisonDTO.builder()
                .version1(result.version1())
                .version2(result.version2())
                .cqlChanged(result.cqlChanged())
                .valueSetsChanged(result.valueSetsChanged())
                .cql1(result.cql1())
                .cql2(result.cql2())
                .valueSets1(result.valueSets1())
                .valueSets2(result.valueSets2())
                .createdAt1(result.createdAt1())
                .createdAt2(result.createdAt2())
                .build();
    }
}
