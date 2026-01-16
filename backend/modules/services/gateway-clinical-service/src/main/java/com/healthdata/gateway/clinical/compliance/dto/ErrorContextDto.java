package com.healthdata.gateway.clinical.compliance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorContextDto {
    private String service;
    private String endpoint;
    private String operation;
    private String errorCode;
    private String severity;
    private String userId;
    private String tenantId;
    @JsonProperty("additionalData")
    private Map<String, Object> additionalData;
}
