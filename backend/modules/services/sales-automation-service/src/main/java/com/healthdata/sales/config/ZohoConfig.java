package com.healthdata.sales.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Zoho ONE Platform Configuration
 * 
 * Supports the full Zoho ONE suite including:
 * - Zoho CRM (Leads, Accounts, Contacts, Deals)
 * - Zoho Campaigns (Email marketing automation)
 * - Zoho Bookings (Meeting scheduler)
 * - Zoho Analytics (Business intelligence)
 * - Zoho Meeting (Video conferencing)
 * - Zoho Flow (Integration platform)
 * - Zoho Desk (Customer support)
 * 
 * All services share the same OAuth credentials via Zoho ONE single sign-on.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "zoho")
public class ZohoConfig {

    private Api api = new Api();
    private OAuth oauth = new OAuth();
    private Sync sync = new Sync();
    private Webhook webhook = new Webhook();
    private Campaigns campaigns = new Campaigns();
    private Bookings bookings = new Bookings();
    private Analytics analytics = new Analytics();
    private Meeting meeting = new Meeting();
    private Flow flow = new Flow();
    private Desk desk = new Desk();

    @Data
    public static class Api {
        private String baseUrl = "https://www.zohoapis.com/crm/v3";
        private String accountsUrl = "https://accounts.zoho.com";
        private String datacenter = "com"; // Options: com, eu, in, com.au, com.cn, jp
    }

    @Data
    public static class OAuth {
        private String clientId;
        private String clientSecret;
        private String refreshToken;
        private String scope = "ZohoCRM.modules.ALL,ZohoCRM.settings.ALL,ZohoCampaigns.contact.ALL,ZohoCampaigns.campaign.ALL,ZohoBookings.fullaccess.ALL,ZohoAnalytics.data.ALL,ZohoMeeting.fullaccess.ALL";
        private String redirectUri;
    }

    @Data
    public static class Sync {
        private boolean enabled = false;
        private int intervalMinutes = 15;
        private String defaultTenantId; // Optional: Default tenant ID for scheduled sync if no tenants found
        private boolean bidirectional = true; // Enable Zoho -> HDIM sync
    }

    @Data
    public static class Webhook {
        private boolean enabled = false;
        private String secret;
        private String callbackUrl; // URL Zoho will call for notifications
    }

    /**
     * Zoho Campaigns Configuration
     * Email marketing automation
     */
    @Data
    public static class Campaigns {
        private boolean enabled = false;
        private String baseUrl = "https://campaigns.zoho.com/api/v1.1";
        private String organizationId; // Zoho Campaigns org ID (ZOID)
        private String defaultListKey; // Default mailing list key
        private EmailDefaults emailDefaults = new EmailDefaults();

        @Data
        public static class EmailDefaults {
            private String fromName = "HealthData-in-Motion";
            private String fromEmail;
            private String replyTo;
            private boolean trackOpens = true;
            private boolean trackClicks = true;
        }
    }

    /**
     * Zoho Bookings Configuration
     * Appointment scheduling
     */
    @Data
    public static class Bookings {
        private boolean enabled = false;
        private String baseUrl = "https://www.zohoapis.com/bookings/v1";
        private String workspaceId; // Bookings workspace ID
        private String defaultStaffId; // Default staff for bookings
        private MeetingTypes meetingTypes = new MeetingTypes();
        private Notifications notifications = new Notifications();

        @Data
        public static class MeetingTypes {
            private String discoveryCall; // 15-min discovery call service ID
            private String productDemo;   // 30-min product demo service ID
            private String technicalDeepDive; // 60-min technical deep dive service ID
            private String dueDiligence;  // 30-min due diligence Q&A service ID
        }

        @Data
        public static class Notifications {
            private boolean emailReminder = true;
            private int reminderHoursBefore = 24;
            private boolean smsReminder = false;
        }
    }

    /**
     * Zoho Analytics Configuration
     * Business intelligence and reporting
     */
    @Data
    public static class Analytics {
        private boolean enabled = false;
        private String baseUrl = "https://analyticsapi.zoho.com/api";
        private String organizationId; // Analytics org ID
        private String workspaceId;    // Workspace containing HDIM reports
        private Dashboards dashboards = new Dashboards();

        @Data
        public static class Dashboards {
            private String investorPipeline; // Dashboard ID for investor pipeline
            private String marketingPerformance; // Dashboard ID for marketing metrics
            private String salesPerformance; // Dashboard ID for sales team metrics
        }
    }

    /**
     * Zoho Meeting Configuration
     * Video conferencing (Zoom alternative)
     */
    @Data
    public static class Meeting {
        private boolean enabled = false;
        private String baseUrl = "https://meeting.zoho.com/api/v2";
        private int defaultDurationMinutes = 30;
        private boolean autoRecord = false;
        private boolean waitingRoom = true;
    }

    /**
     * Zoho Flow Configuration
     * Integration platform (Zapier alternative)
     */
    @Data
    public static class Flow {
        private boolean enabled = false;
        private String baseUrl = "https://flow.zoho.com/api";
        private String webhookUrl; // Flow webhook trigger URL
    }

    /**
     * Zoho Desk Configuration
     * Customer support
     */
    @Data
    public static class Desk {
        private boolean enabled = false;
        private String baseUrl = "https://desk.zoho.com/api/v1";
        private String organizationId;
        private String defaultDepartmentId;
    }
}
