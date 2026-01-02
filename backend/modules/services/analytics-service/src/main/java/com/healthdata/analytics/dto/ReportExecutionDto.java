package com.healthdata.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportExecutionDto {

    private UUID id;

    private UUID reportId;

    private String status; // PENDING, RUNNING, COMPLETED, FAILED, CANCELLED

    private Map<String, Object> parameters;

    private Map<String, Object> resultData;

    private String resultFilePath;

    private Long resultFileSize;

    private Integer rowCount;

    private String triggeredBy;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private String errorMessage;

    private Long durationMs;

    public Long getDurationMs() {
        if (startedAt != null && completedAt != null) {
            return java.time.Duration.between(startedAt, completedAt).toMillis();
        }
        return durationMs;
    }
}
