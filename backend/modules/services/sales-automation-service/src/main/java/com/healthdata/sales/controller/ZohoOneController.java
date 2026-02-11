package com.healthdata.sales.controller;

import com.healthdata.sales.client.*;
import com.healthdata.sales.config.ZohoConfig;
import com.healthdata.sales.service.ZohoSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Zoho ONE Platform Controller
 * Unified API for all Zoho services integration
 *
 * Provides access to:
 * - Zoho CRM (leads, accounts, contacts, deals)
 * - Zoho Campaigns (email marketing)
 * - Zoho Bookings (meeting scheduler)
 * - Zoho Analytics (reports & dashboards)
 *
 * All endpoints require authentication via JWT.
 */
@RestController
@RequestMapping("/api/sales/zoho")
@RequiredArgsConstructor
@Slf4j
@Tag(
    name = "Zoho ONE Platform",
    description = """
        Unified Zoho ONE integration for HDIM investor tracking and marketing automation.
        
        **Included Services:**
        - **CRM**: Lead/Contact/Account/Deal management and sync
        - **Campaigns**: Email marketing and automation sequences
        - **Bookings**: Investor meeting scheduler
        - **Analytics**: Pipeline dashboards and KPIs
        
        **Authentication:**
        All endpoints require a valid JWT token in the Authorization header.
        
        **Rate Limits:**
        - 10,000 API calls/day per organization (Zoho ONE limit)
        - Circuit breaker protection enabled
        """
)
@SecurityRequirement(name = "bearer-jwt")
public class ZohoOneController {

    private final ZohoConfig zohoConfig;
    private final ZohoOAuthService oAuthService;
    private final ZohoClient crmClient;
    private final ZohoCampaignsClient campaignsClient;
    private final ZohoBookingsClient bookingsClient;
    private final ZohoAnalyticsClient analyticsClient;
    private final ZohoSyncService syncService;

    // ==================== Platform Status ====================

    @GetMapping("/status")
    @Operation(
        summary = "Get Zoho ONE platform status",
        description = "Returns configuration status for all Zoho services"
    )
    public ResponseEntity<ZohoPlatformStatus> getPlatformStatus() {
        ZohoPlatformStatus status = new ZohoPlatformStatus();
        
        status.setOauthConfigured(oAuthService.isConfigured());
        status.setCrmEnabled(zohoConfig.getSync().isEnabled());
        status.setCampaignsEnabled(zohoConfig.getCampaigns().isEnabled());
        status.setBookingsEnabled(zohoConfig.getBookings().isEnabled());
        status.setAnalyticsEnabled(zohoConfig.getAnalytics().isEnabled());
        status.setWebhookEnabled(zohoConfig.getWebhook().isEnabled());
        
        return ResponseEntity.ok(status);
    }

    // ==================== CRM Endpoints ====================

    @PostMapping("/crm/sync")
    @Operation(
        summary = "Trigger full CRM sync",
        description = "Synchronizes all CRM modules (Leads, Accounts, Contacts, Deals)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sync completed"),
        @ApiResponse(responseCode = "503", description = "CRM sync is disabled")
    })
    public ResponseEntity<ZohoSyncService.FullSyncResult> triggerCrmSync(
        @RequestHeader("X-Tenant-ID") UUID tenantId
    ) {
        if (!zohoConfig.getSync().isEnabled()) {
            return ResponseEntity.status(503).build();
        }
        
        ZohoSyncService.FullSyncResult result = syncService.fullSync(tenantId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/crm/search/{module}")
    @Operation(
        summary = "Search CRM records",
        description = "Search for records in a CRM module using criteria"
    )
    public ResponseEntity<List<Map<String, Object>>> searchCrmRecords(
        @Parameter(description = "CRM module: Leads, Accounts, Contacts, or Deals")
        @PathVariable String module,
        @Parameter(description = "Search criteria (e.g., '(Email:equals:john@example.com)')")
        @RequestParam String criteria
    ) {
        List<Map<String, Object>> results = crmClient.searchRecords(module, criteria);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/crm/{module}/{zohoId}")
    @Operation(
        summary = "Get CRM record by ID",
        description = "Fetch a single record from Zoho CRM"
    )
    public ResponseEntity<Map<String, Object>> getCrmRecord(
        @PathVariable String module,
        @PathVariable String zohoId
    ) {
        Map<String, Object> record = crmClient.fetchRecord(module, zohoId);
        if (record == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(record);
    }

    // ==================== Campaigns Endpoints ====================

    @PostMapping("/campaigns/contacts")
    @Operation(
        summary = "Add contact to mailing list",
        description = "Add or update a contact in Zoho Campaigns mailing list"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Contact added"),
        @ApiResponse(responseCode = "503", description = "Campaigns is disabled")
    })
    public ResponseEntity<ZohoCampaignsClient.CampaignsResult> addCampaignContact(
        @RequestBody ZohoCampaignsClient.ContactInfo contact,
        @Parameter(description = "Mailing list key (optional, uses default if not provided)")
        @RequestParam(required = false) String listKey
    ) {
        if (!zohoConfig.getCampaigns().isEnabled()) {
            return ResponseEntity.status(503).build();
        }
        
        ZohoCampaignsClient.CampaignsResult result = campaignsClient.addContact(contact, listKey);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/campaigns/contacts/{email}")
    @Operation(
        summary = "Unsubscribe contact",
        description = "Unsubscribe a contact from a mailing list"
    )
    public ResponseEntity<ZohoCampaignsClient.CampaignsResult> unsubscribeContact(
        @PathVariable String email,
        @RequestParam(required = false) String listKey
    ) {
        if (!zohoConfig.getCampaigns().isEnabled()) {
            return ResponseEntity.status(503).build();
        }
        
        ZohoCampaignsClient.CampaignsResult result = campaignsClient.unsubscribeContact(email, listKey);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/campaigns/sequence/enroll")
    @Operation(
        summary = "Enroll in email sequence",
        description = "Add a contact to an automated email sequence (workflow)"
    )
    public ResponseEntity<ZohoCampaignsClient.CampaignsResult> enrollInSequence(
        @RequestParam String email,
        @RequestParam String workflowId,
        @RequestBody(required = false) Map<String, Object> triggerData
    ) {
        if (!zohoConfig.getCampaigns().isEnabled()) {
            return ResponseEntity.status(503).build();
        }
        
        ZohoCampaignsClient.CampaignsResult result = 
            campaignsClient.enrollInSequence(email, workflowId, triggerData);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/campaigns/sequence/unenroll")
    @Operation(
        summary = "Remove from email sequence",
        description = "Remove a contact from an automated email sequence"
    )
    public ResponseEntity<ZohoCampaignsClient.CampaignsResult> unenrollFromSequence(
        @RequestParam String email,
        @RequestParam String workflowId
    ) {
        if (!zohoConfig.getCampaigns().isEnabled()) {
            return ResponseEntity.status(503).build();
        }
        
        ZohoCampaignsClient.CampaignsResult result = 
            campaignsClient.unenrollFromSequence(email, workflowId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/campaigns/stats/{campaignKey}")
    @Operation(
        summary = "Get campaign statistics",
        description = "Get open/click/bounce statistics for a campaign"
    )
    public ResponseEntity<ZohoCampaignsClient.CampaignStats> getCampaignStats(
        @PathVariable String campaignKey
    ) {
        if (!zohoConfig.getCampaigns().isEnabled()) {
            return ResponseEntity.status(503).build();
        }
        
        ZohoCampaignsClient.CampaignStats stats = campaignsClient.getCampaignStats(campaignKey);
        if (stats == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/campaigns/contacts/{email}/activity")
    @Operation(
        summary = "Get contact email activity",
        description = "Get email opens, clicks, and subscription status for a contact"
    )
    public ResponseEntity<ZohoCampaignsClient.ContactActivity> getContactActivity(
        @PathVariable String email
    ) {
        if (!zohoConfig.getCampaigns().isEnabled()) {
            return ResponseEntity.status(503).build();
        }
        
        ZohoCampaignsClient.ContactActivity activity = campaignsClient.getContactActivity(email);
        if (activity == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(activity);
    }

    @GetMapping("/campaigns/lists")
    @Operation(
        summary = "Get mailing lists",
        description = "List all mailing lists in Zoho Campaigns"
    )
    public ResponseEntity<List<Map<String, Object>>> getMailingLists() {
        if (!zohoConfig.getCampaigns().isEnabled()) {
            return ResponseEntity.status(503).build();
        }
        
        List<Map<String, Object>> lists = campaignsClient.getLists();
        return ResponseEntity.ok(lists);
    }

    // ==================== Bookings Endpoints ====================

    @GetMapping("/bookings/services")
    @Operation(
        summary = "Get meeting types",
        description = "List available meeting types (discovery call, demo, etc.)"
    )
    public ResponseEntity<List<ZohoBookingsClient.ServiceInfo>> getBookingServices() {
        if (!zohoConfig.getBookings().isEnabled()) {
            return ResponseEntity.status(503).build();
        }
        
        List<ZohoBookingsClient.ServiceInfo> services = bookingsClient.getServices();
        return ResponseEntity.ok(services);
    }

    @GetMapping("/bookings/availability")
    @Operation(
        summary = "Get available time slots",
        description = "Get available time slots for a meeting type on a specific date"
    )
    public ResponseEntity<List<ZohoBookingsClient.TimeSlot>> getAvailability(
        @RequestParam String serviceId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam(required = false) String staffId
    ) {
        if (!zohoConfig.getBookings().isEnabled()) {
            return ResponseEntity.status(503).build();
        }
        
        List<ZohoBookingsClient.TimeSlot> slots = 
            bookingsClient.getAvailability(serviceId, date, staffId);
        return ResponseEntity.ok(slots);
    }

    @GetMapping("/bookings/available-dates")
    @Operation(
        summary = "Get available dates",
        description = "Get dates with availability within a range"
    )
    public ResponseEntity<List<LocalDate>> getAvailableDates(
        @RequestParam String serviceId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(required = false) String staffId
    ) {
        if (!zohoConfig.getBookings().isEnabled()) {
            return ResponseEntity.status(503).build();
        }
        
        List<LocalDate> dates = 
            bookingsClient.getAvailableDates(serviceId, startDate, endDate, staffId);
        return ResponseEntity.ok(dates);
    }

    @PostMapping("/bookings/appointments")
    @Operation(
        summary = "Book an appointment",
        description = "Schedule a meeting with an investor or prospect"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "Appointment booked",
            content = @Content(schema = @Schema(implementation = ZohoBookingsClient.BookingResult.class))
        ),
        @ApiResponse(responseCode = "503", description = "Bookings is disabled")
    })
    public ResponseEntity<ZohoBookingsClient.BookingResult> bookAppointment(
        @RequestBody ZohoBookingsClient.BookingRequest request
    ) {
        if (!zohoConfig.getBookings().isEnabled()) {
            return ResponseEntity.status(503).build();
        }
        
        ZohoBookingsClient.BookingResult result = bookingsClient.bookAppointment(request);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/bookings/appointments/{bookingId}/reschedule")
    @Operation(
        summary = "Reschedule appointment",
        description = "Change the date/time of an existing appointment"
    )
    public ResponseEntity<ZohoBookingsClient.BookingResult> rescheduleAppointment(
        @PathVariable String bookingId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime newStartTime
    ) {
        if (!zohoConfig.getBookings().isEnabled()) {
            return ResponseEntity.status(503).build();
        }
        
        ZohoBookingsClient.BookingResult result = 
            bookingsClient.rescheduleAppointment(bookingId, newStartTime);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/bookings/appointments/{bookingId}")
    @Operation(
        summary = "Cancel appointment",
        description = "Cancel a scheduled appointment"
    )
    public ResponseEntity<ZohoBookingsClient.BookingResult> cancelAppointment(
        @PathVariable String bookingId,
        @RequestParam(required = false) String reason
    ) {
        if (!zohoConfig.getBookings().isEnabled()) {
            return ResponseEntity.status(503).build();
        }
        
        ZohoBookingsClient.BookingResult result = 
            bookingsClient.cancelAppointment(bookingId, reason);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/bookings/appointments/{bookingId}")
    @Operation(
        summary = "Get appointment details",
        description = "Get details of a scheduled appointment"
    )
    public ResponseEntity<ZohoBookingsClient.AppointmentDetails> getAppointment(
        @PathVariable String bookingId
    ) {
        if (!zohoConfig.getBookings().isEnabled()) {
            return ResponseEntity.status(503).build();
        }
        
        ZohoBookingsClient.AppointmentDetails details = bookingsClient.getAppointment(bookingId);
        if (details == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(details);
    }

    @GetMapping("/bookings/appointments")
    @Operation(
        summary = "List appointments",
        description = "List appointments within a date range"
    )
    public ResponseEntity<List<ZohoBookingsClient.AppointmentDetails>> listAppointments(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(required = false) String staffId,
        @RequestParam(required = false) String status
    ) {
        if (!zohoConfig.getBookings().isEnabled()) {
            return ResponseEntity.status(503).build();
        }
        
        List<ZohoBookingsClient.AppointmentDetails> appointments = 
            bookingsClient.listAppointments(startDate, endDate, staffId, status);
        return ResponseEntity.ok(appointments);
    }

    @GetMapping("/bookings/link/demo")
    @Operation(
        summary = "Get demo booking link",
        description = "Generate a direct booking link for product demos"
    )
    public ResponseEntity<Map<String, String>> getDemoBookingLink() {
        if (!zohoConfig.getBookings().isEnabled()) {
            return ResponseEntity.status(503).build();
        }
        
        String link = bookingsClient.generateDemoBookingLink();
        return ResponseEntity.ok(Map.of("bookingLink", link));
    }

    @GetMapping("/bookings/link/discovery")
    @Operation(
        summary = "Get discovery call booking link",
        description = "Generate a direct booking link for discovery calls"
    )
    public ResponseEntity<Map<String, String>> getDiscoveryBookingLink() {
        if (!zohoConfig.getBookings().isEnabled()) {
            return ResponseEntity.status(503).build();
        }
        
        String link = bookingsClient.generateDiscoveryCallLink();
        return ResponseEntity.ok(Map.of("bookingLink", link));
    }

    // ==================== Analytics Endpoints ====================

    @GetMapping("/analytics/pipeline")
    @Operation(
        summary = "Get pipeline metrics",
        description = "Get investor pipeline summary (deals by stage, total value)"
    )
    public ResponseEntity<ZohoAnalyticsClient.PipelineMetrics> getPipelineMetrics() {
        if (!zohoConfig.getAnalytics().isEnabled()) {
            return ResponseEntity.status(503).build();
        }
        
        ZohoAnalyticsClient.PipelineMetrics metrics = analyticsClient.getPipelineMetrics();
        if (metrics == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/analytics/marketing")
    @Operation(
        summary = "Get marketing metrics",
        description = "Get email campaign performance metrics"
    )
    public ResponseEntity<ZohoAnalyticsClient.MarketingMetrics> getMarketingMetrics(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        if (!zohoConfig.getAnalytics().isEnabled()) {
            return ResponseEntity.status(503).build();
        }
        
        ZohoAnalyticsClient.MarketingMetrics metrics = 
            analyticsClient.getMarketingMetrics(startDate, endDate);
        if (metrics == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/analytics/sales")
    @Operation(
        summary = "Get sales metrics",
        description = "Get sales team performance metrics"
    )
    public ResponseEntity<ZohoAnalyticsClient.SalesMetrics> getSalesMetrics(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        if (!zohoConfig.getAnalytics().isEnabled()) {
            return ResponseEntity.status(503).build();
        }
        
        ZohoAnalyticsClient.SalesMetrics metrics = 
            analyticsClient.getSalesMetrics(startDate, endDate);
        if (metrics == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/analytics/lead-source-roi")
    @Operation(
        summary = "Get lead source ROI",
        description = "Get ROI analysis by lead source (ads, referral, organic)"
    )
    public ResponseEntity<List<ZohoAnalyticsClient.LeadSourceROI>> getLeadSourceROI() {
        if (!zohoConfig.getAnalytics().isEnabled()) {
            return ResponseEntity.status(503).build();
        }
        
        List<ZohoAnalyticsClient.LeadSourceROI> roi = analyticsClient.getLeadSourceROI();
        return ResponseEntity.ok(roi);
    }

    @GetMapping("/analytics/dashboard/pipeline")
    @Operation(
        summary = "Get pipeline dashboard URL",
        description = "Get embeddable dashboard URL for investor pipeline"
    )
    public ResponseEntity<Map<String, String>> getInvestorPipelineDashboard() {
        if (!zohoConfig.getAnalytics().isEnabled()) {
            return ResponseEntity.status(503).build();
        }
        
        String url = analyticsClient.getInvestorPipelineDashboardUrl();
        if (url == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("embedUrl", url));
    }

    @GetMapping("/analytics/dashboard/marketing")
    @Operation(
        summary = "Get marketing dashboard URL",
        description = "Get embeddable dashboard URL for marketing performance"
    )
    public ResponseEntity<Map<String, String>> getMarketingDashboard() {
        if (!zohoConfig.getAnalytics().isEnabled()) {
            return ResponseEntity.status(503).build();
        }
        
        String url = analyticsClient.getMarketingDashboardUrl();
        if (url == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("embedUrl", url));
    }

    @GetMapping("/analytics/dashboard/sales")
    @Operation(
        summary = "Get sales dashboard URL",
        description = "Get embeddable dashboard URL for sales performance"
    )
    public ResponseEntity<Map<String, String>> getSalesDashboard() {
        if (!zohoConfig.getAnalytics().isEnabled()) {
            return ResponseEntity.status(503).build();
        }
        
        String url = analyticsClient.getSalesDashboardUrl();
        if (url == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("embedUrl", url));
    }

    @PostMapping("/analytics/query")
    @Operation(
        summary = "Execute custom analytics query",
        description = "Run a custom SQL query against analytics data"
    )
    public ResponseEntity<List<Map<String, Object>>> executeQuery(
        @RequestBody QueryRequest request
    ) {
        if (!zohoConfig.getAnalytics().isEnabled()) {
            return ResponseEntity.status(503).build();
        }
        
        List<Map<String, Object>> results = analyticsClient.executeQuery(request.getQuery());
        return ResponseEntity.ok(results);
    }

    @PostMapping("/analytics/sync-crm")
    @Operation(
        summary = "Sync CRM data to Analytics",
        description = "Trigger sync of CRM deals data to Analytics for reporting"
    )
    public ResponseEntity<ZohoAnalyticsClient.AnalyticsResult> syncCrmToAnalytics() {
        if (!zohoConfig.getAnalytics().isEnabled()) {
            return ResponseEntity.status(503).build();
        }
        
        ZohoAnalyticsClient.AnalyticsResult result = analyticsClient.syncDealsFromCRM();
        return ResponseEntity.ok(result);
    }

    // ==================== DTOs ====================

    @Data
    public static class ZohoPlatformStatus {
        private boolean oauthConfigured;
        private boolean crmEnabled;
        private boolean campaignsEnabled;
        private boolean bookingsEnabled;
        private boolean analyticsEnabled;
        private boolean webhookEnabled;
    }

    @Data
    public static class QueryRequest {
        private String query;
    }
}
