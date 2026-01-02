package com.healthdata.enrichment.model;

/**
 * Types of medical entities that can be extracted.
 */
public enum EntityType {
    DIAGNOSIS,
    MEDICATION,
    PROCEDURE,
    LAB_RESULT,
    VITAL_SIGN,
    ALLERGY,
    FAMILY_HISTORY,
    SYMPTOM,
    ANATOMICAL_LOCATION,
    TEMPORAL_EXPRESSION
}
