package com.healthdata.payer.service;

public record ClearinghouseSubmissionResult(
        boolean accepted,
        String externalTrackingId
) {
}
