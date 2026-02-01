package com.healthdata.eventsourcing.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Base class for events that represent FHIR resource operations.
 *
 * FHIR resource events extend domain events with capabilities specifically for
 * FHIR resources (Patient, Observation, Condition, etc.). They support indexed
 * fields for efficient projection queries.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public abstract class FhirResourceEvent extends AbstractDomainEvent {

    /**
     * FHIR resource type (Patient, Observation, Condition, CarePlan, etc.)
     */
    protected String resourceType;

    /**
     * Indexed fields for efficient projection queries
     * Map of field name -> field value for database indexing
     */
    protected Map<String, String> indexedFields = new HashMap<>();

    /**
     * Initialize FHIR resource event with defaults
     */
    public FhirResourceEvent(String eventType, String tenantId, String resourceType) {
        super(eventType, tenantId);
        this.resourceType = resourceType;
        this.indexedFields = new HashMap<>();
    }

    /**
     * Add an indexed field for projection queries
     *
     * @param fieldName the name of the field
     * @param fieldValue the value of the field
     */
    public void addIndexedField(String fieldName, String fieldValue) {
        this.indexedFields.put(fieldName, fieldValue);
    }

    @Override
    public String getResourceType() {
        return resourceType;
    }

    /**
     * Get the value of an indexed field
     *
     * @param fieldName the name of the field
     * @return the field value, or null if not indexed
     */
    public String getIndexedField(String fieldName) {
        return indexedFields.get(fieldName);
    }
}
