package com.healthdata.gateway.clinical.compliance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorSyncResponse {
    private int synced;
    private String timestamp;
    private String message;
}
