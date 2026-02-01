package com.healthdata.audit.dto.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Batch Review Error
 * 
 * Error information for a failed batch review operation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchReviewError {
    private String decisionId;
    private String errorMessage;
    private String errorCode;
}
