package com.healthdata.quality.dto;

import com.healthdata.quality.persistence.EvaluationDefaultPresetEntity;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EvaluationDefaultPresetDTO {
    private String measureId;
    private UUID patientId;
    private Boolean useCqlEngine;
    private OffsetDateTime savedAt;

    public static EvaluationDefaultPresetDTO fromEntity(EvaluationDefaultPresetEntity entity) {
        if (entity == null) {
            return null;
        }
        OffsetDateTime savedAt = entity.getUpdatedAt() != null ? entity.getUpdatedAt() : entity.getCreatedAt();
        return EvaluationDefaultPresetDTO.builder()
            .measureId(entity.getMeasureId())
            .patientId(entity.getPatientId())
            .useCqlEngine(entity.getUseCqlEngine())
            .savedAt(savedAt)
            .build();
    }
}
