package com.healthdata.featureflags;

/**
 * Exception thrown when a feature is disabled for a tenant
 *
 * This exception can be handled by @ControllerAdvice to return
 * HTTP 403 Forbidden or a custom error response.
 */
public class FeatureFlagDisabledException extends RuntimeException {

    private final String tenantId;
    private final String featureKey;

    public FeatureFlagDisabledException(String tenantId, String featureKey) {
        super(String.format("Feature '%s' is disabled for tenant '%s'", featureKey, tenantId));
        this.tenantId = tenantId;
        this.featureKey = featureKey;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getFeatureKey() {
        return featureKey;
    }
}
