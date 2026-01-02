package com.healthdata.fhir.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.healthdata.fhir.domain.Condition;
import com.healthdata.fhir.domain.MedicationRequest;
import com.healthdata.fhir.domain.Observation;
import com.healthdata.fhir.repository.ConditionRepository;
import com.healthdata.fhir.repository.MedicationRequestRepository;
import com.healthdata.fhir.repository.ObservationRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * FHIR Integration Service - Comprehensive FHIR resource management
 *
 * Provides:
 * - Import/Export of FHIR resources (Observations, Conditions, Medications)
 * - FHIR resource validation
 * - Code system transformations (LOINC, SNOMED, RxNorm)
 * - Batch import capabilities
 * - FHIR Bundle format support
 *
 * Spring Boot 3.3.5 compatible with zero compilation errors
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class FhirIntegrationService {

    private final ObservationRepository observationRepository;
    private final ConditionRepository conditionRepository;
    private final MedicationRequestRepository medicationRepository;
    private final ObjectMapper objectMapper;

    /**
     * Code system constants for FHIR standards
     */
    public static final String LOINC_SYSTEM = "http://loinc.org";
    public static final String SNOMED_SYSTEM = "http://snomed.info/sct";
    public static final String RXNORM_SYSTEM = "http://www.nlm.nih.gov/research/umls/rxnorm";
    public static final String ICD10_SYSTEM = "http://hl7.org/fhir/sid/icd-10-cm";

    // ==================== FHIR IMPORT METHODS ====================

    /**
     * Import observations for a patient
     *
     * @param patientId Patient identifier
     * @param observations List of FHIR observation objects (as maps)
     * @param tenantId Tenant identifier
     * @return List of persisted Observation entities
     */
    public List<Observation> importObservations(String patientId, List<Map<String, Object>> observations, String tenantId) {
        log.info("Importing {} observations for patient: {}", observations.size(), patientId);

        return observations.stream()
            .map(obsMap -> {
                ValidationResult validation = validateObservation(obsMap);
                if (!validation.isValid()) {
                    log.warn("Skipping invalid observation: {}", validation.getErrors());
                    return null;
                }

                Observation obs = mapToObservationEntity(obsMap, patientId, tenantId);
                return observationRepository.save(obs);
            })
            .filter(obs -> obs != null)
            .collect(Collectors.toList());
    }

    /**
     * Import conditions for a patient
     *
     * @param patientId Patient identifier
     * @param conditions List of FHIR condition objects (as maps)
     * @param tenantId Tenant identifier
     * @return List of persisted Condition entities
     */
    public List<Condition> importConditions(String patientId, List<Map<String, Object>> conditions, String tenantId) {
        log.info("Importing {} conditions for patient: {}", conditions.size(), patientId);

        return conditions.stream()
            .map(condMap -> {
                ValidationResult validation = validateCondition(condMap);
                if (!validation.isValid()) {
                    log.warn("Skipping invalid condition: {}", validation.getErrors());
                    return null;
                }

                Condition condition = mapToConditionEntity(condMap, patientId, tenantId);
                return conditionRepository.save(condition);
            })
            .filter(cond -> cond != null)
            .collect(Collectors.toList());
    }

    /**
     * Import medications for a patient
     *
     * @param patientId Patient identifier
     * @param medications List of FHIR medication request objects (as maps)
     * @param tenantId Tenant identifier
     * @return List of persisted MedicationRequest entities
     */
    public List<MedicationRequest> importMedications(String patientId, List<Map<String, Object>> medications, String tenantId) {
        log.info("Importing {} medications for patient: {}", medications.size(), patientId);

        return medications.stream()
            .map(medMap -> {
                ValidationResult validation = validateMedication(medMap);
                if (!validation.isValid()) {
                    log.warn("Skipping invalid medication: {}", validation.getErrors());
                    return null;
                }

                MedicationRequest med = mapToMedicationEntity(medMap, patientId, tenantId);
                return medicationRepository.save(med);
            })
            .filter(med -> med != null)
            .collect(Collectors.toList());
    }

    // ==================== FHIR EXPORT METHODS ====================

    /**
     * Export all FHIR resources for a patient as a Bundle
     *
     * @param patientId Patient identifier
     * @param tenantId Tenant identifier
     * @return FhirBundle containing all patient resources
     */
    @Transactional(readOnly = true)
    public FhirBundle exportPatientResources(String patientId, String tenantId) {
        log.info("Exporting FHIR resources for patient: {}", patientId);

        List<Observation> observations = observationRepository.findByPatientId(patientId);
        List<Condition> conditions = conditionRepository.findByPatientId(patientId);
        List<MedicationRequest> medications = medicationRepository.findByPatientId(patientId);

        List<Map<String, Object>> entries = new ArrayList<>();

        // Add observations to bundle
        observations.forEach(obs -> {
            Map<String, Object> entry = mapObservationToFhir(obs);
            entries.add(entry);
        });

        // Add conditions to bundle
        conditions.forEach(cond -> {
            Map<String, Object> entry = mapConditionToFhir(cond);
            entries.add(entry);
        });

        // Add medications to bundle
        medications.forEach(med -> {
            Map<String, Object> entry = mapMedicationToFhir(med);
            entries.add(entry);
        });

        return FhirBundle.builder()
            .resourceType("Bundle")
            .type("searchset")
            .total(entries.size())
            .entries(entries)
            .timestamp(LocalDateTime.now())
            .build();
    }

    /**
     * Batch import FHIR Bundle
     *
     * @param tenantId Tenant identifier
     * @param fhirBundle FHIR Bundle object
     * @return BundleResult with import statistics
     */
    public BundleResult batchImport(String tenantId, FhirBundle fhirBundle) {
        log.info("Batch importing FHIR bundle with {} entries for tenant: {}", fhirBundle.getTotal(), tenantId);

        BundleResult result = BundleResult.builder()
            .bundleId(java.util.UUID.randomUUID().toString())
            .tenantId(tenantId)
            .totalEntries(fhirBundle.getTotal())
            .importedAt(LocalDateTime.now())
            .build();

        int observationCount = 0;
        int conditionCount = 0;
        int medicationCount = 0;
        int errorCount = 0;

        for (Map<String, Object> entry : fhirBundle.getEntries()) {
            String resourceType = (String) entry.get("resourceType");

            try {
                if ("Observation".equals(resourceType)) {
                    ValidationResult validation = validateObservation(entry);
                    if (validation.isValid()) {
                        observationRepository.save(mapToObservationEntity(entry, null, tenantId));
                        observationCount++;
                    } else {
                        errorCount++;
                        result.addError("Observation", validation.getErrors().toString());
                    }
                } else if ("Condition".equals(resourceType)) {
                    ValidationResult validation = validateCondition(entry);
                    if (validation.isValid()) {
                        conditionRepository.save(mapToConditionEntity(entry, null, tenantId));
                        conditionCount++;
                    } else {
                        errorCount++;
                        result.addError("Condition", validation.getErrors().toString());
                    }
                } else if ("MedicationRequest".equals(resourceType)) {
                    ValidationResult validation = validateMedication(entry);
                    if (validation.isValid()) {
                        medicationRepository.save(mapToMedicationEntity(entry, null, tenantId));
                        medicationCount++;
                    } else {
                        errorCount++;
                        result.addError("MedicationRequest", validation.getErrors().toString());
                    }
                }
            } catch (Exception e) {
                errorCount++;
                result.addError(resourceType, e.getMessage());
                log.error("Error processing bundle entry of type {}", resourceType, e);
            }
        }

        result.setObservationsImported(observationCount);
        result.setConditionsImported(conditionCount);
        result.setMedicationsImported(medicationCount);
        result.setErrorCount(errorCount);
        result.setSuccessCount(observationCount + conditionCount + medicationCount);

        log.info("Batch import complete: {} observations, {} conditions, {} medications, {} errors",
            observationCount, conditionCount, medicationCount, errorCount);

        return result;
    }

    // ==================== FHIR VALIDATION METHODS ====================

    /**
     * Validate FHIR Observation
     *
     * @param observation FHIR observation as map
     * @return ValidationResult with validity status and error messages
     */
    public ValidationResult validateObservation(Map<String, Object> observation) {
        ValidationResult result = ValidationResult.builder().valid(true).build();
        List<String> errors = new ArrayList<>();

        // Check resource type
        if (!"Observation".equals(observation.get("resourceType"))) {
            errors.add("Invalid resourceType - expected 'Observation'");
        }

        // Check required fields
        if (observation.get("code") == null) {
            errors.add("Missing required field: code");
        }

        if (observation.get("status") == null) {
            errors.add("Missing required field: status");
        }

        // Validate code system and value
        Map<String, Object> code = (Map<String, Object>) observation.get("code");
        if (code != null) {
            if (code.get("coding") == null && code.get("text") == null) {
                errors.add("Code must have either 'coding' or 'text'");
            }
        }

        if (!errors.isEmpty()) {
            result.setValid(false);
            result.setErrors(errors);
        }

        return result;
    }

    /**
     * Validate FHIR Condition
     *
     * @param condition FHIR condition as map
     * @return ValidationResult with validity status and error messages
     */
    public ValidationResult validateCondition(Map<String, Object> condition) {
        ValidationResult result = ValidationResult.builder().valid(true).build();
        List<String> errors = new ArrayList<>();

        // Check resource type
        if (!"Condition".equals(condition.get("resourceType"))) {
            errors.add("Invalid resourceType - expected 'Condition'");
        }

        // Check required fields
        if (condition.get("code") == null) {
            errors.add("Missing required field: code");
        }

        if (condition.get("subject") == null) {
            errors.add("Missing required field: subject (patient reference)");
        }

        // Validate clinical status if present
        String clinicalStatus = (String) condition.get("clinicalStatus");
        if (clinicalStatus != null) {
            List<String> validStatuses = List.of("active", "recurrence", "relapse", "inactive", "remission", "resolved");
            if (!validStatuses.contains(clinicalStatus)) {
                errors.add("Invalid clinicalStatus: " + clinicalStatus);
            }
        }

        if (!errors.isEmpty()) {
            result.setValid(false);
            result.setErrors(errors);
        }

        return result;
    }

    /**
     * Validate FHIR MedicationRequest
     *
     * @param medication FHIR medication request as map
     * @return ValidationResult with validity status and error messages
     */
    public ValidationResult validateMedication(Map<String, Object> medication) {
        ValidationResult result = ValidationResult.builder().valid(true).build();
        List<String> errors = new ArrayList<>();

        // Check resource type
        if (!"MedicationRequest".equals(medication.get("resourceType"))) {
            errors.add("Invalid resourceType - expected 'MedicationRequest'");
        }

        // Check required fields
        if (medication.get("status") == null) {
            errors.add("Missing required field: status");
        }

        if (medication.get("intent") == null) {
            errors.add("Missing required field: intent");
        }

        if (medication.get("subject") == null) {
            errors.add("Missing required field: subject (patient reference)");
        }

        // Validate status
        String status = (String) medication.get("status");
        if (status != null) {
            List<String> validStatuses = List.of("active", "on-hold", "cancelled", "completed",
                "entered-in-error", "stopped", "draft");
            if (!validStatuses.contains(status)) {
                errors.add("Invalid status: " + status);
            }
        }

        if (!errors.isEmpty()) {
            result.setValid(false);
            result.setErrors(errors);
        }

        return result;
    }

    /**
     * Validate FHIR Bundle
     *
     * @param bundle FHIR bundle object
     * @return ValidationResult with validity status and error messages
     */
    public ValidationResult validateBundle(FhirBundle bundle) {
        ValidationResult result = ValidationResult.builder().valid(true).build();
        List<String> errors = new ArrayList<>();

        // Check bundle type
        if (!"Bundle".equals(bundle.getResourceType())) {
            errors.add("Invalid resourceType - expected 'Bundle'");
        }

        // Check entries
        if (bundle.getEntries() == null || bundle.getEntries().isEmpty()) {
            errors.add("Bundle must contain at least one entry");
        } else {
            // Validate each entry
            for (Map<String, Object> entry : bundle.getEntries()) {
                String resourceType = (String) entry.get("resourceType");
                ValidationResult entryValidation = null;

                if ("Observation".equals(resourceType)) {
                    entryValidation = validateObservation(entry);
                } else if ("Condition".equals(resourceType)) {
                    entryValidation = validateCondition(entry);
                } else if ("MedicationRequest".equals(resourceType)) {
                    entryValidation = validateMedication(entry);
                }

                if (entryValidation != null && !entryValidation.isValid()) {
                    errors.add("Entry validation failed for " + resourceType + ": " + entryValidation.getErrors());
                }
            }
        }

        if (!errors.isEmpty()) {
            result.setValid(false);
            result.setErrors(errors);
        }

        return result;
    }

    // ==================== CODE SYSTEM TRANSFORMATIONS ====================

    /**
     * Map LOINC code to standard observation
     *
     * @param loincCode LOINC code
     * @return Mapping information including display name and category
     */
    public CodeSystemMapping mapLoincCode(String loincCode) {
        Map<String, CodeSystemMapping> loincMappings = getLOINCMappings();
        return loincMappings.getOrDefault(loincCode,
            CodeSystemMapping.builder()
                .code(loincCode)
                .system(LOINC_SYSTEM)
                .display("Unknown LOINC code: " + loincCode)
                .category("laboratory")
                .build());
    }

    /**
     * Map SNOMED code to standard condition
     *
     * @param snomedCode SNOMED code
     * @return Mapping information including display name
     */
    public CodeSystemMapping mapSnomedCode(String snomedCode) {
        Map<String, CodeSystemMapping> snomedMappings = getSNOMEDMappings();
        return snomedMappings.getOrDefault(snomedCode,
            CodeSystemMapping.builder()
                .code(snomedCode)
                .system(SNOMED_SYSTEM)
                .display("Unknown SNOMED code: " + snomedCode)
                .category("condition")
                .build());
    }

    /**
     * Map RxNorm code to standard medication
     *
     * @param rxnormCode RxNorm code
     * @return Mapping information including display name
     */
    public CodeSystemMapping mapRxNormCode(String rxnormCode) {
        Map<String, CodeSystemMapping> rxnormMappings = getRxNormMappings();
        return rxnormMappings.getOrDefault(rxnormCode,
            CodeSystemMapping.builder()
                .code(rxnormCode)
                .system(RXNORM_SYSTEM)
                .display("Unknown RxNorm code: " + rxnormCode)
                .category("medication")
                .build());
    }

    /**
     * Map custom code system
     *
     * @param system Code system URI
     * @param code Code value
     * @return Mapping information
     */
    public CodeSystemMapping mapCustomCodeSystem(String system, String code) {
        log.debug("Mapping custom code system: {} - {}", system, code);
        return CodeSystemMapping.builder()
            .code(code)
            .system(system)
            .display("Custom code: " + code)
            .category("custom")
            .build();
    }

    // ==================== ENTITY MAPPING METHODS ====================

    /**
     * Convert FHIR Observation to Observation entity
     *
     * @param obsMap FHIR observation as map
     * @param patientId Patient identifier
     * @param tenantId Tenant identifier
     * @return Observation entity
     */
    private Observation mapToObservationEntity(Map<String, Object> obsMap, String patientId, String tenantId) {
        Observation obs = Observation.builder()
            .patientId(patientId != null ? patientId : extractPatientId(obsMap))
            .tenantId(tenantId)
            .build();

        // Extract code
        Map<String, Object> code = (Map<String, Object>) obsMap.get("code");
        if (code != null) {
            List<Map<String, Object>> codings = (List<Map<String, Object>>) code.get("coding");
            if (codings != null && !codings.isEmpty()) {
                Map<String, Object> firstCoding = codings.get(0);
                obs.setCode((String) firstCoding.get("code"));
                obs.setSystem((String) firstCoding.get("system"));
                obs.setDisplay((String) firstCoding.get("display"));
            }
            obs.setDisplay((String) code.get("text"));
        }

        // Extract status
        obs.setStatus((String) obsMap.get("status"));

        // Extract category
        List<Map<String, Object>> categories = (List<Map<String, Object>>) obsMap.get("category");
        if (categories != null && !categories.isEmpty()) {
            List<Map<String, Object>> codings = (List<Map<String, Object>>) categories.get(0).get("coding");
            if (codings != null && !codings.isEmpty()) {
                obs.setCategory((String) codings.get(0).get("code"));
            }
        }

        // Extract value
        Map<String, Object> valueQuantity = (Map<String, Object>) obsMap.get("valueQuantity");
        if (valueQuantity != null) {
            Object value = valueQuantity.get("value");
            if (value instanceof Number) {
                obs.setValueQuantity(new BigDecimal(value.toString()));
            }
            obs.setValueUnit((String) valueQuantity.get("unit"));
        } else {
            Object valueString = obsMap.get("valueString");
            if (valueString != null) {
                obs.setValueString(valueString.toString());
            }
        }

        // Extract effective date
        String effectiveDateTime = (String) obsMap.get("effectiveDateTime");
        if (effectiveDateTime != null) {
            obs.setEffectiveDate(parseDateTime(effectiveDateTime));
        }

        // Store original FHIR JSON
        try {
            obs.setFhirResource(objectMapper.writeValueAsString(obsMap));
        } catch (Exception e) {
            log.warn("Failed to serialize FHIR observation", e);
        }

        return obs;
    }

    /**
     * Convert FHIR Condition to Condition entity
     *
     * @param condMap FHIR condition as map
     * @param patientId Patient identifier
     * @param tenantId Tenant identifier
     * @return Condition entity
     */
    private Condition mapToConditionEntity(Map<String, Object> condMap, String patientId, String tenantId) {
        Condition cond = Condition.builder()
            .patientId(patientId != null ? patientId : extractPatientId(condMap))
            .tenantId(tenantId)
            .build();

        // Extract code
        Map<String, Object> code = (Map<String, Object>) condMap.get("code");
        if (code != null) {
            List<Map<String, Object>> codings = (List<Map<String, Object>>) code.get("coding");
            if (codings != null && !codings.isEmpty()) {
                Map<String, Object> firstCoding = codings.get(0);
                cond.setCode((String) firstCoding.get("code"));
                cond.setDisplay((String) firstCoding.get("display"));
            }
        }

        // Extract status fields
        cond.setClinicalStatus((String) condMap.get("clinicalStatus"));
        cond.setVerificationStatus((String) condMap.get("verificationStatus"));

        // Extract category
        List<Map<String, Object>> categories = (List<Map<String, Object>>) condMap.get("category");
        if (categories != null && !categories.isEmpty()) {
            List<Map<String, Object>> codings = (List<Map<String, Object>>) categories.get(0).get("coding");
            if (codings != null && !codings.isEmpty()) {
                cond.setCategory((String) codings.get(0).get("code"));
            }
        }

        // Extract severity
        Map<String, Object> severity = (Map<String, Object>) condMap.get("severity");
        if (severity != null) {
            List<Map<String, Object>> codings = (List<Map<String, Object>>) severity.get("coding");
            if (codings != null && !codings.isEmpty()) {
                cond.setSeverity((String) codings.get(0).get("code"));
            }
        }

        // Extract dates
        String onsetDateTime = (String) condMap.get("onsetDateTime");
        if (onsetDateTime != null) {
            cond.setOnsetDate(parseDateTime(onsetDateTime));
        }

        String recordedDate = (String) condMap.get("recordedDate");
        if (recordedDate != null) {
            cond.setRecordedDate(parseDateTime(recordedDate));
        }

        String abatementDateTime = (String) condMap.get("abatementDateTime");
        if (abatementDateTime != null) {
            cond.setAbatementDate(parseDateTime(abatementDateTime));
        }

        return cond;
    }

    /**
     * Convert FHIR MedicationRequest to MedicationRequest entity
     *
     * @param medMap FHIR medication request as map
     * @param patientId Patient identifier
     * @param tenantId Tenant identifier
     * @return MedicationRequest entity
     */
    private MedicationRequest mapToMedicationEntity(Map<String, Object> medMap, String patientId, String tenantId) {
        MedicationRequest med = MedicationRequest.builder()
            .patientId(patientId != null ? patientId : extractPatientId(medMap))
            .tenantId(tenantId)
            .build();

        // Extract medication code
        Map<String, Object> medicationCodeableConcept = (Map<String, Object>) medMap.get("medicationCodeableConcept");
        if (medicationCodeableConcept != null) {
            List<Map<String, Object>> codings = (List<Map<String, Object>>) medicationCodeableConcept.get("coding");
            if (codings != null && !codings.isEmpty()) {
                Map<String, Object> firstCoding = codings.get(0);
                med.setMedicationCode((String) firstCoding.get("code"));
                med.setMedicationDisplay((String) firstCoding.get("display"));
            }
        }

        // Extract status and intent
        med.setStatus((String) medMap.get("status"));
        med.setIntent((String) medMap.get("intent"));
        med.setPriority((String) medMap.get("priority"));

        // Extract dates
        String authoredOn = (String) medMap.get("authoredOn");
        if (authoredOn != null) {
            med.setAuthoredOn(parseDateTime(authoredOn));
        }

        // Extract dosage
        List<Map<String, Object>> dosageInstructions = (List<Map<String, Object>>) medMap.get("dosageInstruction");
        if (dosageInstructions != null && !dosageInstructions.isEmpty()) {
            Map<String, Object> dosage = dosageInstructions.get(0);
            med.setDosageInstruction((String) dosage.get("text"));

            Map<String, Object> timing = (Map<String, Object>) dosage.get("timing");
            if (timing != null) {
                med.setDosageTiming((String) timing.get("code"));
            }

            Map<String, Object> doseQuantity = (Map<String, Object>) dosage.get("doseQuantity");
            if (doseQuantity != null) {
                Object value = doseQuantity.get("value");
                if (value instanceof Number) {
                    med.setDosageQuantity(((Number) value).doubleValue());
                }
                med.setDosageUnit((String) doseQuantity.get("unit"));
            }
        }

        // Extract dispense request
        Map<String, Object> dispenseRequest = (Map<String, Object>) medMap.get("dispenseRequest");
        if (dispenseRequest != null) {
            Object daysSupply = dispenseRequest.get("daysSupply");
            if (daysSupply instanceof Number) {
                med.setDaysSupply(((Number) daysSupply).intValue());
            }

            Object quantity = dispenseRequest.get("quantity");
            if (quantity instanceof Map) {
                Map<String, Object> quantityMap = (Map<String, Object>) quantity;
                Object value = quantityMap.get("value");
                if (value instanceof Number) {
                    med.setDispenseQuantity(((Number) value).intValue());
                }
                med.setDispenseUnit((String) quantityMap.get("unit"));
            }
        }

        return med;
    }

    /**
     * Convert Observation entity to FHIR representation
     *
     * @param obs Observation entity
     * @return Map representing FHIR Observation
     */
    private Map<String, Object> mapObservationToFhir(Observation obs) {
        Map<String, Object> fhir = new HashMap<>();
        fhir.put("resourceType", "Observation");
        fhir.put("id", obs.getId());
        fhir.put("status", obs.getStatus());

        // Add code
        Map<String, Object> code = new HashMap<>();
        List<Map<String, Object>> codings = new ArrayList<>();
        Map<String, Object> coding = new HashMap<>();
        coding.put("system", obs.getSystem() != null ? obs.getSystem() : LOINC_SYSTEM);
        coding.put("code", obs.getCode());
        coding.put("display", obs.getDisplay());
        codings.add(coding);
        code.put("coding", codings);
        fhir.put("code", code);

        // Add subject (patient)
        Map<String, Object> subject = new HashMap<>();
        subject.put("reference", "Patient/" + obs.getPatientId());
        fhir.put("subject", subject);

        // Add effective date
        if (obs.getEffectiveDate() != null) {
            fhir.put("effectiveDateTime", obs.getEffectiveDate().format(DateTimeFormatter.ISO_DATE_TIME));
        }

        // Add category
        if (obs.getCategory() != null) {
            List<Map<String, Object>> categories = new ArrayList<>();
            Map<String, Object> category = new HashMap<>();
            List<Map<String, Object>> categoryCodes = new ArrayList<>();
            Map<String, Object> categoryCode = new HashMap<>();
            categoryCode.put("code", obs.getCategory());
            categoryCodes.add(categoryCode);
            category.put("coding", categoryCodes);
            categories.add(category);
            fhir.put("category", categories);
        }

        // Add value
        if (obs.getValueQuantity() != null) {
            Map<String, Object> valueQuantity = new HashMap<>();
            valueQuantity.put("value", obs.getValueQuantity());
            valueQuantity.put("unit", obs.getValueUnit());
            fhir.put("valueQuantity", valueQuantity);
        } else if (obs.getValueString() != null) {
            fhir.put("valueString", obs.getValueString());
        }

        return fhir;
    }

    /**
     * Convert Condition entity to FHIR representation
     *
     * @param cond Condition entity
     * @return Map representing FHIR Condition
     */
    private Map<String, Object> mapConditionToFhir(Condition cond) {
        Map<String, Object> fhir = new HashMap<>();
        fhir.put("resourceType", "Condition");
        fhir.put("id", cond.getId());
        fhir.put("clinicalStatus", cond.getClinicalStatus());
        fhir.put("verificationStatus", cond.getVerificationStatus());

        // Add code
        Map<String, Object> code = new HashMap<>();
        List<Map<String, Object>> codings = new ArrayList<>();
        Map<String, Object> coding = new HashMap<>();
        coding.put("system", SNOMED_SYSTEM);
        coding.put("code", cond.getCode());
        coding.put("display", cond.getDisplay());
        codings.add(coding);
        code.put("coding", codings);
        fhir.put("code", code);

        // Add subject (patient)
        Map<String, Object> subject = new HashMap<>();
        subject.put("reference", "Patient/" + cond.getPatientId());
        fhir.put("subject", subject);

        // Add category
        if (cond.getCategory() != null) {
            List<Map<String, Object>> categories = new ArrayList<>();
            Map<String, Object> category = new HashMap<>();
            List<Map<String, Object>> categoryCodes = new ArrayList<>();
            Map<String, Object> categoryCode = new HashMap<>();
            categoryCode.put("code", cond.getCategory());
            categoryCodes.add(categoryCode);
            category.put("coding", categoryCodes);
            categories.add(category);
            fhir.put("category", categories);
        }

        // Add severity
        if (cond.getSeverity() != null) {
            Map<String, Object> severity = new HashMap<>();
            List<Map<String, Object>> severityCodes = new ArrayList<>();
            Map<String, Object> severityCode = new HashMap<>();
            severityCode.put("code", cond.getSeverity());
            severityCodes.add(severityCode);
            severity.put("coding", severityCodes);
            fhir.put("severity", severity);
        }

        // Add dates
        if (cond.getOnsetDate() != null) {
            fhir.put("onsetDateTime", cond.getOnsetDate().format(DateTimeFormatter.ISO_DATE_TIME));
        }
        if (cond.getRecordedDate() != null) {
            fhir.put("recordedDate", cond.getRecordedDate().format(DateTimeFormatter.ISO_DATE_TIME));
        }
        if (cond.getAbatementDate() != null) {
            fhir.put("abatementDateTime", cond.getAbatementDate().format(DateTimeFormatter.ISO_DATE_TIME));
        }

        return fhir;
    }

    /**
     * Convert MedicationRequest entity to FHIR representation
     *
     * @param med MedicationRequest entity
     * @return Map representing FHIR MedicationRequest
     */
    private Map<String, Object> mapMedicationToFhir(MedicationRequest med) {
        Map<String, Object> fhir = new HashMap<>();
        fhir.put("resourceType", "MedicationRequest");
        fhir.put("id", med.getId());
        fhir.put("status", med.getStatus());
        fhir.put("intent", med.getIntent());

        if (med.getPriority() != null) {
            fhir.put("priority", med.getPriority());
        }

        // Add medication
        Map<String, Object> medication = new HashMap<>();
        List<Map<String, Object>> codings = new ArrayList<>();
        Map<String, Object> coding = new HashMap<>();
        coding.put("system", RXNORM_SYSTEM);
        coding.put("code", med.getMedicationCode());
        coding.put("display", med.getMedicationDisplay());
        codings.add(coding);
        medication.put("coding", codings);
        fhir.put("medicationCodeableConcept", medication);

        // Add subject (patient)
        Map<String, Object> subject = new HashMap<>();
        subject.put("reference", "Patient/" + med.getPatientId());
        fhir.put("subject", subject);

        // Add authored on date
        if (med.getAuthoredOn() != null) {
            fhir.put("authoredOn", med.getAuthoredOn().format(DateTimeFormatter.ISO_DATE_TIME));
        }

        // Add dosage instruction
        if (med.getDosageInstruction() != null) {
            List<Map<String, Object>> dosageInstructions = new ArrayList<>();
            Map<String, Object> dosage = new HashMap<>();
            dosage.put("text", med.getDosageInstruction());

            if (med.getDosageTiming() != null || med.getDosageQuantity() != null) {
                if (med.getDosageQuantity() != null) {
                    Map<String, Object> doseQuantity = new HashMap<>();
                    doseQuantity.put("value", med.getDosageQuantity());
                    doseQuantity.put("unit", med.getDosageUnit());
                    dosage.put("doseQuantity", doseQuantity);
                }
                if (med.getDosageTiming() != null) {
                    Map<String, Object> timing = new HashMap<>();
                    timing.put("code", med.getDosageTiming());
                    dosage.put("timing", timing);
                }
            }

            dosageInstructions.add(dosage);
            fhir.put("dosageInstruction", dosageInstructions);
        }

        return fhir;
    }

    // ==================== HELPER METHODS ====================

    /**
     * Extract patient ID from FHIR resource
     *
     * @param resource FHIR resource as map
     * @return Patient ID or null
     */
    private String extractPatientId(Map<String, Object> resource) {
        Map<String, Object> subject = (Map<String, Object>) resource.get("subject");
        if (subject != null) {
            String reference = (String) subject.get("reference");
            if (reference != null && reference.contains("/")) {
                return reference.split("/")[1];
            }
        }
        return null;
    }

    /**
     * Parse ISO date time string
     *
     * @param dateTimeStr ISO date time string
     * @return LocalDateTime or null if parsing fails
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        try {
            if (dateTimeStr == null) return null;
            // Handle ISO 8601 format
            if (dateTimeStr.length() == 10) {
                // Date only format (YYYY-MM-DD)
                return LocalDateTime.of(
                    java.time.LocalDate.parse(dateTimeStr).getYear(),
                    java.time.LocalDate.parse(dateTimeStr).getMonthValue(),
                    java.time.LocalDate.parse(dateTimeStr).getDayOfMonth(),
                    0, 0, 0
                );
            } else {
                // DateTime format
                return LocalDateTime.parse(dateTimeStr.replace("Z", "").split("\\+")[0]);
            }
        } catch (Exception e) {
            log.warn("Failed to parse date: {}", dateTimeStr, e);
            return null;
        }
    }

    /**
     * Get LOINC code mappings
     *
     * @return Map of LOINC codes to mappings
     */
    private Map<String, CodeSystemMapping> getLOINCMappings() {
        Map<String, CodeSystemMapping> mappings = new HashMap<>();

        // Vital signs
        mappings.put("85354-9", CodeSystemMapping.builder()
            .code("85354-9")
            .system(LOINC_SYSTEM)
            .display("Blood Pressure systolic and diastolic")
            .category("vital-signs")
            .build());

        mappings.put("8867-4", CodeSystemMapping.builder()
            .code("8867-4")
            .system(LOINC_SYSTEM)
            .display("Heart rate")
            .category("vital-signs")
            .build());

        mappings.put("8310-5", CodeSystemMapping.builder()
            .code("8310-5")
            .system(LOINC_SYSTEM)
            .display("Body temperature")
            .category("vital-signs")
            .build());

        // Laboratory
        mappings.put("2345-7", CodeSystemMapping.builder()
            .code("2345-7")
            .system(LOINC_SYSTEM)
            .display("Glucose [Mass/volume] in Serum or Plasma")
            .category("laboratory")
            .build());

        mappings.put("4548-4", CodeSystemMapping.builder()
            .code("4548-4")
            .system(LOINC_SYSTEM)
            .display("Hemoglobin A1c/Hemoglobin.total in Blood")
            .category("laboratory")
            .build());

        return mappings;
    }

    /**
     * Get SNOMED code mappings
     *
     * @return Map of SNOMED codes to mappings
     */
    private Map<String, CodeSystemMapping> getSNOMEDMappings() {
        Map<String, CodeSystemMapping> mappings = new HashMap<>();

        // Common conditions
        mappings.put("44054006", CodeSystemMapping.builder()
            .code("44054006")
            .system(SNOMED_SYSTEM)
            .display("Diabetes mellitus")
            .category("condition")
            .build());

        mappings.put("38341003", CodeSystemMapping.builder()
            .code("38341003")
            .system(SNOMED_SYSTEM)
            .display("Hypertension")
            .category("condition")
            .build());

        mappings.put("13645005", CodeSystemMapping.builder()
            .code("13645005")
            .system(SNOMED_SYSTEM)
            .display("Chronic obstructive pulmonary disease")
            .category("condition")
            .build());

        return mappings;
    }

    /**
     * Get RxNorm code mappings
     *
     * @return Map of RxNorm codes to mappings
     */
    private Map<String, CodeSystemMapping> getRxNormMappings() {
        Map<String, CodeSystemMapping> mappings = new HashMap<>();

        // Common medications
        mappings.put("849574", CodeSystemMapping.builder()
            .code("849574")
            .system(RXNORM_SYSTEM)
            .display("Lisinopril 10 MG Oral Tablet")
            .category("medication")
            .build());

        mappings.put("1000001", CodeSystemMapping.builder()
            .code("1000001")
            .system(RXNORM_SYSTEM)
            .display("Metformin 500 MG Oral Tablet")
            .category("medication")
            .build());

        return mappings;
    }

    // ==================== DTO/MODEL CLASSES ====================

    /**
     * FHIR Bundle representation
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FhirBundle {
        private String resourceType;
        private String type;
        private Integer total;
        private List<Map<String, Object>> entries;
        private LocalDateTime timestamp;
    }

    /**
     * Bundle import result
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BundleResult {
        private String bundleId;
        private String tenantId;
        private Integer totalEntries;
        private Integer successCount;
        private Integer observationsImported;
        private Integer conditionsImported;
        private Integer medicationsImported;
        private Integer errorCount;
        private LocalDateTime importedAt;
        @lombok.Builder.Default
        private List<String> errors = new ArrayList<>();

        public void addError(String resourceType, String message) {
            errors.add(resourceType + ": " + message);
        }
    }

    /**
     * Code system mapping result
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CodeSystemMapping {
        private String code;
        private String system;
        private String display;
        private String category;
    }

    /**
     * Validation result
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ValidationResult {
        private boolean valid;
        @lombok.Builder.Default
        private List<String> errors = new ArrayList<>();
    }
}
