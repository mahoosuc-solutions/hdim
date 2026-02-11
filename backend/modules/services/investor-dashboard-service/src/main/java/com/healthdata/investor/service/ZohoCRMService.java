package com.healthdata.investor.service;

import com.healthdata.investor.dto.ZohoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for Zoho CRM operations.
 * Manages leads, contacts, deals, and activities.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ZohoCRMService {

    private final ZohoOAuthService oauthService;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Create or update lead in Zoho CRM
     */
    public ZohoDTO.CRMLead createOrUpdateLead(UUID userId, ZohoDTO.CreateLeadRequest request) {
        // Check if lead exists
        ZohoDTO.SearchResults existing = searchLeadsByEmail(userId, request.getEmail());
        if (existing.getTotalRecords() > 0) {
            // Update existing lead
            ZohoDTO.CRMLead existingLead = existing.getLeads().get(0);
            return updateLead(userId, existingLead.getId(), request);
        }

        // Create new lead
        String accessToken = oauthService.getAccessToken(userId);
        String apiDomain = oauthService.getApiDomain(userId);
        String apiUrl = "https://www.zohoapis." + apiDomain + "/crm/v2/Leads";

        Map<String, Object> leadData = buildLeadData(request);
        Map<String, Object> requestBody = Map.of("data", List.of(leadData));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> httpRequest = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    httpRequest,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            List<Map<String, Object>> data = (List<Map<String, Object>>) responseBody.get("data");
            Map<String, Object> createdLead = data.get(0);

            String leadId = (String) ((Map<String, Object>) createdLead.get("details")).get("id");
            log.info("Created Zoho CRM lead: {} for email: {}", leadId, request.getEmail());

            return getLead(userId, leadId);
        } catch (Exception e) {
            log.error("Failed to create Zoho CRM lead", e);
            throw new RuntimeException("Failed to create CRM lead: " + e.getMessage());
        }
    }

    /**
     * Get lead by ID
     */
    public ZohoDTO.CRMLead getLead(UUID userId, String leadId) {
        String accessToken = oauthService.getAccessToken(userId);
        String apiDomain = oauthService.getApiDomain(userId);
        String apiUrl = "https://www.zohoapis." + apiDomain + "/crm/v2/Leads/" + leadId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.GET,
                    request,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            List<Map<String, Object>> data = (List<Map<String, Object>>) responseBody.get("data");
            Map<String, Object> leadData = data.get(0);

            return mapToLeadDTO(leadData);
        } catch (Exception e) {
            log.error("Failed to get Zoho CRM lead: {}", leadId, e);
            throw new RuntimeException("Failed to get CRM lead: " + e.getMessage());
        }
    }

    /**
     * Update existing lead
     */
    public ZohoDTO.CRMLead updateLead(UUID userId, String leadId, ZohoDTO.CreateLeadRequest request) {
        String accessToken = oauthService.getAccessToken(userId);
        String apiDomain = oauthService.getApiDomain(userId);
        String apiUrl = "https://www.zohoapis." + apiDomain + "/crm/v2/Leads/" + leadId;

        Map<String, Object> leadData = buildLeadData(request);
        leadData.put("id", leadId);
        Map<String, Object> requestBody = Map.of("data", List.of(leadData));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> httpRequest = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.PUT,
                    httpRequest,
                    Map.class
            );

            log.info("Updated Zoho CRM lead: {}", leadId);
            return getLead(userId, leadId);
        } catch (Exception e) {
            log.error("Failed to update Zoho CRM lead: {}", leadId, e);
            throw new RuntimeException("Failed to update CRM lead: " + e.getMessage());
        }
    }

    /**
     * Search leads by email
     */
    public ZohoDTO.SearchResults searchLeadsByEmail(UUID userId, String email) {
        String accessToken = oauthService.getAccessToken(userId);
        String apiDomain = oauthService.getApiDomain(userId);
        String apiUrl = "https://www.zohoapis." + apiDomain + "/crm/v2/Leads/search";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        String searchCriteria = "(Email:equals:" + email + ")";
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl + "?criteria=" + searchCriteria,
                    HttpMethod.GET,
                    request,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            List<Map<String, Object>> data = (List<Map<String, Object>>) responseBody.get("data");
            Map<String, Object> info = (Map<String, Object>) responseBody.get("info");

            List<ZohoDTO.CRMLead> leads = data != null ?
                    data.stream().map(this::mapToLeadDTO).collect(Collectors.toList()) :
                    new ArrayList<>();

            return ZohoDTO.SearchResults.builder()
                    .leads(leads)
                    .totalRecords(leads.size())
                    .moreRecords((Boolean) info.get("more_records"))
                    .build();
        } catch (Exception e) {
            log.warn("No leads found for email: {}", email);
            return ZohoDTO.SearchResults.builder()
                    .leads(new ArrayList<>())
                    .totalRecords(0)
                    .moreRecords(false)
                    .build();
        }
    }

    /**
     * Log activity in CRM
     */
    public ZohoDTO.Activity logActivity(UUID userId, ZohoDTO.LogActivityRequest request) {
        // First, find the lead by email
        ZohoDTO.SearchResults leadResults = searchLeadsByEmail(userId, request.getLeadEmail());
        if (leadResults.getTotalRecords() == 0) {
            throw new RuntimeException("Lead not found: " + request.getLeadEmail());
        }

        String leadId = leadResults.getLeads().get(0).getId();
        String accessToken = oauthService.getAccessToken(userId);
        String apiDomain = oauthService.getApiDomain(userId);
        String apiUrl = "https://www.zohoapis." + apiDomain + "/crm/v2/Tasks";

        Map<String, Object> taskData = new HashMap<>();
        taskData.put("Subject", request.getSubject());
        taskData.put("Description", request.getDescription());
        taskData.put("Status", "Completed");
        taskData.put("What_Id", leadId);
        taskData.put("Due_Date", request.getActivityTime() != null ?
                request.getActivityTime().toString() : java.time.Instant.now().toString());

        Map<String, Object> requestBody = Map.of("data", List.of(taskData));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> httpRequest = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    httpRequest,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            List<Map<String, Object>> data = (List<Map<String, Object>>) responseBody.get("data");
            Map<String, Object> createdTask = data.get(0);

            String taskId = (String) ((Map<String, Object>) createdTask.get("details")).get("id");
            log.info("Logged activity in Zoho CRM: {} for lead: {}", taskId, leadId);

            return ZohoDTO.Activity.builder()
                    .id(taskId)
                    .activityType(request.getActivityType())
                    .subject(request.getSubject())
                    .description(request.getDescription())
                    .relatedTo(leadId)
                    .activityTime(request.getActivityTime())
                    .status("Completed")
                    .build();
        } catch (Exception e) {
            log.error("Failed to log activity in Zoho CRM", e);
            throw new RuntimeException("Failed to log CRM activity: " + e.getMessage());
        }
    }

    private Map<String, Object> buildLeadData(ZohoDTO.CreateLeadRequest request) {
        Map<String, Object> data = new HashMap<>();
        data.put("First_Name", request.getFirstName());
        data.put("Last_Name", request.getLastName());
        data.put("Email", request.getEmail());
        data.put("Company", request.getCompany());
        data.put("Phone", request.getPhone());
        data.put("Lead_Source", request.getLeadSource() != null ? request.getLeadSource() : "Website");
        data.put("Lead_Status", request.getLeadStatus() != null ? request.getLeadStatus() : "Not Contacted");
        data.put("Industry", request.getIndustry());
        data.put("Description", request.getDescription());

        if (request.getCustomFields() != null) {
            data.putAll(request.getCustomFields());
        }

        return data;
    }

    private ZohoDTO.CRMLead mapToLeadDTO(Map<String, Object> data) {
        return ZohoDTO.CRMLead.builder()
                .id((String) data.get("id"))
                .firstName((String) data.get("First_Name"))
                .lastName((String) data.get("Last_Name"))
                .email((String) data.get("Email"))
                .company((String) data.get("Company"))
                .phone((String) data.get("Phone"))
                .leadSource((String) data.get("Lead_Source"))
                .leadStatus((String) data.get("Lead_Status"))
                .industry((String) data.get("Industry"))
                .description((String) data.get("Description"))
                .createdTime(data.get("Created_Time") != null ?
                        java.time.Instant.parse((String) data.get("Created_Time")) : null)
                .modifiedTime(data.get("Modified_Time") != null ?
                        java.time.Instant.parse((String) data.get("Modified_Time")) : null)
                .build();
    }
}
