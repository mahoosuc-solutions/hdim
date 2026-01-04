package com.healthdata.demo.client;

import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;

/**
 * Client for creating care gaps in the Care Gap service.
 *
 * Analyzes patient data from FHIR bundles and creates appropriate care gaps
 * based on missing preventive screenings and chronic disease management gaps.
 */
@Component
public class CareGapServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(CareGapServiceClient.class);

    private final RestTemplate restTemplate;
    private final String careGapServiceUrl;

    // HEDIS Measure Codes
    private static final String BCS = "BCS"; // Breast Cancer Screening
    private static final String COL = "COL"; // Colorectal Cancer Screening
    private static final String CBP = "CBP"; // Controlling High Blood Pressure
    private static final String CDC = "CDC"; // Comprehensive Diabetes Care
    private static final String CCS = "CCS"; // Cervical Cancer Screening

    public CareGapServiceClient(
            RestTemplate restTemplate,
            @Value("${demo.services.care-gap-service.url:http://care-gap-service:8086/care-gap}") String careGapServiceUrl) {
        this.restTemplate = restTemplate;
        this.careGapServiceUrl = careGapServiceUrl;
    }

    /**
     * Create care gaps for patients in a bundle based on their demographics and conditions.
     *
     * @param bundle   The FHIR Bundle containing patient data
     * @param tenantId The tenant ID
     * @param targetCareGapCount Target number of care gaps to create
     * @return Number of care gaps created
     */
    public int createCareGapsFromBundle(Bundle bundle, String tenantId, int targetCareGapCount) {
        logger.info("Creating care gaps for tenant: {}, target count: {}", tenantId, targetCareGapCount);

        List<Patient> patients = extractPatients(bundle);
        List<Condition> conditions = extractConditions(bundle);
        List<Procedure> procedures = extractProcedures(bundle);

        int careGapsCreated = 0;
        int patientsWithCareGaps = 0;

        // Create care gaps for a subset of patients
        for (Patient patient : patients) {
            if (careGapsCreated >= targetCareGapCount) {
                break;
            }

            String patientId = patient.getIdPart();
            if (patientId == null || patientId.isEmpty()) {
                patientId = patient.getId();
            }

            int age = calculateAge(patient);
            String gender = patient.getGender() != null ? patient.getGender().toCode() : "unknown";

            List<String> patientConditions = getPatientConditionCodes(conditions, patientId);
            List<String> patientProcedures = getPatientProcedureCodes(procedures, patientId);

            List<CareGapRequest> gaps = identifyCareGaps(patientId, age, gender, patientConditions, patientProcedures);

            for (CareGapRequest gap : gaps) {
                if (careGapsCreated >= targetCareGapCount) {
                    break;
                }

                try {
                    createCareGap(gap, tenantId);
                    careGapsCreated++;
                } catch (Exception e) {
                    logger.warn("Failed to create care gap for patient {}: {}", patientId, e.getMessage());
                }
            }

            if (!gaps.isEmpty()) {
                patientsWithCareGaps++;
            }
        }

        logger.info("Created {} care gaps for {} patients", careGapsCreated, patientsWithCareGaps);
        return careGapsCreated;
    }

    /**
     * Identify care gaps for a patient based on demographics and clinical data.
     */
    private List<CareGapRequest> identifyCareGaps(
            String patientId, int age, String gender,
            List<String> conditions, List<String> procedures) {

        List<CareGapRequest> gaps = new ArrayList<>();

        // Breast Cancer Screening (BCS) - Women 50-74
        if ("female".equalsIgnoreCase(gender) && age >= 50 && age <= 74) {
            if (!hasProcedure(procedures, "mammography", "77067", "77066")) {
                gaps.add(createGapRequest(patientId, BCS, "Breast Cancer Screening",
                    "Patient is due for mammography screening", "HIGH"));
            }
        }

        // Colorectal Cancer Screening (COL) - Adults 45-75
        if (age >= 45 && age <= 75) {
            if (!hasProcedure(procedures, "colonoscopy", "45378", "45380", "45384", "45385")) {
                gaps.add(createGapRequest(patientId, COL, "Colorectal Cancer Screening",
                    "Patient is due for colorectal cancer screening", "MEDIUM"));
            }
        }

        // Controlling High Blood Pressure (CBP)
        if (hasCondition(conditions, "hypertension", "I10", "I11", "I12", "I13")) {
            gaps.add(createGapRequest(patientId, CBP, "Blood Pressure Control",
                "Blood pressure management review needed", "HIGH"));
        }

        // Comprehensive Diabetes Care (CDC)
        if (hasCondition(conditions, "diabetes", "E11", "E10", "E13")) {
            if (!hasProcedure(procedures, "hba1c", "83036", "83037")) {
                gaps.add(createGapRequest(patientId, CDC, "Diabetes Care - HbA1c",
                    "HbA1c test needed for diabetes management", "HIGH"));
            }
        }

        // Cervical Cancer Screening (CCS) - Women 21-64
        if ("female".equalsIgnoreCase(gender) && age >= 21 && age <= 64) {
            if (!hasProcedure(procedures, "papsmear", "88141", "88142", "88143", "88147")) {
                gaps.add(createGapRequest(patientId, CCS, "Cervical Cancer Screening",
                    "Cervical cancer screening recommended", "MEDIUM"));
            }
        }

        return gaps;
    }

    private CareGapRequest createGapRequest(String patientId, String measureCode,
            String measureName, String description, String priority) {
        CareGapRequest request = new CareGapRequest();
        request.setPatientId(patientId);
        request.setMeasureCode(measureCode);
        request.setMeasureName(measureName);
        request.setDescription(description);
        request.setPriority(priority);
        request.setStatus("OPEN");
        request.setIdentifiedDate(LocalDate.now().toString());
        request.setDueDate(LocalDate.now().plusMonths(3).toString());
        return request;
    }

    private void createCareGap(CareGapRequest gap, String tenantId) {
        String url = careGapServiceUrl + "/api/v1/care-gaps";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Tenant-Id", tenantId);
        headers.set("X-Demo-Mode", "true");

        HttpEntity<CareGapRequest> request = new HttpEntity<>(gap, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("HTTP " + response.getStatusCode());
            }
        } catch (Exception e) {
            // Log but don't fail - care gap service might not have the endpoint
            logger.debug("Care gap creation skipped: {}", e.getMessage());
        }
    }

    // Helper methods

    private List<Patient> extractPatients(Bundle bundle) {
        return bundle.getEntry().stream()
            .filter(e -> e.getResource() instanceof Patient)
            .map(e -> (Patient) e.getResource())
            .toList();
    }

    private List<Condition> extractConditions(Bundle bundle) {
        return bundle.getEntry().stream()
            .filter(e -> e.getResource() instanceof Condition)
            .map(e -> (Condition) e.getResource())
            .toList();
    }

    private List<Procedure> extractProcedures(Bundle bundle) {
        return bundle.getEntry().stream()
            .filter(e -> e.getResource() instanceof Procedure)
            .map(e -> (Procedure) e.getResource())
            .toList();
    }

    private int calculateAge(Patient patient) {
        if (patient.getBirthDate() == null) return 50;
        java.time.LocalDate birthDate = patient.getBirthDate().toInstant()
            .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        return java.time.Period.between(birthDate, java.time.LocalDate.now()).getYears();
    }

    private List<String> getPatientConditionCodes(List<Condition> conditions, String patientId) {
        return conditions.stream()
            .filter(c -> c.getSubject() != null &&
                        c.getSubject().getReference() != null &&
                        c.getSubject().getReference().contains(patientId))
            .filter(c -> c.getCode() != null && !c.getCode().getCoding().isEmpty())
            .map(c -> c.getCode().getCodingFirstRep().getCode())
            .toList();
    }

    private List<String> getPatientProcedureCodes(List<Procedure> procedures, String patientId) {
        return procedures.stream()
            .filter(p -> p.getSubject() != null &&
                        p.getSubject().getReference() != null &&
                        p.getSubject().getReference().contains(patientId))
            .filter(p -> p.getCode() != null && !p.getCode().getCoding().isEmpty())
            .map(p -> p.getCode().getCodingFirstRep().getCode())
            .toList();
    }

    private boolean hasCondition(List<String> conditions, String keyword, String... codes) {
        Set<String> codeSet = new HashSet<>(Arrays.asList(codes));
        return conditions.stream().anyMatch(c ->
            codeSet.contains(c) || c.toLowerCase().contains(keyword.toLowerCase()));
    }

    private boolean hasProcedure(List<String> procedures, String keyword, String... codes) {
        Set<String> codeSet = new HashSet<>(Arrays.asList(codes));
        return procedures.stream().anyMatch(p ->
            codeSet.contains(p) || p.toLowerCase().contains(keyword.toLowerCase()));
    }

    /**
     * Check if the Care Gap service is available.
     */
    public boolean isServiceAvailable() {
        try {
            String url = careGapServiceUrl.replace("/care-gap", "") + "/actuator/health";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            logger.warn("Care Gap service not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Care gap creation request DTO.
     */
    public static class CareGapRequest {
        private String patientId;
        private String measureCode;
        private String measureName;
        private String description;
        private String priority;
        private String status;
        private String identifiedDate;
        private String dueDate;

        // Getters and setters
        public String getPatientId() { return patientId; }
        public void setPatientId(String patientId) { this.patientId = patientId; }
        public String getMeasureCode() { return measureCode; }
        public void setMeasureCode(String measureCode) { this.measureCode = measureCode; }
        public String getMeasureName() { return measureName; }
        public void setMeasureName(String measureName) { this.measureName = measureName; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getIdentifiedDate() { return identifiedDate; }
        public void setIdentifiedDate(String identifiedDate) { this.identifiedDate = identifiedDate; }
        public String getDueDate() { return dueDate; }
        public void setDueDate(String dueDate) { this.dueDate = dueDate; }
    }
}
