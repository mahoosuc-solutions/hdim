package com.healthdata.demo.application;

import ca.uhn.fhir.context.FhirContext;
import com.healthdata.demo.client.CareGapServiceClient;
import com.healthdata.demo.client.FhirServiceClient;
import com.healthdata.demo.client.QualityMeasureServiceClient;
import com.healthdata.demo.client.UserSeedingClient;
import com.healthdata.demo.domain.model.DemoScenario;
import com.healthdata.demo.domain.model.DemoSession;
import com.healthdata.demo.domain.model.SyntheticPatientTemplate;
import com.healthdata.demo.domain.repository.DemoScenarioRepository;
import com.healthdata.demo.domain.repository.DemoSessionRepository;
import com.healthdata.demo.domain.repository.SyntheticPatientTemplateRepository;
import com.healthdata.demo.generator.*;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Main service for demo data seeding operations.
 *
 * Responsibilities:
 * - Generate synthetic patient cohorts
 * - Seed demo data for scenarios
 * - Orchestrate data generation across generators
 * - Track seeding progress and metrics
 */
@Service
@Transactional
public class DemoSeedingService {

    private static final Logger logger = LoggerFactory.getLogger(DemoSeedingService.class);

    private final SyntheticPatientGenerator patientGenerator;
    private final MedicationGenerator medicationGenerator;
    private final ObservationGenerator observationGenerator;
    private final EncounterGenerator encounterGenerator;
    private final ProcedureGenerator procedureGenerator;
    private final DemoScenarioRepository scenarioRepository;
    private final DemoSessionRepository sessionRepository;
    private final SyntheticPatientTemplateRepository templateRepository;
    private final FhirContext fhirContext;
    private final FhirServiceClient fhirServiceClient;
    private final CareGapServiceClient careGapServiceClient;
    private final QualityMeasureServiceClient qualityMeasureServiceClient;
    private final UserSeedingClient userSeedingClient;
    private final boolean persistToServices;

    public DemoSeedingService(
            SyntheticPatientGenerator patientGenerator,
            MedicationGenerator medicationGenerator,
            ObservationGenerator observationGenerator,
            EncounterGenerator encounterGenerator,
            ProcedureGenerator procedureGenerator,
            DemoScenarioRepository scenarioRepository,
            DemoSessionRepository sessionRepository,
            SyntheticPatientTemplateRepository templateRepository,
            FhirContext fhirContext,
            FhirServiceClient fhirServiceClient,
            CareGapServiceClient careGapServiceClient,
            QualityMeasureServiceClient qualityMeasureServiceClient,
            UserSeedingClient userSeedingClient,
            @Value("${demo.persistence.enabled:true}") boolean persistToServices) {
        this.patientGenerator = patientGenerator;
        this.medicationGenerator = medicationGenerator;
        this.observationGenerator = observationGenerator;
        this.encounterGenerator = encounterGenerator;
        this.procedureGenerator = procedureGenerator;
        this.scenarioRepository = scenarioRepository;
        this.sessionRepository = sessionRepository;
        this.templateRepository = templateRepository;
        this.fhirContext = fhirContext;
        this.fhirServiceClient = fhirServiceClient;
        this.careGapServiceClient = careGapServiceClient;
        this.qualityMeasureServiceClient = qualityMeasureServiceClient;
        this.userSeedingClient = userSeedingClient;
        this.persistToServices = persistToServices;
    }

    /**
     * Generate a cohort of synthetic patients with all associated resources.
     *
     * @param count Number of patients to generate
     * @param tenantId Tenant identifier
     * @param careGapPercentage Percentage of patients with care gaps (0-100)
     * @return Generation result with statistics
     */
    public GenerationResult generatePatientCohort(int count, String tenantId, int careGapPercentage) {
        logger.info("Starting generation of {} patients for tenant: {}", count, tenantId);
        Instant startTime = Instant.now();

        GenerationResult result = new GenerationResult();
        result.setTenantId(tenantId);
        result.setRequestedCount(count);

        try {
            // Generate base patients
            Bundle patientBundle = patientGenerator.generateCohort(count, tenantId);
            result.setPatientCount(patientBundle.getEntry().size());

            // Enhance each patient with additional resources
            // Use a copy to avoid ConcurrentModificationException when generators add entries
            int careGapCount = 0;
            for (Bundle.BundleEntryComponent entry : new java.util.ArrayList<>(patientBundle.getEntry())) {
                if (entry.getResource() instanceof Patient patient) {
                    boolean createCareGap = (careGapCount * 100 / count) < careGapPercentage;

                    enhancePatientWithResources(patient, patientBundle, createCareGap);

                    if (createCareGap) {
                        careGapCount++;
                    }
                }
            }

            result.setCareGapCount(careGapCount);
            result.setGeneratedBundle(patientBundle);

            // Calculate statistics
            long medicationCount = countResourceType(patientBundle, "MedicationRequest");
            long observationCount = countResourceType(patientBundle, "Observation");
            long encounterCount = countResourceType(patientBundle, "Encounter");
            long procedureCount = countResourceType(patientBundle, "Procedure");

            result.setMedicationCount((int) medicationCount);
            result.setObservationCount((int) observationCount);
            result.setEncounterCount((int) encounterCount);
            result.setProcedureCount((int) procedureCount);

            // Persist to downstream services if enabled
            if (persistToServices) {
                logger.info("Persisting generated data to downstream services...");

                // Persist FHIR resources
                if (fhirServiceClient.isServiceAvailable()) {
                    FhirServiceClient.PersistenceResult persistResult =
                        fhirServiceClient.persistBundle(patientBundle, tenantId);

                    if (!persistResult.isSuccess()) {
                        logger.warn("Some resources failed to persist: {} errors", persistResult.getErrorCount());
                    }

                    // Update counts from actual persistence
                    result.setPatientCount(persistResult.getPatientCount());
                } else {
                    logger.warn("FHIR service not available - data generated but not persisted");
                }

                // Create care gaps
                if (careGapServiceClient.isServiceAvailable()) {
                    int actualCareGaps = careGapServiceClient.createCareGapsFromBundle(
                        patientBundle, tenantId, careGapCount);
                    result.setCareGapCount(actualCareGaps);
                } else {
                    logger.warn("Care Gap service not available - care gaps not created");
                }

                // Generate evaluation results for demo
                if (qualityMeasureServiceClient.isServiceAvailable()) {
                    logger.info("Generating quality measure evaluation results...");

                    // First seed measure definitions
                    int measureCount = qualityMeasureServiceClient.seedMeasureDefinitions(tenantId);
                    logger.info("Seeded {} HEDIS measure definitions", measureCount);

                    // Generate evaluation results based on patient data
                    int evaluationResults = qualityMeasureServiceClient.generateDemoResults(
                        patientBundle, tenantId, careGapPercentage);
                    result.setEvaluationResultCount(evaluationResults);
                    logger.info("Generated {} evaluation results", evaluationResults);
                } else {
                    logger.warn("Quality Measure service not available - evaluation results not generated");
                }
            } else {
                logger.info("Persistence disabled - data generated in memory only");
            }

            result.setSuccess(true);

            Instant endTime = Instant.now();
            result.setGenerationTimeMs(endTime.toEpochMilli() - startTime.toEpochMilli());

            logger.info("Generation complete: {} patients, {} care gaps, {} medications, {} observations in {}ms (persisted: {})",
                result.getPatientCount(), result.getCareGapCount(),
                result.getMedicationCount(), result.getObservationCount(),
                result.getGenerationTimeMs(), persistToServices);

        } catch (Exception e) {
            logger.error("Error generating patient cohort", e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    /**
     * Enhance a patient with medications, observations, encounters, and procedures.
     */
    private void enhancePatientWithResources(Patient patient, Bundle bundle, boolean createCareGap) {
        // Extract patient info for generators
        String patientId = patient.getId();
        String riskCategory = extractRiskCategory(patient);
        List<String> conditionCodes = extractConditionCodes(bundle, patientId);
        int age = calculateAge(patient);
        String gender = patient.getGender() != null ? patient.getGender().toCode() : "unknown";

        // Generate medications based on conditions
        medicationGenerator.generateMedications(patient, conditionCodes, bundle);

        // Generate observations (vitals, labs)
        observationGenerator.generateObservations(patient, conditionCodes, riskCategory, bundle);

        // Generate encounter history
        encounterGenerator.generateEncounters(patient, riskCategory, bundle);

        // Generate procedures (with care gap flag)
        procedureGenerator.generateProcedures(patient, age, gender, conditionCodes, createCareGap, bundle);
    }

    /**
     * Generate patient from a pre-defined template.
     *
     * @param templateName Name of the template (e.g., "complex-diabetic")
     * @param tenantId Tenant identifier
     * @return Generated FHIR Bundle
     */
    public Bundle generateFromTemplate(String templateName, String tenantId) {
        logger.info("Generating patient from template: {}", templateName);

        SyntheticPatientTemplate template = templateRepository.findByPersonaName(templateName)
            .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateName));

        return patientGenerator.generateFromTemplate(
            convertToGeneratorTemplate(template), tenantId);
    }

    /**
     * Seed all pre-defined patient templates for a tenant.
     */
    public void seedPatientTemplates(String tenantId) {
        logger.info("Seeding patient templates for tenant: {}", tenantId);

        List<SyntheticPatientTemplate> templates = PatientTemplates.getAllTemplates();
        for (SyntheticPatientTemplate template : templates) {
            if (!templateRepository.existsByPersonaName(template.getPersonaName())) {
                templateRepository.save(template);
                logger.info("Saved template: {}", template.getPersonaName());
            }
        }
    }

    /**
     * Initialize demo scenarios in the database.
     */
    public void initializeScenarios() {
        logger.info("Initializing demo scenarios");

        // HEDIS Evaluation scenario
        if (!scenarioRepository.existsByName("hedis-evaluation")) {
            DemoScenario hedis = new DemoScenario(
                "hedis-evaluation",
                "HEDIS Quality Measure Evaluation",
                DemoScenario.ScenarioType.HEDIS_EVALUATION,
                5000,
                "acme-health"
            );
            hedis.setDescription(
                "Demonstrates automated HEDIS measure evaluation and care gap identification. " +
                "Includes BCS, COL, CBP, CDC, EED, and SPC measures with realistic care gap distribution."
            );
            hedis.setEstimatedLoadTimeSeconds(30);
            scenarioRepository.save(hedis);
        }

        // Patient Journey scenario
        if (!scenarioRepository.existsByName("patient-journey")) {
            DemoScenario patientJourney = new DemoScenario(
                "patient-journey",
                "Patient Care Journey",
                DemoScenario.ScenarioType.PATIENT_JOURNEY,
                1000,
                "acme-health"
            );
            patientJourney.setDescription(
                "Demonstrates 360-degree patient view with clinical decision support and SDOH factors. " +
                "Features 4 pre-defined patient personas with different clinical profiles."
            );
            patientJourney.setEstimatedLoadTimeSeconds(15);
            scenarioRepository.save(patientJourney);
        }

        // Risk Stratification scenario
        if (!scenarioRepository.existsByName("risk-stratification")) {
            DemoScenario risk = new DemoScenario(
                "risk-stratification",
                "Risk Stratification & Analytics",
                DemoScenario.ScenarioType.RISK_STRATIFICATION,
                10000,
                "acme-health"
            );
            risk.setDescription(
                "Demonstrates population risk stratification and predictive analytics. " +
                "Includes HCC risk scoring and admission risk prediction."
            );
            risk.setEstimatedLoadTimeSeconds(45);
            scenarioRepository.save(risk);
        }

        // Multi-Tenant scenario
        if (!scenarioRepository.existsByName("multi-tenant")) {
            DemoScenario multiTenant = new DemoScenario(
                "multi-tenant",
                "Multi-Tenant Administration",
                DemoScenario.ScenarioType.MULTI_TENANT,
                25000,
                "demo-admin"
            );
            multiTenant.setDescription(
                "Demonstrates secure multi-tenant SaaS architecture. " +
                "Includes 3 demo tenants: Demo Tenant (5K), Summit Care (12K), Valley Health (8K)."
            );
            multiTenant.setEstimatedLoadTimeSeconds(60);
            scenarioRepository.save(multiTenant);
        }

        // Seed demo users for authentication
        if (persistToServices && userSeedingClient.isDatabaseAvailable()) {
            logger.info("Seeding demo users...");
            userSeedingClient.seedDemoUsers("demo-tenant");
            userSeedingClient.seedDemoUsers("acme-health");
            userSeedingClient.seedDemoUsers("demo-admin");
        }

        logger.info("Demo scenarios initialized");
    }

    /**
     * Get all available scenarios.
     */
    @Transactional(readOnly = true)
    public List<DemoScenario> getAvailableScenarios() {
        return scenarioRepository.findByIsActiveTrueOrderByDisplayNameAsc();
    }

    /**
     * Get current demo status.
     */
    @Transactional(readOnly = true)
    public DemoStatus getDemoStatus() {
        DemoStatus status = new DemoStatus();

        // Count scenarios
        status.setScenarioCount(scenarioRepository.findByIsActiveTrueOrderByDisplayNameAsc().size());

        // Get current session
        sessionRepository.findCurrentSession().ifPresent(session -> {
            status.setCurrentSessionId(session.getId());
            status.setCurrentScenario(session.getScenario() != null ?
                session.getScenario().getName() : null);
            status.setSessionStatus(session.getStatus().name());
        });

        // Count templates
        status.setTemplateCount(templateRepository.findByIsActiveTrueOrderByDisplayNameAsc().size());

        status.setReady(status.getScenarioCount() > 0);

        return status;
    }

    // Helper methods

    private String extractRiskCategory(Patient patient) {
        return patient.getExtension().stream()
            .filter(ext -> ext.getUrl().contains("hcc-risk-score"))
            .findFirst()
            .map(ext -> {
                double score = ((org.hl7.fhir.r4.model.DecimalType) ext.getValue()).getValueAsNumber().doubleValue();
                if (score < 1.0) return "LOW";
                if (score < 2.0) return "MODERATE";
                return "HIGH";
            })
            .orElse("LOW");
    }

    private List<String> extractConditionCodes(Bundle bundle, String patientId) {
        List<String> codes = new ArrayList<>();
        bundle.getEntry().stream()
            .filter(entry -> entry.getResource() instanceof org.hl7.fhir.r4.model.Condition)
            .map(entry -> (org.hl7.fhir.r4.model.Condition) entry.getResource())
            .filter(condition -> condition.getSubject().getReference().contains(patientId))
            .forEach(condition -> {
                if (condition.getCode() != null && !condition.getCode().getCoding().isEmpty()) {
                    codes.add(condition.getCode().getCodingFirstRep().getCode());
                }
            });
        return codes;
    }

    private int calculateAge(Patient patient) {
        if (patient.getBirthDate() == null) return 50;

        java.time.LocalDate birthDate = patient.getBirthDate().toInstant()
            .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        return java.time.Period.between(birthDate, java.time.LocalDate.now()).getYears();
    }

    private long countResourceType(Bundle bundle, String resourceType) {
        return bundle.getEntry().stream()
            .filter(entry -> entry.getResource().fhirType().equals(resourceType))
            .count();
    }

    private SyntheticPatientGenerator.PatientTemplate convertToGeneratorTemplate(SyntheticPatientTemplate template) {
        SyntheticPatientGenerator.PatientTemplate genTemplate = new SyntheticPatientGenerator.PatientTemplate();
        genTemplate.setPersonaName(template.getPersonaName());
        // Set additional attributes as needed
        return genTemplate;
    }

    // Result and status classes

    public static class GenerationResult {
        private String tenantId;
        private int requestedCount;
        private int patientCount;
        private int careGapCount;
        private int evaluationResultCount;
        private int medicationCount;
        private int observationCount;
        private int encounterCount;
        private int procedureCount;
        private long generationTimeMs;
        private boolean success;
        private String errorMessage;
        private Bundle generatedBundle;

        // Getters and setters
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public int getRequestedCount() { return requestedCount; }
        public void setRequestedCount(int requestedCount) { this.requestedCount = requestedCount; }
        public int getPatientCount() { return patientCount; }
        public void setPatientCount(int patientCount) { this.patientCount = patientCount; }
        public int getCareGapCount() { return careGapCount; }
        public void setCareGapCount(int careGapCount) { this.careGapCount = careGapCount; }
        public int getEvaluationResultCount() { return evaluationResultCount; }
        public void setEvaluationResultCount(int evaluationResultCount) { this.evaluationResultCount = evaluationResultCount; }
        public int getMedicationCount() { return medicationCount; }
        public void setMedicationCount(int medicationCount) { this.medicationCount = medicationCount; }
        public int getObservationCount() { return observationCount; }
        public void setObservationCount(int observationCount) { this.observationCount = observationCount; }
        public int getEncounterCount() { return encounterCount; }
        public void setEncounterCount(int encounterCount) { this.encounterCount = encounterCount; }
        public int getProcedureCount() { return procedureCount; }
        public void setProcedureCount(int procedureCount) { this.procedureCount = procedureCount; }
        public long getGenerationTimeMs() { return generationTimeMs; }
        public void setGenerationTimeMs(long generationTimeMs) { this.generationTimeMs = generationTimeMs; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public Bundle getGeneratedBundle() { return generatedBundle; }
        public void setGeneratedBundle(Bundle generatedBundle) { this.generatedBundle = generatedBundle; }
    }

    public static class DemoStatus {
        private int scenarioCount;
        private int templateCount;
        private UUID currentSessionId;
        private String currentScenario;
        private String sessionStatus;
        private boolean ready;

        // Getters and setters
        public int getScenarioCount() { return scenarioCount; }
        public void setScenarioCount(int scenarioCount) { this.scenarioCount = scenarioCount; }
        public int getTemplateCount() { return templateCount; }
        public void setTemplateCount(int templateCount) { this.templateCount = templateCount; }
        public UUID getCurrentSessionId() { return currentSessionId; }
        public void setCurrentSessionId(UUID currentSessionId) { this.currentSessionId = currentSessionId; }
        public String getCurrentScenario() { return currentScenario; }
        public void setCurrentScenario(String currentScenario) { this.currentScenario = currentScenario; }
        public String getSessionStatus() { return sessionStatus; }
        public void setSessionStatus(String sessionStatus) { this.sessionStatus = sessionStatus; }
        public boolean isReady() { return ready; }
        public void setReady(boolean ready) { this.ready = ready; }
    }
}
