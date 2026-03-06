package com.healthdata.events.intelligence.controller;

public class ForbiddenIntelligenceOperationException extends RuntimeException {

    public ForbiddenIntelligenceOperationException(String message) {
        super(message);
    }
}
