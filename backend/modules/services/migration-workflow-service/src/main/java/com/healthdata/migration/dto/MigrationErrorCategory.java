package com.healthdata.migration.dto;

/**
 * Categories of migration errors for quality reporting
 */
public enum MigrationErrorCategory {
    PARSE_ERROR,          // Invalid HL7/CDA syntax
    VALIDATION_ERROR,     // Failed FHIR validation
    MAPPING_ERROR,        // Unable to map to FHIR
    DUPLICATE_RECORD,     // Already exists in target
    MISSING_REQUIRED,     // Required field missing
    INVALID_CODE,         // Invalid terminology code
    CONNECTIVITY_ERROR,   // Source connection issue
    SYSTEM_ERROR          // Internal processing error
}
