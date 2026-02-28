package com.healthdata.payer.service;

public interface ClearinghouseBackoffExecutor {
    void backoff(long millis);
}
