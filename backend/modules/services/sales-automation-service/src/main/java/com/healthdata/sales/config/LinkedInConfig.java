package com.healthdata.sales.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * LinkedIn API Configuration
 *
 * Note: LinkedIn API access requires partner approval for most features.
 * - Basic profile data: Available with LinkedIn Login
 * - Connections API: Requires Marketing Developer Platform access
 * - InMail: Requires Sales Navigator Team/Enterprise
 */
@Configuration
@ConfigurationProperties(prefix = "linkedin")
@Data
public class LinkedInConfig {

    private OAuth oauth = new OAuth();
    private Api api = new Api();
    private Outreach outreach = new Outreach();

    @Data
    public static class OAuth {
        private String clientId;
        private String clientSecret;
        private String redirectUri;
        private String scope = "r_liteprofile r_emailaddress w_member_social";
    }

    @Data
    public static class Api {
        private String baseUrl = "https://api.linkedin.com/v2";
        private String authUrl = "https://www.linkedin.com/oauth/v2";
        private boolean enabled = false;
    }

    @Data
    public static class Outreach {
        /**
         * Enable automated outreach tracking
         */
        private boolean enabled = true;

        /**
         * Maximum connection requests per day (LinkedIn limit: ~100)
         */
        private int maxConnectionsPerDay = 50;

        /**
         * Maximum InMails per day (depends on subscription)
         */
        private int maxInMailsPerDay = 25;

        /**
         * Delay between automated actions (ms)
         */
        private long actionDelayMs = 30000; // 30 seconds

        /**
         * Personalization templates directory
         */
        private String templatesPath = "classpath:linkedin/templates/";
    }
}
