package com.healthdata.audit.dto.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Batch Review Result
 * 
 * Result of a batch review operation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchReviewResult {
    private int totalRequested;
    private int successful;
    private int failed;
    private List<String> successfulIds;
    private List<BatchReviewError> errors;
}
