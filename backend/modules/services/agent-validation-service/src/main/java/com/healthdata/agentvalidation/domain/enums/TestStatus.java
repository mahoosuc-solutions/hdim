package com.healthdata.agentvalidation.domain.enums;

/**
 * Status of test suite or test case execution.
 */
public enum TestStatus {

    /**
     * Test is queued for execution.
     */
    PENDING,

    /**
     * Test is currently running.
     */
    RUNNING,

    /**
     * Test completed and passed all thresholds.
     */
    PASSED,

    /**
     * Test completed but failed one or more thresholds.
     */
    FAILED,

    /**
     * Test execution was cancelled.
     */
    CANCELLED,

    /**
     * Test encountered an error during execution.
     */
    ERROR,

    /**
     * Test was skipped (e.g., due to dependency failure).
     */
    SKIPPED,

    /**
     * Test is flagged for QA review.
     */
    FLAGGED_FOR_REVIEW,

    /**
     * Test was approved by QA reviewer.
     */
    QA_APPROVED,

    /**
     * Test was rejected by QA reviewer.
     */
    QA_REJECTED
}
