package com.healthdata.priorauth.dto;

import com.healthdata.priorauth.persistence.PriorAuthRequestEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for Prior Authorization request submission.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriorAuthRequestDTO {

    @NotNull(message = "Patient ID is required")
    private UUID patientId;

    @NotBlank(message = "Service code is required")
    private String serviceCode;

    private String serviceDescription;

    @NotNull(message = "Urgency is required")
    private PriorAuthRequestEntity.Urgency urgency;

    @NotBlank(message = "Payer ID is required")
    private String payerId;

    private String providerId;
    private String providerNpi;
    private String facilityId;

    private List<String> diagnosisCodes;
    private List<String> procedureCodes;

    private Integer quantityRequested;

    private Map<String, Object> supportingInfo;

    /**
     * Response DTO with full PA request details.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private UUID id;
        private String paRequestId;
        private UUID patientId;
        private String serviceCode;
        private String serviceDescription;
        private PriorAuthRequestEntity.Urgency urgency;
        private PriorAuthRequestEntity.Status status;
        private String payerId;
        private String payerName;
        private String providerId;
        private String providerNpi;
        private String facilityId;
        private List<String> diagnosisCodes;
        private List<String> procedureCodes;
        private Integer quantityRequested;
        private Integer quantityApproved;
        private LocalDateTime submittedAt;
        private LocalDateTime slaDeadline;
        private LocalDateTime decisionAt;
        private String decisionReason;
        private String authNumber;
        private LocalDateTime authEffectiveDate;
        private LocalDateTime authExpirationDate;
        private boolean slaBreached;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    /**
     * Status update DTO.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusUpdate {
        private PriorAuthRequestEntity.Status status;
        private String decisionReason;
        private String authNumber;
        private Integer quantityApproved;
        private LocalDateTime authEffectiveDate;
        private LocalDateTime authExpirationDate;
    }

    /**
     * Summary statistics DTO.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Statistics {
        private long totalRequests;
        private long pendingRequests;
        private long approvedRequests;
        private long deniedRequests;
        private long expiredRequests;
        private double approvalRate;
        private double averageProcessingTimeHours;
        private long slaBreaches;
    }
}
