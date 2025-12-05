package com.healthdata.cql.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating and updating CQL Libraries
 * Includes comprehensive validation constraints
 */
public class CqlLibraryRequest {

    @NotBlank(message = "Library name is required")
    @Size(min = 1, max = 255, message = "Library name must be between 1 and 255 characters")
    private String name;

    @NotBlank(message = "Version is required")
    @Size(min = 1, max = 32, message = "Version must be between 1 and 32 characters")
    @Pattern(regexp = "^\\d+\\.\\d+\\.\\d+$", message = "Version must follow semantic versioning (e.g., 1.0.0)")
    private String version;

    @NotBlank(message = "CQL content is required")
    private String cqlContent;

    @Size(max = 32, message = "Status must not exceed 32 characters")
    private String status;

    @Size(max = 4000, message = "Description must not exceed 4000 characters")
    private String description;

    @Size(max = 255, message = "Publisher must not exceed 255 characters")
    private String publisher;

    // Constructors
    public CqlLibraryRequest() {
    }

    public CqlLibraryRequest(String name, String version, String cqlContent) {
        this.name = name;
        this.version = version;
        this.cqlContent = cqlContent;
    }

    // Getters and Setters
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

    public String getCqlContent() {
        return cqlContent;
    }

    public void setCqlContent(String cqlContent) {
        this.cqlContent = cqlContent;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
}
