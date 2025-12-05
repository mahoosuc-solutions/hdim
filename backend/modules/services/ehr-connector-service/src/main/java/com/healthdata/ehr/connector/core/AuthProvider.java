package com.healthdata.ehr.connector.core;

/**
 * Interface for EHR system authentication providers.
 */
public interface AuthProvider {

    /**
     * Obtain an access token for API calls.
     *
     * @return valid access token
     */
    String getAccessToken();

    /**
     * Refresh the current access token.
     *
     * @return new access token
     */
    String refreshToken();

    /**
     * Check if the current token is valid.
     *
     * @return true if token is valid and not expired
     */
    boolean isTokenValid();

    /**
     * Invalidate the current token.
     */
    void invalidateToken();
}
