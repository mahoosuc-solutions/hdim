package com.healthdata.gateway.clinical.compliance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceErrorDto {
    private String id;
    private String timestamp;
    @JsonProperty("context")
    private ErrorContextDto context;
    private String message;
    private String stack;
}
