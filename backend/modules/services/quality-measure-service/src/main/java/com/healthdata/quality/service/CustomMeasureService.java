package com.healthdata.quality.service;

import com.healthdata.quality.persistence.CustomMeasureEntity;
import com.healthdata.quality.persistence.CustomMeasureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomMeasureService {

    private final CustomMeasureRepository repository;

    @Transactional
    public CustomMeasureEntity createDraft(String tenantId, String name, String description, String category, Integer year, String createdBy) {
        CustomMeasureEntity entity = CustomMeasureEntity.builder()
                .tenantId(tenantId)
                .name(name)
                .description(description)
                .category(category)
                .year(year)
                .status("DRAFT")
                .createdBy(createdBy)
                .build();
        CustomMeasureEntity saved = repository.save(entity);
        log.info("Created custom measure draft [{}] for tenant {}", saved.getId(), tenantId);
        return saved;
    }

    @Transactional
    public CustomMeasureEntity updateDraft(String tenantId, UUID id, String name, String description, String category, Integer year) {
        CustomMeasureEntity existing = getById(tenantId, id);
        if (name != null && !name.isBlank()) {
            existing.setName(name);
        }
        if (description != null) {
            existing.setDescription(description);
        }
        if (category != null) {
            existing.setCategory(category);
        }
        if (year != null) {
            existing.setYear(year);
        }
        CustomMeasureEntity saved = repository.save(existing);
        log.info("Updated custom measure draft [{}] for tenant {}", saved.getId(), tenantId);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<CustomMeasureEntity> list(String tenantId, String status) {
        if (status != null && !status.isBlank()) {
            return repository.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, status);
        }
        return repository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    @Transactional(readOnly = true)
    public CustomMeasureEntity getById(String tenantId, UUID id) {
        return repository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new NoSuchElementException("Custom measure not found"));
    }

    /**
     * Batch publish draft measures.
     * Only DRAFT measures are published. Already published measures are skipped.
     */
    @Transactional
    public BatchPublishResult batchPublish(String tenantId, List<UUID> measureIds, String publishedBy) {
        log.info("Batch publishing {} measures for tenant {}", measureIds.size(), tenantId);

        List<CustomMeasureEntity> measures = repository.findByTenantIdAndIdIn(tenantId, measureIds);

        // Validate that all requested IDs were found
        if (measures.size() != measureIds.size()) {
            List<UUID> foundIds = measures.stream().map(CustomMeasureEntity::getId).collect(Collectors.toList());
            List<UUID> notFound = measureIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toList());
            throw new IllegalArgumentException("Measures not found or not accessible: " + notFound);
        }

        int publishedCount = 0;
        int skippedCount = 0;
        List<String> errors = new ArrayList<>();

        for (CustomMeasureEntity measure : measures) {
            if ("DRAFT".equals(measure.getStatus())) {
                measure.setStatus("PUBLISHED");
                measure.setPublishedDate(LocalDateTime.now());
                repository.save(measure);
                publishedCount++;
                log.info("Published measure [{}] for tenant {}", measure.getId(), tenantId);
            } else {
                skippedCount++;
                log.info("Skipped measure [{}] - already published (status: {})", measure.getId(), measure.getStatus());
            }
        }

        log.info("Batch publish complete: {} published, {} skipped", publishedCount, skippedCount);
        return new BatchPublishResult(publishedCount, skippedCount, 0, errors);
    }

    /**
     * Batch delete custom measures.
     * Uses soft delete (sets deletedAt timestamp) for HIPAA compliance.
     */
    @Transactional
    public BatchDeleteResult batchDelete(String tenantId, List<UUID> measureIds, String deletedBy, boolean force) {
        log.info("Batch deleting {} measures for tenant {} (force={})", measureIds.size(), tenantId, force);

        List<CustomMeasureEntity> measures = repository.findByTenantIdAndIdIn(tenantId, measureIds);

        // Validate that all requested IDs were found
        if (measures.size() != measureIds.size()) {
            List<UUID> foundIds = measures.stream().map(CustomMeasureEntity::getId).collect(Collectors.toList());
            List<UUID> notFound = measureIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toList());
            throw new IllegalArgumentException("Measures not found or not accessible: " + notFound);
        }

        // Check which measures are in use (have evaluations)
        List<UUID> measureIdsToCheck = measures.stream().map(CustomMeasureEntity::getId).collect(Collectors.toList());
        long evaluationsCount = repository.countEvaluationsByMeasureIds(measureIdsToCheck);

        int deletedCount = 0;
        int failedCount = 0;
        List<String> errors = new ArrayList<>();
        List<String> measuresInUse = new ArrayList<>();

        for (CustomMeasureEntity measure : measures) {
            // For simplicity, we'll allow deletion if force=true or if no evaluations exist
            if (!force && evaluationsCount > 0) {
                failedCount++;
                measuresInUse.add(measure.getId().toString());
                errors.add("Measure " + measure.getId() + " has evaluations and cannot be deleted without force flag");
                log.warn("Cannot delete measure [{}] - has evaluations", measure.getId());
            } else {
                // Soft delete
                measure.setDeletedAt(LocalDateTime.now());
                measure.setDeletedBy(deletedBy);
                repository.save(measure);
                deletedCount++;
                log.info("Soft deleted measure [{}] for tenant {}", measure.getId(), tenantId);
            }
        }

        log.info("Batch delete complete: {} deleted, {} failed", deletedCount, failedCount);
        return new BatchDeleteResult(deletedCount, failedCount, errors, measuresInUse);
    }

    /**
     * Delete a single custom measure (soft delete).
     */
    @Transactional
    public void delete(String tenantId, UUID id, String deletedBy) {
        CustomMeasureEntity measure = getById(tenantId, id);
        measure.setDeletedAt(LocalDateTime.now());
        measure.setDeletedBy(deletedBy);
        repository.save(measure);
        log.info("Soft deleted measure [{}] for tenant {}", id, tenantId);
    }

    // Result classes
    public record BatchPublishResult(
            int publishedCount,
            int skippedCount,
            int failedCount,
            List<String> errors
    ) {}

    public record BatchDeleteResult(
            int deletedCount,
            int failedCount,
            List<String> errors,
            List<String> measuresInUse
    ) {}
}
