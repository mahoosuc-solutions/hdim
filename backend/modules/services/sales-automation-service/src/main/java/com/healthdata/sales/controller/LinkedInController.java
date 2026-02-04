package com.healthdata.sales.controller;

import com.healthdata.sales.dto.LinkedInBulkCampaignResponse;
import com.healthdata.sales.dto.LinkedInCampaignDTO;
import com.healthdata.sales.dto.LinkedInOutreachDTO;
import com.healthdata.sales.entity.LinkedInCampaign.CampaignStatus;
import com.healthdata.sales.entity.LinkedInOutreach.OutreachStatus;
import com.healthdata.sales.entity.LinkedInOutreach.OutreachType;
import com.healthdata.sales.service.LinkedInCampaignService;
import com.healthdata.sales.service.LinkedInOutreachService;
import com.healthdata.sales.service.LinkedInOutreachService.LinkedInAnalytics;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@Tag(
    name = "LinkedIn Outreach",
    description = """
        APIs for managing LinkedIn outreach activities and campaigns.

        LinkedIn outreach enables social selling through:
        - Connection requests with personalized notes
        - InMail messages for premium outreach
        - Campaign management for bulk outreach
        - Status tracking (pending, sent, accepted, replied)
        - Performance analytics

        Note: These endpoints schedule outreach activities. Actual LinkedIn
        execution requires integration with LinkedIn Sales Navigator or
        manual execution with status updates.

        All endpoints require JWT authentication and X-Tenant-ID header.
        """
)
@SecurityRequirement(name = "bearer-jwt")
public class LinkedInController {

    private final LinkedInOutreachService linkedInService;
    private final LinkedInCampaignService campaignService;

    // ==================== List & Search ====================

    @GetMapping
    @Operation(
        summary = "List all LinkedIn outreach",
        description = "Retrieves a paginated list of all LinkedIn outreach activities for the tenant."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved outreach activities"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Page<LinkedInOutreachDTO>> findAll(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @Parameter(description = "Pagination parameters")
            Pageable pageable) {
        return ResponseEntity.ok(linkedInService.findAll(tenantId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get outreach by ID",
        description = "Retrieves details of a specific LinkedIn outreach activity."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved outreach"),
        @ApiResponse(responseCode = "404", description = "Outreach not found")
    })
    public ResponseEntity<LinkedInOutreachDTO> findById(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @Parameter(description = "Outreach ID", required = true)
            @PathVariable UUID id) {
        return linkedInService.findById(tenantId, id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    @Operation(
        summary = "List by status",
        description = "Retrieves outreach activities filtered by status."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved outreach by status"),
        @ApiResponse(responseCode = "400", description = "Invalid status value")
    })
    public ResponseEntity<Page<LinkedInOutreachDTO>> findByStatus(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @Parameter(description = "Outreach status", required = true,
                schema = @Schema(allowableValues = {"PENDING", "SENT", "ACCEPTED", "REPLIED", "CANCELLED"}))
            @PathVariable OutreachStatus status,
            @Parameter(description = "Pagination parameters")
            Pageable pageable) {
        return ResponseEntity.ok(linkedInService.findByStatus(tenantId, status, pageable));
    }

    @GetMapping("/campaign/{campaignName}")
    @Operation(
        summary = "List by campaign",
        description = "Retrieves all outreach activities associated with a specific campaign."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved campaign outreach"),
        @ApiResponse(responseCode = "404", description = "Campaign not found")
    })
    public ResponseEntity<Page<LinkedInOutreachDTO>> findByCampaign(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @Parameter(description = "Campaign name", required = true, example = "Q1 Healthcare Execs")
            @PathVariable String campaignName,
            @Parameter(description = "Pagination parameters")
            Pageable pageable) {
        return ResponseEntity.ok(linkedInService.findByCampaign(tenantId, campaignName, pageable));
    }

    // ==================== Connection Requests ====================

    @PostMapping("/connect/lead/{leadId}")
    @Operation(
        summary = "Schedule connection request for lead",
        description = """
            Schedules a LinkedIn connection request for a lead.

            The connection note should be personalized and under 300 characters.
            Optionally assign to a campaign for tracking and bulk management.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Connection request scheduled"),
        @ApiResponse(responseCode = "400", description = "Invalid request - lead has no LinkedIn URL"),
        @ApiResponse(responseCode = "404", description = "Lead not found")
    })
    public ResponseEntity<LinkedInOutreachDTO> scheduleConnectionForLead(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @Parameter(description = "User ID for attribution (optional)")
            @RequestHeader(value = "X-User-ID", required = false) UUID userId,
            @Parameter(description = "Lead ID", required = true)
            @PathVariable UUID leadId,
            @Parameter(description = "Connection request details", required = true)
            @RequestBody ConnectionRequestDTO request) {
        return ResponseEntity.ok(linkedInService.scheduleConnectionRequest(
            tenantId, leadId, request.getConnectionNote(),
            request.getCampaignName(), request.getScheduledAt(), userId));
    }

    @PostMapping("/connect/contact/{contactId}")
    @Operation(
        summary = "Schedule connection request for contact",
        description = "Schedules a LinkedIn connection request for an existing contact."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Connection request scheduled"),
        @ApiResponse(responseCode = "400", description = "Invalid request - contact has no LinkedIn URL"),
        @ApiResponse(responseCode = "404", description = "Contact not found")
    })
    public ResponseEntity<LinkedInOutreachDTO> scheduleConnectionForContact(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @Parameter(description = "User ID for attribution (optional)")
            @RequestHeader(value = "X-User-ID", required = false) UUID userId,
            @Parameter(description = "Contact ID", required = true)
            @PathVariable UUID contactId,
            @Parameter(description = "Connection request details", required = true)
            @RequestBody ConnectionRequestDTO request) {
        return ResponseEntity.ok(linkedInService.scheduleConnectionRequestForContact(
            tenantId, contactId, request.getConnectionNote(),
            request.getCampaignName(), request.getScheduledAt(), userId));
    }

    // ==================== InMail ====================

    @PostMapping("/inmail/lead/{leadId}")
    @Operation(
        summary = "Schedule InMail for lead",
        description = """
            Schedules a LinkedIn InMail message for a lead.

            InMail requires LinkedIn Premium/Sales Navigator credits.
            More effective for reaching decision makers not in your network.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "InMail scheduled"),
        @ApiResponse(responseCode = "400", description = "Invalid request - missing subject or message"),
        @ApiResponse(responseCode = "404", description = "Lead not found")
    })
    public ResponseEntity<LinkedInOutreachDTO> scheduleInMail(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @Parameter(description = "User ID for attribution (optional)")
            @RequestHeader(value = "X-User-ID", required = false) UUID userId,
            @Parameter(description = "Lead ID", required = true)
            @PathVariable UUID leadId,
            @Parameter(description = "InMail content", required = true)
            @RequestBody InMailRequestDTO request) {
        return ResponseEntity.ok(linkedInService.scheduleInMail(
            tenantId, leadId, request.getSubject(), request.getMessage(),
            request.getCampaignName(), request.getScheduledAt(), userId));
    }

    // ==================== Campaign CRUD ====================

    @GetMapping("/campaigns")
    @Operation(
        summary = "List all campaigns",
        description = "Retrieves a paginated list of all LinkedIn campaigns for the tenant."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved campaigns"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Page<LinkedInCampaignDTO>> listCampaigns(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @Parameter(description = "Filter by status")
            @RequestParam(required = false) CampaignStatus status,
            @Parameter(description = "Search by name")
            @RequestParam(required = false) String search,
            @Parameter(description = "Pagination parameters")
            Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(campaignService.searchByName(tenantId, search, pageable));
        }
        if (status != null) {
            return ResponseEntity.ok(campaignService.findByStatus(tenantId, status, pageable));
        }
        return ResponseEntity.ok(campaignService.findAll(tenantId, pageable));
    }

    @GetMapping("/campaigns/{id}")
    @Operation(
        summary = "Get campaign by ID",
        description = "Retrieves details of a specific LinkedIn campaign."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved campaign"),
        @ApiResponse(responseCode = "404", description = "Campaign not found")
    })
    public ResponseEntity<LinkedInCampaignDTO> getCampaign(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @Parameter(description = "Campaign ID", required = true)
            @PathVariable UUID id) {
        return campaignService.findById(tenantId, id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/campaigns")
    @Operation(
        summary = "Create a new campaign",
        description = "Creates a new LinkedIn campaign for managing outreach activities."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Campaign created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "409", description = "Campaign with this name already exists")
    })
    public ResponseEntity<LinkedInCampaignDTO> createCampaignEntity(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @Parameter(description = "User ID for attribution (optional)")
            @RequestHeader(value = "X-User-ID", required = false) UUID userId,
            @Parameter(description = "Campaign details", required = true)
            @Valid @RequestBody LinkedInCampaignDTO campaign) {
        return ResponseEntity.ok(campaignService.create(tenantId, campaign, userId));
    }

    @PutMapping("/campaigns/{id}")
    @Operation(
        summary = "Update a campaign",
        description = "Updates an existing LinkedIn campaign."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Campaign updated successfully"),
        @ApiResponse(responseCode = "404", description = "Campaign not found"),
        @ApiResponse(responseCode = "409", description = "Campaign with this name already exists")
    })
    public ResponseEntity<LinkedInCampaignDTO> updateCampaign(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @Parameter(description = "Campaign ID", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Campaign details", required = true)
            @Valid @RequestBody LinkedInCampaignDTO campaign) {
        return ResponseEntity.ok(campaignService.update(tenantId, id, campaign));
    }

    @DeleteMapping("/campaigns/{id}")
    @Operation(
        summary = "Delete a campaign",
        description = "Deletes a LinkedIn campaign."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Campaign deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Campaign not found")
    })
    public ResponseEntity<Void> deleteCampaign(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @Parameter(description = "Campaign ID", required = true)
            @PathVariable UUID id) {
        campaignService.delete(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/campaigns/{id}/activate")
    @Operation(
        summary = "Activate a campaign",
        description = "Activates a campaign to start processing outreach."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Campaign activated"),
        @ApiResponse(responseCode = "404", description = "Campaign not found")
    })
    public ResponseEntity<LinkedInCampaignDTO> activateCampaign(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @Parameter(description = "Campaign ID", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(campaignService.activate(tenantId, id));
    }

    @PostMapping("/campaigns/{id}/pause")
    @Operation(
        summary = "Pause a campaign",
        description = "Pauses a campaign to stop processing outreach."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Campaign paused"),
        @ApiResponse(responseCode = "404", description = "Campaign not found")
    })
    public ResponseEntity<LinkedInCampaignDTO> pauseCampaign(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @Parameter(description = "Campaign ID", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(campaignService.pause(tenantId, id));
    }

    // ==================== Bulk Campaign with Leads ====================

    @PostMapping("/campaign/bulk")
    @Operation(
        summary = "Create bulk campaign with leads",
        description = """
            Creates a campaign and schedules outreach for multiple leads.

            Outreach is staggered based on delayMinutesBetween to avoid
            LinkedIn rate limits and appear more natural.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Campaign created with scheduled outreach"),
        @ApiResponse(responseCode = "400", description = "Invalid request - no leads or missing template"),
        @ApiResponse(responseCode = "404", description = "One or more leads not found")
    })
    public ResponseEntity<LinkedInBulkCampaignResponse> createBulkCampaign(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @Parameter(description = "User ID for attribution (optional)")
            @RequestHeader(value = "X-User-ID", required = false) UUID userId,
            @Parameter(description = "Campaign configuration", required = true)
            @RequestBody BulkCampaignRequestDTO request) {
        return ResponseEntity.ok(linkedInService.createBulkCampaign(
            tenantId, request.getCampaignName(), request.getLeadIds(),
            request.getOutreachType(), request.getMessageTemplate(),
            request.getStartDate(), request.getDelayMinutesBetween(), userId));
    }

    // ==================== Status Updates ====================

    @PostMapping("/{id}/sent")
    @Operation(
        summary = "Mark as sent",
        description = "Records that the outreach was manually sent via LinkedIn."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status updated to SENT"),
        @ApiResponse(responseCode = "400", description = "Invalid state transition"),
        @ApiResponse(responseCode = "404", description = "Outreach not found")
    })
    public ResponseEntity<LinkedInOutreachDTO> markAsSent(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @Parameter(description = "Outreach ID", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(linkedInService.markAsSent(tenantId, id));
    }

    @PostMapping("/{id}/accepted")
    @Operation(
        summary = "Mark as accepted",
        description = "Records that the connection request was accepted by the prospect."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status updated to ACCEPTED"),
        @ApiResponse(responseCode = "400", description = "Invalid state transition"),
        @ApiResponse(responseCode = "404", description = "Outreach not found")
    })
    public ResponseEntity<LinkedInOutreachDTO> markAsAccepted(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @Parameter(description = "Outreach ID", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(linkedInService.markAsAccepted(tenantId, id));
    }

    @PostMapping("/{id}/replied")
    @Operation(
        summary = "Mark as replied",
        description = "Records that the prospect replied to the outreach. Key conversion metric."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status updated to REPLIED"),
        @ApiResponse(responseCode = "400", description = "Invalid state transition"),
        @ApiResponse(responseCode = "404", description = "Outreach not found")
    })
    public ResponseEntity<LinkedInOutreachDTO> markAsReplied(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @Parameter(description = "Outreach ID", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(linkedInService.markAsReplied(tenantId, id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(
        summary = "Cancel outreach",
        description = "Cancels a pending outreach that hasn't been sent yet."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Outreach cancelled"),
        @ApiResponse(responseCode = "400", description = "Cannot cancel - already sent"),
        @ApiResponse(responseCode = "404", description = "Outreach not found")
    })
    public ResponseEntity<LinkedInOutreachDTO> cancel(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @Parameter(description = "Outreach ID", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(linkedInService.cancel(tenantId, id));
    }

    // ==================== Analytics ====================

    @GetMapping("/analytics")
    @Operation(
        summary = "Get LinkedIn analytics",
        description = """
            Retrieves performance metrics for LinkedIn outreach.

            Includes:
            - Total sent, accepted, replied counts
            - Acceptance rate
            - Reply rate
            - Campaign breakdown
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved analytics")
    })
    public ResponseEntity<LinkedInAnalytics> getAnalytics(
            @Parameter(description = "Tenant identifier", required = true)
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @Parameter(description = "Number of days to analyze", example = "30",
                schema = @Schema(defaultValue = "30", minimum = "1", maximum = "365"))
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
    public static class BulkCampaignRequestDTO {
        private String campaignName;
        private List<UUID> leadIds;
        private OutreachType outreachType;
        private String messageTemplate;
        private LocalDateTime startDate;
        private int delayMinutesBetween = 30;
    }
}
