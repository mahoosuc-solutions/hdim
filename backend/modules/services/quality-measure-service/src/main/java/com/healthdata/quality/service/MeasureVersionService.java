package com.healthdata.quality.service;

import com.healthdata.quality.persistence.CustomMeasureEntity;
import com.healthdata.quality.persistence.CustomMeasureRepository;
import com.healthdata.quality.persistence.MeasureVersionEntity;
import com.healthdata.quality.persistence.MeasureVersionEntity.VersionType;
import com.healthdata.quality.persistence.MeasureVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing measure version history.
 *
 * HIPAA COMPLIANCE: This service maintains immutable version records.
 * Once a version is created, its CQL content cannot be modified.
 * Only metadata like is_current and is_published can be updated.
 *
 * Issue #152: Measure Versioning and Audit Trail
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MeasureVersionService {

    private final MeasureVersionRepository versionRepository;
    private final CustomMeasureRepository measureRepository;

    /**
     * Create a new version for a custom measure.
     *
     * @param tenantId Tenant identifier
     * @param measureId Custom measure ID
     * @param versionType Type of version change (MAJOR, MINOR, PATCH)
     * @param cqlText CQL code for this version
     * @param valueSets JSON string of value sets
     * @param changeSummary Description of changes
     * @param createdBy User ID creating the version
     * @param createdByName User name for display
     * @return Created version entity
     */
    @Transactional
    public MeasureVersionEntity createVersion(
            String tenantId,
            UUID measureId,
            VersionType versionType,
            String cqlText,
            String valueSets,
            String changeSummary,
            UUID createdBy,
            String createdByName) {

        // Validate measure exists
        CustomMeasureEntity measure = measureRepository.findByTenantIdAndId(tenantId, measureId)
                .orElseThrow(() -> new NoSuchElementException("Custom measure not found: " + measureId));

        // Get current version to calculate next version number
        Optional<MeasureVersionEntity> latestVersion = versionRepository.findLatestVersion(tenantId, measureId);
        String currentVersion = latestVersion.map(MeasureVersionEntity::getVersion).orElse("0.0.0");
        String nextVersion = MeasureVersionEntity.nextVersion(currentVersion, versionType);

        // Validate version doesn't already exist
        if (versionRepository.existsByTenantIdAndMeasureIdAndVersion(tenantId, measureId, nextVersion)) {
            throw new IllegalStateException("Version " + nextVersion + " already exists for measure " + measureId);
        }

        // Clear current flag on all existing versions
        versionRepository.clearCurrentFlag(tenantId, measureId);

        // Create new version
        MeasureVersionEntity newVersion = MeasureVersionEntity.builder()
                .tenantId(tenantId)
                .measureId(measureId)
                .version(nextVersion)
                .versionType(versionType)
                .cqlText(cqlText)
                .valueSets(valueSets)
                .changeSummary(changeSummary)
                .createdBy(createdBy)
                .createdByName(createdByName)
                .isCurrent(true)
                .isPublished(false)
                .build();

        MeasureVersionEntity saved = versionRepository.save(newVersion);

        // Update the measure's version field
        measure.setVersion(nextVersion);
        measure.setCqlText(cqlText);
        measure.setValueSets(valueSets);
        measureRepository.save(measure);

        log.info("Created version {} (type={}) for measure {} in tenant {}",
                nextVersion, versionType, measureId, tenantId);

        return saved;
    }

    /**
     * Create the initial version for a new measure.
     */
    @Transactional
    public MeasureVersionEntity createInitialVersion(
            String tenantId,
            UUID measureId,
            String cqlText,
            String valueSets,
            UUID createdBy,
            String createdByName) {

        // Validate measure exists
        CustomMeasureEntity measure = measureRepository.findByTenantIdAndId(tenantId, measureId)
                .orElseThrow(() -> new NoSuchElementException("Custom measure not found: " + measureId));

        // Validate no versions exist yet
        if (versionRepository.countByTenantIdAndMeasureId(tenantId, measureId) > 0) {
            throw new IllegalStateException("Measure already has versions. Use createVersion() instead.");
        }

        MeasureVersionEntity initialVersion = MeasureVersionEntity.builder()
                .tenantId(tenantId)
                .measureId(measureId)
                .version("1.0.0")
                .versionType(VersionType.MAJOR)
                .cqlText(cqlText)
                .valueSets(valueSets)
                .changeSummary("Initial version")
                .createdBy(createdBy)
                .createdByName(createdByName)
                .isCurrent(true)
                .isPublished(false)
                .build();

        MeasureVersionEntity saved = versionRepository.save(initialVersion);

        // Update measure version
        measure.setVersion("1.0.0");
        measure.setCqlText(cqlText);
        measure.setValueSets(valueSets);
        measureRepository.save(measure);

        log.info("Created initial version 1.0.0 for measure {} in tenant {}", measureId, tenantId);

        return saved;
    }

    /**
     * Get all versions for a measure.
     */
    @Transactional(readOnly = true)
    public List<MeasureVersionEntity> getVersionHistory(String tenantId, UUID measureId) {
        return versionRepository.findByTenantIdAndMeasureIdOrderByCreatedAtDesc(tenantId, measureId);
    }

    /**
     * Get version history with pagination.
     */
    @Transactional(readOnly = true)
    public Page<MeasureVersionEntity> getVersionHistory(String tenantId, UUID measureId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return versionRepository.findByTenantIdAndMeasureIdOrderByCreatedAtDesc(tenantId, measureId, pageable);
    }

    /**
     * Get version summaries for dropdowns/lists (lightweight).
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getVersionSummaries(String tenantId, UUID measureId) {
        return versionRepository.findVersionSummaries(tenantId, measureId);
    }

    /**
     * Get a specific version.
     */
    @Transactional(readOnly = true)
    public MeasureVersionEntity getVersion(String tenantId, UUID measureId, String version) {
        return versionRepository.findByTenantIdAndMeasureIdAndVersion(tenantId, measureId, version)
                .orElseThrow(() -> new NoSuchElementException(
                        "Version " + version + " not found for measure " + measureId));
    }

    /**
     * Get a version by ID.
     */
    @Transactional(readOnly = true)
    public MeasureVersionEntity getVersionById(String tenantId, UUID versionId) {
        return versionRepository.findById(versionId)
                .filter(v -> v.getTenantId().equals(tenantId))
                .orElseThrow(() -> new NoSuchElementException("Version not found: " + versionId));
    }

    /**
     * Get the current active version.
     */
    @Transactional(readOnly = true)
    public Optional<MeasureVersionEntity> getCurrentVersion(String tenantId, UUID measureId) {
        return versionRepository.findByTenantIdAndMeasureIdAndIsCurrentTrue(tenantId, measureId);
    }

    /**
     * Get the latest version (regardless of current flag).
     */
    @Transactional(readOnly = true)
    public Optional<MeasureVersionEntity> getLatestVersion(String tenantId, UUID measureId) {
        return versionRepository.findLatestVersion(tenantId, measureId);
    }

    /**
     * Set a specific version as the current version.
     * This is useful for rolling back to a previous version.
     */
    @Transactional
    public MeasureVersionEntity setCurrentVersion(String tenantId, UUID measureId, String version) {
        MeasureVersionEntity targetVersion = getVersion(tenantId, measureId, version);

        // Clear current flag on all versions
        versionRepository.clearCurrentFlag(tenantId, measureId);

        // Set target as current
        targetVersion.setIsCurrent(true);
        MeasureVersionEntity saved = versionRepository.save(targetVersion);

        // Update the measure to reflect the current version
        CustomMeasureEntity measure = measureRepository.findByTenantIdAndId(tenantId, measureId)
                .orElseThrow(() -> new NoSuchElementException("Custom measure not found: " + measureId));
        measure.setVersion(version);
        measure.setCqlText(targetVersion.getCqlText());
        measure.setValueSets(targetVersion.getValueSets());
        measureRepository.save(measure);

        log.info("Set version {} as current for measure {} in tenant {}", version, measureId, tenantId);

        return saved;
    }

    /**
     * Publish a version.
     * Publishing marks the version as production-ready and records the publish timestamp.
     */
    @Transactional
    public MeasureVersionEntity publishVersion(
            String tenantId,
            UUID measureId,
            String version,
            UUID publishedBy) {

        MeasureVersionEntity targetVersion = getVersion(tenantId, measureId, version);

        if (targetVersion.getIsPublished()) {
            log.warn("Version {} for measure {} is already published", version, measureId);
            return targetVersion;
        }

        targetVersion.setIsPublished(true);
        targetVersion.setPublishedAt(OffsetDateTime.now());
        targetVersion.setPublishedBy(publishedBy);

        MeasureVersionEntity saved = versionRepository.save(targetVersion);

        log.info("Published version {} for measure {} in tenant {}", version, measureId, tenantId);

        return saved;
    }

    /**
     * Get all published versions for a measure.
     */
    @Transactional(readOnly = true)
    public List<MeasureVersionEntity> getPublishedVersions(String tenantId, UUID measureId) {
        return versionRepository.findByTenantIdAndMeasureIdAndIsPublishedTrueOrderByCreatedAtDesc(
                tenantId, measureId);
    }

    /**
     * Retire a version.
     * Retiring marks the version as no longer in use.
     */
    @Transactional
    public MeasureVersionEntity retireVersion(
            String tenantId,
            UUID measureId,
            String version,
            UUID retiredBy) {

        MeasureVersionEntity targetVersion = getVersion(tenantId, measureId, version);

        // Mark as retired by clearing published flag and setting a retire indicator
        targetVersion.setIsPublished(false);
        targetVersion.setIsCurrent(false);

        MeasureVersionEntity saved = versionRepository.save(targetVersion);

        log.info("Retired version {} for measure {} in tenant {}", version, measureId, tenantId);

        return saved;
    }

    /**
     * Compare two versions (returns diff information).
     */
    @Transactional(readOnly = true)
    public VersionComparisonResult compareVersions(
            String tenantId,
            UUID measureId,
            String version1,
            String version2) {

        MeasureVersionEntity v1 = getVersion(tenantId, measureId, version1);
        MeasureVersionEntity v2 = getVersion(tenantId, measureId, version2);

        boolean cqlChanged = !v1.getCqlText().equals(v2.getCqlText());
        boolean valueSetsChanged = (v1.getValueSets() == null && v2.getValueSets() != null) ||
                (v1.getValueSets() != null && !v1.getValueSets().equals(v2.getValueSets()));

        return new VersionComparisonResult(
                v1.getVersion(),
                v2.getVersion(),
                cqlChanged,
                valueSetsChanged,
                v1.getCqlText(),
                v2.getCqlText(),
                v1.getValueSets(),
                v2.getValueSets(),
                v1.getCreatedAt(),
                v2.getCreatedAt()
        );
    }

    /**
     * Get version count for a measure.
     */
    @Transactional(readOnly = true)
    public long getVersionCount(String tenantId, UUID measureId) {
        return versionRepository.countByTenantIdAndMeasureId(tenantId, measureId);
    }

    /**
     * Get all versions created by a user (for user activity audit).
     */
    @Transactional(readOnly = true)
    public List<MeasureVersionEntity> getVersionsByUser(String tenantId, UUID userId) {
        return versionRepository.findByTenantIdAndCreatedByOrderByCreatedAtDesc(tenantId, userId);
    }

    /**
     * Get all versions in tenant with pagination (admin audit view).
     */
    @Transactional(readOnly = true)
    public Page<MeasureVersionEntity> getAllVersions(String tenantId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return versionRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
    }

    // Result records

    public record VersionComparisonResult(
            String version1,
            String version2,
            boolean cqlChanged,
            boolean valueSetsChanged,
            String cql1,
            String cql2,
            String valueSets1,
            String valueSets2,
            OffsetDateTime createdAt1,
            OffsetDateTime createdAt2
    ) {}
}
