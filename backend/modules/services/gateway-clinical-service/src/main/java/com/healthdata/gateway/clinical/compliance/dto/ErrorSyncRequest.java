package com.healthdata.gateway.clinical.compliance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorSyncRequest {
    private List<ComplianceErrorDto> errors;
    @JsonProperty("syncedAt")
    private String syncedAt;
}
