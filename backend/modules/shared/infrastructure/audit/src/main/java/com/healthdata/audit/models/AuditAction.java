package com.healthdata.audit.models;

/**
 * HIPAA-compliant audit actions.
 * Based on FHIR AuditEvent.action codes and HIPAA requirements.
 */
public enum AuditAction {
    // CRUD operations
    CREATE("C", "Create"),
    READ("R", "Read/View/Print/Query"),
    UPDATE("U", "Update"),
    DELETE("D", "Delete"),
    EXECUTE("E", "Execute"),

    // Authentication
    LOGIN("LOGIN", "User Login"),
    LOGOUT("LOGOUT", "User Logout"),
    LOGIN_FAILED("LOGIN_FAILED", "Failed Login Attempt"),

    // Authorization
    ACCESS_GRANTED("ACCESS_GRANTED", "Access Granted"),
    ACCESS_DENIED("ACCESS_DENIED", "Access Denied"),

    // Data operations
    SEARCH("SEARCH", "Search/Query"),
    EXPORT("EXPORT", "Data Export"),
    IMPORT("IMPORT", "Data Import"),
    PRINT("PRINT", "Print Document"),
    DOWNLOAD("DOWNLOAD", "Download"),

    // Administrative
    CONSENT_CHANGE("CONSENT_CHANGE", "Consent Modified"),
    EMERGENCY_ACCESS("EMERGENCY_ACCESS", "Emergency Override Access"),

    // System
    SYSTEM_START("SYSTEM_START", "System Startup"),
    SYSTEM_STOP("SYSTEM_STOP", "System Shutdown"),
    CONFIG_CHANGE("CONFIG_CHANGE", "Configuration Change");

    private final String code;
    private final String display;

    AuditAction(String code, String display) {
        this.code = code;
        this.display = display;
    }

    public String getCode() {
        return code;
    }

    public String getDisplay() {
        return display;
    }
}
