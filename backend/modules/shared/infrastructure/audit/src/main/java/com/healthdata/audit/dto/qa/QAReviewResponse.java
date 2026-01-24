package com.healthdata.audit.dto.qa;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * QA review response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QAReviewResponse {
    private String eventId;
    private String status; // approved, rejected, flagged, false-positive, false-negative
    private String reviewedBy;
    private Instant reviewedAt;
    private String message;
}
