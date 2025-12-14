package com.healthdata.fhir.security.smart;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SMART on FHIR Launch Context.
 *
 * Contains context parameters returned in the token response
 * when a SMART app is launched with context.
 *
 * Supports:
 * - EHR launch: App launched from within EHR with existing context
 * - Standalone launch: App launched independently, context requested via scopes
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SmartLaunchContext {

    /**
     * The patient ID in context.
     * Present when launch/patient scope is granted.
     */
    @JsonProperty("patient")
    private String patient;

    /**
     * The encounter ID in context.
     * Present when launch/encounter scope is granted.
     */
    @JsonProperty("encounter")
    private String encounter;

    /**
     * The practitioner (current user) FHIR resource ID.
     * Present when fhirUser scope is granted.
     */
    @JsonProperty("fhirUser")
    private String fhirUser;

    /**
     * Token intended audience (the FHIR server URL).
     */
    @JsonProperty("aud")
    private String audience;

    /**
     * The client ID that was authorized.
     */
    @JsonProperty("client_id")
    private String clientId;

    /**
     * Tenant identifier for multi-tenant deployments.
     */
    @JsonProperty("tenant")
    private String tenant;

    /**
     * The launch parameter passed during EHR launch.
     */
    @JsonProperty("launch")
    private String launch;

    /**
     * Indicates if this is a standalone launch.
     */
    @JsonProperty("standalone")
    private Boolean standalone;

    /**
     * Smart styling preferences from the EHR.
     */
    @JsonProperty("smart_style_url")
    private String smartStyleUrl;

    /**
     * Additional context parameters.
     */
    @JsonProperty("need_patient_banner")
    private Boolean needPatientBanner;

    /**
     * Intent parameter for app workflow.
     */
    @JsonProperty("intent")
    private String intent;

    /**
     * Check if patient context is available.
     */
    public boolean hasPatientContext() {
        return patient != null && !patient.isEmpty();
    }

    /**
     * Check if encounter context is available.
     */
    public boolean hasEncounterContext() {
        return encounter != null && !encounter.isEmpty();
    }

    /**
     * Check if fhirUser context is available.
     */
    public boolean hasFhirUserContext() {
        return fhirUser != null && !fhirUser.isEmpty();
    }
}
