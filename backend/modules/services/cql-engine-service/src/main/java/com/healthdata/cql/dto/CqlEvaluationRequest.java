package com.healthdata.cql.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request DTO for creating CQL Evaluations
 * Includes comprehensive validation constraints
 */
public class CqlEvaluationRequest {

    @NotNull(message = "Library ID is required")
    private UUID libraryId;

    @NotBlank(message = "Patient ID is required")
    private String patientId;

    // Optional fields - no validation required
    private String contextData;

    private String parameters;

    // Constructors
    public CqlEvaluationRequest() {
    }

    public CqlEvaluationRequest(UUID libraryId, String patientId) {
        this.libraryId = libraryId;
        this.patientId = patientId;
    }

    // Getters and Setters
    public UUID getLibraryId() {
        return libraryId;
    }

    public void setLibraryId(UUID libraryId) {
        this.libraryId = libraryId;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getContextData() {
        return contextData;
    }

    public void setContextData(String contextData) {
        this.contextData = contextData;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }
}
