package com.healthdata.devops.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.healthdata.devops.client.FhirServiceClient;
import com.healthdata.devops.model.FhirValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FhirDataValidationService {
    private final FhirServiceClient fhirClient;
    
    private static final Map<String, Integer> MIN_RESOURCE_COUNTS = Map.of(
        "Patient", 50, "Condition", 50, "Observation", 200,
        "MedicationRequest", 50, "Encounter", 50, "Procedure", 20,
        "Immunization", 30, "AllergyIntolerance", 20
    );
    
    private static final List<CodeCheck> REQUIRED_CODES = List.of(
        new CodeCheck("Condition", "44054006", "Diabetes Mellitus Type 2 (SNOMED)"),
        new CodeCheck("Condition", "59621000", "Essential Hypertension (SNOMED)"),
        new CodeCheck("Observation", "4548-4", "HbA1c (LOINC)"),
        new CodeCheck("Observation", "8480-6", "Systolic BP (LOINC)"),
        new CodeCheck("Observation", "44249-1", "PHQ-9 Depression Screening (LOINC)")
    );
    
    public FhirDataValidationService(FhirServiceClient fhirClient) {
        this.fhirClient = fhirClient;
    }
    
    public FhirValidationResult validateDemoData() {
        log.info("Starting FHIR demo data validation...");
        String validationId = UUID.randomUUID().toString();
        Instant validationTimestamp = Instant.now();
        
        List<FhirValidationResult.ResourceCountCheck> resourceCountChecks = validateResourceCounts();
        List<FhirValidationResult.CodeSystemCheck> codeSystemChecks = validateCodeSystems();
        List<FhirValidationResult.AuthenticityCheck> authenticityChecks = validateDataAuthenticity();
        List<FhirValidationResult.ComplianceCheck> complianceChecks = validateFhirCompliance();
        List<FhirValidationResult.RelationshipCheck> relationshipChecks = validateResourceRelationships();
        
        int totalChecks = resourceCountChecks.size() + codeSystemChecks.size() + 
                         authenticityChecks.size() + complianceChecks.size() + relationshipChecks.size();
        int passedChecks = countPassed(resourceCountChecks, codeSystemChecks, 
                                       authenticityChecks, complianceChecks, relationshipChecks);
        int failedChecks = countFailed(resourceCountChecks, codeSystemChecks, 
                                       authenticityChecks, complianceChecks, relationshipChecks);
        int warningChecks = totalChecks - passedChecks - failedChecks;
        String overallStatus = failedChecks > 0 ? "FAIL" : (warningChecks > 0 ? "WARN" : "PASS");
        
        Map<String, Object> summary = generateSummary(resourceCountChecks, relationshipChecks);
        
        FhirValidationResult result = FhirValidationResult.builder()
            .validationId(validationId).validationTimestamp(validationTimestamp)
            .overallStatus(overallStatus).totalChecks(totalChecks)
            .passedChecks(passedChecks).failedChecks(failedChecks).warningChecks(warningChecks)
            .resourceCountChecks(resourceCountChecks).codeSystemChecks(codeSystemChecks)
            .authenticityChecks(authenticityChecks).complianceChecks(complianceChecks)
            .relationshipChecks(relationshipChecks).summary(summary).build();
        
        log.info("FHIR validation completed: {} passed, {} failed, {} warnings", 
            passedChecks, failedChecks, warningChecks);
        return result;
    }
    
    private List<FhirValidationResult.ResourceCountCheck> validateResourceCounts() {
        List<FhirValidationResult.ResourceCountCheck> checks = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : MIN_RESOURCE_COUNTS.entrySet()) {
            String resourceType = entry.getKey();
            Integer minimumRequired = entry.getValue();
            Integer actualCount = fhirClient.getResourceCount(resourceType);
            String status = actualCount >= minimumRequired ? "PASS" : 
                          (actualCount >= minimumRequired * 0.5 ? "WARN" : "FAIL");
            String message = String.format("Found %d %s resources (minimum: %d)", 
                actualCount, resourceType, minimumRequired);
            checks.add(FhirValidationResult.ResourceCountCheck.builder()
                .resourceType(resourceType).actualCount(actualCount)
                .minimumRequired(minimumRequired).status(status).message(message).build());
        }
        return checks;
    }
    
    private List<FhirValidationResult.CodeSystemCheck> validateCodeSystems() {
        List<FhirValidationResult.CodeSystemCheck> checks = new ArrayList<>();
        for (CodeCheck requiredCode : REQUIRED_CODES) {
            Integer count = fhirClient.getResourceCountByCode(requiredCode.resourceType, requiredCode.code);
            String status = count > 0 ? "PASS" : "FAIL";
            String message = count > 0 
                ? String.format("Found %d resources with code %s", count, requiredCode.code)
                : String.format("No resources found with code %s (%s)", 
                    requiredCode.code, requiredCode.description);
            checks.add(FhirValidationResult.CodeSystemCheck.builder()
                .resourceType(requiredCode.resourceType).code(requiredCode.code)
                .description(requiredCode.description).count(count).status(status).message(message).build());
        }
        return checks;
    }
    
    private List<FhirValidationResult.AuthenticityCheck> validateDataAuthenticity() {
        List<FhirValidationResult.AuthenticityCheck> checks = new ArrayList<>();
        JsonNode patientBundle = fhirClient.getResourceSamples("Patient", 100);
        if (patientBundle != null && patientBundle.has("entry")) {
            int totalPatients = patientBundle.get("entry").size();
            int patientsWithNames = 0;
            int patientsWithBirthDates = 0;
            for (JsonNode entry : patientBundle.get("entry")) {
                JsonNode resource = entry.get("resource");
                if (resource != null && resource.has("name") && resource.get("name").size() > 0) {
                    patientsWithNames++;
                }
                if (resource != null && resource.has("birthDate")) {
                    patientsWithBirthDates++;
                }
            }
            String status = patientsWithNames == totalPatients ? "PASS" : "WARN";
            checks.add(FhirValidationResult.AuthenticityCheck.builder()
                .checkName("Patient Name Completeness").description("All patients should have names")
                .status(status).message(String.format("%d/%d patients have names", 
                    patientsWithNames, totalPatients))
                .details(Map.of("patientsWithNames", patientsWithNames, "totalPatients", totalPatients))
                .build());
            status = patientsWithBirthDates == totalPatients ? "PASS" : "WARN";
            checks.add(FhirValidationResult.AuthenticityCheck.builder()
                .checkName("Patient Birth Date Completeness")
                .description("All patients should have birth dates").status(status)
                .message(String.format("%d/%d patients have birth dates", 
                    patientsWithBirthDates, totalPatients))
                .details(Map.of("patientsWithBirthDates", patientsWithBirthDates, 
                               "totalPatients", totalPatients)).build());
        }
        return checks;
    }
    
    private List<FhirValidationResult.ComplianceCheck> validateFhirCompliance() {
        List<FhirValidationResult.ComplianceCheck> checks = new ArrayList<>();
        JsonNode metadata = fhirClient.getMetadata();
        if (metadata != null && metadata.has("fhirVersion")) {
            String fhirVersion = metadata.get("fhirVersion").asText();
            String status = "4.0.1".equals(fhirVersion) ? "PASS" : "WARN";
            checks.add(FhirValidationResult.ComplianceCheck.builder()
                .checkName("FHIR Version").description("FHIR server should be R4 (4.0.1)")
                .status(status).message(String.format("FHIR version: %s", fhirVersion))
                .errors(status.equals("WARN") ? List.of("Expected FHIR R4 (4.0.1)") : List.of())
                .warnings(status.equals("WARN") ? List.of("Version mismatch") : List.of()).build());
        } else {
            checks.add(FhirValidationResult.ComplianceCheck.builder()
                .checkName("FHIR Metadata").description("FHIR server metadata endpoint")
                .status("FAIL").message("Cannot retrieve FHIR server metadata")
                .errors(List.of("Metadata endpoint not accessible")).warnings(List.of()).build());
        }
        return checks;
    }
    
    private List<FhirValidationResult.RelationshipCheck> validateResourceRelationships() {
        List<FhirValidationResult.RelationshipCheck> checks = new ArrayList<>();
        Integer patientCount = fhirClient.getResourceCount("Patient");
        Integer observationCount = fhirClient.getResourceCount("Observation");
        if (patientCount > 0) {
            double avgObservationsPerPatient = (double) observationCount / patientCount;
            String status = avgObservationsPerPatient >= 2.0 ? "PASS" : 
                          (avgObservationsPerPatient >= 1.0 ? "WARN" : "FAIL");
            checks.add(FhirValidationResult.RelationshipCheck.builder()
                .relationshipType("Patient-Observation").patientCount(patientCount)
                .relatedResourceCount(observationCount)
                .averageResourcesPerPatient(avgObservationsPerPatient).status(status)
                .message(String.format("Average %.2f observations per patient", avgObservationsPerPatient))
                .build());
        }
        return checks;
    }
    
    private Map<String, Object> generateSummary(
        List<FhirValidationResult.ResourceCountCheck> resourceCountChecks,
        List<FhirValidationResult.RelationshipCheck> relationshipChecks
    ) {
        Map<String, Object> summary = new HashMap<>();
        Map<String, Integer> resourceCounts = resourceCountChecks.stream()
            .collect(Collectors.toMap(
                FhirValidationResult.ResourceCountCheck::getResourceType,
                FhirValidationResult.ResourceCountCheck::getActualCount
            ));
        summary.put("resourceCounts", resourceCounts);
        int totalResources = resourceCounts.values().stream().mapToInt(Integer::intValue).sum();
        summary.put("totalResources", totalResources);
        return summary;
    }
    
    private int countPassed(List<?>... checkLists) {
        return Arrays.stream(checkLists).flatMap(List::stream)
            .mapToInt(c -> "PASS".equals(getStatus(c)) ? 1 : 0).sum();
    }
    
    private int countFailed(List<?>... checkLists) {
        return Arrays.stream(checkLists).flatMap(List::stream)
            .mapToInt(c -> "FAIL".equals(getStatus(c)) ? 1 : 0).sum();
    }
    
    private String getStatus(Object check) {
        try {
            return (String) check.getClass().getMethod("getStatus").invoke(check);
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
    
    private record CodeCheck(String resourceType, String code, String description) {}
}
