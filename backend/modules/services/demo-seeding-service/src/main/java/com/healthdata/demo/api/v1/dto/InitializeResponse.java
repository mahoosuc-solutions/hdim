package com.healthdata.demo.api.v1.dto;

/**
 * Response DTO for demo initialization.
 */
public class InitializeResponse {
    private boolean success;
    private String message;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
