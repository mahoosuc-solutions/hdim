package com.healthdata.fhir.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * FHIR Patient/$everything operation service.
 *
 * Returns a Bundle of type "collection" containing a Patient resource and all
 * associated clinical resources across all resource types.  Follows HL7 FHIR R4
 * §3.1.3.7 (Patient/$everything).
 *
 * Multi-tenant isolation is enforced by forwarding the tenantId to every
 * delegated service call.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FhirEverythingService {

    // Maximum resources per type returned in a single $everything call.
    // At pilot scale (≤5 K members) this is safe; revisit before GA.
    private static final int MAX_RESOURCES_PER_TYPE = 1_000;

    private final PatientService patientService;
    private final EncounterService encounterService;
    private final ConditionService conditionService;
    private final ObservationService observationService;
    private final ProcedureService procedureService;
    private final MedicationRequestService medicationRequestService;
    private final MedicationAdministrationService medicationAdministrationService;
    private final ImmunizationService immunizationService;
    private final AllergyIntoleranceService allergyIntoleranceService;
    private final DiagnosticReportService diagnosticReportService;
    private final CarePlanService carePlanService;
    private final GoalService goalService;
    private final TaskService taskService;
    private final AppointmentService appointmentService;

    /**
     * Aggregate all FHIR resources for a given patient into a single Bundle.
     *
     * @param tenantId  tenant scope for multi-tenant isolation
     * @param patientId logical patient ID (UUID string)
     * @return FHIR Bundle (type = collection) containing all resources
     * @throws PatientService.PatientNotFoundException if the patient does not exist
     */
    @Transactional(readOnly = true)
    public Bundle getEverything(String tenantId, String patientId) {
        log.debug("Building $everything bundle for patient {} in tenant {}", patientId, tenantId);

        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);
        bundle.getMeta().setLastUpdated(new Date());

        // Patient must exist — throws PatientNotFoundException if absent
        patientService.getPatient(tenantId, patientId)
                .ifPresent(patient -> addEntry(bundle, patient, "Patient/" + patientId));

        Pageable page = PageRequest.of(0, MAX_RESOURCES_PER_TYPE);
        UUID patientUuid = parseUuid(patientId);

        // Encounters
        addBundleEntries(bundle, encounterService.searchEncountersByPatient(tenantId, patientId, page));

        // Conditions
        addBundleEntries(bundle, conditionService.searchConditionsByPatient(tenantId, patientId, page));

        // Observations
        addBundleEntries(bundle, observationService.searchObservationsByPatient(tenantId, patientId, page));

        // Procedures
        addBundleEntries(bundle, procedureService.searchProceduresByPatient(tenantId, patientId, page));

        // Medication requests
        addBundleEntries(bundle, medicationRequestService.searchMedicationRequestsByPatient(tenantId, patientId, page));

        // Medication administrations
        addBundleEntries(bundle, medicationAdministrationService.searchAdministrationsByPatient(tenantId, patientId, page));

        // Immunizations
        addBundleEntries(bundle, immunizationService.getImmunizationsByPatient(tenantId, patientId, page));

        // Allergies
        addBundleEntries(bundle, allergyIntoleranceService.getAllergiesByPatient(tenantId, patientId, page));

        // Diagnostic reports
        List<org.hl7.fhir.r4.model.DiagnosticReport> reports = diagnosticReportService.getReportsByPatient(tenantId, patientUuid);
        reports.forEach(r -> addEntry(bundle, r, "DiagnosticReport/" + r.getIdElement().getIdPart()));

        // Care plans
        carePlanService.searchCarePlans(tenantId, patientUuid, null, null, null, null, page)
                .forEach(cp -> addEntry(bundle, cp, "CarePlan/" + cp.getIdElement().getIdPart()));

        // Goals (lifecycleStatus=null, achievementStatus=null, categoryCode=null, priority=null)
        goalService.searchGoals(tenantId, patientUuid, null, null, null, null, page)
                .forEach(g -> addEntry(bundle, g, "Goal/" + g.getIdElement().getIdPart()));

        // Tasks
        addBundleEntries(bundle, taskService.searchTasksByPatient(tenantId, patientId, page));

        // Appointments
        addBundleEntries(bundle, appointmentService.searchAppointmentsByPatient(tenantId, patientId, page));

        bundle.setTotal(bundle.getEntry().size());
        log.debug("$everything bundle for patient {} contains {} resources", patientId, bundle.getTotal());

        return bundle;
    }

    private void addBundleEntries(Bundle target, Bundle source) {
        if (source == null) {
            return;
        }
        for (Bundle.BundleEntryComponent entry : source.getEntry()) {
            if (entry.getResource() != null) {
                target.addEntry(entry);
            }
        }
    }

    private void addEntry(Bundle bundle, Resource resource, String fullUrl) {
        Bundle.BundleEntryComponent entry = new Bundle.BundleEntryComponent();
        entry.setFullUrl(fullUrl);
        entry.setResource(resource);
        bundle.addEntry(entry);
    }

    private UUID parseUuid(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException ex) {
            throw new PatientService.PatientValidationException("Patient id must be a valid UUID: " + id);
        }
    }
}
