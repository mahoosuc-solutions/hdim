package com.healthdata.cms.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OAuth2 Error Response DTO
 * 
 * Represents error responses from CMS OAuth2 token endpoint.
 * According to RFC 6749 Section 5.2 (Error Response)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class OAuth2ErrorResponse {

    /**
     * REQUIRED. A single ASCII error code.
     * Valid values: invalid_request, invalid_client, invalid_grant, unauthorized_client,
     * unsupported_grant_type, invalid_scope
     */
    @JsonProperty("error")
    private String error;

    /**
     * OPTIONAL. Human-readable ASCII encoded text providing additional information.
     */
    @JsonProperty("error_description")
    private String errorDescription;

    /**
     * OPTIONAL. A URI identifying a human-readable web page with information about the error.
     */
    @JsonProperty("error_uri")
    private String errorUri;

    /**
     * Common OAuth2 error codes
     */
    public enum ErrorCode {
        INVALID_REQUEST("invalid_request"),
        INVALID_CLIENT("invalid_client"),
        INVALID_GRANT("invalid_grant"),
        UNAUTHORIZED_CLIENT("unauthorized_client"),
        UNSUPPORTED_GRANT_TYPE("unsupported_grant_type"),
        INVALID_SCOPE("invalid_scope");

        private final String code;

        ErrorCode(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        public static ErrorCode fromCode(String code) {
            for (ErrorCode e : ErrorCode.values()) {
                if (e.code.equals(code)) {
                    return e;
                }
            }
            return null;
        }
    }

    /**
     * Check if this is a client credential error (client secret expired, invalid, etc.)
     */
    public boolean isClientError() {
        return "invalid_client".equals(error) || "unauthorized_client".equals(error);
    }

    /**
     * Check if this is a grant error (grant expired, revoked, etc.)
     */
    public boolean isGrantError() {
        return "invalid_grant".equals(error);
    }

    /**
     * Check if this is a retriable error
     */
    public boolean isRetriable() {
        // Only retry on transient errors, not on client/grant errors
        return !isClientError() && !isGrantError();
    }

    /**
     * Get human-readable error message
     */
    public String getErrorMessage() {
        if (errorDescription != null && !errorDescription.isEmpty()) {
            return errorDescription;
        }
        return "OAuth2 error: " + error;
    }
}
