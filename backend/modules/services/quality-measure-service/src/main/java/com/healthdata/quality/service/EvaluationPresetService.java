package com.healthdata.quality.service;

import com.healthdata.quality.dto.SaveEvaluationPresetRequest;
import com.healthdata.quality.persistence.EvaluationDefaultPresetEntity;
import com.healthdata.quality.persistence.EvaluationDefaultPresetRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EvaluationPresetService {

    static final String SHARED_USER_ID = "shared";

    private final EvaluationDefaultPresetRepository repository;

    public Optional<EvaluationDefaultPresetEntity> findDefaultPreset(String tenantId, String userId) {
        return repository.findByTenantIdAndUserId(tenantId, resolveUserId(userId));
    }

    @Transactional
    public EvaluationDefaultPresetEntity saveDefaultPreset(
        String tenantId,
        String userId,
        SaveEvaluationPresetRequest request
    ) {
        String resolvedUserId = resolveUserId(userId);
        EvaluationDefaultPresetEntity preset = repository.findByTenantIdAndUserId(tenantId, resolvedUserId)
            .orElseGet(EvaluationDefaultPresetEntity::new);

        preset.setTenantId(tenantId);
        preset.setUserId(resolvedUserId);
        preset.setMeasureId(request.getMeasureId());
        preset.setPatientId(request.getPatientId());
        preset.setUseCqlEngine(Boolean.TRUE.equals(request.getUseCqlEngine()));

        return repository.save(preset);
    }

    @Transactional
    public void clearDefaultPreset(String tenantId, String userId) {
        repository.deleteByTenantIdAndUserId(tenantId, resolveUserId(userId));
    }

    private String resolveUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return SHARED_USER_ID;
        }
        return userId;
    }
}
