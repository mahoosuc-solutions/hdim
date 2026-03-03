package com.healthdata.fhir.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hl7.fhir.r4.model.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;

/**
 * Processes FHIR transaction and batch Bundles by routing each entry to the
 * appropriate resource service.
 *
 * <p>Transaction bundles are atomic — processed within a single @Transactional
 * boundary so any failure rolls back all entries. Batch bundles process each
 * entry independently, collecting individual success/failure responses.</p>
 *
 * <p>This service is a pure orchestrator — it delegates all persistence,
 * validation, caching, and per-resource Kafka events to the existing 20
 * resource services.</p>
 */
@Slf4j
@Service
public class BundleTransactionService {

    private final PatientService patientService;
    private final ConditionService conditionService;
    private final ObservationService observationService;
    private final EncounterService encounterService;
    private final ProcedureService procedureService;
    private final MedicationRequestService medicationRequestService;
    private final MedicationAdministrationService medicationAdministrationService;
    private final ImmunizationService immunizationService;
    private final AllergyIntoleranceService allergyIntoleranceService;
    private final DiagnosticReportService diagnosticReportService;
    private final DocumentReferenceService documentReferenceService;
    private final CarePlanService carePlanService;
    private final GoalService goalService;
    private final TaskService taskService;
    private final CoverageService coverageService;
    private final AppointmentService appointmentService;
    private final PractitionerService practitionerService;
    private final PractitionerRoleService practitionerRoleService;
    private final OrganizationService organizationService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final Timer transactionTimer;
    private final Counter entriesProcessedCounter;
    private final Counter entriesFailedCounter;

    public BundleTransactionService(
            PatientService patientService,
            ConditionService conditionService,
            ObservationService observationService,
            EncounterService encounterService,
            ProcedureService procedureService,
            MedicationRequestService medicationRequestService,
            MedicationAdministrationService medicationAdministrationService,
            ImmunizationService immunizationService,
            AllergyIntoleranceService allergyIntoleranceService,
            DiagnosticReportService diagnosticReportService,
            DocumentReferenceService documentReferenceService,
            CarePlanService carePlanService,
            GoalService goalService,
            TaskService taskService,
            CoverageService coverageService,
            AppointmentService appointmentService,
            PractitionerService practitionerService,
            PractitionerRoleService practitionerRoleService,
            OrganizationService organizationService,
            KafkaTemplate<String, Object> kafkaTemplate,
            MeterRegistry meterRegistry) {
        this.patientService = patientService;
        this.conditionService = conditionService;
        this.observationService = observationService;
        this.encounterService = encounterService;
        this.procedureService = procedureService;
        this.medicationRequestService = medicationRequestService;
        this.medicationAdministrationService = medicationAdministrationService;
        this.immunizationService = immunizationService;
        this.allergyIntoleranceService = allergyIntoleranceService;
        this.diagnosticReportService = diagnosticReportService;
        this.documentReferenceService = documentReferenceService;
        this.carePlanService = carePlanService;
        this.goalService = goalService;
        this.taskService = taskService;
        this.coverageService = coverageService;
        this.appointmentService = appointmentService;
        this.practitionerService = practitionerService;
        this.practitionerRoleService = practitionerRoleService;
        this.organizationService = organizationService;
        this.kafkaTemplate = kafkaTemplate;

        this.transactionTimer = Timer.builder("fhir.bundle.processing")
                .tag("type", "transaction")
                .description("Time to process a FHIR Bundle transaction")
                .register(meterRegistry);
        this.entriesProcessedCounter = Counter.builder("fhir.bundle.entries")
                .tag("outcome", "success")
                .description("Number of Bundle entries successfully processed")
                .register(meterRegistry);
        this.entriesFailedCounter = Counter.builder("fhir.bundle.entries")
                .tag("outcome", "failure")
                .description("Number of Bundle entries that failed processing")
                .register(meterRegistry);
    }

    /**
     * Process a FHIR Bundle. Dispatches to transaction or batch processing
     * based on bundle type.
     */
    public Bundle processBundle(String tenantId, Bundle bundle, String actor) {
        validateBundle(bundle);

        return transactionTimer.record(() -> {
            if (bundle.getType() == Bundle.BundleType.TRANSACTION) {
                return processTransaction(tenantId, bundle, actor);
            } else {
                return processBatch(tenantId, bundle, actor);
            }
        });
    }

    /**
     * Transaction: all-or-nothing within a single @Transactional boundary.
     * Any failure rolls back every entry.
     */
    @Transactional
    public Bundle processTransaction(String tenantId, Bundle bundle, String actor) {
        Bundle response = new Bundle();
        response.setType(Bundle.BundleType.TRANSACTIONRESPONSE);

        List<String> processedPatientIds = new ArrayList<>();
        int successCount = 0;

        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            Bundle.BundleEntryResponseComponent entryResponse = processEntry(tenantId, entry, actor);

            String status = entryResponse.getStatus();
            if (status != null && status.startsWith("4") || status != null && status.startsWith("5")) {
                // Transaction semantics: fail the whole bundle
                throw new BundleProcessingException(
                        "Transaction failed at entry " + successCount + ": " + status
                        + (entryResponse.hasOutcome() ? " — " + entryResponse.getOutcome() : ""));
            }

            response.addEntry().setResponse(entryResponse);
            successCount++;
            entriesProcessedCounter.increment();

            collectPatientIds(entry, processedPatientIds);
        }

        response.setTotal(successCount);
        publishBundleEvent(tenantId, actor, "transaction", successCount, 0, processedPatientIds);

        log.info("Bundle transaction completed: tenant={}, entries={}, actor={}", tenantId, successCount, actor);
        return response;
    }

    /**
     * Batch: process each entry independently — failures don't affect other entries.
     */
    public Bundle processBatch(String tenantId, Bundle bundle, String actor) {
        Bundle response = new Bundle();
        response.setType(Bundle.BundleType.BATCHRESPONSE);

        List<String> processedPatientIds = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            try {
                Bundle.BundleEntryResponseComponent entryResponse = processEntry(tenantId, entry, actor);
                response.addEntry().setResponse(entryResponse);
                successCount++;
                entriesProcessedCounter.increment();
                collectPatientIds(entry, processedPatientIds);
            } catch (Exception e) {
                log.warn("Batch entry failed: {}", e.getMessage());
                Bundle.BundleEntryResponseComponent errorResponse = new Bundle.BundleEntryResponseComponent();
                errorResponse.setStatus("400 Bad Request");
                errorResponse.setOutcome(createOperationOutcome(e.getMessage()));
                response.addEntry().setResponse(errorResponse);
                failureCount++;
                entriesFailedCounter.increment();
            }
        }

        response.setTotal(successCount);
        publishBundleEvent(tenantId, actor, "batch", successCount, failureCount, processedPatientIds);

        log.info("Bundle batch completed: tenant={}, success={}, failed={}, actor={}",
                tenantId, successCount, failureCount, actor);
        return response;
    }

    private Bundle.BundleEntryResponseComponent processEntry(
            String tenantId, Bundle.BundleEntryComponent entry, String actor) {

        Bundle.BundleEntryRequestComponent request = entry.getRequest();
        Resource resource = entry.getResource();

        // If no request component, infer POST for entries with resources
        String method = "POST";
        if (request != null && request.getMethod() != null) {
            method = request.getMethod().toCode();
        }

        return switch (method) {
            case "POST" -> handleCreate(tenantId, resource, actor);
            case "PUT" -> handleUpdate(tenantId, resource, request, actor);
            case "DELETE" -> handleDelete(tenantId, request, actor);
            default -> {
                Bundle.BundleEntryResponseComponent resp = new Bundle.BundleEntryResponseComponent();
                resp.setStatus("405 Method Not Allowed");
                resp.setOutcome(createOperationOutcome("Unsupported HTTP method: " + method));
                yield resp;
            }
        };
    }

    private Bundle.BundleEntryResponseComponent handleCreate(
            String tenantId, Resource resource, String actor) {

        if (resource == null) {
            Bundle.BundleEntryResponseComponent resp = new Bundle.BundleEntryResponseComponent();
            resp.setStatus("400 Bad Request");
            resp.setOutcome(createOperationOutcome("POST entry must include a resource"));
            return resp;
        }

        String resourceType = resource.fhirType();

        try {
            Resource created = createResource(tenantId, resource, actor);

            Bundle.BundleEntryResponseComponent resp = new Bundle.BundleEntryResponseComponent();
            resp.setStatus("201 Created");
            resp.setLocation(resourceType + "/" + created.getIdElement().getIdPart());
            resp.setLastModified(new java.util.Date());
            return resp;
        } catch (Exception e) {
            log.warn("Failed to create {} in bundle: {}", resourceType, e.getMessage());
            Bundle.BundleEntryResponseComponent resp = new Bundle.BundleEntryResponseComponent();
            resp.setStatus("400 Bad Request");
            resp.setOutcome(createOperationOutcome("Failed to create " + resourceType + ": " + e.getMessage()));
            return resp;
        }
    }

    private Bundle.BundleEntryResponseComponent handleUpdate(
            String tenantId, Resource resource, Bundle.BundleEntryRequestComponent request, String actor) {

        if (resource == null) {
            Bundle.BundleEntryResponseComponent resp = new Bundle.BundleEntryResponseComponent();
            resp.setStatus("400 Bad Request");
            resp.setOutcome(createOperationOutcome("PUT entry must include a resource"));
            return resp;
        }

        String resourceType = resource.fhirType();

        // Extract ID from request URL (e.g., "Patient/123") or from resource
        String resourceId = extractIdFromRequest(request, resource);
        if (resourceId == null) {
            Bundle.BundleEntryResponseComponent resp = new Bundle.BundleEntryResponseComponent();
            resp.setStatus("400 Bad Request");
            resp.setOutcome(createOperationOutcome("PUT entry must specify a resource ID"));
            return resp;
        }

        try {
            Resource updated = updateResource(tenantId, resourceType, resourceId, resource, actor);

            Bundle.BundleEntryResponseComponent resp = new Bundle.BundleEntryResponseComponent();
            resp.setStatus("200 OK");
            resp.setLocation(resourceType + "/" + updated.getIdElement().getIdPart());
            resp.setLastModified(new java.util.Date());
            return resp;
        } catch (Exception e) {
            log.warn("Failed to update {}/{} in bundle: {}", resourceType, resourceId, e.getMessage());
            Bundle.BundleEntryResponseComponent resp = new Bundle.BundleEntryResponseComponent();
            resp.setStatus("400 Bad Request");
            resp.setOutcome(createOperationOutcome(
                    "Failed to update " + resourceType + "/" + resourceId + ": " + e.getMessage()));
            return resp;
        }
    }

    private Bundle.BundleEntryResponseComponent handleDelete(
            String tenantId, Bundle.BundleEntryRequestComponent request, String actor) {

        if (request == null || request.getUrl() == null) {
            Bundle.BundleEntryResponseComponent resp = new Bundle.BundleEntryResponseComponent();
            resp.setStatus("400 Bad Request");
            resp.setOutcome(createOperationOutcome("DELETE entry must specify a URL"));
            return resp;
        }

        String url = request.getUrl();
        String[] parts = url.split("/");
        if (parts.length < 2) {
            Bundle.BundleEntryResponseComponent resp = new Bundle.BundleEntryResponseComponent();
            resp.setStatus("400 Bad Request");
            resp.setOutcome(createOperationOutcome("DELETE URL must be ResourceType/id, got: " + url));
            return resp;
        }

        String resourceType = parts[0];
        String resourceId = parts[1];

        try {
            deleteResource(tenantId, resourceType, resourceId, actor);

            Bundle.BundleEntryResponseComponent resp = new Bundle.BundleEntryResponseComponent();
            resp.setStatus("204 No Content");
            return resp;
        } catch (Exception e) {
            log.warn("Failed to delete {}/{} in bundle: {}", resourceType, resourceId, e.getMessage());
            Bundle.BundleEntryResponseComponent resp = new Bundle.BundleEntryResponseComponent();
            resp.setStatus("400 Bad Request");
            resp.setOutcome(createOperationOutcome(
                    "Failed to delete " + resourceType + "/" + resourceId + ": " + e.getMessage()));
            return resp;
        }
    }

    /**
     * Routes a resource to the correct service for creation.
     */
    @SuppressWarnings("unchecked")
    private Resource createResource(String tenantId, Resource resource, String actor) {
        return switch (resource.fhirType()) {
            case "Patient" -> patientService.createPatient(tenantId, (Patient) resource, actor);
            case "Condition" -> conditionService.createCondition(tenantId, (Condition) resource, actor);
            case "Observation" -> observationService.createObservation(tenantId, (Observation) resource, actor);
            case "Encounter" -> encounterService.createEncounter(tenantId, (Encounter) resource, actor);
            case "Procedure" -> procedureService.createProcedure(tenantId, (Procedure) resource, actor);
            case "MedicationRequest" -> medicationRequestService.createMedicationRequest(
                    tenantId, (MedicationRequest) resource, actor);
            case "MedicationAdministration" -> medicationAdministrationService.createMedicationAdministration(
                    tenantId, (MedicationAdministration) resource, actor);
            case "Immunization" -> immunizationService.createImmunization(tenantId, (Immunization) resource, actor);
            case "AllergyIntolerance" -> allergyIntoleranceService.createAllergyIntolerance(
                    tenantId, (AllergyIntolerance) resource, actor);
            case "DiagnosticReport" -> diagnosticReportService.createDiagnosticReport(
                    tenantId, (DiagnosticReport) resource, actor);
            case "DocumentReference" -> documentReferenceService.createDocumentReference(
                    tenantId, (DocumentReference) resource, actor);
            case "CarePlan" -> carePlanService.createCarePlan(tenantId, (CarePlan) resource, actor);
            case "Goal" -> goalService.createGoal(tenantId, (Goal) resource, actor);
            case "Task" -> taskService.createTask(tenantId, (org.hl7.fhir.r4.model.Task) resource, actor);
            case "Coverage" -> coverageService.createCoverage(tenantId, (Coverage) resource, actor);
            case "Appointment" -> appointmentService.createAppointment(tenantId, (Appointment) resource, actor);
            case "Practitioner" -> practitionerService.createPractitioner(tenantId, (Practitioner) resource, actor);
            case "PractitionerRole" -> practitionerRoleService.createPractitionerRole(
                    tenantId, (PractitionerRole) resource, actor);
            case "Organization" -> organizationService.createOrganization(tenantId, (Organization) resource, actor);
            default -> throw new BundleValidationException("Unsupported resource type: " + resource.fhirType());
        };
    }

    /**
     * Routes a resource to the correct service for update.
     */
    private Resource updateResource(
            String tenantId, String resourceType, String resourceId, Resource resource, String actor) {
        return switch (resourceType) {
            case "Patient" -> patientService.updatePatient(tenantId, resourceId, (Patient) resource, actor);
            default -> throw new BundleValidationException(
                    "Update not yet supported for resource type: " + resourceType
                    + ". POST (create) is supported for all 19 FHIR resource types.");
        };
    }

    /**
     * Routes a delete to the correct service.
     */
    private void deleteResource(String tenantId, String resourceType, String resourceId, String actor) {
        switch (resourceType) {
            case "Patient" -> patientService.deletePatient(tenantId, resourceId, actor);
            default -> throw new BundleValidationException(
                    "Delete not yet supported for resource type: " + resourceType);
        }
    }

    private void validateBundle(Bundle bundle) {
        if (bundle == null) {
            throw new BundleValidationException("Bundle must not be null");
        }
        if (bundle.getType() != Bundle.BundleType.TRANSACTION && bundle.getType() != Bundle.BundleType.BATCH) {
            throw new BundleValidationException(
                    "Bundle type must be 'transaction' or 'batch', got: " + bundle.getType());
        }
        if (!bundle.hasEntry() || bundle.getEntry().isEmpty()) {
            throw new BundleValidationException("Bundle must contain at least one entry");
        }
    }

    private String extractIdFromRequest(Bundle.BundleEntryRequestComponent request, Resource resource) {
        if (request != null && request.getUrl() != null) {
            String url = request.getUrl();
            String[] parts = url.split("/");
            if (parts.length >= 2) {
                return parts[1];
            }
        }
        if (resource.hasIdElement() && resource.getIdElement().hasIdPart()) {
            return resource.getIdElement().getIdPart();
        }
        return null;
    }

    private void collectPatientIds(Bundle.BundleEntryComponent entry, List<String> patientIds) {
        Resource resource = entry.getResource();
        if (resource == null) return;

        if (resource instanceof Patient patient) {
            if (patient.hasIdElement() && patient.getIdElement().hasIdPart()) {
                patientIds.add(patient.getIdElement().getIdPart());
            }
        }
    }

    private void publishBundleEvent(String tenantId, String actor, String bundleType,
                                     int successCount, int failureCount, List<String> patientIds) {
        try {
            kafkaTemplate.send("fhir.bundle.transaction.completed",
                    UUID.randomUUID().toString(),
                    Map.of(
                            "tenantId", tenantId,
                            "actor", actor,
                            "bundleType", bundleType,
                            "successCount", successCount,
                            "failureCount", failureCount,
                            "patientIds", patientIds,
                            "timestamp", Instant.now().toString()
                    ));
        } catch (Exception e) {
            log.warn("Failed to publish bundle transaction event: {}", e.getMessage());
        }
    }

    private OperationOutcome createOperationOutcome(String message) {
        OperationOutcome outcome = new OperationOutcome();
        outcome.addIssue()
                .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                .setCode(OperationOutcome.IssueType.PROCESSING)
                .setDiagnostics(message);
        return outcome;
    }

    public static class BundleValidationException extends RuntimeException {
        public BundleValidationException(String message) {
            super(message);
        }
    }

    public static class BundleProcessingException extends RuntimeException {
        public BundleProcessingException(String message) {
            super(message);
        }
    }
}
