package com.healthdata.sales.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "zoho")
public class ZohoConfig {

    private Api api = new Api();
    private OAuth oauth = new OAuth();
    private Sync sync = new Sync();
    private Webhook webhook = new Webhook();

    @Data
    public static class Api {
        private String baseUrl = "https://www.zohoapis.com/crm/v3";
        private String accountsUrl = "https://accounts.zoho.com";
    }

    @Data
    public static class OAuth {
        private String clientId;
        private String clientSecret;
        private String refreshToken;
        private String scope = "ZohoCRM.modules.ALL,ZohoCRM.settings.ALL";
    }

    @Data
    public static class Sync {
        private boolean enabled = false;
        private int intervalMinutes = 15;
    }

    @Data
    public static class Webhook {
        private boolean enabled = false;
        private String secret;
    }
}
