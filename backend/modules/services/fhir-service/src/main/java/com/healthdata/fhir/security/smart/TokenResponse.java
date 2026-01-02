package com.healthdata.fhir.security.smart;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OAuth 2.0 / SMART on FHIR Token Response.
 *
 * Contains the access token and optional refresh token,
 * along with SMART-specific context parameters.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenResponse {

    /**
     * The access token issued by the authorization server.
     */
    @JsonProperty("access_token")
    private String accessToken;

    /**
     * The type of token issued (always "Bearer" for SMART).
     */
    @JsonProperty("token_type")
    private String tokenType;

    /**
     * The lifetime in seconds of the access token.
     */
    @JsonProperty("expires_in")
    private Integer expiresIn;

    /**
     * The refresh token, if offline_access scope was granted.
     */
    @JsonProperty("refresh_token")
    private String refreshToken;

    /**
     * The scopes that were actually granted.
     */
    @JsonProperty("scope")
    private String scope;

    /**
     * Patient ID in context (SMART launch context).
     */
    @JsonProperty("patient")
    private String patient;

    /**
     * Encounter ID in context (SMART launch context).
     */
    @JsonProperty("encounter")
    private String encounter;

    /**
     * FHIR User resource reference (SMART identity).
     */
    @JsonProperty("fhirUser")
    private String fhirUser;

    /**
     * ID Token for OpenID Connect (if openid scope granted).
     */
    @JsonProperty("id_token")
    private String idToken;

    /**
     * Whether to show patient banner in EHR-launched apps.
     */
    @JsonProperty("need_patient_banner")
    private Boolean needPatientBanner;

    /**
     * URL to SMART styling for visual integration.
     */
    @JsonProperty("smart_style_url")
    private String smartStyleUrl;

    /**
     * Tenant identifier for multi-tenant support.
     */
    @JsonProperty("tenant")
    private String tenant;
}
