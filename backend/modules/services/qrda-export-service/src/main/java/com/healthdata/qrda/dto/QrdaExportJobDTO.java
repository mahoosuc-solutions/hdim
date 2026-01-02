package com.healthdata.qrda.dto;

import com.healthdata.qrda.persistence.QrdaExportJobEntity.QrdaJobStatus;
import com.healthdata.qrda.persistence.QrdaExportJobEntity.QrdaJobType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO representing a QRDA export job.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QrdaExportJobDTO {

    private UUID id;
    private String tenantId;
    private QrdaJobType jobType;
    private QrdaJobStatus status;
    private List<String> measureIds;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String documentLocation;
    private Integer documentCount;
    private Integer patientCount;
    private String errorMessage;
    private List<String> validationErrors;
    private String requestedBy;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    /**
     * Progress percentage (0-100) for running jobs.
     */
    private Integer progressPercent;

    /**
     * Estimated time remaining in seconds for running jobs.
     */
    private Long estimatedSecondsRemaining;

    /**
     * Download URL for completed jobs (presigned, time-limited).
     */
    private String downloadUrl;
}
