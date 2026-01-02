package com.healthdata.cql.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating and updating Value Sets
 * Includes comprehensive validation constraints
 */
public class ValueSetRequest {

    @NotBlank(message = "OID is required")
    @Size(min = 1, max = 255, message = "OID must be between 1 and 255 characters")
    private String oid;

    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 512, message = "Name must be between 1 and 512 characters")
    private String name;

    @Size(max = 32, message = "Version must not exceed 32 characters")
    private String version;

    @NotBlank(message = "Code system is required")
    @Size(max = 128, message = "Code system must not exceed 128 characters")
    private String codeSystem;

    private String codes; // JSON array of codes

    @Size(max = 4000, message = "Description must not exceed 4000 characters")
    private String description;

    @Size(max = 255, message = "Publisher must not exceed 255 characters")
    private String publisher;

    @Size(max = 32, message = "Status must not exceed 32 characters")
    private String status;

    // Constructors
    public ValueSetRequest() {
    }

    public ValueSetRequest(String oid, String name, String codeSystem) {
        this.oid = oid;
        this.name = name;
        this.codeSystem = codeSystem;
    }

    // Getters and Setters
    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getCodeSystem() {
        return codeSystem;
    }

    public void setCodeSystem(String codeSystem) {
        this.codeSystem = codeSystem;
    }

    public String getCodes() {
        return codes;
    }

    public void setCodes(String codes) {
        this.codes = codes;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
