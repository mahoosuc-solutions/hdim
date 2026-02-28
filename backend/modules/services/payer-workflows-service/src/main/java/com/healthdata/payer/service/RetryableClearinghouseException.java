package com.healthdata.payer.service;

public class RetryableClearinghouseException extends RuntimeException {
    public RetryableClearinghouseException(String message) {
        super(message);
    }
}
