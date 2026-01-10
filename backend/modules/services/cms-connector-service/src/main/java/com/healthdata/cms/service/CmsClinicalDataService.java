package com.healthdata.cms.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.healthdata.cms.client.DpcClient;
import com.healthdata.cms.model.CmsCondition;
import com.healthdata.cms.model.CmsMedicationRequest;
import com.healthdata.cms.model.CmsObservation;
import com.healthdata.cms.model.CmsProcedure;
import com.healthdata.cms.repository.CmsConditionRepository;
import com.healthdata.cms.repository.CmsMedicationRequestRepository;
import com.healthdata.cms.repository.CmsObservationRepository;
import com.healthdata.cms.repository.CmsProcedureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * CMS Clinical Data Service
 *
 * Handles fetching, parsing, and persisting clinical data from CMS DPC API:
 * - Conditions (diagnoses)
 * - Procedures
 * - Medication Requests (Part D prescriptions)
 * - Observations (lab results, vital signs)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CmsClinicalDataService {

    private final DpcClient dpcClient;
    private final FhirContext fhirContext;
    private final CmsConditionRepository conditionRepository;
    private final CmsProcedureRepository procedureRepository;
    private final CmsMedicationRequestRepository medicationRequestRepository;
    private final CmsObservationRepository observationRepository;

    // ==================== Conditions ====================

    /**
     * Fetch and parse Condition resources for a patient
     */
    @Transactional(readOnly = true)
    public List<CmsCondition> fetchConditions(String patientId, UUID tenantId) {
        log.info("Fetching conditions for patient: {} (tenant: {})", patientId, tenantId);

        try {
            String conditionsJson = dpcClient.getConditions(patientId);
            IParser parser = fhirContext.newJsonParser();
            Bundle bundle = parser.parseResource(Bundle.class, conditionsJson);

            List<CmsCondition> conditions = parseConditionBundle(bundle, patientId, tenantId);
            log.info("Parsed {} conditions for patient: {}", conditions.size(), patientId);
            return conditions;

        } catch (Exception e) {
            log.error("Failed to fetch conditions for patient {}: {}", patientId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch conditions: " + e.getMessage(), e);
        }
    }

    /**
     * Sync conditions to database
     */
    @Transactional
    public int syncConditions(String patientId, UUID tenantId) {
        List<CmsCondition> conditions = fetchConditions(patientId, tenantId);
        if (conditions.isEmpty()) {
            return 0;
        }

        // Filter out duplicates by content hash
        List<CmsCondition> newConditions = conditions.stream()
                .filter(c -> !conditionRepository.existsByContentHash(c.getContentHash()))
                .toList();

        List<CmsCondition> saved = conditionRepository.saveAll(newConditions);
        log.info("Synced {} new conditions for patient: {}", saved.size(), patientId);
        return saved.size();
    }

    private List<CmsCondition> parseConditionBundle(Bundle bundle, String patientId, UUID tenantId) {
        List<CmsCondition> conditions = new ArrayList<>();

        if (bundle == null || bundle.getEntry() == null) {
            return conditions;
        }

        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.getResource() instanceof Condition fhirCondition) {
                try {
                    CmsCondition condition = convertCondition(fhirCondition, patientId, tenantId);
                    conditions.add(condition);
                } catch (Exception e) {
                    log.warn("Failed to parse condition entry: {}", e.getMessage());
                }
            }
        }

        return conditions;
    }

    private CmsCondition convertCondition(Condition fhirCondition, String patientId, UUID tenantId) {
        CmsCondition condition = new CmsCondition();
        condition.setId(UUID.randomUUID());
        condition.setTenantId(tenantId);
        condition.setPatientId(patientId);
        condition.setConditionId(fhirCondition.getIdElement().getIdPart());
        condition.setDataSource("DPC");
        condition.setImportedAt(LocalDateTime.now());
        condition.setIsProcessed(false);
        condition.setHasValidationErrors(false);

        // Code
        if (fhirCondition.hasCode() && fhirCondition.getCode().hasCoding()) {
            Coding coding = fhirCondition.getCode().getCodingFirstRep();
            condition.setCodeSystem(coding.getSystem());
            condition.setCodeValue(coding.getCode());
            condition.setCodeDisplay(coding.getDisplay());
        }

        // Clinical status
        if (fhirCondition.hasClinicalStatus() && fhirCondition.getClinicalStatus().hasCoding()) {
            condition.setClinicalStatus(fhirCondition.getClinicalStatus().getCodingFirstRep().getCode());
        }

        // Verification status
        if (fhirCondition.hasVerificationStatus() && fhirCondition.getVerificationStatus().hasCoding()) {
            condition.setVerificationStatus(fhirCondition.getVerificationStatus().getCodingFirstRep().getCode());
        }

        // Category
        if (fhirCondition.hasCategory() && !fhirCondition.getCategory().isEmpty()) {
            CodeableConcept category = fhirCondition.getCategoryFirstRep();
            if (category.hasCoding()) {
                condition.setCategory(category.getCodingFirstRep().getCode());
            }
        }

        // Severity
        if (fhirCondition.hasSeverity() && fhirCondition.getSeverity().hasCoding()) {
            condition.setSeverity(fhirCondition.getSeverity().getCodingFirstRep().getCode());
        }

        // Onset
        if (fhirCondition.hasOnsetDateTimeType()) {
            condition.setOnsetDate(toLocalDate(fhirCondition.getOnsetDateTimeType().getValue()));
        }

        // Abatement
        if (fhirCondition.hasAbatementDateTimeType()) {
            condition.setAbatementDate(toLocalDate(fhirCondition.getAbatementDateTimeType().getValue()));
        }

        // Recorded date
        if (fhirCondition.hasRecordedDate()) {
            condition.setRecordedDate(toLocalDate(fhirCondition.getRecordedDate()));
        }

        // Body site
        if (fhirCondition.hasBodySite() && !fhirCondition.getBodySite().isEmpty()) {
            CodeableConcept bodySite = fhirCondition.getBodySiteFirstRep();
            if (bodySite.hasCoding()) {
                condition.setBodySite(bodySite.getCodingFirstRep().getDisplay());
            }
        }

        condition.setContentHash(generateContentHash(fhirCondition));
        return condition;
    }

    // ==================== Procedures ====================

    /**
     * Fetch and parse Procedure resources for a patient
     */
    @Transactional(readOnly = true)
    public List<CmsProcedure> fetchProcedures(String patientId, UUID tenantId) {
        log.info("Fetching procedures for patient: {} (tenant: {})", patientId, tenantId);

        try {
            String proceduresJson = dpcClient.getProcedures(patientId);
            IParser parser = fhirContext.newJsonParser();
            Bundle bundle = parser.parseResource(Bundle.class, proceduresJson);

            List<CmsProcedure> procedures = parseProcedureBundle(bundle, patientId, tenantId);
            log.info("Parsed {} procedures for patient: {}", procedures.size(), patientId);
            return procedures;

        } catch (Exception e) {
            log.error("Failed to fetch procedures for patient {}: {}", patientId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch procedures: " + e.getMessage(), e);
        }
    }

    /**
     * Sync procedures to database
     */
    @Transactional
    public int syncProcedures(String patientId, UUID tenantId) {
        List<CmsProcedure> procedures = fetchProcedures(patientId, tenantId);
        if (procedures.isEmpty()) {
            return 0;
        }

        List<CmsProcedure> newProcedures = procedures.stream()
                .filter(p -> !procedureRepository.existsByContentHash(p.getContentHash()))
                .toList();

        List<CmsProcedure> saved = procedureRepository.saveAll(newProcedures);
        log.info("Synced {} new procedures for patient: {}", saved.size(), patientId);
        return saved.size();
    }

    private List<CmsProcedure> parseProcedureBundle(Bundle bundle, String patientId, UUID tenantId) {
        List<CmsProcedure> procedures = new ArrayList<>();

        if (bundle == null || bundle.getEntry() == null) {
            return procedures;
        }

        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.getResource() instanceof Procedure fhirProcedure) {
                try {
                    CmsProcedure procedure = convertProcedure(fhirProcedure, patientId, tenantId);
                    procedures.add(procedure);
                } catch (Exception e) {
                    log.warn("Failed to parse procedure entry: {}", e.getMessage());
                }
            }
        }

        return procedures;
    }

    private CmsProcedure convertProcedure(Procedure fhirProcedure, String patientId, UUID tenantId) {
        CmsProcedure procedure = new CmsProcedure();
        procedure.setId(UUID.randomUUID());
        procedure.setTenantId(tenantId);
        procedure.setPatientId(patientId);
        procedure.setProcedureId(fhirProcedure.getIdElement().getIdPart());
        procedure.setDataSource("DPC");
        procedure.setImportedAt(LocalDateTime.now());
        procedure.setIsProcessed(false);
        procedure.setHasValidationErrors(false);

        // Code
        if (fhirProcedure.hasCode() && fhirProcedure.getCode().hasCoding()) {
            Coding coding = fhirProcedure.getCode().getCodingFirstRep();
            procedure.setCodeSystem(coding.getSystem());
            procedure.setCodeValue(coding.getCode());
            procedure.setCodeDisplay(coding.getDisplay());
        }

        // Status
        if (fhirProcedure.hasStatus()) {
            procedure.setStatus(fhirProcedure.getStatus().toCode());
        }

        // Category
        if (fhirProcedure.hasCategory() && fhirProcedure.getCategory().hasCoding()) {
            procedure.setCategory(fhirProcedure.getCategory().getCodingFirstRep().getCode());
        }

        // Performed date
        if (fhirProcedure.hasPerformedDateTimeType()) {
            procedure.setPerformedDate(toLocalDate(fhirProcedure.getPerformedDateTimeType().getValue()));
        } else if (fhirProcedure.hasPerformedPeriod()) {
            Period period = fhirProcedure.getPerformedPeriod();
            if (period.hasStart()) {
                procedure.setPerformedDate(toLocalDate(period.getStart()));
            }
            if (period.hasEnd()) {
                procedure.setPerformedDateEnd(toLocalDate(period.getEnd()));
            }
        }

        // Location
        if (fhirProcedure.hasLocation()) {
            procedure.setLocation(fhirProcedure.getLocation().getDisplay());
        }

        // Performer
        if (fhirProcedure.hasPerformer() && !fhirProcedure.getPerformer().isEmpty()) {
            Procedure.ProcedurePerformerComponent performer = fhirProcedure.getPerformerFirstRep();
            if (performer.hasActor()) {
                procedure.setPerformerReference(performer.getActor().getReference());
            }
        }

        // Body site
        if (fhirProcedure.hasBodySite() && !fhirProcedure.getBodySite().isEmpty()) {
            procedure.setBodySite(fhirProcedure.getBodySiteFirstRep().getCodingFirstRep().getDisplay());
        }

        // Outcome
        if (fhirProcedure.hasOutcome() && fhirProcedure.getOutcome().hasCoding()) {
            procedure.setOutcome(fhirProcedure.getOutcome().getCodingFirstRep().getCode());
        }

        // Reason code
        if (fhirProcedure.hasReasonCode() && !fhirProcedure.getReasonCode().isEmpty()) {
            procedure.setReasonCode(fhirProcedure.getReasonCodeFirstRep().getCodingFirstRep().getCode());
        }

        procedure.setContentHash(generateContentHash(fhirProcedure));
        return procedure;
    }

    // ==================== Medication Requests ====================

    /**
     * Fetch and parse MedicationRequest resources for a patient
     */
    @Transactional(readOnly = true)
    public List<CmsMedicationRequest> fetchMedicationRequests(String patientId, UUID tenantId) {
        log.info("Fetching medication requests for patient: {} (tenant: {})", patientId, tenantId);

        try {
            String medicationsJson = dpcClient.getMedicationRequests(patientId);
            IParser parser = fhirContext.newJsonParser();
            Bundle bundle = parser.parseResource(Bundle.class, medicationsJson);

            List<CmsMedicationRequest> medications = parseMedicationRequestBundle(bundle, patientId, tenantId);
            log.info("Parsed {} medication requests for patient: {}", medications.size(), patientId);
            return medications;

        } catch (Exception e) {
            log.error("Failed to fetch medication requests for patient {}: {}", patientId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch medication requests: " + e.getMessage(), e);
        }
    }

    /**
     * Sync medication requests to database
     */
    @Transactional
    public int syncMedicationRequests(String patientId, UUID tenantId) {
        List<CmsMedicationRequest> medications = fetchMedicationRequests(patientId, tenantId);
        if (medications.isEmpty()) {
            return 0;
        }

        List<CmsMedicationRequest> newMedications = medications.stream()
                .filter(m -> !medicationRequestRepository.existsByContentHash(m.getContentHash()))
                .toList();

        List<CmsMedicationRequest> saved = medicationRequestRepository.saveAll(newMedications);
        log.info("Synced {} new medication requests for patient: {}", saved.size(), patientId);
        return saved.size();
    }

    private List<CmsMedicationRequest> parseMedicationRequestBundle(Bundle bundle, String patientId, UUID tenantId) {
        List<CmsMedicationRequest> medications = new ArrayList<>();

        if (bundle == null || bundle.getEntry() == null) {
            return medications;
        }

        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.getResource() instanceof MedicationRequest fhirMedRequest) {
                try {
                    CmsMedicationRequest medication = convertMedicationRequest(fhirMedRequest, patientId, tenantId);
                    medications.add(medication);
                } catch (Exception e) {
                    log.warn("Failed to parse medication request entry: {}", e.getMessage());
                }
            }
        }

        return medications;
    }

    private CmsMedicationRequest convertMedicationRequest(MedicationRequest fhirMedRequest, String patientId, UUID tenantId) {
        CmsMedicationRequest medication = new CmsMedicationRequest();
        medication.setId(UUID.randomUUID());
        medication.setTenantId(tenantId);
        medication.setPatientId(patientId);
        medication.setMedicationRequestId(fhirMedRequest.getIdElement().getIdPart());
        medication.setDataSource("DPC");
        medication.setImportedAt(LocalDateTime.now());
        medication.setIsProcessed(false);
        medication.setHasValidationErrors(false);

        // Medication code
        if (fhirMedRequest.hasMedicationCodeableConcept() && fhirMedRequest.getMedicationCodeableConcept().hasCoding()) {
            Coding coding = fhirMedRequest.getMedicationCodeableConcept().getCodingFirstRep();
            medication.setMedicationCodeSystem(coding.getSystem());
            medication.setMedicationCodeValue(coding.getCode());
            medication.setMedicationCodeDisplay(coding.getDisplay());
        }

        // Status
        if (fhirMedRequest.hasStatus()) {
            medication.setStatus(fhirMedRequest.getStatus().toCode());
        }

        // Intent
        if (fhirMedRequest.hasIntent()) {
            medication.setIntent(fhirMedRequest.getIntent().toCode());
        }

        // Category
        if (fhirMedRequest.hasCategory() && !fhirMedRequest.getCategory().isEmpty()) {
            medication.setCategory(fhirMedRequest.getCategoryFirstRep().getCodingFirstRep().getCode());
        }

        // Priority
        if (fhirMedRequest.hasPriority()) {
            medication.setPriority(fhirMedRequest.getPriority().toCode());
        }

        // Authored on
        if (fhirMedRequest.hasAuthoredOn()) {
            medication.setAuthoredOn(toLocalDate(fhirMedRequest.getAuthoredOn()));
        }

        // Requester
        if (fhirMedRequest.hasRequester()) {
            medication.setRequesterReference(fhirMedRequest.getRequester().getReference());
        }

        // Reason code
        if (fhirMedRequest.hasReasonCode() && !fhirMedRequest.getReasonCode().isEmpty()) {
            medication.setReasonCode(fhirMedRequest.getReasonCodeFirstRep().getCodingFirstRep().getCode());
        }

        // Dosage instruction
        if (fhirMedRequest.hasDosageInstruction() && !fhirMedRequest.getDosageInstruction().isEmpty()) {
            Dosage dosage = fhirMedRequest.getDosageInstructionFirstRep();
            if (dosage.hasText()) {
                medication.setDosageInstruction(dosage.getText());
            }
        }

        // Dispense request
        if (fhirMedRequest.hasDispenseRequest()) {
            MedicationRequest.MedicationRequestDispenseRequestComponent dispense = fhirMedRequest.getDispenseRequest();

            // Quantity
            if (dispense.hasQuantity()) {
                medication.setDispenseQuantity(dispense.getQuantity().getValue().doubleValue());
                medication.setDispenseQuantityUnit(dispense.getQuantity().getUnit());
            }

            // Number of refills
            if (dispense.hasNumberOfRepeatsAllowed()) {
                medication.setNumberOfRefills(dispense.getNumberOfRepeatsAllowed());
            }

            // Expected supply duration (days supply)
            if (dispense.hasExpectedSupplyDuration()) {
                medication.setDaysSupply(dispense.getExpectedSupplyDuration().getValue().intValue());
            }
        }

        // Substitution
        if (fhirMedRequest.hasSubstitution()) {
            medication.setSubstitutionAllowed(fhirMedRequest.getSubstitution().getAllowedBooleanType().getValue());
        }

        medication.setContentHash(generateContentHash(fhirMedRequest));
        return medication;
    }

    // ==================== Observations ====================

    /**
     * Fetch and parse Observation resources for a patient
     */
    @Transactional(readOnly = true)
    public List<CmsObservation> fetchObservations(String patientId, UUID tenantId) {
        log.info("Fetching observations for patient: {} (tenant: {})", patientId, tenantId);

        try {
            String observationsJson = dpcClient.getObservations(patientId);
            IParser parser = fhirContext.newJsonParser();
            Bundle bundle = parser.parseResource(Bundle.class, observationsJson);

            List<CmsObservation> observations = parseObservationBundle(bundle, patientId, tenantId);
            log.info("Parsed {} observations for patient: {}", observations.size(), patientId);
            return observations;

        } catch (Exception e) {
            log.error("Failed to fetch observations for patient {}: {}", patientId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch observations: " + e.getMessage(), e);
        }
    }

    /**
     * Sync observations to database
     */
    @Transactional
    public int syncObservations(String patientId, UUID tenantId) {
        List<CmsObservation> observations = fetchObservations(patientId, tenantId);
        if (observations.isEmpty()) {
            return 0;
        }

        List<CmsObservation> newObservations = observations.stream()
                .filter(o -> !observationRepository.existsByContentHash(o.getContentHash()))
                .toList();

        List<CmsObservation> saved = observationRepository.saveAll(newObservations);
        log.info("Synced {} new observations for patient: {}", saved.size(), patientId);
        return saved.size();
    }

    private List<CmsObservation> parseObservationBundle(Bundle bundle, String patientId, UUID tenantId) {
        List<CmsObservation> observations = new ArrayList<>();

        if (bundle == null || bundle.getEntry() == null) {
            return observations;
        }

        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.getResource() instanceof Observation fhirObservation) {
                try {
                    CmsObservation observation = convertObservation(fhirObservation, patientId, tenantId);
                    observations.add(observation);
                } catch (Exception e) {
                    log.warn("Failed to parse observation entry: {}", e.getMessage());
                }
            }
        }

        return observations;
    }

    private CmsObservation convertObservation(Observation fhirObservation, String patientId, UUID tenantId) {
        CmsObservation observation = new CmsObservation();
        observation.setId(UUID.randomUUID());
        observation.setTenantId(tenantId);
        observation.setPatientId(patientId);
        observation.setObservationId(fhirObservation.getIdElement().getIdPart());
        observation.setDataSource("DPC");
        observation.setImportedAt(LocalDateTime.now());
        observation.setIsProcessed(false);
        observation.setHasValidationErrors(false);

        // Code
        if (fhirObservation.hasCode() && fhirObservation.getCode().hasCoding()) {
            Coding coding = fhirObservation.getCode().getCodingFirstRep();
            observation.setCodeSystem(coding.getSystem());
            observation.setCodeValue(coding.getCode());
            observation.setCodeDisplay(coding.getDisplay());
        }

        // Status
        if (fhirObservation.hasStatus()) {
            observation.setStatus(fhirObservation.getStatus().toCode());
        }

        // Category
        if (fhirObservation.hasCategory() && !fhirObservation.getCategory().isEmpty()) {
            observation.setCategory(fhirObservation.getCategoryFirstRep().getCodingFirstRep().getCode());
        }

        // Effective date/time
        if (fhirObservation.hasEffectiveDateTimeType()) {
            observation.setEffectiveDatetime(toLocalDateTime(fhirObservation.getEffectiveDateTimeType().getValue()));
        }

        // Issued
        if (fhirObservation.hasIssued()) {
            observation.setIssued(toLocalDateTime(fhirObservation.getIssued()));
        }

        // Value
        if (fhirObservation.hasValueQuantity()) {
            Quantity quantity = fhirObservation.getValueQuantity();
            if (quantity.hasValue()) {
                observation.setValueQuantity(quantity.getValue().doubleValue());
            }
            observation.setValueUnit(quantity.getUnit());
            observation.setValueSystem(quantity.getSystem());
            observation.setValueCode(quantity.getCode());
        } else if (fhirObservation.hasValueStringType()) {
            observation.setValueString(fhirObservation.getValueStringType().getValue());
        } else if (fhirObservation.hasValueCodeableConcept()) {
            observation.setValueCodeableConcept(fhirObservation.getValueCodeableConcept().getCodingFirstRep().getDisplay());
        }

        // Reference range
        if (fhirObservation.hasReferenceRange() && !fhirObservation.getReferenceRange().isEmpty()) {
            Observation.ObservationReferenceRangeComponent refRange = fhirObservation.getReferenceRangeFirstRep();
            if (refRange.hasLow() && refRange.getLow().hasValue()) {
                observation.setReferenceRangeLow(refRange.getLow().getValue().doubleValue());
            }
            if (refRange.hasHigh() && refRange.getHigh().hasValue()) {
                observation.setReferenceRangeHigh(refRange.getHigh().getValue().doubleValue());
            }
        }

        // Interpretation
        if (fhirObservation.hasInterpretation() && !fhirObservation.getInterpretation().isEmpty()) {
            observation.setInterpretation(fhirObservation.getInterpretationFirstRep().getCodingFirstRep().getCode());
        }

        // Body site
        if (fhirObservation.hasBodySite() && fhirObservation.getBodySite().hasCoding()) {
            observation.setBodySite(fhirObservation.getBodySite().getCodingFirstRep().getDisplay());
        }

        // Method
        if (fhirObservation.hasMethod() && fhirObservation.getMethod().hasCoding()) {
            observation.setMethod(fhirObservation.getMethod().getCodingFirstRep().getDisplay());
        }

        // Performer
        if (fhirObservation.hasPerformer() && !fhirObservation.getPerformer().isEmpty()) {
            observation.setPerformerReference(fhirObservation.getPerformerFirstRep().getReference());
        }

        // Note
        if (fhirObservation.hasNote() && !fhirObservation.getNote().isEmpty()) {
            observation.setNote(fhirObservation.getNoteFirstRep().getText());
        }

        observation.setContentHash(generateContentHash(fhirObservation));
        return observation;
    }

    // ==================== Sync All ====================

    /**
     * Sync all clinical data for a patient
     */
    @Transactional
    public ClinicalDataSyncResult syncAllClinicalData(String patientId, UUID tenantId) {
        log.info("Syncing all clinical data for patient: {} (tenant: {})", patientId, tenantId);

        int conditions = syncConditions(patientId, tenantId);
        int procedures = syncProcedures(patientId, tenantId);
        int medications = syncMedicationRequests(patientId, tenantId);
        int observations = syncObservations(patientId, tenantId);

        ClinicalDataSyncResult result = ClinicalDataSyncResult.builder()
                .patientId(patientId)
                .tenantId(tenantId)
                .conditionsSynced(conditions)
                .proceduresSynced(procedures)
                .medicationRequestsSynced(medications)
                .observationsSynced(observations)
                .syncedAt(LocalDateTime.now())
                .build();

        log.info("Clinical data sync complete: {}", result);
        return result;
    }

    // ==================== Utility Methods ====================

    private String generateContentHash(Resource resource) {
        try {
            String content = fhirContext.newJsonParser().encodeResourceToString(resource);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to generate content hash: {}", e.getMessage());
            return null;
        }
    }

    private LocalDate toLocalDate(Date date) {
        if (date == null) return null;
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private LocalDateTime toLocalDateTime(Date date) {
        if (date == null) return null;
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     * Clinical Data Sync Result DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ClinicalDataSyncResult {
        private String patientId;
        private UUID tenantId;
        private int conditionsSynced;
        private int proceduresSynced;
        private int medicationRequestsSynced;
        private int observationsSynced;
        private LocalDateTime syncedAt;

        public int getTotalSynced() {
            return conditionsSynced + proceduresSynced + medicationRequestsSynced + observationsSynced;
        }
    }
}
