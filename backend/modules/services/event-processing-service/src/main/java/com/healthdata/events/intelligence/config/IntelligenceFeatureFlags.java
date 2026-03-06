package com.healthdata.events.intelligence.config;

import com.healthdata.events.intelligence.controller.FeatureDisabledException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "intelligence.features")
public class IntelligenceFeatureFlags {

    private boolean ingestEnabled = true;
    private boolean recommendationReviewEnabled = true;
    private boolean validationStatusUpdateEnabled = true;
    private boolean trustDashboardEnabled = true;

    private Set<String> ingestTenantAllowlist = new HashSet<>();
    private Set<String> recommendationReviewTenantAllowlist = new HashSet<>();
    private Set<String> validationStatusUpdateTenantAllowlist = new HashSet<>();
    private Set<String> trustDashboardTenantAllowlist = new HashSet<>();

    public void requireIngestEnabled(String tenantId) {
        if (!isEnabledForTenant(ingestEnabled, ingestTenantAllowlist, tenantId)) {
            throw new FeatureDisabledException("Intelligence ingest is disabled for tenant");
        }
    }

    public void requireRecommendationReviewEnabled(String tenantId) {
        if (!isEnabledForTenant(recommendationReviewEnabled, recommendationReviewTenantAllowlist, tenantId)) {
            throw new FeatureDisabledException("Recommendation review is disabled for tenant");
        }
    }

    public void requireValidationStatusUpdateEnabled(String tenantId) {
        if (!isEnabledForTenant(validationStatusUpdateEnabled, validationStatusUpdateTenantAllowlist, tenantId)) {
            throw new FeatureDisabledException("Validation finding updates are disabled for tenant");
        }
    }

    public void requireTrustDashboardEnabled(String tenantId) {
        if (!isEnabledForTenant(trustDashboardEnabled, trustDashboardTenantAllowlist, tenantId)) {
            throw new FeatureDisabledException("Tenant trust dashboard is disabled for tenant");
        }
    }

    private boolean isEnabledForTenant(boolean enabled, Set<String> allowlist, String tenantId) {
        if (!enabled) {
            return false;
        }
        if (allowlist == null || allowlist.isEmpty()) {
            return true;
        }
        return allowlist.stream().map(String::trim).filter(s -> !s.isEmpty()).anyMatch(tenantId::equals);
    }

    public boolean isIngestEnabled() {
        return ingestEnabled;
    }

    public void setIngestEnabled(boolean ingestEnabled) {
        this.ingestEnabled = ingestEnabled;
    }

    public boolean isRecommendationReviewEnabled() {
        return recommendationReviewEnabled;
    }

    public void setRecommendationReviewEnabled(boolean recommendationReviewEnabled) {
        this.recommendationReviewEnabled = recommendationReviewEnabled;
    }

    public boolean isValidationStatusUpdateEnabled() {
        return validationStatusUpdateEnabled;
    }

    public void setValidationStatusUpdateEnabled(boolean validationStatusUpdateEnabled) {
        this.validationStatusUpdateEnabled = validationStatusUpdateEnabled;
    }

    public boolean isTrustDashboardEnabled() {
        return trustDashboardEnabled;
    }

    public void setTrustDashboardEnabled(boolean trustDashboardEnabled) {
        this.trustDashboardEnabled = trustDashboardEnabled;
    }

    public Set<String> getIngestTenantAllowlist() {
        return ingestTenantAllowlist;
    }

    public void setIngestTenantAllowlist(Set<String> ingestTenantAllowlist) {
        this.ingestTenantAllowlist = ingestTenantAllowlist;
    }

    public Set<String> getRecommendationReviewTenantAllowlist() {
        return recommendationReviewTenantAllowlist;
    }

    public void setRecommendationReviewTenantAllowlist(Set<String> recommendationReviewTenantAllowlist) {
        this.recommendationReviewTenantAllowlist = recommendationReviewTenantAllowlist;
    }

    public Set<String> getValidationStatusUpdateTenantAllowlist() {
        return validationStatusUpdateTenantAllowlist;
    }

    public void setValidationStatusUpdateTenantAllowlist(Set<String> validationStatusUpdateTenantAllowlist) {
        this.validationStatusUpdateTenantAllowlist = validationStatusUpdateTenantAllowlist;
    }

    public Set<String> getTrustDashboardTenantAllowlist() {
        return trustDashboardTenantAllowlist;
    }

    public void setTrustDashboardTenantAllowlist(Set<String> trustDashboardTenantAllowlist) {
        this.trustDashboardTenantAllowlist = trustDashboardTenantAllowlist;
    }
}
