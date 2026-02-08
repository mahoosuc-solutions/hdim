package com.healthdata.sales.client;

import com.healthdata.sales.config.ZohoConfig;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Zoho Campaigns API Client
 * Email marketing automation for investor outreach sequences
 *
 * Features:
 * - Contact list management
 * - Email campaign creation
 * - Campaign scheduling and sending
 * - Email tracking (opens, clicks, bounces)
 * - Automation workflows (email sequences)
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ZohoCampaignsClient {

    private final ZohoConfig zohoConfig;
    private final ZohoOAuthService oAuthService;
    private final WebClient.Builder webClientBuilder;

    private static final DateTimeFormatter ZOHO_DATE_FORMAT = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==================== Contact Management ====================

    /**
     * Add a contact to a mailing list
     */
    @CircuitBreaker(name = "zohoService", fallbackMethod = "addContactFallback")
    @Retry(name = "zohoService")
    public CampaignsResult addContact(ContactInfo contact, String listKey) {
        if (!isEnabled()) {
            return CampaignsResult.disabled();
        }

        try {
            String accessToken = oAuthService.getAccessToken();
            WebClient webClient = buildWebClient(accessToken);

            Map<String, Object> contactData = new HashMap<>();
            contactData.put("Contact Email", contact.getEmail());
            if (contact.getFirstName() != null) {
                contactData.put("First Name", contact.getFirstName());
            }
            if (contact.getLastName() != null) {
                contactData.put("Last Name", contact.getLastName());
            }
            if (contact.getCompany() != null) {
                contactData.put("Company", contact.getCompany());
            }
            if (contact.getPhone() != null) {
                contactData.put("Phone", contact.getPhone());
            }
            // Custom fields for investor tracking
            if (contact.getInvestorType() != null) {
                contactData.put("Investor_Type", contact.getInvestorType());
            }
            if (contact.getInvestmentThesis() != null) {
                contactData.put("Investment_Thesis", contact.getInvestmentThesis());
            }

            String effectiveListKey = listKey != null ? listKey : 
                zohoConfig.getCampaigns().getDefaultListKey();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                .uri(uriBuilder -> uriBuilder
                    .path("/json/listsubscribe")
                    .queryParam("resfmt", "JSON")
                    .queryParam("listkey", effectiveListKey)
                    .build())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("contactinfo", contactData))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && "success".equals(response.get("status"))) {
                log.info("Added contact {} to Zoho Campaigns list {}", 
                    contact.getEmail(), effectiveListKey);
                return CampaignsResult.success((String) response.get("message"));
            } else {
                String error = response != null ? (String) response.get("message") : "Unknown error";
                return CampaignsResult.failed(error);
            }
        } catch (Exception e) {
            log.error("Failed to add contact to Zoho Campaigns: {}", e.getMessage());
            return CampaignsResult.failed(e.getMessage());
        }
    }

    public CampaignsResult addContactFallback(ContactInfo contact, String listKey, Throwable t) {
        log.warn("Zoho Campaigns addContact fallback triggered: {}", t.getMessage());
        return CampaignsResult.failed("Circuit breaker open: " + t.getMessage());
    }

    /**
     * Update contact custom fields
     */
    @CircuitBreaker(name = "zohoService")
    @Retry(name = "zohoService")
    public CampaignsResult updateContact(String email, Map<String, Object> fields) {
        if (!isEnabled()) {
            return CampaignsResult.disabled();
        }

        try {
            String accessToken = oAuthService.getAccessToken();
            WebClient webClient = buildWebClient(accessToken);

            Map<String, Object> contactData = new HashMap<>(fields);
            contactData.put("Contact Email", email);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                .uri(uriBuilder -> uriBuilder
                    .path("/json/listsubscribe")
                    .queryParam("resfmt", "JSON")
                    .queryParam("listkey", zohoConfig.getCampaigns().getDefaultListKey())
                    .build())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("contactinfo", contactData))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && "success".equals(response.get("status"))) {
                log.info("Updated contact {} in Zoho Campaigns", email);
                return CampaignsResult.success((String) response.get("message"));
            }
            return CampaignsResult.failed(response != null ? 
                (String) response.get("message") : "Unknown error");
        } catch (Exception e) {
            log.error("Failed to update contact in Zoho Campaigns: {}", e.getMessage());
            return CampaignsResult.failed(e.getMessage());
        }
    }

    /**
     * Unsubscribe a contact from a list
     */
    @CircuitBreaker(name = "zohoService")
    @Retry(name = "zohoService")
    public CampaignsResult unsubscribeContact(String email, String listKey) {
        if (!isEnabled()) {
            return CampaignsResult.disabled();
        }

        try {
            String accessToken = oAuthService.getAccessToken();
            WebClient webClient = buildWebClient(accessToken);

            String effectiveListKey = listKey != null ? listKey : 
                zohoConfig.getCampaigns().getDefaultListKey();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                .uri(uriBuilder -> uriBuilder
                    .path("/json/listunsubscribe")
                    .queryParam("resfmt", "JSON")
                    .queryParam("listkey", effectiveListKey)
                    .queryParam("contactinfo", email)
                    .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && "success".equals(response.get("status"))) {
                log.info("Unsubscribed contact {} from Zoho Campaigns list {}", 
                    email, effectiveListKey);
                return CampaignsResult.success((String) response.get("message"));
            }
            return CampaignsResult.failed(response != null ? 
                (String) response.get("message") : "Unknown error");
        } catch (Exception e) {
            log.error("Failed to unsubscribe contact from Zoho Campaigns: {}", e.getMessage());
            return CampaignsResult.failed(e.getMessage());
        }
    }

    // ==================== Campaign Management ====================

    /**
     * Create an email campaign
     */
    @CircuitBreaker(name = "zohoService")
    @Retry(name = "zohoService")
    public CampaignsResult createCampaign(CampaignInfo campaign) {
        if (!isEnabled()) {
            return CampaignsResult.disabled();
        }

        try {
            String accessToken = oAuthService.getAccessToken();
            WebClient webClient = buildWebClient(accessToken);

            Map<String, Object> campaignData = new HashMap<>();
            campaignData.put("campaignname", campaign.getName());
            campaignData.put("subject", campaign.getSubject());
            campaignData.put("from_email", campaign.getFromEmail() != null ? 
                campaign.getFromEmail() : zohoConfig.getCampaigns().getEmailDefaults().getFromEmail());
            campaignData.put("from_name", campaign.getFromName() != null ?
                campaign.getFromName() : zohoConfig.getCampaigns().getEmailDefaults().getFromName());
            campaignData.put("content", campaign.getHtmlContent());
            if (campaign.getTextContent() != null) {
                campaignData.put("text_content", campaign.getTextContent());
            }
            campaignData.put("track_opens", 
                zohoConfig.getCampaigns().getEmailDefaults().isTrackOpens() ? "true" : "false");
            campaignData.put("track_clicks", 
                zohoConfig.getCampaigns().getEmailDefaults().isTrackClicks() ? "true" : "false");

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                .uri(uriBuilder -> uriBuilder
                    .path("/json/createcampaign")
                    .queryParam("resfmt", "JSON")
                    .build())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(campaignData)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && "success".equals(response.get("status"))) {
                String campaignKey = (String) response.get("campaign_key");
                log.info("Created campaign {} with key {}", campaign.getName(), campaignKey);
                return CampaignsResult.success(campaignKey);
            }
            return CampaignsResult.failed(response != null ? 
                (String) response.get("message") : "Unknown error");
        } catch (Exception e) {
            log.error("Failed to create campaign in Zoho Campaigns: {}", e.getMessage());
            return CampaignsResult.failed(e.getMessage());
        }
    }

    /**
     * Send a campaign to a list
     */
    @CircuitBreaker(name = "zohoService")
    @Retry(name = "zohoService")
    public CampaignsResult sendCampaign(String campaignKey, String listKey, 
                                        LocalDateTime scheduleTime) {
        if (!isEnabled()) {
            return CampaignsResult.disabled();
        }

        try {
            String accessToken = oAuthService.getAccessToken();
            WebClient webClient = buildWebClient(accessToken);

            var uriBuilder = webClient.post()
                .uri(builder -> {
                    var b = builder
                        .path("/json/sendcampaign")
                        .queryParam("resfmt", "JSON")
                        .queryParam("campaignkey", campaignKey)
                        .queryParam("listkey", listKey != null ? listKey : 
                            zohoConfig.getCampaigns().getDefaultListKey());
                    if (scheduleTime != null) {
                        b.queryParam("schedule_time", scheduleTime.format(ZOHO_DATE_FORMAT));
                    }
                    return b.build();
                });

            @SuppressWarnings("unchecked")
            Map<String, Object> response = uriBuilder
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && "success".equals(response.get("status"))) {
                log.info("Sent/scheduled campaign {}", campaignKey);
                return CampaignsResult.success((String) response.get("message"));
            }
            return CampaignsResult.failed(response != null ? 
                (String) response.get("message") : "Unknown error");
        } catch (Exception e) {
            log.error("Failed to send campaign in Zoho Campaigns: {}", e.getMessage());
            return CampaignsResult.failed(e.getMessage());
        }
    }

    // ==================== Campaign Statistics ====================

    /**
     * Get campaign statistics (opens, clicks, bounces, etc.)
     */
    @CircuitBreaker(name = "zohoService")
    @Retry(name = "zohoService")
    public CampaignStats getCampaignStats(String campaignKey) {
        if (!isEnabled()) {
            return null;
        }

        try {
            String accessToken = oAuthService.getAccessToken();
            WebClient webClient = buildWebClient(accessToken);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/json/campaigndetails")
                    .queryParam("resfmt", "JSON")
                    .queryParam("campaignkey", campaignKey)
                    .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && "success".equals(response.get("status"))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                return mapToCampaignStats(data);
            }
        } catch (Exception e) {
            log.error("Failed to get campaign stats: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Get contact activity (emails opened, links clicked)
     */
    @CircuitBreaker(name = "zohoService")
    @Retry(name = "zohoService")
    public ContactActivity getContactActivity(String email) {
        if (!isEnabled()) {
            return null;
        }

        try {
            String accessToken = oAuthService.getAccessToken();
            WebClient webClient = buildWebClient(accessToken);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/json/getcontactactivity")
                    .queryParam("resfmt", "JSON")
                    .queryParam("email", email)
                    .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && "success".equals(response.get("status"))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                return mapToContactActivity(data);
            }
        } catch (Exception e) {
            log.error("Failed to get contact activity: {}", e.getMessage());
        }
        return null;
    }

    // ==================== Automation (Email Sequences) ====================

    /**
     * Add contact to an email sequence (workflow)
     */
    @CircuitBreaker(name = "zohoService")
    @Retry(name = "zohoService")
    public CampaignsResult enrollInSequence(String email, String workflowId, 
                                            Map<String, Object> triggerData) {
        if (!isEnabled()) {
            return CampaignsResult.disabled();
        }

        try {
            String accessToken = oAuthService.getAccessToken();
            WebClient webClient = buildWebClient(accessToken);

            Map<String, Object> enrollmentData = new HashMap<>();
            enrollmentData.put("email", email);
            enrollmentData.put("workflow_id", workflowId);
            if (triggerData != null) {
                enrollmentData.put("trigger_data", triggerData);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                .uri(uriBuilder -> uriBuilder
                    .path("/json/workflow/enroll")
                    .queryParam("resfmt", "JSON")
                    .build())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(enrollmentData)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && "success".equals(response.get("status"))) {
                log.info("Enrolled {} in workflow {}", email, workflowId);
                return CampaignsResult.success((String) response.get("message"));
            }
            return CampaignsResult.failed(response != null ? 
                (String) response.get("message") : "Unknown error");
        } catch (Exception e) {
            log.error("Failed to enroll in workflow: {}", e.getMessage());
            return CampaignsResult.failed(e.getMessage());
        }
    }

    /**
     * Remove contact from an email sequence
     */
    @CircuitBreaker(name = "zohoService")
    @Retry(name = "zohoService")
    public CampaignsResult unenrollFromSequence(String email, String workflowId) {
        if (!isEnabled()) {
            return CampaignsResult.disabled();
        }

        try {
            String accessToken = oAuthService.getAccessToken();
            WebClient webClient = buildWebClient(accessToken);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                .uri(uriBuilder -> uriBuilder
                    .path("/json/workflow/unenroll")
                    .queryParam("resfmt", "JSON")
                    .queryParam("email", email)
                    .queryParam("workflow_id", workflowId)
                    .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && "success".equals(response.get("status"))) {
                log.info("Unenrolled {} from workflow {}", email, workflowId);
                return CampaignsResult.success((String) response.get("message"));
            }
            return CampaignsResult.failed(response != null ? 
                (String) response.get("message") : "Unknown error");
        } catch (Exception e) {
            log.error("Failed to unenroll from workflow: {}", e.getMessage());
            return CampaignsResult.failed(e.getMessage());
        }
    }

    // ==================== List Management ====================

    /**
     * Create a new mailing list
     */
    @CircuitBreaker(name = "zohoService")
    @Retry(name = "zohoService")
    public CampaignsResult createList(String listName, String signupForm, String description) {
        if (!isEnabled()) {
            return CampaignsResult.disabled();
        }

        try {
            String accessToken = oAuthService.getAccessToken();
            WebClient webClient = buildWebClient(accessToken);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                .uri(uriBuilder -> uriBuilder
                    .path("/json/createlist")
                    .queryParam("resfmt", "JSON")
                    .queryParam("listname", listName)
                    .queryParam("signupform", signupForm != null ? signupForm : "embeded")
                    .queryParam("description", description != null ? description : "")
                    .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && "success".equals(response.get("status"))) {
                String listKey = (String) response.get("list_key");
                log.info("Created mailing list {} with key {}", listName, listKey);
                return CampaignsResult.success(listKey);
            }
            return CampaignsResult.failed(response != null ? 
                (String) response.get("message") : "Unknown error");
        } catch (Exception e) {
            log.error("Failed to create mailing list: {}", e.getMessage());
            return CampaignsResult.failed(e.getMessage());
        }
    }

    /**
     * Get all mailing lists
     */
    @CircuitBreaker(name = "zohoService")
    @Retry(name = "zohoService")
    public List<Map<String, Object>> getLists() {
        if (!isEnabled()) {
            return List.of();
        }

        try {
            String accessToken = oAuthService.getAccessToken();
            WebClient webClient = buildWebClient(accessToken);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/json/getmailinglists")
                    .queryParam("resfmt", "JSON")
                    .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && "success".equals(response.get("status"))) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> lists = 
                    (List<Map<String, Object>>) response.get("list_of_details");
                return lists != null ? lists : List.of();
            }
        } catch (Exception e) {
            log.error("Failed to get mailing lists: {}", e.getMessage());
        }
        return List.of();
    }

    // ==================== Helper Methods ====================

    private boolean isEnabled() {
        return zohoConfig.getCampaigns().isEnabled() && oAuthService.isConfigured();
    }

    private WebClient buildWebClient(String accessToken) {
        return webClientBuilder
            .baseUrl(zohoConfig.getCampaigns().getBaseUrl())
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Zoho-oauthtoken " + accessToken)
            .build();
    }

    private CampaignStats mapToCampaignStats(Map<String, Object> data) {
        CampaignStats stats = new CampaignStats();
        stats.setCampaignName((String) data.get("campaign_name"));
        stats.setTotalSent(getIntValue(data, "total_sent"));
        stats.setTotalOpened(getIntValue(data, "total_opened"));
        stats.setUniqueOpened(getIntValue(data, "unique_opened"));
        stats.setTotalClicked(getIntValue(data, "total_clicked"));
        stats.setUniqueClicked(getIntValue(data, "unique_clicked"));
        stats.setBounced(getIntValue(data, "bounced"));
        stats.setUnsubscribed(getIntValue(data, "unsubscribed"));
        stats.setSpamReported(getIntValue(data, "spam_reported"));
        
        // Calculate rates
        if (stats.getTotalSent() > 0) {
            stats.setOpenRate((double) stats.getUniqueOpened() / stats.getTotalSent() * 100);
            stats.setClickRate((double) stats.getUniqueClicked() / stats.getTotalSent() * 100);
            stats.setBounceRate((double) stats.getBounced() / stats.getTotalSent() * 100);
        }
        return stats;
    }

    private ContactActivity mapToContactActivity(Map<String, Object> data) {
        ContactActivity activity = new ContactActivity();
        activity.setEmail((String) data.get("email"));
        activity.setTotalOpens(getIntValue(data, "total_opens"));
        activity.setTotalClicks(getIntValue(data, "total_clicks"));
        activity.setLastOpenDate((String) data.get("last_open_date"));
        activity.setLastClickDate((String) data.get("last_click_date"));
        activity.setSubscriptionStatus((String) data.get("subscription_status"));
        return activity;
    }

    private int getIntValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    // ==================== DTOs ====================

    @Data
    public static class ContactInfo {
        private String email;
        private String firstName;
        private String lastName;
        private String company;
        private String phone;
        private String investorType;
        private String investmentThesis;
        private Map<String, String> customFields;
    }

    @Data
    public static class CampaignInfo {
        private String name;
        private String subject;
        private String fromEmail;
        private String fromName;
        private String htmlContent;
        private String textContent;
        private String templateKey; // Use existing template
    }

    @Data
    public static class CampaignStats {
        private String campaignName;
        private int totalSent;
        private int totalOpened;
        private int uniqueOpened;
        private int totalClicked;
        private int uniqueClicked;
        private int bounced;
        private int unsubscribed;
        private int spamReported;
        private double openRate;
        private double clickRate;
        private double bounceRate;
    }

    @Data
    public static class ContactActivity {
        private String email;
        private int totalOpens;
        private int totalClicks;
        private String lastOpenDate;
        private String lastClickDate;
        private String subscriptionStatus;
    }

    @Data
    public static class CampaignsResult {
        private boolean success;
        private boolean skipped;
        private String message;
        private String id;

        public static CampaignsResult success(String message) {
            CampaignsResult result = new CampaignsResult();
            result.success = true;
            result.message = message;
            result.id = message; // Often the key/ID is returned in message
            return result;
        }

        public static CampaignsResult failed(String error) {
            CampaignsResult result = new CampaignsResult();
            result.success = false;
            result.message = error;
            return result;
        }

        public static CampaignsResult disabled() {
            CampaignsResult result = new CampaignsResult();
            result.skipped = true;
            result.message = "Zoho Campaigns is disabled";
            return result;
        }
    }
}
