package com.healthdata.patient.service;

import com.healthdata.audit.annotations.Audited;
import com.healthdata.patient.entity.ProviderPanelAssignmentEntity;
import com.healthdata.patient.repository.ProviderPanelAssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing provider panel assignments.
 * Issue #135: Create Provider Panel Assignment API
 *
 * HIPAA Compliance:
 * - All PHI access methods are audited
 * - Cache TTL is limited to 5 minutes (300 seconds)
 * - Multi-tenant filtering is enforced
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProviderPanelService {

    private final ProviderPanelAssignmentRepository assignmentRepository;

    /**
     * Get provider's patient panel with pagination.
     * Cache TTL: 5 minutes max for HIPAA compliance
     */
    @Audited(eventType = "PROVIDER_PANEL_ACCESS", description = "Provider panel lookup")
    @Cacheable(value = "providerPanel", key = "#tenantId + ':' + #providerId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<ProviderPanelAssignmentEntity> getProviderPanel(
            String tenantId,
            UUID providerId,
            Pageable pageable) {
        log.debug("Getting provider panel for provider {} in tenant {}", providerId, tenantId);
        return assignmentRepository.findActiveByTenantAndProvider(tenantId, providerId, pageable);
    }

    /**
     * Get all patient IDs in a provider's panel.
     */
    @Audited(eventType = "PROVIDER_PANEL_ACCESS", description = "Provider panel patient IDs lookup")
    @Cacheable(value = "providerPanelIds", key = "#tenantId + ':' + #providerId")
    public List<UUID> getProviderPanelPatientIds(String tenantId, UUID providerId) {
        log.debug("Getting patient IDs for provider {} in tenant {}", providerId, tenantId);
        return assignmentRepository.findPatientIdsByTenantAndProvider(tenantId, providerId);
    }

    /**
     * Get count of patients in provider's panel.
     */
    @Audited(eventType = "PROVIDER_PANEL_ACCESS", description = "Provider panel count")
    @Cacheable(value = "providerPanelCount", key = "#tenantId + ':' + #providerId")
    public long getProviderPanelCount(String tenantId, UUID providerId) {
        return assignmentRepository.countActiveByTenantAndProvider(tenantId, providerId);
    }

    /**
     * Check if a patient is assigned to a provider.
     */
    public boolean isPatientInPanel(String tenantId, UUID providerId, UUID patientId) {
        return assignmentRepository.isPatientAssignedToProvider(tenantId, providerId, patientId);
    }

    /**
     * Get all providers assigned to a patient.
     */
    @Audited(eventType = "PATIENT_PROVIDER_ACCESS", description = "Patient's provider lookup")
    public List<ProviderPanelAssignmentEntity> getPatientProviders(String tenantId, UUID patientId) {
        return assignmentRepository.findProvidersByPatient(tenantId, patientId);
    }

    /**
     * Assign a patient to a provider's panel.
     */
    @Transactional
    @Audited(eventType = "PROVIDER_PANEL_MODIFY", description = "Assign patient to provider")
    @CacheEvict(value = {"providerPanel", "providerPanelIds", "providerPanelCount"}, allEntries = true)
    public ProviderPanelAssignmentEntity assignPatientToProvider(
            String tenantId,
            UUID providerId,
            UUID patientId,
            ProviderPanelAssignmentEntity.AssignmentType assignmentType,
            String notes) {

        log.info("Assigning patient {} to provider {} in tenant {}", patientId, providerId, tenantId);

        // Check for existing assignment
        Optional<ProviderPanelAssignmentEntity> existing =
                assignmentRepository.findByTenantIdAndProviderIdAndPatientId(tenantId, providerId, patientId);

        if (existing.isPresent()) {
            // Reactivate if inactive
            ProviderPanelAssignmentEntity assignment = existing.get();
            if (!assignment.getActive()) {
                assignment.setActive(true);
                assignment.setAssignmentType(assignmentType);
                assignment.setNotes(notes);
                return assignmentRepository.save(assignment);
            }
            // Already active, update type/notes if different
            assignment.setAssignmentType(assignmentType);
            assignment.setNotes(notes);
            return assignmentRepository.save(assignment);
        }

        // Create new assignment
        ProviderPanelAssignmentEntity assignment = ProviderPanelAssignmentEntity.builder()
                .tenantId(tenantId)
                .providerId(providerId)
                .patientId(patientId)
                .assignmentType(assignmentType)
                .assignedDate(Instant.now())
                .active(true)
                .notes(notes)
                .build();

        return assignmentRepository.save(assignment);
    }

    /**
     * Remove a patient from a provider's panel (soft delete).
     */
    @Transactional
    @Audited(eventType = "PROVIDER_PANEL_MODIFY", description = "Remove patient from provider")
    @CacheEvict(value = {"providerPanel", "providerPanelIds", "providerPanelCount"}, allEntries = true)
    public void removePatientFromProvider(String tenantId, UUID providerId, UUID patientId) {
        log.info("Removing patient {} from provider {} in tenant {}", patientId, providerId, tenantId);

        assignmentRepository.findByTenantIdAndProviderIdAndPatientId(tenantId, providerId, patientId)
                .ifPresent(assignment -> {
                    assignment.setActive(false);
                    assignmentRepository.save(assignment);
                });
    }

    /**
     * Bulk assign patients to a provider.
     */
    @Transactional
    @Audited(eventType = "PROVIDER_PANEL_BULK_MODIFY", description = "Bulk assign patients to provider")
    @CacheEvict(value = {"providerPanel", "providerPanelIds", "providerPanelCount"}, allEntries = true)
    public int bulkAssignPatientsToProvider(
            String tenantId,
            UUID providerId,
            List<UUID> patientIds,
            ProviderPanelAssignmentEntity.AssignmentType assignmentType) {

        log.info("Bulk assigning {} patients to provider {} in tenant {}", patientIds.size(), providerId, tenantId);

        int assigned = 0;
        for (UUID patientId : patientIds) {
            assignPatientToProvider(tenantId, providerId, patientId, assignmentType, "Bulk assignment");
            assigned++;
        }
        return assigned;
    }
}
