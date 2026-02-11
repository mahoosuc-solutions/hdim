package com.healthdata.sales.service;

import com.healthdata.sales.config.LinkedInConfig;
import com.healthdata.sales.dto.LinkedInBulkCampaignResponse;
import com.healthdata.sales.dto.LinkedInOutreachDTO;
import com.healthdata.sales.entity.Contact;
import com.healthdata.sales.entity.Lead;
import com.healthdata.sales.entity.LinkedInOutreach;
import com.healthdata.sales.entity.LinkedInOutreach.OutreachStatus;
import com.healthdata.sales.entity.LinkedInOutreach.OutreachType;
import com.healthdata.sales.exception.DuplicateResourceException;
import com.healthdata.sales.repository.ContactRepository;
import com.healthdata.sales.repository.LeadRepository;
import com.healthdata.sales.repository.LinkedInOutreachRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LinkedIn Outreach Service
 *
 * Manages LinkedIn outreach campaigns including:
 * - Connection requests with personalized notes
 * - InMail messages
 * - Profile engagement tracking
 * - Multi-step outreach sequences
 *
 * Note: Due to LinkedIn API restrictions, this service primarily tracks
 * outreach activities and generates tasks for manual execution.
 * Full automation requires LinkedIn Marketing API partner access.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LinkedInOutreachService {

    private final LinkedInOutreachRepository outreachRepository;
    private final LeadRepository leadRepository;
    private final ContactRepository contactRepository;
    private final LinkedInConfig linkedInConfig;

    private static final Pattern MERGE_FIELD_PATTERN = Pattern.compile("\\{\\{(\\w+)\\}\\}");

    // ==================== CRUD Operations ====================

    @Transactional(readOnly = true)
    public Page<LinkedInOutreachDTO> findAll(UUID tenantId, Pageable pageable) {
        return outreachRepository.findByTenantId(tenantId, pageable)
            .map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Optional<LinkedInOutreachDTO> findById(UUID tenantId, UUID id) {
        return outreachRepository.findByIdAndTenantId(id, tenantId)
            .map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<LinkedInOutreachDTO> findByStatus(UUID tenantId, OutreachStatus status, Pageable pageable) {
        return outreachRepository.findByTenantIdAndStatus(tenantId, status, pageable)
            .map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<LinkedInOutreachDTO> findByCampaign(UUID tenantId, String campaignName, Pageable pageable) {
        return outreachRepository.findByTenantIdAndCampaignName(tenantId, campaignName, pageable)
            .map(this::toDTO);
    }

    // ==================== Connection Requests ====================

    /**
     * Schedule a connection request for a lead
     */
    @Transactional
    public LinkedInOutreachDTO scheduleConnectionRequest(UUID tenantId, UUID leadId,
            String connectionNote, String campaignName, LocalDateTime scheduledAt, UUID createdBy) {

        Lead lead = leadRepository.findByIdAndTenantId(leadId, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Lead not found: " + leadId));

        if (lead.getLinkedinUrl() == null || lead.getLinkedinUrl().isBlank()) {
            throw new IllegalArgumentException("Lead does not have a LinkedIn profile URL");
        }

        // Check for duplicate
        if (outreachRepository.existsByProfileAndType(tenantId, lead.getLinkedinUrl(),
                OutreachType.CONNECTION_REQUEST)) {
            throw new DuplicateResourceException("Connection request already exists for this profile");
        }

        // Check daily limit
        checkDailyLimit(tenantId, OutreachType.CONNECTION_REQUEST);

        // Personalize note
        String personalizedNote = personalizeMessage(connectionNote, lead);

        // Validate note length (LinkedIn limit: 300 chars)
        if (personalizedNote != null && personalizedNote.length() > 300) {
            personalizedNote = personalizedNote.substring(0, 297) + "...";
        }

        LinkedInOutreach outreach = LinkedInOutreach.builder()
            .tenantId(tenantId)
            .leadId(leadId)
            .linkedinProfileUrl(lead.getLinkedinUrl())
            .targetName(lead.getFirstName() + " " + lead.getLastName())
            .targetTitle(lead.getTitle())
            .targetCompany(lead.getCompany())
            .outreachType(OutreachType.CONNECTION_REQUEST)
            .status(OutreachStatus.PENDING)
            .connectionNote(personalizedNote)
            .campaignName(campaignName)
            .scheduledAt(scheduledAt != null ? scheduledAt : LocalDateTime.now())
            .createdBy(createdBy)
            .build();

        return toDTO(outreachRepository.save(outreach));
    }

    /**
     * Schedule a connection request for a contact
     */
    @Transactional
    public LinkedInOutreachDTO scheduleConnectionRequestForContact(UUID tenantId, UUID contactId,
            String connectionNote, String campaignName, LocalDateTime scheduledAt, UUID createdBy) {

        Contact contact = contactRepository.findByIdAndTenantId(contactId, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Contact not found: " + contactId));

        if (contact.getLinkedinUrl() == null || contact.getLinkedinUrl().isBlank()) {
            throw new IllegalArgumentException("Contact does not have a LinkedIn profile URL");
        }

        // Check for duplicate
        if (outreachRepository.existsByProfileAndType(tenantId, contact.getLinkedinUrl(),
                OutreachType.CONNECTION_REQUEST)) {
            throw new DuplicateResourceException("Connection request already exists for this profile");
        }

        checkDailyLimit(tenantId, OutreachType.CONNECTION_REQUEST);

        String personalizedNote = personalizeMessageForContact(connectionNote, contact);

        if (personalizedNote != null && personalizedNote.length() > 300) {
            personalizedNote = personalizedNote.substring(0, 297) + "...";
        }

        LinkedInOutreach outreach = LinkedInOutreach.builder()
            .tenantId(tenantId)
            .contactId(contactId)
            .linkedinProfileUrl(contact.getLinkedinUrl())
            .targetName(contact.getFirstName() + " " + contact.getLastName())
            .targetTitle(contact.getTitle())
            .targetCompany(null) // Contact is linked to Account via accountId
            .outreachType(OutreachType.CONNECTION_REQUEST)
            .status(OutreachStatus.PENDING)
            .connectionNote(personalizedNote)
            .campaignName(campaignName)
            .scheduledAt(scheduledAt != null ? scheduledAt : LocalDateTime.now())
            .createdBy(createdBy)
            .build();

        return toDTO(outreachRepository.save(outreach));
    }

    // ==================== InMail ====================

    /**
     * Schedule an InMail message
     */
    @Transactional
    public LinkedInOutreachDTO scheduleInMail(UUID tenantId, UUID leadId, String subject,
            String messageContent, String campaignName, LocalDateTime scheduledAt, UUID createdBy) {

        Lead lead = leadRepository.findByIdAndTenantId(leadId, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Lead not found: " + leadId));

        if (lead.getLinkedinUrl() == null || lead.getLinkedinUrl().isBlank()) {
            throw new IllegalArgumentException("Lead does not have a LinkedIn profile URL");
        }

        checkDailyLimit(tenantId, OutreachType.INMAIL);

        String personalizedMessage = personalizeMessage(messageContent, lead);

        LinkedInOutreach outreach = LinkedInOutreach.builder()
            .tenantId(tenantId)
            .leadId(leadId)
            .linkedinProfileUrl(lead.getLinkedinUrl())
            .targetName(lead.getFirstName() + " " + lead.getLastName())
            .targetTitle(lead.getTitle())
            .targetCompany(lead.getCompany())
            .outreachType(OutreachType.INMAIL)
            .status(OutreachStatus.PENDING)
            .messageContent(personalizedMessage)
            .campaignName(campaignName)
            .scheduledAt(scheduledAt != null ? scheduledAt : LocalDateTime.now())
            .createdBy(createdBy)
            .build();

        return toDTO(outreachRepository.save(outreach));
    }

    // ==================== Bulk Operations ====================

    /**
     * Create a bulk campaign and schedule outreach for multiple leads
     */
    @Transactional
    public LinkedInBulkCampaignResponse createBulkCampaign(UUID tenantId, String campaignName,
            List<UUID> leadIds, OutreachType type, String messageTemplate,
            LocalDateTime startDate, int delayMinutesBetween, UUID createdBy) {

        List<LinkedInOutreach> scheduled = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        LocalDateTime nextSchedule = startDate != null ? startDate : LocalDateTime.now();

        for (UUID leadId : leadIds) {
            try {
                Lead lead = leadRepository.findByIdAndTenantId(leadId, tenantId)
                    .orElseThrow(() -> new IllegalArgumentException("Lead not found"));

                if (lead.getLinkedinUrl() == null) {
                    errors.add("Lead " + leadId + ": No LinkedIn URL");
                    continue;
                }

                if (outreachRepository.existsByProfileAndType(tenantId, lead.getLinkedinUrl(), type)) {
                    errors.add("Lead " + leadId + ": Already contacted");
                    continue;
                }

                String personalizedMessage = personalizeMessage(messageTemplate, lead);

                LinkedInOutreach outreach = LinkedInOutreach.builder()
                    .tenantId(tenantId)
                    .leadId(leadId)
                    .linkedinProfileUrl(lead.getLinkedinUrl())
                    .targetName(lead.getFirstName() + " " + lead.getLastName())
                    .targetTitle(lead.getTitle())
                    .targetCompany(lead.getCompany())
                    .outreachType(type)
                    .status(OutreachStatus.PENDING)
                    .messageTemplate(messageTemplate)
                    .messageContent(type == OutreachType.INMAIL ? personalizedMessage : null)
                    .connectionNote(type == OutreachType.CONNECTION_REQUEST ? personalizedMessage : null)
                    .campaignName(campaignName)
                    .scheduledAt(nextSchedule)
                    .createdBy(createdBy)
                    .build();

                scheduled.add(outreachRepository.save(outreach));
                nextSchedule = nextSchedule.plusMinutes(delayMinutesBetween);

            } catch (Exception e) {
                errors.add("Lead " + leadId + ": " + e.getMessage());
            }
        }

        return LinkedInBulkCampaignResponse.builder()
            .campaignName(campaignName)
            .totalLeads(leadIds.size())
            .scheduled(scheduled.size())
            .errors(errors)
            .startDate(startDate)
            .estimatedEndDate(nextSchedule)
            .build();
    }

    // ==================== Status Updates ====================

    @Transactional
    public LinkedInOutreachDTO markAsSent(UUID tenantId, UUID id) {
        LinkedInOutreach outreach = outreachRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Outreach not found: " + id));

        outreach.markSent();
        return toDTO(outreachRepository.save(outreach));
    }

    @Transactional
    public LinkedInOutreachDTO markAsAccepted(UUID tenantId, UUID id) {
        LinkedInOutreach outreach = outreachRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Outreach not found: " + id));

        outreach.markAccepted();
        return toDTO(outreachRepository.save(outreach));
    }

    @Transactional
    public LinkedInOutreachDTO markAsReplied(UUID tenantId, UUID id) {
        LinkedInOutreach outreach = outreachRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Outreach not found: " + id));

        outreach.markReplied();
        return toDTO(outreachRepository.save(outreach));
    }

    @Transactional
    public LinkedInOutreachDTO cancel(UUID tenantId, UUID id) {
        LinkedInOutreach outreach = outreachRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Outreach not found: " + id));

        outreach.setStatus(OutreachStatus.CANCELLED);
        return toDTO(outreachRepository.save(outreach));
    }

    // ==================== Analytics ====================

    @Transactional(readOnly = true)
    public LinkedInAnalytics getAnalytics(UUID tenantId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);

        long connectionsSent = outreachRepository.countSentSince(tenantId,
            OutreachType.CONNECTION_REQUEST, since);
        long accepted = outreachRepository.countAcceptedSince(tenantId, since);
        long replied = outreachRepository.countRepliedSince(tenantId, since);
        long inmailsSent = outreachRepository.countSentSince(tenantId,
            OutreachType.INMAIL, since);

        double acceptanceRate = connectionsSent > 0 ? (accepted * 100.0) / connectionsSent : 0;
        double replyRate = (connectionsSent + inmailsSent) > 0 ?
            (replied * 100.0) / (connectionsSent + inmailsSent) : 0;

        Map<String, Long> statusCounts = new HashMap<>();
        outreachRepository.countByStatus(tenantId).forEach(row ->
            statusCounts.put(((OutreachStatus) row[0]).name(), (Long) row[1]));

        return LinkedInAnalytics.builder()
            .periodDays(days)
            .connectionRequestsSent(connectionsSent)
            .connectionsAccepted(accepted)
            .inmailsSent(inmailsSent)
            .totalReplies(replied)
            .acceptanceRate(Math.round(acceptanceRate * 100.0) / 100.0)
            .replyRate(Math.round(replyRate * 100.0) / 100.0)
            .statusBreakdown(statusCounts)
            .build();
    }

    // ==================== Scheduled Tasks ====================

    /**
     * Process scheduled outreach - creates tasks for manual execution
     * Runs every 5 minutes
     */
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void processScheduledOutreach() {
        if (!linkedInConfig.getOutreach().isEnabled()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        List<LinkedInOutreach> dueOutreach = outreachRepository.findDueForSending(now);

        if (dueOutreach.isEmpty()) {
            return;
        }

        log.info("Processing {} scheduled LinkedIn outreach items", dueOutreach.size());

        for (LinkedInOutreach outreach : dueOutreach) {
            // For now, we just log the task - full automation requires LinkedIn API access
            log.info("LinkedIn outreach due: {} - {} to {} ({})",
                outreach.getOutreachType(),
                outreach.getCampaignName(),
                outreach.getTargetName(),
                outreach.getLinkedinProfileUrl());

            // Move to "ready" status - user will mark as sent when they execute
            // In a full implementation, this would call the LinkedIn API
        }
    }

    // ==================== Helper Methods ====================

    private void checkDailyLimit(UUID tenantId, OutreachType type) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        long dailyCount = outreachRepository.countDailyOutreach(tenantId, type, startOfDay, endOfDay);

        int limit = type == OutreachType.CONNECTION_REQUEST ?
            linkedInConfig.getOutreach().getMaxConnectionsPerDay() :
            linkedInConfig.getOutreach().getMaxInMailsPerDay();

        if (dailyCount >= limit) {
            throw new IllegalStateException(
                String.format("Daily %s limit reached (%d/%d)",
                    type.name().toLowerCase().replace("_", " "), dailyCount, limit));
        }
    }

    private String personalizeMessage(String template, Lead lead) {
        if (template == null) return null;

        Map<String, String> fields = Map.of(
            "firstName", lead.getFirstName() != null ? lead.getFirstName() : "",
            "lastName", lead.getLastName() != null ? lead.getLastName() : "",
            "company", lead.getCompany() != null ? lead.getCompany() : "",
            "title", lead.getTitle() != null ? lead.getTitle() : ""
        );

        return replaceMergeFields(template, fields);
    }

    private String personalizeMessageForContact(String template, Contact contact) {
        if (template == null) return null;

        Map<String, String> fields = Map.of(
            "firstName", contact.getFirstName() != null ? contact.getFirstName() : "",
            "lastName", contact.getLastName() != null ? contact.getLastName() : "",
            "company", "", // Contact is linked to Account via accountId
            "title", contact.getTitle() != null ? contact.getTitle() : ""
        );

        return replaceMergeFields(template, fields);
    }

    private String replaceMergeFields(String template, Map<String, String> fields) {
        Matcher matcher = MERGE_FIELD_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String field = matcher.group(1);
            String replacement = fields.getOrDefault(field, "{{" + field + "}}");
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private LinkedInOutreachDTO toDTO(LinkedInOutreach entity) {
        return LinkedInOutreachDTO.builder()
            .id(entity.getId())
            .tenantId(entity.getTenantId())
            .leadId(entity.getLeadId())
            .contactId(entity.getContactId())
            .linkedinProfileUrl(entity.getLinkedinProfileUrl())
            .targetName(entity.getTargetName())
            .targetTitle(entity.getTargetTitle())
            .targetCompany(entity.getTargetCompany())
            .outreachType(entity.getOutreachType())
            .status(entity.getStatus())
            .messageContent(entity.getMessageContent())
            .connectionNote(entity.getConnectionNote())
            .campaignName(entity.getCampaignName())
            .scheduledAt(entity.getScheduledAt())
            .sentAt(entity.getSentAt())
            .connectionAccepted(entity.getConnectionAccepted())
            .acceptedAt(entity.getAcceptedAt())
            .replied(entity.getReplied())
            .repliedAt(entity.getRepliedAt())
            .createdAt(entity.getCreatedAt())
            .build();
    }

    // ==================== DTOs ====================

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class LinkedInAnalytics {
        private int periodDays;
        private long connectionRequestsSent;
        private long connectionsAccepted;
        private long inmailsSent;
        private long totalReplies;
        private double acceptanceRate;
        private double replyRate;
        private Map<String, Long> statusBreakdown;
    }
}
