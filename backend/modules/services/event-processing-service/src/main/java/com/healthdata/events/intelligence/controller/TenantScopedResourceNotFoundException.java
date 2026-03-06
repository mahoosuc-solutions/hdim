package com.healthdata.events.intelligence.controller;

public class TenantScopedResourceNotFoundException extends RuntimeException {

    public TenantScopedResourceNotFoundException(String message) {
        super(message);
    }
}
