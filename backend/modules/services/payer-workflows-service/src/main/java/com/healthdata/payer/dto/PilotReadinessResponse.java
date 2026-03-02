package com.healthdata.payer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PilotReadinessResponse {

    @JsonProperty("id")
    private String id;

    @JsonProperty("customerId")
    private String customerId;

    @JsonProperty("customerName")
    private String customerName;

    @JsonProperty("ehrType")
    private String ehrType;

    @JsonProperty("integrationStatus")
    private String integrationStatus;

    @JsonProperty("fhirEndpointUrl")
    private String fhirEndpointUrl;

    @JsonProperty("dataIngestionStatus")
    private String dataIngestionStatus;

    @JsonProperty("demoDataSeeded")
    private Boolean demoDataSeeded;

    @JsonProperty("qualityMeasuresConfigured")
    private Boolean qualityMeasuresConfigured;

    @JsonProperty("userAccountsProvisioned")
    private Boolean userAccountsProvisioned;

    @JsonProperty("readinessScore")
    private Integer readinessScore;

    @JsonProperty("blockers")
    private String blockers;

    @JsonProperty("createdAt")
    private Instant createdAt;

    @JsonProperty("updatedAt")
    private Instant updatedAt;
}
