package com.healthdata.sales.client;

import com.healthdata.sales.config.ZohoConfig;
import com.healthdata.sales.entity.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Zoho CRM API Client
 * Handles bidirectional sync between HDIM and Zoho CRM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ZohoClient {

    private final ZohoConfig zohoConfig;
    private final ZohoOAuthService oAuthService;
    private final WebClient.Builder webClientBuilder;

    // ==================== Lead Operations ====================

    @CircuitBreaker(name = "zohoService", fallbackMethod = "syncLeadFallback")
    @Retry(name = "zohoService")
    public ZohoSyncResult syncLead(Lead lead) {
        if (!zohoConfig.getSync().isEnabled() || !oAuthService.isConfigured()) {
            log.debug("Zoho sync disabled or not configured, skipping lead sync");
            return ZohoSyncResult.skipped("Zoho sync is disabled");
        }

        try {
            Map<String, Object> zohoLead = mapLeadToZoho(lead);

            if (lead.getZohoLeadId() != null) {
                // Update existing
                return updateZohoRecord("Leads", lead.getZohoLeadId(), zohoLead);
            } else {
                // Create new
                return createZohoRecord("Leads", zohoLead);
            }
        } catch (Exception e) {
            log.error("Failed to sync lead {} to Zoho: {}", lead.getId(), e.getMessage());
            return ZohoSyncResult.failed(e.getMessage());
        }
    }

    public ZohoSyncResult syncLeadFallback(Lead lead, Throwable t) {
        log.warn("Zoho lead sync fallback triggered for {}: {}", lead.getId(), t.getMessage());
        return ZohoSyncResult.failed("Circuit breaker open: " + t.getMessage());
    }

    // ==================== Account Operations ====================

    @CircuitBreaker(name = "zohoService", fallbackMethod = "syncAccountFallback")
    @Retry(name = "zohoService")
    public ZohoSyncResult syncAccount(Account account) {
        if (!zohoConfig.getSync().isEnabled() || !oAuthService.isConfigured()) {
            return ZohoSyncResult.skipped("Zoho sync is disabled");
        }

        try {
            Map<String, Object> zohoAccount = mapAccountToZoho(account);

            if (account.getZohoAccountId() != null) {
                return updateZohoRecord("Accounts", account.getZohoAccountId(), zohoAccount);
            } else {
                return createZohoRecord("Accounts", zohoAccount);
            }
        } catch (Exception e) {
            log.error("Failed to sync account {} to Zoho: {}", account.getId(), e.getMessage());
            return ZohoSyncResult.failed(e.getMessage());
        }
    }

    public ZohoSyncResult syncAccountFallback(Account account, Throwable t) {
        log.warn("Zoho account sync fallback triggered for {}: {}", account.getId(), t.getMessage());
        return ZohoSyncResult.failed("Circuit breaker open: " + t.getMessage());
    }

    // ==================== Contact Operations ====================

    @CircuitBreaker(name = "zohoService", fallbackMethod = "syncContactFallback")
    @Retry(name = "zohoService")
    public ZohoSyncResult syncContact(Contact contact) {
        if (!zohoConfig.getSync().isEnabled() || !oAuthService.isConfigured()) {
            return ZohoSyncResult.skipped("Zoho sync is disabled");
        }

        try {
            Map<String, Object> zohoContact = mapContactToZoho(contact);

            if (contact.getZohoContactId() != null) {
                return updateZohoRecord("Contacts", contact.getZohoContactId(), zohoContact);
            } else {
                return createZohoRecord("Contacts", zohoContact);
            }
        } catch (Exception e) {
            log.error("Failed to sync contact {} to Zoho: {}", contact.getId(), e.getMessage());
            return ZohoSyncResult.failed(e.getMessage());
        }
    }

    public ZohoSyncResult syncContactFallback(Contact contact, Throwable t) {
        log.warn("Zoho contact sync fallback triggered for {}: {}", contact.getId(), t.getMessage());
        return ZohoSyncResult.failed("Circuit breaker open: " + t.getMessage());
    }

    // ==================== Opportunity (Deal) Operations ====================

    @CircuitBreaker(name = "zohoService", fallbackMethod = "syncOpportunityFallback")
    @Retry(name = "zohoService")
    public ZohoSyncResult syncOpportunity(Opportunity opportunity) {
        if (!zohoConfig.getSync().isEnabled() || !oAuthService.isConfigured()) {
            return ZohoSyncResult.skipped("Zoho sync is disabled");
        }

        try {
            Map<String, Object> zohoDeal = mapOpportunityToZoho(opportunity);

            if (opportunity.getZohoOpportunityId() != null) {
                return updateZohoRecord("Deals", opportunity.getZohoOpportunityId(), zohoDeal);
            } else {
                return createZohoRecord("Deals", zohoDeal);
            }
        } catch (Exception e) {
            log.error("Failed to sync opportunity {} to Zoho: {}", opportunity.getId(), e.getMessage());
            return ZohoSyncResult.failed(e.getMessage());
        }
    }

    public ZohoSyncResult syncOpportunityFallback(Opportunity opportunity, Throwable t) {
        log.warn("Zoho opportunity sync fallback triggered for {}: {}", opportunity.getId(), t.getMessage());
        return ZohoSyncResult.failed("Circuit breaker open: " + t.getMessage());
    }

    // ==================== Fetch Operations ====================

    /**
     * Fetch a single record from Zoho by ID
     */
    @CircuitBreaker(name = "zohoService")
    @Retry(name = "zohoService")
    public Map<String, Object> fetchRecord(String module, String zohoId) {
        if (!oAuthService.isConfigured()) {
            log.warn("Zoho not configured, cannot fetch record");
            return null;
        }

        try {
            String accessToken = oAuthService.getAccessToken();
            WebClient webClient = webClientBuilder
                .baseUrl(zohoConfig.getApi().getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Zoho-oauthtoken " + accessToken)
                .build();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.get()
                .uri("/" + module + "/" + zohoId)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && response.containsKey("data")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) response.get("data");
                if (!dataList.isEmpty()) {
                    return dataList.get(0);
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch {} record {}: {}", module, zohoId, e.getMessage());
        }
        return null;
    }

    /**
     * Fetch records modified since a given time
     */
    @CircuitBreaker(name = "zohoService")
    @Retry(name = "zohoService")
    public List<Map<String, Object>> fetchModifiedRecords(String module, String modifiedSince) {
        if (!oAuthService.isConfigured()) {
            log.warn("Zoho not configured, cannot fetch records");
            return List.of();
        }

        try {
            String accessToken = oAuthService.getAccessToken();
            WebClient webClient = webClientBuilder
                .baseUrl(zohoConfig.getApi().getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Zoho-oauthtoken " + accessToken)
                .defaultHeader("If-Modified-Since", modifiedSince)
                .build();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/" + module)
                    .queryParam("fields", getModuleFields(module))
                    .queryParam("per_page", 200)
                    .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && response.containsKey("data")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) response.get("data");
                log.info("Fetched {} modified {} records from Zoho", dataList.size(), module);
                return dataList;
            }
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("304")) {
                log.debug("No modified {} records since {}", module, modifiedSince);
                return List.of();
            }
            log.error("Failed to fetch modified {} records: {}", module, e.getMessage());
        }
        return List.of();
    }

    /**
     * Search records in Zoho by criteria
     */
    @CircuitBreaker(name = "zohoService")
    @Retry(name = "zohoService")
    public List<Map<String, Object>> searchRecords(String module, String criteria) {
        if (!oAuthService.isConfigured()) {
            return List.of();
        }

        try {
            String accessToken = oAuthService.getAccessToken();
            WebClient webClient = webClientBuilder
                .baseUrl(zohoConfig.getApi().getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Zoho-oauthtoken " + accessToken)
                .build();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/" + module + "/search")
                    .queryParam("criteria", criteria)
                    .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && response.containsKey("data")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) response.get("data");
                return dataList;
            }
        } catch (Exception e) {
            log.error("Failed to search {} records: {}", module, e.getMessage());
        }
        return List.of();
    }

    /**
     * Find record by HDIM ID (custom field)
     */
    public Map<String, Object> findByHdimId(String module, String hdimIdField, String hdimId) {
        String criteria = "(" + hdimIdField + ":equals:" + hdimId + ")";
        List<Map<String, Object>> results = searchRecords(module, criteria);
        return results.isEmpty() ? null : results.get(0);
    }

    private String getModuleFields(String module) {
        return switch (module) {
            case "Leads" -> "id,First_Name,Last_Name,Email,Phone,Company,Designation,Lead_Source,Lead_Status,State,Modified_Time,HDIM_Lead_ID";
            case "Accounts" -> "id,Account_Name,Website,Phone,Billing_State,Industry,Modified_Time,HDIM_Account_ID";
            case "Contacts" -> "id,First_Name,Last_Name,Email,Phone,Title,Department,Modified_Time,HDIM_Contact_ID";
            case "Deals" -> "id,Deal_Name,Amount,Stage,Probability,Closing_Date,Modified_Time,HDIM_Opportunity_ID";
            default -> "id,Modified_Time";
        };
    }

    // ==================== Delete Operations ====================

    @CircuitBreaker(name = "zohoService")
    @Retry(name = "zohoService")
    public boolean deleteZohoRecord(String module, String zohoId) {
        if (!zohoConfig.getSync().isEnabled() || !oAuthService.isConfigured()) {
            return false;
        }

        try {
            String accessToken = oAuthService.getAccessToken();
            WebClient webClient = webClientBuilder
                .baseUrl(zohoConfig.getApi().getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Zoho-oauthtoken " + accessToken)
                .build();

            webClient.delete()
                .uri("/" + module + "/" + zohoId)
                .retrieve()
                .bodyToMono(Void.class)
                .block();

            log.info("Deleted {} record from Zoho: {}", module, zohoId);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete {} record {}: {}", module, zohoId, e.getMessage());
            return false;
        }
    }

    // ==================== API Operations ====================

    private ZohoSyncResult createZohoRecord(String module, Map<String, Object> data) {
        String accessToken = oAuthService.getAccessToken();

        WebClient webClient = webClientBuilder
            .baseUrl(zohoConfig.getApi().getBaseUrl())
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Zoho-oauthtoken " + accessToken)
            .build();

        Map<String, Object> requestBody = Map.of("data", List.of(data));

        @SuppressWarnings("unchecked")
        Map<String, Object> response = webClient.post()
            .uri("/" + module)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(Map.class)
            .block();

        if (response != null && response.containsKey("data")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> dataList = (List<Map<String, Object>>) response.get("data");
            if (!dataList.isEmpty()) {
                Map<String, Object> firstRecord = dataList.get(0);
                @SuppressWarnings("unchecked")
                Map<String, Object> details = (Map<String, Object>) firstRecord.get("details");
                String zohoId = (String) details.get("id");
                log.info("Created {} record in Zoho with ID: {}", module, zohoId);
                return ZohoSyncResult.success(zohoId);
            }
        }

        return ZohoSyncResult.failed("Unknown error creating record");
    }

    private ZohoSyncResult updateZohoRecord(String module, String zohoId, Map<String, Object> data) {
        String accessToken = oAuthService.getAccessToken();

        WebClient webClient = webClientBuilder
            .baseUrl(zohoConfig.getApi().getBaseUrl())
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Zoho-oauthtoken " + accessToken)
            .build();

        data.put("id", zohoId);
        Map<String, Object> requestBody = Map.of("data", List.of(data));

        @SuppressWarnings("unchecked")
        Map<String, Object> response = webClient.put()
            .uri("/" + module)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(Map.class)
            .block();

        if (response != null && response.containsKey("data")) {
            log.info("Updated {} record in Zoho: {}", module, zohoId);
            return ZohoSyncResult.success(zohoId);
        }

        return ZohoSyncResult.failed("Unknown error updating record");
    }

    // ==================== Mapping Methods ====================

    private Map<String, Object> mapLeadToZoho(Lead lead) {
        Map<String, Object> zoho = new HashMap<>();
        zoho.put("First_Name", lead.getFirstName());
        zoho.put("Last_Name", lead.getLastName());
        zoho.put("Email", lead.getEmail());
        zoho.put("Phone", lead.getPhone());
        zoho.put("Company", lead.getCompany());
        zoho.put("Designation", lead.getTitle());
        zoho.put("Website", lead.getWebsite());
        zoho.put("Lead_Source", mapLeadSource(lead.getSource()));
        zoho.put("Lead_Status", mapLeadStatus(lead.getStatus()));
        zoho.put("State", lead.getState());
        zoho.put("Rating", mapScoreToRating(lead.getScore()));
        zoho.put("Description", lead.getNotes());
        // Custom fields
        zoho.put("HDIM_Lead_ID", lead.getId().toString());
        zoho.put("Organization_Type", lead.getOrganizationType() != null ?
            lead.getOrganizationType().name() : null);
        zoho.put("Patient_Count", lead.getPatientCount());
        zoho.put("EHR_Count", lead.getEhrCount());
        return zoho;
    }

    private Map<String, Object> mapAccountToZoho(Account account) {
        Map<String, Object> zoho = new HashMap<>();
        zoho.put("Account_Name", account.getName());
        zoho.put("Website", account.getWebsite());
        zoho.put("Phone", account.getPhone());
        zoho.put("Billing_Street", account.getAddressLine1());
        zoho.put("Billing_City", account.getCity());
        zoho.put("Billing_State", account.getState());
        zoho.put("Billing_Code", account.getZipCode());
        zoho.put("Industry", account.getIndustry());
        zoho.put("Annual_Revenue", account.getAnnualRevenue());
        zoho.put("Employees", account.getEmployeeCount());
        zoho.put("Description", account.getDescription());
        // Custom fields
        zoho.put("HDIM_Account_ID", account.getId().toString());
        zoho.put("Organization_Type", account.getOrganizationType() != null ?
            account.getOrganizationType().name() : null);
        zoho.put("Patient_Count", account.getPatientCount());
        zoho.put("EHR_Count", account.getEhrCount());
        zoho.put("EHR_Systems", account.getEhrSystems());
        return zoho;
    }

    private Map<String, Object> mapContactToZoho(Contact contact) {
        Map<String, Object> zoho = new HashMap<>();
        zoho.put("First_Name", contact.getFirstName());
        zoho.put("Last_Name", contact.getLastName());
        zoho.put("Email", contact.getEmail());
        zoho.put("Phone", contact.getPhone());
        zoho.put("Mobile", contact.getMobile());
        zoho.put("Title", contact.getTitle());
        zoho.put("Department", contact.getDepartment());
        zoho.put("Do_Not_Call", contact.getDoNotCall());
        zoho.put("Email_Opt_Out", contact.getDoNotEmail());
        zoho.put("Description", contact.getNotes());
        // Custom fields
        zoho.put("HDIM_Contact_ID", contact.getId().toString());
        zoho.put("Contact_Type", contact.getContactType() != null ?
            contact.getContactType().name() : null);
        zoho.put("Is_Primary", contact.getPrimary());
        return zoho;
    }

    private Map<String, Object> mapOpportunityToZoho(Opportunity opportunity) {
        Map<String, Object> zoho = new HashMap<>();
        zoho.put("Deal_Name", opportunity.getName());
        zoho.put("Amount", opportunity.getAmount());
        zoho.put("Stage", mapOpportunityStage(opportunity.getStage()));
        zoho.put("Probability", opportunity.getProbability());
        zoho.put("Closing_Date", opportunity.getExpectedCloseDate() != null ?
            opportunity.getExpectedCloseDate().toString() : null);
        zoho.put("Description", opportunity.getDescription());
        zoho.put("Next_Step", opportunity.getNextStep());
        // Custom fields
        zoho.put("HDIM_Opportunity_ID", opportunity.getId().toString());
        zoho.put("Product_Tier", opportunity.getProductTier());
        zoho.put("Contract_Length_Months", opportunity.getContractLengthMonths());
        zoho.put("Competitor", opportunity.getCompetitor());
        if (opportunity.getLostReason() != null) {
            zoho.put("Lost_Reason", opportunity.getLostReason().name());
            zoho.put("Lost_Reason_Detail", opportunity.getLostReasonDetail());
        }
        return zoho;
    }

    // ==================== Value Mapping ====================

    private String mapLeadSource(LeadSource source) {
        if (source == null) return null;
        return switch (source) {
            case WEBSITE -> "Website";
            case ROI_CALCULATOR -> "Partner";
            case DEMO_REQUEST -> "Demo Request";
            case REFERRAL -> "Employee Referral";
            case CONFERENCE -> "Trade Show";
            case WEBINAR -> "Seminar Partner";
            case LINKEDIN -> "Social Media";
            case COLD_OUTREACH -> "Cold Call";
            case CONTENT_DOWNLOAD -> "External Referral";
            case PARTNER -> "Partner";
            case OTHER -> "Other";
        };
    }

    private String mapLeadStatus(LeadStatus status) {
        if (status == null) return "Not Contacted";
        return switch (status) {
            case NEW -> "Not Contacted";
            case CONTACTED -> "Attempted to Contact";
            case ENGAGED -> "Contact in Future";
            case QUALIFIED -> "Contact in Future";
            case UNQUALIFIED -> "Junk Lead";
            case CONVERTED -> "Converted";
            case LOST -> "Lost Lead";
        };
    }

    private String mapOpportunityStage(OpportunityStage stage) {
        if (stage == null) return "Qualification";
        return switch (stage) {
            case DISCOVERY -> "Qualification";
            case DEMO -> "Needs Analysis";
            case PROPOSAL -> "Value Proposition";
            case NEGOTIATION -> "Negotiation/Review";
            case CONTRACT -> "Proposal/Price Quote";
            case CLOSED_WON -> "Closed Won";
            case CLOSED_LOST -> "Closed Lost";
        };
    }

    private String mapScoreToRating(Integer score) {
        if (score == null || score < 30) return "-None-";
        if (score < 50) return "Cold";
        if (score < 70) return "Warm";
        return "Hot";
    }

    // ==================== Result Class ====================

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    public static class ZohoSyncResult {
        private boolean success;
        private boolean skipped;
        private String zohoId;
        private String errorMessage;

        public static ZohoSyncResult success(String zohoId) {
            return ZohoSyncResult.builder()
                .success(true)
                .zohoId(zohoId)
                .build();
        }

        public static ZohoSyncResult skipped(String reason) {
            return ZohoSyncResult.builder()
                .skipped(true)
                .errorMessage(reason)
                .build();
        }

        public static ZohoSyncResult failed(String error) {
            return ZohoSyncResult.builder()
                .success(false)
                .errorMessage(error)
                .build();
        }
    }
}
