package com.healthdata.payer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PilotOnboardRequest {

    @NotBlank(message = "Customer ID is required")
    @JsonProperty("customerId")
    private String customerId;

    @NotBlank(message = "Customer name is required")
    @JsonProperty("customerName")
    private String customerName;

    @NotNull(message = "EHR type is required")
    @JsonProperty("ehrType")
    private String ehrType; // EPIC, CERNER, ALLSCRIPTS, ATHENA, MEDITECH, OTHER

    @JsonProperty("fhirEndpointUrl")
    private String fhirEndpointUrl;
}
