package com.healthdata.migration.dto;

/**
 * Types of healthcare data being migrated
 */
public enum DataType {
    HL7V2,       // HL7 v2.x messages (ADT, ORU, ORM, RDE, RAS, VXU)
    CDA,         // CDA/C-CDA documents
    FHIR_BUNDLE  // FHIR R4 Bundle resources
}
