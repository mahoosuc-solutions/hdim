package com.healthdata.cdr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for batch processing HL7 messages.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchProcessResponse {

    /**
     * Total number of messages submitted.
     */
    private int totalMessages;

    /**
     * Number of successfully processed messages.
     */
    private int successCount;

    /**
     * Number of failed messages.
     */
    private int failureCount;

    /**
     * List of processed messages.
     */
    private List<Hl7v2Message> processedMessages;

    /**
     * Processing time in milliseconds.
     */
    private long processingTimeMs;
}
