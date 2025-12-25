package com.healthdata.sales.service;

import com.healthdata.sales.client.ZohoClient;
import com.healthdata.sales.dto.*;
import com.healthdata.sales.entity.*;
import com.healthdata.sales.exception.*;
import com.healthdata.sales.mapper.LeadMapper;
import com.healthdata.sales.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class LeadService {

    private final LeadRepository leadRepository;
    private final AccountRepository accountRepository;
    private final ContactRepository contactRepository;
    private final OpportunityRepository opportunityRepository;
    private final LeadMapper leadMapper;
    private final ZohoClient zohoClient;
    private final EmailSequenceService emailSequenceService;
    private final EmailSequenceRepository emailSequenceRepository;

    @Transactional(readOnly = true)
    public Page<LeadDTO> findAll(UUID tenantId, Pageable pageable) {
        return leadRepository.findByTenantId(tenantId, pageable)
            .map(leadMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public LeadDTO findById(UUID tenantId, UUID id) {
        Lead lead = leadRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new LeadNotFoundException(id));
        return leadMapper.toDTO(lead);
    }

    @Transactional
    public LeadDTO create(UUID tenantId, LeadDTO dto) {
        dto.setTenantId(tenantId);
        Lead lead = leadMapper.toEntity(dto);
        lead.calculateScore();
        lead = leadRepository.save(lead);
        log.info("Created lead {} for tenant {}", lead.getId(), tenantId);
        return leadMapper.toDTO(lead);
    }

    @Transactional
    public LeadDTO captureLead(UUID tenantId, LeadCaptureRequest request) {
        // Check for duplicate
        if (leadRepository.existsByEmailAndTenantId(request.getEmail(), tenantId)) {
            Lead existing = leadRepository.findByEmailAndTenantId(request.getEmail(), tenantId)
                .orElseThrow();
            log.info("Lead already exists for email {}, updating", request.getEmail());
            // Update existing lead
            if (request.getNotes() != null) {
                String updatedNotes = existing.getNotes() != null
                    ? existing.getNotes() + "\n---\n" + request.getNotes()
                    : request.getNotes();
                existing.setNotes(updatedNotes);
            }
            existing = leadRepository.save(existing);
            return leadMapper.toDTO(existing);
        }

        Lead lead = leadMapper.fromCaptureRequest(request, tenantId);
        lead.calculateScore();
        lead = leadRepository.save(lead);
        log.info("Captured new lead {} from source {} for tenant {}",
            lead.getId(), lead.getSource(), tenantId);

        // Trigger Zoho sync (async to not block response)
        triggerZohoSync(lead);

        // Auto-enroll in welcome sequence if available
        autoEnrollInWelcomeSequence(tenantId, lead);

        return leadMapper.toDTO(lead);
    }

    @Transactional
    public LeadDTO update(UUID tenantId, UUID id, LeadDTO dto) {
        Lead lead = leadRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new LeadNotFoundException(id));

        leadMapper.updateEntity(lead, dto);
        lead.calculateScore();
        lead = leadRepository.save(lead);
        log.info("Updated lead {}", lead.getId());
        return leadMapper.toDTO(lead);
    }

    @Transactional
    public void delete(UUID tenantId, UUID id) {
        Lead lead = leadRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new LeadNotFoundException(id));
        leadRepository.delete(lead);
        log.info("Deleted lead {}", id);
    }

    @Transactional
    public LeadDTO convertLead(UUID tenantId, UUID leadId, LeadConversionRequest request) {
        Lead lead = leadRepository.findByIdAndTenantId(leadId, tenantId)
            .orElseThrow(() -> new LeadNotFoundException(leadId));

        if (lead.getStatus() == LeadStatus.CONVERTED) {
            throw new InvalidStageTransitionException("Lead is already converted");
        }

        // Create or use existing account
        Account account;
        if (request.getExistingAccountId() != null) {
            account = accountRepository.findByIdAndTenantId(request.getExistingAccountId(), tenantId)
                .orElseThrow(() -> new AccountNotFoundException(request.getExistingAccountId()));
        } else {
            account = new Account();
            account.setId(UUID.randomUUID());
            account.setTenantId(tenantId);
            account.setName(request.getAccountName());
            account.setOrganizationType(lead.getOrganizationType());
            account.setPatientCount(lead.getPatientCount());
            account.setEhrCount(lead.getEhrCount());
            account.setState(lead.getState());
            account.setWebsite(lead.getWebsite());
            account.setStage(AccountStage.PROSPECT);
            account.setOwnerUserId(request.getOwnerUserId() != null ?
                request.getOwnerUserId() : lead.getAssignedToUserId());
            account = accountRepository.save(account);
            log.info("Created account {} from lead {}", account.getId(), leadId);
        }

        // Create contact
        Contact contact = new Contact();
        contact.setId(UUID.randomUUID());
        contact.setTenantId(tenantId);
        contact.setAccountId(account.getId());
        contact.setFirstName(lead.getFirstName());
        contact.setLastName(lead.getLastName());
        contact.setEmail(lead.getEmail());
        contact.setPhone(lead.getPhone());
        contact.setTitle(lead.getTitle());
        contact.setContactType(ContactType.DECISION_MAKER);
        contact.setPrimary(true);
        contact.setOwnerUserId(request.getOwnerUserId() != null ?
            request.getOwnerUserId() : lead.getAssignedToUserId());
        contact = contactRepository.save(contact);
        log.info("Created contact {} from lead {}", contact.getId(), leadId);

        // Create opportunity
        Opportunity opportunity = new Opportunity();
        opportunity.setId(UUID.randomUUID());
        opportunity.setTenantId(tenantId);
        opportunity.setAccountId(account.getId());
        opportunity.setPrimaryContactId(contact.getId());
        opportunity.setName(request.getOpportunityName());
        opportunity.setAmount(request.getOpportunityAmount());
        opportunity.setStage(OpportunityStage.DISCOVERY);
        opportunity.setExpectedCloseDate(request.getExpectedCloseDate());
        opportunity.setProductTier(request.getProductTier());
        opportunity.setContractLengthMonths(request.getContractLengthMonths());
        opportunity.setOwnerUserId(request.getOwnerUserId() != null ?
            request.getOwnerUserId() : lead.getAssignedToUserId());
        opportunity.updateProbabilityFromStage();
        opportunity = opportunityRepository.save(opportunity);
        log.info("Created opportunity {} from lead {}", opportunity.getId(), leadId);

        // Update lead status
        lead.setStatus(LeadStatus.CONVERTED);
        lead.setConvertedAt(LocalDateTime.now());
        lead.setConvertedContactId(contact.getId());
        lead.setConvertedOpportunityId(opportunity.getId());
        lead = leadRepository.save(lead);

        log.info("Converted lead {} to account {}, contact {}, opportunity {}",
            leadId, account.getId(), contact.getId(), opportunity.getId());

        return leadMapper.toDTO(lead);
    }

    @Transactional(readOnly = true)
    public Page<LeadDTO> findByStatus(UUID tenantId, LeadStatus status, Pageable pageable) {
        return leadRepository.findByTenantIdAndStatus(tenantId, status, pageable)
            .map(leadMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<LeadDTO> findBySource(UUID tenantId, LeadSource source, Pageable pageable) {
        return leadRepository.findByTenantIdAndSource(tenantId, source, pageable)
            .map(leadMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<LeadDTO> findHighScoreLeads(UUID tenantId, Integer minScore, Pageable pageable) {
        return leadRepository.findByTenantIdAndMinScore(tenantId, minScore, pageable)
            .map(leadMapper::toDTO);
    }

    // ==================== Private Helper Methods ====================

    /**
     * Trigger async Zoho CRM sync for the lead
     */
    @Async
    protected void triggerZohoSync(Lead lead) {
        try {
            ZohoClient.ZohoSyncResult result = zohoClient.syncLead(lead);
            if (result.isSuccess()) {
                log.info("Successfully synced lead {} to Zoho with ID {}",
                    lead.getId(), result.getZohoId());
                // Update lead with Zoho ID (requires new transaction)
                leadRepository.findById(lead.getId()).ifPresent(l -> {
                    l.setZohoLeadId(result.getZohoId());
                    leadRepository.save(l);
                });
            } else if (!result.isSkipped()) {
                log.warn("Failed to sync lead {} to Zoho: {}",
                    lead.getId(), result.getErrorMessage());
            }
        } catch (Exception e) {
            log.error("Error during Zoho sync for lead {}: {}", lead.getId(), e.getMessage());
        }
    }

    /**
     * Auto-enroll lead in a WELCOME type sequence if available
     */
    private void autoEnrollInWelcomeSequence(UUID tenantId, Lead lead) {
        try {
            // Find active sequences for leads
            List<EmailSequence> sequences = emailSequenceRepository
                .findActiveSequencesForTargetType(tenantId, TargetType.LEAD);

            // Look for a WELCOME sequence first, then any NURTURE sequence
            EmailSequence welcomeSequence = sequences.stream()
                .filter(s -> s.getSequenceType() == SequenceType.WELCOME)
                .findFirst()
                .orElseGet(() -> sequences.stream()
                    .filter(s -> s.getSequenceType() == SequenceType.NURTURE)
                    .findFirst()
                    .orElse(null));

            if (welcomeSequence != null) {
                emailSequenceService.enrollLead(
                    tenantId,
                    lead.getId(),
                    welcomeSequence.getId(),
                    lead.getAssignedToUserId()
                );
                log.info("Auto-enrolled lead {} in sequence {}",
                    lead.getId(), welcomeSequence.getName());
            } else {
                log.debug("No welcome sequence available for lead auto-enrollment in tenant {}",
                    tenantId);
            }
        } catch (Exception e) {
            // Don't fail lead capture if sequence enrollment fails
            log.warn("Failed to auto-enroll lead {} in sequence: {}",
                lead.getId(), e.getMessage());
        }
    }
}
