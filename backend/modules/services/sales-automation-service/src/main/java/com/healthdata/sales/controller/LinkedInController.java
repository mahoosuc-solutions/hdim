package com.healthdata.sales.controller;

import com.healthdata.sales.dto.LinkedInCampaignDTO;
import com.healthdata.sales.dto.LinkedInOutreachDTO;
import com.healthdata.sales.entity.LinkedInOutreach.OutreachStatus;
import com.healthdata.sales.entity.LinkedInOutreach.OutreachType;
import com.healthdata.sales.service.LinkedInOutreachService;
import com.healthdata.sales.service.LinkedInOutreachService.LinkedInAnalytics;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/sales/linkedin")
@RequiredArgsConstructor
@Tag(name = "LinkedIn Outreach", description = "LinkedIn connection requests, InMail, and campaign management")
public class LinkedInController {

    private final LinkedInOutreachService linkedInService;

    // ==================== List & Search ====================

    @GetMapping
    @Operation(summary = "List all LinkedIn outreach", description = "Get paginated list of LinkedIn outreach activities")
    public ResponseEntity<Page<LinkedInOutreachDTO>> findAll(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            Pageable pageable) {
        return ResponseEntity.ok(linkedInService.findAll(tenantId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get outreach by ID")
    public ResponseEntity<LinkedInOutreachDTO> findById(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @PathVariable UUID id) {
        return linkedInService.findById(tenantId, id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "List by status", description = "Get outreach activities by status")
    public ResponseEntity<Page<LinkedInOutreachDTO>> findByStatus(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @PathVariable OutreachStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(linkedInService.findByStatus(tenantId, status, pageable));
    }

    @GetMapping("/campaign/{campaignName}")
    @Operation(summary = "List by campaign", description = "Get outreach activities for a campaign")
    public ResponseEntity<Page<LinkedInOutreachDTO>> findByCampaign(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @PathVariable String campaignName,
            Pageable pageable) {
        return ResponseEntity.ok(linkedInService.findByCampaign(tenantId, campaignName, pageable));
    }

    // ==================== Connection Requests ====================

    @PostMapping("/connect/lead/{leadId}")
    @Operation(summary = "Schedule connection request for lead",
               description = "Schedule a LinkedIn connection request with personalized note")
    public ResponseEntity<LinkedInOutreachDTO> scheduleConnectionForLead(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @RequestHeader(value = "X-User-ID", required = false) UUID userId,
            @PathVariable UUID leadId,
            @RequestBody ConnectionRequestDTO request) {
        return ResponseEntity.ok(linkedInService.scheduleConnectionRequest(
            tenantId, leadId, request.getConnectionNote(),
            request.getCampaignName(), request.getScheduledAt(), userId));
    }

    @PostMapping("/connect/contact/{contactId}")
    @Operation(summary = "Schedule connection request for contact",
               description = "Schedule a LinkedIn connection request for an existing contact")
    public ResponseEntity<LinkedInOutreachDTO> scheduleConnectionForContact(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @RequestHeader(value = "X-User-ID", required = false) UUID userId,
            @PathVariable UUID contactId,
            @RequestBody ConnectionRequestDTO request) {
        return ResponseEntity.ok(linkedInService.scheduleConnectionRequestForContact(
            tenantId, contactId, request.getConnectionNote(),
            request.getCampaignName(), request.getScheduledAt(), userId));
    }

    // ==================== InMail ====================

    @PostMapping("/inmail/lead/{leadId}")
    @Operation(summary = "Schedule InMail for lead",
               description = "Schedule a LinkedIn InMail message")
    public ResponseEntity<LinkedInOutreachDTO> scheduleInMail(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @RequestHeader(value = "X-User-ID", required = false) UUID userId,
            @PathVariable UUID leadId,
            @RequestBody InMailRequestDTO request) {
        return ResponseEntity.ok(linkedInService.scheduleInMail(
            tenantId, leadId, request.getSubject(), request.getMessage(),
            request.getCampaignName(), request.getScheduledAt(), userId));
    }

    // ==================== Campaigns ====================

    @PostMapping("/campaign")
    @Operation(summary = "Create LinkedIn campaign",
               description = "Create a campaign and schedule outreach for multiple leads")
    public ResponseEntity<LinkedInCampaignDTO> createCampaign(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @RequestHeader(value = "X-User-ID", required = false) UUID userId,
            @RequestBody CampaignRequestDTO request) {
        return ResponseEntity.ok(linkedInService.createCampaign(
            tenantId, request.getCampaignName(), request.getLeadIds(),
            request.getOutreachType(), request.getMessageTemplate(),
            request.getStartDate(), request.getDelayMinutesBetween(), userId));
    }

    // ==================== Status Updates ====================

    @PostMapping("/{id}/sent")
    @Operation(summary = "Mark as sent", description = "Mark outreach as sent after manual execution")
    public ResponseEntity<LinkedInOutreachDTO> markAsSent(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(linkedInService.markAsSent(tenantId, id));
    }

    @PostMapping("/{id}/accepted")
    @Operation(summary = "Mark as accepted", description = "Record that connection was accepted")
    public ResponseEntity<LinkedInOutreachDTO> markAsAccepted(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(linkedInService.markAsAccepted(tenantId, id));
    }

    @PostMapping("/{id}/replied")
    @Operation(summary = "Mark as replied", description = "Record that prospect replied")
    public ResponseEntity<LinkedInOutreachDTO> markAsReplied(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(linkedInService.markAsReplied(tenantId, id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel outreach", description = "Cancel a pending outreach")
    public ResponseEntity<LinkedInOutreachDTO> cancel(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(linkedInService.cancel(tenantId, id));
    }

    // ==================== Analytics ====================

    @GetMapping("/analytics")
    @Operation(summary = "Get LinkedIn analytics",
               description = "Get performance metrics for LinkedIn outreach")
    public ResponseEntity<LinkedInAnalytics> getAnalytics(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(linkedInService.getAnalytics(tenantId, days));
    }

    // ==================== Request DTOs ====================

    @lombok.Data
    public static class ConnectionRequestDTO {
        private String connectionNote;
        private String campaignName;
        private LocalDateTime scheduledAt;
    }

    @lombok.Data
    public static class InMailRequestDTO {
        private String subject;
        private String message;
        private String campaignName;
        private LocalDateTime scheduledAt;
    }

    @lombok.Data
    public static class CampaignRequestDTO {
        private String campaignName;
        private List<UUID> leadIds;
        private OutreachType outreachType;
        private String messageTemplate;
        private LocalDateTime startDate;
        private int delayMinutesBetween = 30;
    }
}
