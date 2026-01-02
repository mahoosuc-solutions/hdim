package com.healthdata.fhir.security.smart;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * SMART on FHIR Scopes.
 *
 * Implements scope patterns from SMART App Launch Framework:
 * - patient/[resource].read - Read access to resources in patient context
 * - patient/[resource].write - Write access to resources in patient context
 * - user/[resource].read - Read access to resources user has access to
 * - user/[resource].write - Write access to resources user has access to
 * - system/[resource].read - System-level read access (backend services)
 * - system/[resource].write - System-level write access (backend services)
 * - launch - Permission to obtain launch context
 * - launch/patient - Permission to obtain patient launch context
 * - launch/encounter - Permission to obtain encounter launch context
 * - openid - OpenID Connect identity token
 * - fhirUser - Access to FHIR resource for the current user
 * - profile - Access to user profile claims
 * - offline_access - Request refresh token for offline access
 */
@Getter
@RequiredArgsConstructor
public enum SmartScope {

    // Launch scopes
    LAUNCH("launch", "Permission to obtain launch context", ScopeCategory.LAUNCH),
    LAUNCH_PATIENT("launch/patient", "Permission to request patient context", ScopeCategory.LAUNCH),
    LAUNCH_ENCOUNTER("launch/encounter", "Permission to request encounter context", ScopeCategory.LAUNCH),

    // Identity scopes
    OPENID("openid", "OpenID Connect identity token", ScopeCategory.IDENTITY),
    FHIR_USER("fhirUser", "Access to FHIR resource for the current user", ScopeCategory.IDENTITY),
    PROFILE("profile", "Access to user profile claims", ScopeCategory.IDENTITY),
    OFFLINE_ACCESS("offline_access", "Request refresh token for offline access", ScopeCategory.IDENTITY),

    // Patient context - Read
    PATIENT_PATIENT_READ("patient/Patient.read", "Read Patient resources in patient context", ScopeCategory.PATIENT),
    PATIENT_OBSERVATION_READ("patient/Observation.read", "Read Observation resources in patient context", ScopeCategory.PATIENT),
    PATIENT_CONDITION_READ("patient/Condition.read", "Read Condition resources in patient context", ScopeCategory.PATIENT),
    PATIENT_ENCOUNTER_READ("patient/Encounter.read", "Read Encounter resources in patient context", ScopeCategory.PATIENT),
    PATIENT_MEDICATION_REQUEST_READ("patient/MedicationRequest.read", "Read MedicationRequest resources in patient context", ScopeCategory.PATIENT),
    PATIENT_MEDICATION_ADMINISTRATION_READ("patient/MedicationAdministration.read", "Read MedicationAdministration resources in patient context", ScopeCategory.PATIENT),
    PATIENT_PROCEDURE_READ("patient/Procedure.read", "Read Procedure resources in patient context", ScopeCategory.PATIENT),
    PATIENT_ALLERGY_INTOLERANCE_READ("patient/AllergyIntolerance.read", "Read AllergyIntolerance resources in patient context", ScopeCategory.PATIENT),
    PATIENT_IMMUNIZATION_READ("patient/Immunization.read", "Read Immunization resources in patient context", ScopeCategory.PATIENT),
    PATIENT_DIAGNOSTIC_REPORT_READ("patient/DiagnosticReport.read", "Read DiagnosticReport resources in patient context", ScopeCategory.PATIENT),
    PATIENT_DOCUMENT_REFERENCE_READ("patient/DocumentReference.read", "Read DocumentReference resources in patient context", ScopeCategory.PATIENT),
    PATIENT_CARE_PLAN_READ("patient/CarePlan.read", "Read CarePlan resources in patient context", ScopeCategory.PATIENT),
    PATIENT_GOAL_READ("patient/Goal.read", "Read Goal resources in patient context", ScopeCategory.PATIENT),
    PATIENT_COVERAGE_READ("patient/Coverage.read", "Read Coverage resources in patient context", ScopeCategory.PATIENT),
    PATIENT_ALL_READ("patient/*.read", "Read all resources in patient context", ScopeCategory.PATIENT),

    // Patient context - Write
    PATIENT_PATIENT_WRITE("patient/Patient.write", "Write Patient resources in patient context", ScopeCategory.PATIENT),
    PATIENT_OBSERVATION_WRITE("patient/Observation.write", "Write Observation resources in patient context", ScopeCategory.PATIENT),
    PATIENT_CONDITION_WRITE("patient/Condition.write", "Write Condition resources in patient context", ScopeCategory.PATIENT),
    PATIENT_ALL_WRITE("patient/*.write", "Write all resources in patient context", ScopeCategory.PATIENT),

    // User context - Read
    USER_PATIENT_READ("user/Patient.read", "Read Patient resources user has access to", ScopeCategory.USER),
    USER_OBSERVATION_READ("user/Observation.read", "Read Observation resources user has access to", ScopeCategory.USER),
    USER_CONDITION_READ("user/Condition.read", "Read Condition resources user has access to", ScopeCategory.USER),
    USER_ENCOUNTER_READ("user/Encounter.read", "Read Encounter resources user has access to", ScopeCategory.USER),
    USER_PRACTITIONER_READ("user/Practitioner.read", "Read Practitioner resources user has access to", ScopeCategory.USER),
    USER_ALL_READ("user/*.read", "Read all resources user has access to", ScopeCategory.USER),

    // User context - Write
    USER_PATIENT_WRITE("user/Patient.write", "Write Patient resources user has access to", ScopeCategory.USER),
    USER_OBSERVATION_WRITE("user/Observation.write", "Write Observation resources user has access to", ScopeCategory.USER),
    USER_ALL_WRITE("user/*.write", "Write all resources user has access to", ScopeCategory.USER),

    // System context - Read (backend services)
    SYSTEM_PATIENT_READ("system/Patient.read", "System-level read access to Patient", ScopeCategory.SYSTEM),
    SYSTEM_OBSERVATION_READ("system/Observation.read", "System-level read access to Observation", ScopeCategory.SYSTEM),
    SYSTEM_CONDITION_READ("system/Condition.read", "System-level read access to Condition", ScopeCategory.SYSTEM),
    SYSTEM_ENCOUNTER_READ("system/Encounter.read", "System-level read access to Encounter", ScopeCategory.SYSTEM),
    SYSTEM_ALL_READ("system/*.read", "System-level read access to all resources", ScopeCategory.SYSTEM),

    // System context - Write (backend services)
    SYSTEM_PATIENT_WRITE("system/Patient.write", "System-level write access to Patient", ScopeCategory.SYSTEM),
    SYSTEM_ALL_WRITE("system/*.write", "System-level write access to all resources", ScopeCategory.SYSTEM),

    // Bulk Data Access
    SYSTEM_EXPORT("system/$export", "System-level bulk data export", ScopeCategory.SYSTEM);

    private final String scope;
    private final String description;
    private final ScopeCategory category;

    /**
     * Scope categories for grouping and validation.
     */
    public enum ScopeCategory {
        LAUNCH,
        IDENTITY,
        PATIENT,
        USER,
        SYSTEM
    }

    /**
     * Get all supported scope strings.
     */
    public static List<String> getAllScopeStrings() {
        return Arrays.stream(values())
            .map(SmartScope::getScope)
            .collect(Collectors.toList());
    }

    /**
     * Get scopes by category.
     */
    public static List<SmartScope> getScopesByCategory(ScopeCategory category) {
        return Arrays.stream(values())
            .filter(s -> s.getCategory() == category)
            .collect(Collectors.toList());
    }

    /**
     * Parse scope string to SmartScope.
     */
    public static SmartScope fromString(String scopeString) {
        return Arrays.stream(values())
            .filter(s -> s.getScope().equals(scopeString))
            .findFirst()
            .orElse(null);
    }

    /**
     * Validate a set of scope strings.
     */
    public static boolean validateScopes(Set<String> scopes) {
        Set<String> validScopes = Arrays.stream(values())
            .map(SmartScope::getScope)
            .collect(Collectors.toSet());
        return validScopes.containsAll(scopes);
    }

    /**
     * Check if scope grants read access to a resource type.
     */
    public static boolean grantsReadAccess(Set<String> scopes, String resourceType, ScopeCategory context) {
        String specificScope = context.name().toLowerCase() + "/" + resourceType + ".read";
        String wildcardScope = context.name().toLowerCase() + "/*.read";
        return scopes.contains(specificScope) || scopes.contains(wildcardScope);
    }

    /**
     * Check if scope grants write access to a resource type.
     */
    public static boolean grantsWriteAccess(Set<String> scopes, String resourceType, ScopeCategory context) {
        String specificScope = context.name().toLowerCase() + "/" + resourceType + ".write";
        String wildcardScope = context.name().toLowerCase() + "/*.write";
        return scopes.contains(specificScope) || scopes.contains(wildcardScope);
    }
}
