package com.healthdata.ehr.connector.epic;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.security.PrivateKey;

/**
 * Configuration properties for Epic FHIR connection.
 */
@Configuration
@ConfigurationProperties(prefix = "epic")
public class EpicConnectionConfig {

    private String baseUrl;
    private String tokenUrl;
    private String clientId;
    private String privateKeyPath;
    private PrivateKey privateKey;
    private boolean sandboxMode = false;
    private int tokenCacheDurationMinutes = 50;
    private int maxRetries = 3;
    private int requestTimeoutSeconds = 30;
    private int rateLimitPerSecond = 10;

    // Epic-specific settings
    private boolean useAppOrchard = false;
    private String appOrchardClientId;
    private boolean myChartEnabled = false;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public void setTokenUrl(String tokenUrl) {
        this.tokenUrl = tokenUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getPrivateKeyPath() {
        return privateKeyPath;
    }

    public void setPrivateKeyPath(String privateKeyPath) {
        this.privateKeyPath = privateKeyPath;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    public boolean isSandboxMode() {
        return sandboxMode;
    }

    public void setSandboxMode(boolean sandboxMode) {
        this.sandboxMode = sandboxMode;
    }

    public int getTokenCacheDurationMinutes() {
        return tokenCacheDurationMinutes;
    }

    public void setTokenCacheDurationMinutes(int tokenCacheDurationMinutes) {
        this.tokenCacheDurationMinutes = tokenCacheDurationMinutes;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public int getRequestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }

    public void setRequestTimeoutSeconds(int requestTimeoutSeconds) {
        this.requestTimeoutSeconds = requestTimeoutSeconds;
    }

    public int getRateLimitPerSecond() {
        return rateLimitPerSecond;
    }

    public void setRateLimitPerSecond(int rateLimitPerSecond) {
        this.rateLimitPerSecond = rateLimitPerSecond;
    }

    public boolean isUseAppOrchard() {
        return useAppOrchard;
    }

    public void setUseAppOrchard(boolean useAppOrchard) {
        this.useAppOrchard = useAppOrchard;
    }

    public String getAppOrchardClientId() {
        return appOrchardClientId;
    }

    public void setAppOrchardClientId(String appOrchardClientId) {
        this.appOrchardClientId = appOrchardClientId;
    }

    public boolean isMyChartEnabled() {
        return myChartEnabled;
    }

    public void setMyChartEnabled(boolean myChartEnabled) {
        this.myChartEnabled = myChartEnabled;
    }
}
