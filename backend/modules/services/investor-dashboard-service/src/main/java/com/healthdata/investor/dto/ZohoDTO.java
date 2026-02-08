package com.healthdata.investor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * DTOs for Zoho ONE integration
 */
public class ZohoDTO {

    /**
     * Response containing Zoho OAuth authorization URL
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorizationUrlResponse {
        private String authorizationUrl;
        private String state;
    }

    /**
     * Request for handling OAuth callback
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OAuthCallbackRequest {
        private String code;
        private String state;
        @JsonProperty("accounts-server")
        private String accountsServer; // Zoho data center domain
    }

    /**
     * Zoho connection status
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConnectionStatus {
        private boolean connected;
        private String zohoEmail;
        private String displayName;
        private String organizationId;
        private String apiDomain;
        private Instant expiresAt;
        private Instant lastSync;
        private String syncError;
    }

    /**
     * Zoho user profile information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Profile {
        private String email;
        private String firstName;
        private String lastName;
        private String organizationId;
        private String timezone;
    }

    /**
     * Zoho CRM Lead record
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CRMLead {
        private String id;
        private String firstName;
        private String lastName;
        private String email;
        private String company;
        private String phone;
        private String leadSource;
        private String leadStatus;
        private String industry;
        private String description;
        private Map<String, Object> customFields;
        private Instant createdTime;
        private Instant modifiedTime;
    }

    /**
     * Request to create/update CRM lead
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateLeadRequest {
        private String firstName;
        private String lastName;
        private String email;
        private String company;
        private String phone;
        private String leadSource;
        private String leadStatus;
        private String industry;
        private String description;
        private Map<String, Object> customFields;
    }

    /**
     * Zoho CRM activity log
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Activity {
        private String id;
        private String activityType; // Email, Call, Meeting, Task
        private String subject;
        private String description;
        private String relatedTo; // Lead ID, Contact ID, Deal ID
        private Instant activityTime;
        private String status;
    }

    /**
     * Request to log activity in CRM
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LogActivityRequest {
        private String activityType;
        private String subject;
        private String description;
        private String leadEmail;
        private Instant activityTime;
    }

    /**
     * Zoho Campaigns email campaign
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Campaign {
        private String campaignKey;
        private String campaignName;
        private String fromEmail;
        private String subject;
        private String status;
        private Integer sent;
        private Integer opened;
        private Integer clicked;
        private Instant sentTime;
    }

    /**
     * Request to enroll lead in email campaign
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnrollCampaignRequest {
        private String listKey; // Zoho Campaigns mailing list ID
        private String email;
        private String firstName;
        private String lastName;
        private Map<String, String> customFields;
    }

    /**
     * Zoho Bookings meeting type
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MeetingType {
        private String serviceId;
        private String serviceName;
        private Integer duration; // in minutes
        private String bookingUrl;
        private String description;
    }

    /**
     * Zoho Bookings appointment
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Appointment {
        private String bookingId;
        private String serviceName;
        private String customerName;
        private String customerEmail;
        private Instant startTime;
        private Instant endTime;
        private String status;
        private String meetingLink;
    }

    /**
     * Search results from Zoho CRM
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchResults {
        private List<CRMLead> leads;
        private Integer totalRecords;
        private Boolean moreRecords;
    }

    /**
     * Generic API response wrapper
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiResponse<T> {
        private boolean success;
        private T data;
        private String message;
        private String errorCode;
    }
}
