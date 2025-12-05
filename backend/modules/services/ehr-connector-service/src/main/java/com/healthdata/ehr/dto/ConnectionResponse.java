package com.healthdata.ehr.dto;

import com.healthdata.ehr.model.EhrConnectionStatus;
import com.healthdata.ehr.model.EhrVendorType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for EHR connection information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionResponse {

    private String connectionId;
    private String tenantId;
    private EhrVendorType vendorType;
    private String baseUrl;
    private EhrConnectionStatus.Status status;
    private LocalDateTime lastSuccessfulConnection;
    private LocalDateTime createdAt;
}
