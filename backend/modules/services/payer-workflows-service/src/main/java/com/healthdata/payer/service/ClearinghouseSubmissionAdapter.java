package com.healthdata.payer.service;

import com.healthdata.payer.revenue.dto.ClaimSubmissionRequest;

public interface ClearinghouseSubmissionAdapter {
    ClearinghouseSubmissionResult submit(ClaimSubmissionRequest request, int attempt)
            throws RetryableClearinghouseException, NonRetryableClearinghouseException;
}
