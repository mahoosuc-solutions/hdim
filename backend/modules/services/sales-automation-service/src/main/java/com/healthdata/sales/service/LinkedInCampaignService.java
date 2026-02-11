package com.healthdata.sales.service;

import com.healthdata.sales.dto.LinkedInCampaignDTO;
import com.healthdata.sales.entity.LinkedInCampaign;
import com.healthdata.sales.entity.LinkedInCampaign.CampaignStatus;
import com.healthdata.sales.exception.DuplicateResourceException;
import com.healthdata.sales.exception.ResourceNotFoundException;
import com.healthdata.sales.repository.LinkedInCampaignRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * LinkedIn Campaign Service
 *
 * Manages LinkedIn outreach campaigns with full CRUD operations.
 * Campaigns group outreach activities and track aggregate metrics.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LinkedInCampaignService {

    private final LinkedInCampaignRepository campaignRepository;

    // ==================== CRUD Operations ====================

    /**
     * Get all campaigns for a tenant
     */
    @Transactional(readOnly = true)
    public Page<LinkedInCampaignDTO> findAll(UUID tenantId, Pageable pageable) {
        return campaignRepository.findByTenantId(tenantId, pageable)
            .map(LinkedInCampaignDTO::fromEntity);
    }

    /**
     * Get all campaigns filtered by status
     */
    @Transactional(readOnly = true)
    public Page<LinkedInCampaignDTO> findByStatus(UUID tenantId, CampaignStatus status, Pageable pageable) {
        return campaignRepository.findByTenantIdAndStatus(tenantId, status, pageable)
            .map(LinkedInCampaignDTO::fromEntity);
    }

    /**
     * Search campaigns by name
     */
    @Transactional(readOnly = true)
    public Page<LinkedInCampaignDTO> searchByName(UUID tenantId, String search, Pageable pageable) {
        return campaignRepository.searchByName(tenantId, search, pageable)
            .map(LinkedInCampaignDTO::fromEntity);
    }

    /**
     * Get a single campaign by ID
     */
    @Transactional(readOnly = true)
    public Optional<LinkedInCampaignDTO> findById(UUID tenantId, UUID id) {
        return campaignRepository.findByIdAndTenantId(id, tenantId)
            .map(LinkedInCampaignDTO::fromEntity);
    }

    /**
     * Create a new campaign
     */
    @Transactional
    public LinkedInCampaignDTO create(UUID tenantId, LinkedInCampaignDTO dto, UUID createdBy) {
        // Check for duplicate name
        if (campaignRepository.existsByTenantIdAndName(tenantId, dto.getName())) {
            throw new DuplicateResourceException("Campaign with name '" + dto.getName() + "' already exists");
        }

        LinkedInCampaign campaign = dto.toEntity(tenantId);
        campaign.setCreatedBy(createdBy);
        campaign.setStatus(CampaignStatus.DRAFT);

        LinkedInCampaign saved = campaignRepository.save(campaign);
        log.info("Created LinkedIn campaign: {} ({})", saved.getName(), saved.getId());

        return LinkedInCampaignDTO.fromEntity(saved);
    }

    /**
     * Update an existing campaign
     */
    @Transactional
    public LinkedInCampaignDTO update(UUID tenantId, UUID id, LinkedInCampaignDTO dto) {
        LinkedInCampaign campaign = campaignRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Campaign not found: " + id));

        // Check for duplicate name if name is being changed
        if (dto.getName() != null && !dto.getName().equals(campaign.getName())) {
            if (campaignRepository.existsByTenantIdAndName(tenantId, dto.getName())) {
                throw new DuplicateResourceException("Campaign with name '" + dto.getName() + "' already exists");
            }
        }

        dto.updateEntity(campaign);
        LinkedInCampaign saved = campaignRepository.save(campaign);
        log.info("Updated LinkedIn campaign: {} ({})", saved.getName(), saved.getId());

        return LinkedInCampaignDTO.fromEntity(saved);
    }

    /**
     * Delete a campaign
     */
    @Transactional
    public void delete(UUID tenantId, UUID id) {
        LinkedInCampaign campaign = campaignRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Campaign not found: " + id));

        campaignRepository.delete(campaign);
        log.info("Deleted LinkedIn campaign: {} ({})", campaign.getName(), campaign.getId());
    }

    // ==================== Status Operations ====================

    /**
     * Activate a campaign
     */
    @Transactional
    public LinkedInCampaignDTO activate(UUID tenantId, UUID id) {
        LinkedInCampaign campaign = campaignRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Campaign not found: " + id));

        campaign.activate();
        LinkedInCampaign saved = campaignRepository.save(campaign);
        log.info("Activated LinkedIn campaign: {} ({})", saved.getName(), saved.getId());

        return LinkedInCampaignDTO.fromEntity(saved);
    }

    /**
     * Pause a campaign
     */
    @Transactional
    public LinkedInCampaignDTO pause(UUID tenantId, UUID id) {
        LinkedInCampaign campaign = campaignRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Campaign not found: " + id));

        campaign.pause();
        LinkedInCampaign saved = campaignRepository.save(campaign);
        log.info("Paused LinkedIn campaign: {} ({})", saved.getName(), saved.getId());

        return LinkedInCampaignDTO.fromEntity(saved);
    }

    /**
     * Complete a campaign
     */
    @Transactional
    public LinkedInCampaignDTO complete(UUID tenantId, UUID id) {
        LinkedInCampaign campaign = campaignRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Campaign not found: " + id));

        campaign.complete();
        LinkedInCampaign saved = campaignRepository.save(campaign);
        log.info("Completed LinkedIn campaign: {} ({})", saved.getName(), saved.getId());

        return LinkedInCampaignDTO.fromEntity(saved);
    }

    // ==================== Metrics Updates ====================

    /**
     * Increment sent count for a campaign
     */
    @Transactional
    public void incrementSent(UUID tenantId, UUID campaignId) {
        campaignRepository.findByIdAndTenantId(campaignId, tenantId)
            .ifPresent(campaign -> {
                campaign.incrementSent();
                campaignRepository.save(campaign);
            });
    }

    /**
     * Increment accepted count for a campaign
     */
    @Transactional
    public void incrementAccepted(UUID tenantId, UUID campaignId) {
        campaignRepository.findByIdAndTenantId(campaignId, tenantId)
            .ifPresent(campaign -> {
                campaign.incrementAccepted();
                campaignRepository.save(campaign);
            });
    }

    /**
     * Increment replied count for a campaign
     */
    @Transactional
    public void incrementReplied(UUID tenantId, UUID campaignId) {
        campaignRepository.findByIdAndTenantId(campaignId, tenantId)
            .ifPresent(campaign -> {
                campaign.incrementReplied();
                campaignRepository.save(campaign);
            });
    }
}
