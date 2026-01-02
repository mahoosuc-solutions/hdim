package com.healthdata.ehr.model;

/**
 * Enumeration of supported EHR vendor types.
 * Each vendor may require different connection protocols and authentication mechanisms.
 */
public enum EhrVendorType {
    /**
     * Epic Systems - Supports FHIR R4 API with OAuth2 Backend Services
     */
    EPIC,

    /**
     * Cerner (Oracle Health) - Supports FHIR R4 API with OAuth2
     */
    CERNER,

    /**
     * athenahealth - Supports proprietary REST API and FHIR
     */
    ATHENA,

    /**
     * Generic FHIR-compliant EHR - Standard FHIR R4 API
     */
    GENERIC
}
