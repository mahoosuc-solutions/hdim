package com.healthdata.sales.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.client.RestTemplate;

import java.util.Properties;

/**
 * Email Configuration
 *
 * Supports multiple email providers:
 * - SMTP (default): Standard SMTP for Zoho, Gmail, etc.
 * - SendGrid: SendGrid API for high-volume sending
 * - SES: Amazon SES for AWS-integrated deployments
 *
 * Configuration in application.yml:
 * ```
 * email:
 *   provider: smtp # or sendgrid, ses
 *   from:
 *     address: noreply@company.com
 *     name: Company Name
 *   tracking:
 *     enabled: true
 *
 * spring.mail:
 *   host: smtp.zoho.com
 *   port: 587
 *   username: ${MAIL_USERNAME}
 *   password: ${MAIL_PASSWORD}
 *
 * # For SendGrid:
 * email.sendgrid:
 *   api-key: ${SENDGRID_API_KEY}
 *
 * # For SES:
 * email.ses:
 *   region: us-east-1
 *   access-key: ${AWS_ACCESS_KEY}
 *   secret-key: ${AWS_SECRET_KEY}
 * ```
 */
@Configuration
@ConfigurationProperties(prefix = "email")
@Data
public class EmailConfig {

    /**
     * Email provider: smtp (default), sendgrid, ses
     */
    private String provider = "smtp";

    /**
     * Default from address
     */
    private From from = new From();

    /**
     * Email tracking configuration
     */
    private Tracking tracking = new Tracking();

    /**
     * SendGrid configuration (if provider = sendgrid)
     */
    private SendGrid sendgrid = new SendGrid();

    /**
     * Amazon SES configuration (if provider = ses)
     */
    private Ses ses = new Ses();

    /**
     * Rate limiting
     */
    private RateLimit rateLimit = new RateLimit();

    @Data
    public static class From {
        private String address;
        private String name = "HealthData-in-Motion";
    }

    @Data
    public static class Tracking {
        private boolean enabled = true;
        private String pixelUrl;
        private String clickUrl;
    }

    @Data
    public static class SendGrid {
        private String apiKey;
        private String apiUrl = "https://api.sendgrid.com/v3/mail/send";
    }

    @Data
    public static class Ses {
        private String region = "us-east-1";
        private String accessKey;
        private String secretKey;
        private String configurationSetName;
    }

    @Data
    public static class RateLimit {
        /**
         * Maximum emails per second
         */
        private int perSecond = 10;

        /**
         * Maximum emails per day (0 = unlimited)
         */
        private int perDay = 0;

        /**
         * Delay between batch sends (ms)
         */
        private long batchDelayMs = 100;
    }

    /**
     * REST template for API-based providers (SendGrid)
     */
    @Bean
    public RestTemplate emailRestTemplate() {
        return new RestTemplate();
    }

    /**
     * Check if SMTP provider is configured
     */
    public boolean isSmtpProvider() {
        return "smtp".equalsIgnoreCase(provider);
    }

    /**
     * Check if SendGrid provider is configured
     */
    public boolean isSendGridProvider() {
        return "sendgrid".equalsIgnoreCase(provider) && sendgrid.getApiKey() != null;
    }

    /**
     * Check if SES provider is configured
     */
    public boolean isSesProvider() {
        return "ses".equalsIgnoreCase(provider);
    }
}
