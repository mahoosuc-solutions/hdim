package com.healthdata.demo.api.v1.dto;

/**
 * Response DTO for reset operations.
 */
public class ResetResponse {
    private boolean success;
    private int patientsDeleted;
    private int conditionsDeleted;
    private int careGapsDeleted;
    private long resetTimeMs;
    private String errorMessage;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public int getPatientsDeleted() { return patientsDeleted; }
    public void setPatientsDeleted(int patientsDeleted) { this.patientsDeleted = patientsDeleted; }

    public int getConditionsDeleted() { return conditionsDeleted; }
    public void setConditionsDeleted(int conditionsDeleted) { this.conditionsDeleted = conditionsDeleted; }

    public int getCareGapsDeleted() { return careGapsDeleted; }
    public void setCareGapsDeleted(int careGapsDeleted) { this.careGapsDeleted = careGapsDeleted; }

    public long getResetTimeMs() { return resetTimeMs; }
    public void setResetTimeMs(long resetTimeMs) { this.resetTimeMs = resetTimeMs; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
