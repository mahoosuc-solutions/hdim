package com.healthdata.fhir.models;

import org.hl7.fhir.r4.model.Resource;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;

/**
 * Wrapper class for FHIR resources with audit metadata.
 * Provides additional context for tracking and auditing FHIR resource operations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FhirResourceWrapper<T extends Resource> {

    /**
     * The FHIR resource being wrapped
     */
    private T resource;

    /**
     * Timestamp when this resource was created in our system
     */
    private Instant createdAt;

    /**
     * Timestamp when this resource was last modified in our system
     */
    private Instant lastModified;

    /**
     * User ID who created this resource
     */
    private String createdBy;

    /**
     * User ID who last modified this resource
     */
    private String modifiedBy;

    /**
     * Organization/tenant ID for multi-tenancy support
     */
    private String organizationId;

    /**
     * Indicates if this resource contains PHI (Protected Health Information)
     */
    private boolean containsPHI;

    /**
     * Version number for optimistic locking
     */
    private Long version;

    /**
     * Creates a new wrapper with the current timestamp
     */
    public static <T extends Resource> FhirResourceWrapper<T> wrap(T resource, String userId, String organizationId) {
        FhirResourceWrapper<T> wrapper = new FhirResourceWrapper<>();
        wrapper.setResource(resource);
        wrapper.setCreatedAt(Instant.now());
        wrapper.setLastModified(Instant.now());
        wrapper.setCreatedBy(userId);
        wrapper.setModifiedBy(userId);
        wrapper.setOrganizationId(organizationId);
        wrapper.setContainsPHI(true); // Default to true for safety
        wrapper.setVersion(1L);
        return wrapper;
    }

    /**
     * Updates modification metadata
     */
    public void markModified(String userId) {
        this.lastModified = Instant.now();
        this.modifiedBy = userId;
        this.version = this.version != null ? this.version + 1 : 1L;
    }

    /**
     * Gets the FHIR resource type
     */
    public String getResourceType() {
        return resource != null ? resource.getResourceType().name() : null;
    }

    /**
     * Gets the FHIR resource ID
     */
    public String getResourceId() {
        return resource != null ? resource.getId() : null;
    }
}
