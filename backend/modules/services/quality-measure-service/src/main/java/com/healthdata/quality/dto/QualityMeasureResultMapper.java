package com.healthdata.quality.dto;

import com.healthdata.quality.persistence.QualityMeasureResultEntity;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper utility to convert between QualityMeasureResultEntity and QualityMeasureResultDTO
 *
 * This mapper excludes internal implementation details (like raw CQL results)
 * from the DTO to prevent OpenAPI schema generation issues.
 */
public class QualityMeasureResultMapper {

    private QualityMeasureResultMapper() {
        // Utility class - prevent instantiation
    }

    /**
     * Convert entity to DTO
     *
     * @param entity the entity to convert
     * @return the DTO representation
     */
    public static QualityMeasureResultDTO toDTO(QualityMeasureResultEntity entity) {
        if (entity == null) {
            return null;
        }

        return QualityMeasureResultDTO.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .patientId(entity.getPatientId())
                .measureId(entity.getMeasureId())
                .measureName(entity.getMeasureName())
                .measureCategory(entity.getMeasureCategory())
                .measureYear(entity.getMeasureYear())
                .numeratorCompliant(entity.getNumeratorCompliant())
                .denominatorEligible(entity.getDenominatorEligible())
                .complianceRate(entity.getComplianceRate())
                .score(entity.getScore())
                .calculationDate(entity.getCalculationDate())
                .cqlLibrary(entity.getCqlLibrary())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .version(entity.getVersion())
                .build();
    }

    /**
     * Convert list of entities to list of DTOs
     *
     * @param entities the entities to convert
     * @return the DTO representations
     */
    public static List<QualityMeasureResultDTO> toDTOList(List<QualityMeasureResultEntity> entities) {
        if (entities == null) {
            return null;
        }

        return entities.stream()
                .map(QualityMeasureResultMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Convert DTO to entity (for create/update operations)
     *
     * Note: This does not set the cqlResult field as that's managed internally
     *
     * @param dto the DTO to convert
     * @return the entity representation
     */
    public static QualityMeasureResultEntity toEntity(QualityMeasureResultDTO dto) {
        if (dto == null) {
            return null;
        }

        return QualityMeasureResultEntity.builder()
                .id(dto.getId())
                .tenantId(dto.getTenantId())
                .patientId(dto.getPatientId())
                .measureId(dto.getMeasureId())
                .measureName(dto.getMeasureName())
                .measureCategory(dto.getMeasureCategory())
                .measureYear(dto.getMeasureYear())
                .numeratorCompliant(dto.getNumeratorCompliant())
                .denominatorEligible(dto.getDenominatorEligible())
                .complianceRate(dto.getComplianceRate())
                .score(dto.getScore())
                .calculationDate(dto.getCalculationDate())
                .cqlLibrary(dto.getCqlLibrary())
                .createdAt(dto.getCreatedAt())
                .createdBy(dto.getCreatedBy())
                .version(dto.getVersion())
                // Note: cqlResult is not mapped from DTO
                .build();
    }
}
