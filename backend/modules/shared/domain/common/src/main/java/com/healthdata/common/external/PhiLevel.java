package com.healthdata.common.external;

/**
 * Classification of PHI exposure level for external integrations.
 * Determines security controls applied at the adapter boundary.
 */
public enum PhiLevel {
    /** No PHI - all identifiers stripped, synthetic IDs only */
    NONE,
    /** De-identified data per HIPAA Safe Harbor */
    DE_IDENTIFIED,
    /** Limited PHI - aggregate + patient-level with RBAC */
    LIMITED,
    /** Full PHI - requires BAA, mTLS, HIPAA audit logging */
    FULL
}
