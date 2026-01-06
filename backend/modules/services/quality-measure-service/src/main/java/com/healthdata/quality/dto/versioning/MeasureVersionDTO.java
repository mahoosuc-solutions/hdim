package com.healthdata.quality.dto.versioning;

import com.healthdata.quality.persistence.MeasureVersionEntity;
import com.healthdata.quality.persistence.MeasureVersionEntity.VersionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO for measure version data.
 *
 * Issue #152: Measure Versioning and Audit Trail
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeasureVersionDTO {

    private UUID id;
    private UUID measureId;
    private String version;
    private VersionType versionType;
    private String cqlText;
    private String valueSets;
    private String metadata;
    private String changeSummary;
    private UUID createdBy;
    private String createdByName;
    private OffsetDateTime createdAt;
    private Boolean isCurrent;
    private Boolean isPublished;
    private OffsetDateTime publishedAt;
    private UUID publishedBy;

    /**
     * Convert entity to DTO.
     */
    public static MeasureVersionDTO fromEntity(MeasureVersionEntity entity) {
        if (entity == null) {
            return null;
        }
        return MeasureVersionDTO.builder()
                .id(entity.getId())
                .measureId(entity.getMeasureId())
                .version(entity.getVersion())
                .versionType(entity.getVersionType())
                .cqlText(entity.getCqlText())
                .valueSets(entity.getValueSets())
                .metadata(entity.getMetadata())
                .changeSummary(entity.getChangeSummary())
                .createdBy(entity.getCreatedBy())
                .createdByName(entity.getCreatedByName())
                .createdAt(entity.getCreatedAt())
                .isCurrent(entity.getIsCurrent())
                .isPublished(entity.getIsPublished())
                .publishedAt(entity.getPublishedAt())
                .publishedBy(entity.getPublishedBy())
                .build();
    }

    /**
     * Convert entity to summary DTO (without CQL text for list views).
     */
    public static MeasureVersionDTO fromEntitySummary(MeasureVersionEntity entity) {
        if (entity == null) {
            return null;
        }
        return MeasureVersionDTO.builder()
                .id(entity.getId())
                .measureId(entity.getMeasureId())
                .version(entity.getVersion())
                .versionType(entity.getVersionType())
                .changeSummary(entity.getChangeSummary())
                .createdBy(entity.getCreatedBy())
                .createdByName(entity.getCreatedByName())
                .createdAt(entity.getCreatedAt())
                .isCurrent(entity.getIsCurrent())
                .isPublished(entity.getIsPublished())
                .publishedAt(entity.getPublishedAt())
                .build();
    }
}
