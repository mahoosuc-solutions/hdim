package com.healthdata.demo.api.v1.dto;

/**
 * Response DTO for patient generation.
 */
public class GeneratePatientsResponse {
    private String tenantId;
    private int patientCount;
    private int careGapCount;
    private int medicationCount;
    private int observationCount;
    private int encounterCount;
    private int procedureCount;
    private long generationTimeMs;
    private boolean success;
    private String errorMessage;

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public int getPatientCount() { return patientCount; }
    public void setPatientCount(int patientCount) { this.patientCount = patientCount; }

    public int getCareGapCount() { return careGapCount; }
    public void setCareGapCount(int careGapCount) { this.careGapCount = careGapCount; }

    public int getMedicationCount() { return medicationCount; }
    public void setMedicationCount(int medicationCount) { this.medicationCount = medicationCount; }

    public int getObservationCount() { return observationCount; }
    public void setObservationCount(int observationCount) { this.observationCount = observationCount; }

    public int getEncounterCount() { return encounterCount; }
    public void setEncounterCount(int encounterCount) { this.encounterCount = encounterCount; }

    public int getProcedureCount() { return procedureCount; }
    public void setProcedureCount(int procedureCount) { this.procedureCount = procedureCount; }

    public long getGenerationTimeMs() { return generationTimeMs; }
    public void setGenerationTimeMs(long generationTimeMs) { this.generationTimeMs = generationTimeMs; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
