package com.healthdata.payer.service;

public class NonRetryableClearinghouseException extends RuntimeException {
    public NonRetryableClearinghouseException(String message) {
        super(message);
    }
}
