package com.healthdata.quality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for bulk signing operation.
 * Indicates how many results were signed and which abnormal results need acknowledgment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response from bulk signing operation")
public class BulkSignResponse {

    @Schema(description = "Number of results successfully signed")
    private int signed;

    @Schema(description = "Number of abnormal results requiring individual acknowledgment")
    private int requiresAcknowledgment;

    @Schema(description = "Number of results that failed to sign")
    private int failed;

    @Schema(description = "Timestamp when signing was completed")
    private LocalDateTime signedAt;

    @Schema(description = "ID of the provider who signed")
    private String signedBy;

    @Schema(description = "List of abnormal results pending acknowledgment")
    private List<PendingAbnormalResult> pendingAbnormal;

    @Schema(description = "List of result IDs that failed to sign with reasons")
    private List<FailedResult> failedResults;

    /**
     * Abnormal result requiring individual acknowledgment
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Abnormal result requiring provider review")
    public static class PendingAbnormalResult {
        @Schema(description = "Result ID")
        private UUID resultId;

        @Schema(description = "Patient name")
        private String patientName;

        @Schema(description = "Type of result (e.g., HbA1c, BP)")
        private String resultType;

        @Schema(description = "Result value")
        private String value;

        @Schema(description = "Reason result is flagged as abnormal")
        private String abnormalReason;

        @Schema(description = "Measure name")
        private String measureName;
    }

    /**
     * Result that failed to sign
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Result that failed to sign")
    public static class FailedResult {
        @Schema(description = "Result ID")
        private UUID resultId;

        @Schema(description = "Reason for failure")
        private String reason;
    }
}
