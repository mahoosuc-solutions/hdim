package com.healthdata.cdr.converter;

import com.healthdata.cdr.dto.CdaDocument;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Converter for transforming parsed CDA documents to FHIR R4 bundles.
 */
@Slf4j
@Component
public class CdaToFhirConverter {

    /**
     * Convert a parsed CDA document to a FHIR transaction bundle.
     *
     * @param cdaDocument Parsed CDA document
     * @return FHIR Bundle containing converted resources
     */
    public Bundle convertToFhir(CdaDocument cdaDocument) {
        if (cdaDocument == null) {
            log.warn("Cannot convert null CDA document to FHIR");
            Bundle bundle = new Bundle();
            bundle.setType(Bundle.BundleType.TRANSACTION);
            bundle.setTimestamp(new Date());
            return bundle;
        }

        log.debug("Converting CDA document to FHIR: type={}", cdaDocument.getDocumentType());

        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.TRANSACTION);
        bundle.setTimestamp(new Date());

        String patientId = null;

        try {
            // Convert patient first (needed for references)
            if (cdaDocument.getPatient() != null && !cdaDocument.getPatient().isEmpty()) {
                Patient patient = convertPatient(cdaDocument.getPatient());
                patientId = patient.getIdElement().getIdPart();
                addResourceToBundle(bundle, patient, "Patient");
            }

            // Convert problems to Conditions
            if (cdaDocument.getProblems() != null) {
                for (Map<String, Object> problem : cdaDocument.getProblems()) {
                    Condition condition = convertProblemToCondition(problem, patientId);
                    addResourceToBundle(bundle, condition, "Condition");
                }
            }

            // Convert medications to MedicationRequest
            if (cdaDocument.getMedications() != null) {
                for (Map<String, Object> medication : cdaDocument.getMedications()) {
                    MedicationRequest medRequest = convertMedicationToMedicationRequest(medication, patientId);
                    addResourceToBundle(bundle, medRequest, "MedicationRequest");
                }
            }

            // Convert allergies to AllergyIntolerance
            if (cdaDocument.getAllergies() != null) {
                for (Map<String, Object> allergy : cdaDocument.getAllergies()) {
                    AllergyIntolerance allergyIntolerance = convertAllergyToAllergyIntolerance(allergy, patientId);
                    addResourceToBundle(bundle, allergyIntolerance, "AllergyIntolerance");
                }
            }

            // Convert immunizations to Immunization
            if (cdaDocument.getImmunizations() != null) {
                for (Map<String, Object> immunization : cdaDocument.getImmunizations()) {
                    Immunization fhirImmunization = convertImmunizationToFhir(immunization, patientId);
                    addResourceToBundle(bundle, fhirImmunization, "Immunization");
                }
            }

            // Convert procedures to Procedure
            if (cdaDocument.getProcedures() != null) {
                for (Map<String, Object> procedure : cdaDocument.getProcedures()) {
                    Procedure fhirProcedure = convertProcedureToFhir(procedure, patientId);
                    addResourceToBundle(bundle, fhirProcedure, "Procedure");
                }
            }

            // Convert results to Observation
            if (cdaDocument.getResults() != null) {
                for (Map<String, Object> result : cdaDocument.getResults()) {
                    Observation observation = convertResultToObservation(result, patientId);
                    addResourceToBundle(bundle, observation, "Observation");
                }
            }

            // Convert vital signs to Observation
            if (cdaDocument.getVitalSigns() != null) {
                for (Map<String, Object> vitalSign : cdaDocument.getVitalSigns()) {
                    Observation observation = convertVitalSignToObservation(vitalSign, patientId);
                    addResourceToBundle(bundle, observation, "Observation");
                }
            }

            // Convert encounters to Encounter
            if (cdaDocument.getEncounters() != null) {
                for (Map<String, Object> encounter : cdaDocument.getEncounters()) {
                    Encounter fhirEncounter = convertEncounterToFhir(encounter, patientId);
                    addResourceToBundle(bundle, fhirEncounter, "Encounter");
                }
            }

            log.info("Converted CDA to FHIR bundle with {} entries", bundle.getEntry().size());

        } catch (Exception e) {
            log.error("Error converting CDA to FHIR: {}", e.getMessage(), e);
        }

        return bundle;
    }

    // Patient conversion
    private Patient convertPatient(Map<String, Object> patientData) {
        Patient patient = new Patient();
        patient.setId(UUID.randomUUID().toString());

        // Identifiers
        @SuppressWarnings("unchecked")
        List<Map<String, String>> identifiers = (List<Map<String, String>>) patientData.get("identifiers");
        if (identifiers != null) {
            for (Map<String, String> id : identifiers) {
                Identifier identifier = new Identifier();
                identifier.setSystem("urn:oid:" + id.get("root"));
                identifier.setValue(id.get("extension"));
                patient.addIdentifier(identifier);
            }
        }

        // Name
        @SuppressWarnings("unchecked")
        Map<String, Object> nameData = (Map<String, Object>) patientData.get("name");
        if (nameData != null) {
            HumanName name = new HumanName();
            name.setFamily((String) nameData.get("family"));

            @SuppressWarnings("unchecked")
            List<String> givenNames = (List<String>) nameData.get("given");
            if (givenNames != null) {
                for (String given : givenNames) {
                    name.addGiven(given);
                }
            }

            String prefix = (String) nameData.get("prefix");
            if (prefix != null) {
                name.addPrefix(prefix);
            }

            String suffix = (String) nameData.get("suffix");
            if (suffix != null) {
                name.addSuffix(suffix);
            }

            patient.addName(name);
        }

        // Gender
        String gender = (String) patientData.get("gender");
        if (gender != null) {
            patient.setGender(mapGender(gender));
        }

        // Birth date
        String birthTime = (String) patientData.get("birthTime");
        if (birthTime != null) {
            patient.setBirthDate(parseHL7Date(birthTime));
        }

        // Addresses
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> addresses = (List<Map<String, Object>>) patientData.get("addresses");
        if (addresses != null) {
            for (Map<String, Object> addrData : addresses) {
                Address address = convertAddress(addrData);
                patient.addAddress(address);
            }
        }

        // Telecoms
        @SuppressWarnings("unchecked")
        List<Map<String, String>> telecoms = (List<Map<String, String>>) patientData.get("telecoms");
        if (telecoms != null) {
            for (Map<String, String> telecomData : telecoms) {
                ContactPoint telecom = convertTelecom(telecomData);
                patient.addTelecom(telecom);
            }
        }

        // Marital status
        String maritalStatus = (String) patientData.get("maritalStatus");
        if (maritalStatus != null) {
            CodeableConcept marital = new CodeableConcept();
            marital.addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/v3-MaritalStatus")
                .setCode(maritalStatus)
                .setDisplay((String) patientData.get("maritalStatusDisplay"));
            patient.setMaritalStatus(marital);
        }

        // Race (US Core extension)
        String race = (String) patientData.get("race");
        if (race != null) {
            Extension raceExt = new Extension();
            raceExt.setUrl("http://hl7.org/fhir/us/core/StructureDefinition/us-core-race");
            Extension ombCategory = new Extension();
            ombCategory.setUrl("ombCategory");
            Coding raceCoding = new Coding();
            raceCoding.setSystem("urn:oid:2.16.840.1.113883.6.238");
            raceCoding.setCode(race);
            raceCoding.setDisplay((String) patientData.get("raceDisplay"));
            ombCategory.setValue(raceCoding);
            raceExt.addExtension(ombCategory);
            patient.addExtension(raceExt);
        }

        // Ethnicity (US Core extension)
        String ethnicity = (String) patientData.get("ethnicity");
        if (ethnicity != null) {
            Extension ethnicityExt = new Extension();
            ethnicityExt.setUrl("http://hl7.org/fhir/us/core/StructureDefinition/us-core-ethnicity");
            Extension ombCategory = new Extension();
            ombCategory.setUrl("ombCategory");
            Coding ethnicityCoding = new Coding();
            ethnicityCoding.setSystem("urn:oid:2.16.840.1.113883.6.238");
            ethnicityCoding.setCode(ethnicity);
            ethnicityCoding.setDisplay((String) patientData.get("ethnicityDisplay"));
            ombCategory.setValue(ethnicityCoding);
            ethnicityExt.addExtension(ombCategory);
            patient.addExtension(ethnicityExt);
        }

        // Language
        String language = (String) patientData.get("language");
        if (language != null) {
            Patient.PatientCommunicationComponent comm = new Patient.PatientCommunicationComponent();
            CodeableConcept langCode = new CodeableConcept();
            langCode.addCoding()
                .setSystem("urn:ietf:bcp:47")
                .setCode(language);
            comm.setLanguage(langCode);
            patient.addCommunication(comm);
        }

        return patient;
    }

    // Condition conversion
    private Condition convertProblemToCondition(Map<String, Object> problem, String patientId) {
        Condition condition = new Condition();
        condition.setId(UUID.randomUUID().toString());

        // Subject reference
        if (patientId != null) {
            condition.setSubject(new Reference("Patient/" + patientId));
        }

        // Code
        @SuppressWarnings("unchecked")
        Map<String, String> codeData = (Map<String, String>) problem.get("code");
        if (codeData != null) {
            CodeableConcept code = new CodeableConcept();
            Coding coding = code.addCoding();
            coding.setCode(codeData.get("code"));
            coding.setDisplay(codeData.get("displayName"));

            String codeSystemUri = (String) problem.get("codeSystemUri");
            if (codeSystemUri != null) {
                coding.setSystem(codeSystemUri);
            }
            condition.setCode(code);
        }

        // Clinical status
        String clinicalStatus = (String) problem.get("clinicalStatus");
        if (clinicalStatus != null) {
            CodeableConcept status = new CodeableConcept();
            status.addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/condition-clinical")
                .setCode(mapClinicalStatus(clinicalStatus));
            condition.setClinicalStatus(status);
        }

        // Verification status
        CodeableConcept verificationStatus = new CodeableConcept();
        verificationStatus.addCoding()
            .setSystem("http://terminology.hl7.org/CodeSystem/condition-ver-status")
            .setCode("confirmed");
        condition.setVerificationStatus(verificationStatus);

        // Onset
        String onsetDate = (String) problem.get("onsetDate");
        if (onsetDate != null) {
            condition.setOnset(new DateTimeType(parseHL7DateTime(onsetDate)));
        }

        // Abatement
        String abatementDate = (String) problem.get("abatementDate");
        if (abatementDate != null) {
            condition.setAbatement(new DateTimeType(parseHL7DateTime(abatementDate)));
        }

        // Severity
        String severity = (String) problem.get("severity");
        if (severity != null) {
            CodeableConcept severityCode = new CodeableConcept();
            severityCode.addCoding()
                .setSystem("http://snomed.info/sct")
                .setCode(severity)
                .setDisplay((String) problem.get("severityDisplay"));
            condition.setSeverity(severityCode);
        }

        return condition;
    }

    // MedicationRequest conversion
    private MedicationRequest convertMedicationToMedicationRequest(Map<String, Object> medication, String patientId) {
        MedicationRequest medRequest = new MedicationRequest();
        medRequest.setId(UUID.randomUUID().toString());

        // Subject reference
        if (patientId != null) {
            medRequest.setSubject(new Reference("Patient/" + patientId));
        }

        // Status
        String status = (String) medication.get("status");
        medRequest.setStatus(mapMedicationStatus(status));

        // Intent
        medRequest.setIntent(MedicationRequest.MedicationRequestIntent.ORDER);

        // Medication
        @SuppressWarnings("unchecked")
        Map<String, String> drugCode = (Map<String, String>) medication.get("drugCode");
        if (drugCode != null) {
            CodeableConcept medicationCode = new CodeableConcept();
            Coding coding = medicationCode.addCoding();
            coding.setCode(drugCode.get("code"));
            coding.setDisplay(drugCode.get("displayName"));

            String codeSystemUri = (String) medication.get("codeSystemUri");
            if (codeSystemUri != null) {
                coding.setSystem(codeSystemUri);
            }
            medRequest.setMedication(medicationCode);
        }

        // Dosage instruction
        Dosage dosage = new Dosage();

        // Route
        @SuppressWarnings("unchecked")
        Map<String, String> route = (Map<String, String>) medication.get("route");
        if (route != null) {
            CodeableConcept routeCode = new CodeableConcept();
            routeCode.addCoding()
                .setCode(route.get("code"))
                .setDisplay(route.get("displayName"));
            dosage.setRoute(routeCode);
        }

        // Dose quantity
        @SuppressWarnings("unchecked")
        Map<String, String> doseQuantity = (Map<String, String>) medication.get("doseQuantity");
        if (doseQuantity != null) {
            Dosage.DosageDoseAndRateComponent doseAndRate = new Dosage.DosageDoseAndRateComponent();
            Quantity dose = new Quantity();
            try {
                dose.setValue(new BigDecimal(doseQuantity.get("value")));
            } catch (Exception e) {
                // Ignore parse errors
            }
            dose.setUnit(doseQuantity.get("unit"));
            doseAndRate.setDose(dose);
            dosage.addDoseAndRate(doseAndRate);
        }

        // Frequency
        @SuppressWarnings("unchecked")
        Map<String, String> frequency = (Map<String, String>) medication.get("frequency");
        if (frequency != null) {
            Timing timing = new Timing();
            Timing.TimingRepeatComponent repeat = new Timing.TimingRepeatComponent();
            try {
                repeat.setPeriod(new BigDecimal(frequency.get("value")));
                repeat.setPeriodUnit(mapTimingUnit(frequency.get("unit")));
            } catch (Exception e) {
                // Ignore parse errors
            }
            timing.setRepeat(repeat);
            dosage.setTiming(timing);
        }

        // Instructions
        String instructions = (String) medication.get("instructions");
        if (instructions != null) {
            dosage.setText(instructions);
        }

        medRequest.addDosageInstruction(dosage);

        // Dispense request
        @SuppressWarnings("unchecked")
        Map<String, Object> supply = (Map<String, Object>) medication.get("supply");
        if (supply != null) {
            MedicationRequest.MedicationRequestDispenseRequestComponent dispenseRequest =
                new MedicationRequest.MedicationRequestDispenseRequestComponent();

            String quantity = (String) supply.get("quantity");
            if (quantity != null) {
                Quantity qty = new Quantity();
                try {
                    qty.setValue(new BigDecimal(quantity));
                } catch (Exception e) {
                    // Ignore
                }
                qty.setUnit((String) supply.get("quantityUnit"));
                dispenseRequest.setQuantity(qty);
            }

            String refills = (String) supply.get("refills");
            if (refills != null) {
                try {
                    dispenseRequest.setNumberOfRepeatsAllowed(Integer.parseInt(refills));
                } catch (Exception e) {
                    // Ignore
                }
            }

            medRequest.setDispenseRequest(dispenseRequest);
        }

        // Validity period
        String startDate = (String) medication.get("startDate");
        String endDate = (String) medication.get("endDate");
        if (startDate != null || endDate != null) {
            Period validityPeriod = new Period();
            if (startDate != null) {
                validityPeriod.setStart(parseHL7DateTime(startDate));
            }
            if (endDate != null) {
                validityPeriod.setEnd(parseHL7DateTime(endDate));
            }
            if (medRequest.getDispenseRequest() == null) {
                medRequest.setDispenseRequest(new MedicationRequest.MedicationRequestDispenseRequestComponent());
            }
            medRequest.getDispenseRequest().setValidityPeriod(validityPeriod);
        }

        return medRequest;
    }

    // AllergyIntolerance conversion
    private AllergyIntolerance convertAllergyToAllergyIntolerance(Map<String, Object> allergy, String patientId) {
        AllergyIntolerance allergyIntolerance = new AllergyIntolerance();
        allergyIntolerance.setId(UUID.randomUUID().toString());

        // Patient reference
        if (patientId != null) {
            allergyIntolerance.setPatient(new Reference("Patient/" + patientId));
        }

        // Clinical status
        String clinicalStatus = (String) allergy.get("clinicalStatus");
        if (clinicalStatus != null) {
            CodeableConcept status = new CodeableConcept();
            status.addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical")
                .setCode(mapAllergyClinicalStatus(clinicalStatus));
            allergyIntolerance.setClinicalStatus(status);
        }

        // Verification status
        CodeableConcept verificationStatus = new CodeableConcept();
        verificationStatus.addCoding()
            .setSystem("http://terminology.hl7.org/CodeSystem/allergyintolerance-verification")
            .setCode("confirmed");
        allergyIntolerance.setVerificationStatus(verificationStatus);

        // Type
        @SuppressWarnings("unchecked")
        Map<String, String> allergyType = (Map<String, String>) allergy.get("allergyType");
        if (allergyType != null) {
            String typeCode = allergyType.get("code");
            if (typeCode != null && typeCode.contains("allergy")) {
                allergyIntolerance.setType(AllergyIntolerance.AllergyIntoleranceType.ALLERGY);
            } else {
                allergyIntolerance.setType(AllergyIntolerance.AllergyIntoleranceType.INTOLERANCE);
            }
        }

        // Allergen code
        @SuppressWarnings("unchecked")
        Map<String, String> allergen = (Map<String, String>) allergy.get("allergen");
        if (allergen != null) {
            CodeableConcept allergenCode = new CodeableConcept();
            Coding coding = allergenCode.addCoding();
            coding.setCode(allergen.get("code"));
            coding.setDisplay(allergen.get("displayName"));

            String codeSystemUri = (String) allergy.get("allergenCodeSystemUri");
            if (codeSystemUri != null) {
                coding.setSystem(codeSystemUri);
            }
            allergyIntolerance.setCode(allergenCode);
        }

        // Criticality
        String criticality = (String) allergy.get("criticality");
        if (criticality != null) {
            allergyIntolerance.setCriticality(mapCriticality(criticality));
        }

        // Onset
        String onsetDate = (String) allergy.get("onsetDate");
        if (onsetDate != null) {
            allergyIntolerance.setOnset(new DateTimeType(parseHL7DateTime(onsetDate)));
        }

        // Reactions
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> reactions = (List<Map<String, Object>>) allergy.get("reactions");
        if (reactions != null) {
            for (Map<String, Object> reactionData : reactions) {
                AllergyIntolerance.AllergyIntoleranceReactionComponent reaction =
                    new AllergyIntolerance.AllergyIntoleranceReactionComponent();

                @SuppressWarnings("unchecked")
                Map<String, String> manifestation = (Map<String, String>) reactionData.get("manifestation");
                if (manifestation != null) {
                    CodeableConcept manifestationCode = new CodeableConcept();
                    manifestationCode.addCoding()
                        .setCode(manifestation.get("code"))
                        .setDisplay(manifestation.get("displayName"))
                        .setSystem(manifestation.get("codeSystem") != null ?
                            "urn:oid:" + manifestation.get("codeSystem") : null);
                    reaction.addManifestation(manifestationCode);
                }

                String reactionSeverity = (String) reactionData.get("severity");
                if (reactionSeverity != null) {
                    reaction.setSeverity(mapReactionSeverity(reactionSeverity));
                }

                allergyIntolerance.addReaction(reaction);
            }
        }

        return allergyIntolerance;
    }

    // Immunization conversion
    private Immunization convertImmunizationToFhir(Map<String, Object> immunization, String patientId) {
        Immunization fhirImmunization = new Immunization();
        fhirImmunization.setId(UUID.randomUUID().toString());

        // Patient reference
        if (patientId != null) {
            fhirImmunization.setPatient(new Reference("Patient/" + patientId));
        }

        // Status
        String status = (String) immunization.get("status");
        Boolean notGiven = (Boolean) immunization.get("notGiven");
        if (Boolean.TRUE.equals(notGiven)) {
            fhirImmunization.setStatus(Immunization.ImmunizationStatus.NOTDONE);
        } else if ("completed".equals(status)) {
            fhirImmunization.setStatus(Immunization.ImmunizationStatus.COMPLETED);
        } else {
            fhirImmunization.setStatus(Immunization.ImmunizationStatus.COMPLETED);
        }

        // Vaccine code
        @SuppressWarnings("unchecked")
        Map<String, String> vaccineCode = (Map<String, String>) immunization.get("vaccineCode");
        if (vaccineCode != null) {
            CodeableConcept code = new CodeableConcept();
            Coding coding = code.addCoding();
            coding.setCode(vaccineCode.get("code"));
            coding.setDisplay(vaccineCode.get("displayName"));

            String codeSystemUri = (String) immunization.get("vaccineCodeSystemUri");
            if (codeSystemUri != null) {
                coding.setSystem(codeSystemUri);
            }
            fhirImmunization.setVaccineCode(code);
        }

        // Occurrence date
        String administrationDate = (String) immunization.get("administrationDate");
        if (administrationDate != null) {
            fhirImmunization.setOccurrence(new DateTimeType(parseHL7DateTime(administrationDate)));
        }

        // Lot number
        String lotNumber = (String) immunization.get("lotNumber");
        if (lotNumber != null) {
            fhirImmunization.setLotNumber(lotNumber);
        }

        // Manufacturer
        String manufacturer = (String) immunization.get("manufacturer");
        if (manufacturer != null) {
            Reference mfrRef = new Reference();
            mfrRef.setDisplay(manufacturer);
            fhirImmunization.setManufacturer(mfrRef);
        }

        // Dose quantity
        @SuppressWarnings("unchecked")
        Map<String, String> doseQuantity = (Map<String, String>) immunization.get("doseQuantity");
        if (doseQuantity != null) {
            Quantity dose = new Quantity();
            try {
                dose.setValue(new BigDecimal(doseQuantity.get("value")));
            } catch (Exception e) {
                // Ignore
            }
            dose.setUnit(doseQuantity.get("unit"));
            fhirImmunization.setDoseQuantity(dose);
        }

        // Route
        @SuppressWarnings("unchecked")
        Map<String, String> route = (Map<String, String>) immunization.get("route");
        if (route != null) {
            CodeableConcept routeCode = new CodeableConcept();
            routeCode.addCoding()
                .setCode(route.get("code"))
                .setDisplay(route.get("displayName"));
            fhirImmunization.setRoute(routeCode);
        }

        // Site
        @SuppressWarnings("unchecked")
        Map<String, String> site = (Map<String, String>) immunization.get("site");
        if (site != null) {
            CodeableConcept siteCode = new CodeableConcept();
            siteCode.addCoding()
                .setCode(site.get("code"))
                .setDisplay(site.get("displayName"));
            fhirImmunization.setSite(siteCode);
        }

        // Primary source
        Boolean primarySource = (Boolean) immunization.get("primarySource");
        fhirImmunization.setPrimarySource(primarySource != null ? primarySource : true);

        // Status reason (refusal reason)
        @SuppressWarnings("unchecked")
        Map<String, String> refusalReason = (Map<String, String>) immunization.get("refusalReason");
        if (refusalReason != null) {
            CodeableConcept reasonCode = new CodeableConcept();
            reasonCode.addCoding()
                .setCode(refusalReason.get("code"))
                .setDisplay(refusalReason.get("displayName"));
            fhirImmunization.setStatusReason(reasonCode);
        }

        return fhirImmunization;
    }

    // Procedure conversion
    private Procedure convertProcedureToFhir(Map<String, Object> procedure, String patientId) {
        Procedure fhirProcedure = new Procedure();
        fhirProcedure.setId(UUID.randomUUID().toString());

        // Subject reference
        if (patientId != null) {
            fhirProcedure.setSubject(new Reference("Patient/" + patientId));
        }

        // Status
        String status = (String) procedure.get("status");
        fhirProcedure.setStatus(mapProcedureStatus(status));

        // Code
        @SuppressWarnings("unchecked")
        Map<String, String> codeData = (Map<String, String>) procedure.get("code");
        if (codeData != null) {
            CodeableConcept code = new CodeableConcept();
            Coding coding = code.addCoding();
            coding.setCode(codeData.get("code"));
            coding.setDisplay(codeData.get("displayName"));

            String codeSystemUri = (String) procedure.get("codeSystemUri");
            if (codeSystemUri != null) {
                coding.setSystem(codeSystemUri);
            }
            fhirProcedure.setCode(code);
        }

        // Performed date/time
        String performedDateTime = (String) procedure.get("performedDateTime");
        if (performedDateTime != null) {
            fhirProcedure.setPerformed(new DateTimeType(parseHL7DateTime(performedDateTime)));
        } else {
            String periodStart = (String) procedure.get("performedPeriodStart");
            String periodEnd = (String) procedure.get("performedPeriodEnd");
            if (periodStart != null || periodEnd != null) {
                Period period = new Period();
                if (periodStart != null) {
                    period.setStart(parseHL7DateTime(periodStart));
                }
                if (periodEnd != null) {
                    period.setEnd(parseHL7DateTime(periodEnd));
                }
                fhirProcedure.setPerformed(period);
            }
        }

        // Body site
        @SuppressWarnings("unchecked")
        Map<String, String> bodySite = (Map<String, String>) procedure.get("bodySite");
        if (bodySite != null) {
            CodeableConcept siteCode = new CodeableConcept();
            siteCode.addCoding()
                .setCode(bodySite.get("code"))
                .setDisplay(bodySite.get("displayName"));
            fhirProcedure.addBodySite(siteCode);
        }

        // Reason (indication)
        @SuppressWarnings("unchecked")
        Map<String, String> indication = (Map<String, String>) procedure.get("indication");
        if (indication != null) {
            CodeableConcept reasonCode = new CodeableConcept();
            reasonCode.addCoding()
                .setCode(indication.get("code"))
                .setDisplay(indication.get("displayName"));
            fhirProcedure.addReasonCode(reasonCode);
        }

        // Note
        String note = (String) procedure.get("note");
        if (note != null) {
            Annotation annotation = new Annotation();
            annotation.setText(note);
            fhirProcedure.addNote(annotation);
        }

        return fhirProcedure;
    }

    // Observation conversion (for lab results)
    private Observation convertResultToObservation(Map<String, Object> result, String patientId) {
        Observation observation = new Observation();
        observation.setId(UUID.randomUUID().toString());

        // Subject reference
        if (patientId != null) {
            observation.setSubject(new Reference("Patient/" + patientId));
        }

        // Status
        String status = (String) result.get("status");
        observation.setStatus(mapObservationStatus(status));

        // Category - laboratory
        CodeableConcept category = new CodeableConcept();
        category.addCoding()
            .setSystem("http://terminology.hl7.org/CodeSystem/observation-category")
            .setCode("laboratory")
            .setDisplay("Laboratory");
        observation.addCategory(category);

        // Code
        @SuppressWarnings("unchecked")
        Map<String, String> codeData = (Map<String, String>) result.get("code");
        if (codeData != null) {
            CodeableConcept code = new CodeableConcept();
            Coding coding = code.addCoding();
            coding.setCode(codeData.get("code"));
            coding.setDisplay(codeData.get("displayName"));

            String codeSystemUri = (String) result.get("codeSystemUri");
            if (codeSystemUri != null) {
                coding.setSystem(codeSystemUri);
            }
            observation.setCode(code);
        }

        // Effective date/time
        String effectiveDateTime = (String) result.get("effectiveDateTime");
        if (effectiveDateTime != null) {
            observation.setEffective(new DateTimeType(parseHL7DateTime(effectiveDateTime)));
        }

        // Value
        String valueType = (String) result.get("valueType");
        if ("Quantity".equals(valueType)) {
            @SuppressWarnings("unchecked")
            Map<String, String> valueQuantity = (Map<String, String>) result.get("valueQuantity");
            if (valueQuantity != null) {
                Quantity qty = new Quantity();
                try {
                    qty.setValue(new BigDecimal(valueQuantity.get("value")));
                } catch (Exception e) {
                    // Ignore
                }
                qty.setUnit(valueQuantity.get("unit"));
                observation.setValue(qty);
            }
        } else if ("CodeableConcept".equals(valueType)) {
            @SuppressWarnings("unchecked")
            Map<String, String> codedValue = (Map<String, String>) result.get("valueCodeableConcept");
            if (codedValue != null) {
                CodeableConcept valueCode = new CodeableConcept();
                valueCode.addCoding()
                    .setCode(codedValue.get("code"))
                    .setDisplay(codedValue.get("displayName"));
                observation.setValue(valueCode);
            }
        } else {
            String valueString = (String) result.get("valueString");
            if (valueString != null) {
                observation.setValue(new StringType(valueString));
            }
        }

        // Interpretation
        @SuppressWarnings("unchecked")
        Map<String, String> interpretation = (Map<String, String>) result.get("interpretation");
        if (interpretation != null) {
            CodeableConcept interpCode = new CodeableConcept();
            interpCode.addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/v3-ObservationInterpretation")
                .setCode(interpretation.get("code"))
                .setDisplay(interpretation.get("displayName"));
            observation.addInterpretation(interpCode);
        }

        // Reference range
        @SuppressWarnings("unchecked")
        Map<String, Object> referenceRange = (Map<String, Object>) result.get("referenceRange");
        if (referenceRange != null) {
            Observation.ObservationReferenceRangeComponent range =
                new Observation.ObservationReferenceRangeComponent();

            String lowValue = (String) referenceRange.get("lowValue");
            String highValue = (String) referenceRange.get("highValue");
            String unit = (String) referenceRange.get("unit");

            if (lowValue != null) {
                Quantity low = new Quantity();
                try {
                    low.setValue(new BigDecimal(lowValue));
                } catch (Exception e) {
                    // Ignore
                }
                low.setUnit(unit);
                range.setLow(low);
            }

            if (highValue != null) {
                Quantity high = new Quantity();
                try {
                    high.setValue(new BigDecimal(highValue));
                } catch (Exception e) {
                    // Ignore
                }
                high.setUnit(unit);
                range.setHigh(high);
            }

            String text = (String) referenceRange.get("text");
            if (text != null) {
                range.setText(text);
            }

            observation.addReferenceRange(range);
        }

        return observation;
    }

    // Observation conversion (for vital signs)
    private Observation convertVitalSignToObservation(Map<String, Object> vitalSign, String patientId) {
        Observation observation = convertResultToObservation(vitalSign, patientId);

        // Override category to vital-signs
        observation.getCategory().clear();
        CodeableConcept category = new CodeableConcept();
        category.addCoding()
            .setSystem("http://terminology.hl7.org/CodeSystem/observation-category")
            .setCode("vital-signs")
            .setDisplay("Vital Signs");
        observation.addCategory(category);

        return observation;
    }

    // Encounter conversion
    private Encounter convertEncounterToFhir(Map<String, Object> encounter, String patientId) {
        Encounter fhirEncounter = new Encounter();
        fhirEncounter.setId(UUID.randomUUID().toString());

        // Subject reference
        if (patientId != null) {
            fhirEncounter.setSubject(new Reference("Patient/" + patientId));
        }

        // Status
        String status = (String) encounter.get("status");
        fhirEncounter.setStatus(mapEncounterStatus(status));

        // Class
        String encounterClass = (String) encounter.get("class");
        if (encounterClass != null) {
            Coding classCoding = new Coding();
            classCoding.setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode");
            classCoding.setCode(encounterClass);
            fhirEncounter.setClass_(classCoding);
        }

        // Type
        @SuppressWarnings("unchecked")
        Map<String, String> type = (Map<String, String>) encounter.get("type");
        if (type != null) {
            CodeableConcept typeCode = new CodeableConcept();
            Coding coding = typeCode.addCoding();
            coding.setCode(type.get("code"));
            coding.setDisplay(type.get("displayName"));

            String codeSystemUri = (String) encounter.get("typeCodeSystemUri");
            if (codeSystemUri != null) {
                coding.setSystem(codeSystemUri);
            }
            fhirEncounter.addType(typeCode);
        }

        // Period
        String periodStart = (String) encounter.get("periodStart");
        String periodEnd = (String) encounter.get("periodEnd");
        if (periodStart != null || periodEnd != null) {
            Period period = new Period();
            if (periodStart != null) {
                period.setStart(parseHL7DateTime(periodStart));
            }
            if (periodEnd != null) {
                period.setEnd(parseHL7DateTime(periodEnd));
            }
            fhirEncounter.setPeriod(period);
        }

        // Reason
        @SuppressWarnings("unchecked")
        Map<String, String> reasonCode = (Map<String, String>) encounter.get("reasonCode");
        if (reasonCode != null) {
            CodeableConcept reason = new CodeableConcept();
            reason.addCoding()
                .setCode(reasonCode.get("code"))
                .setDisplay(reasonCode.get("displayName"));
            fhirEncounter.addReasonCode(reason);
        }

        // Hospitalization (discharge disposition)
        @SuppressWarnings("unchecked")
        Map<String, String> dischargeDisposition = (Map<String, String>) encounter.get("dischargeDisposition");
        if (dischargeDisposition != null) {
            Encounter.EncounterHospitalizationComponent hospitalization =
                new Encounter.EncounterHospitalizationComponent();
            CodeableConcept disposition = new CodeableConcept();
            disposition.addCoding()
                .setCode(dischargeDisposition.get("code"))
                .setDisplay(dischargeDisposition.get("displayName"));
            hospitalization.setDischargeDisposition(disposition);
            fhirEncounter.setHospitalization(hospitalization);
        }

        return fhirEncounter;
    }

    // Helper methods

    private void addResourceToBundle(Bundle bundle, Resource resource, String resourceType) {
        Bundle.BundleEntryComponent entry = bundle.addEntry();
        entry.setFullUrl("urn:uuid:" + resource.getIdElement().getIdPart());
        entry.setResource(resource);

        Bundle.BundleEntryRequestComponent request = new Bundle.BundleEntryRequestComponent();
        request.setMethod(Bundle.HTTPVerb.POST);
        request.setUrl(resourceType);
        entry.setRequest(request);
    }

    private Address convertAddress(Map<String, Object> addrData) {
        Address address = new Address();

        @SuppressWarnings("unchecked")
        List<String> streetLines = (List<String>) addrData.get("streetAddressLines");
        if (streetLines != null) {
            for (String line : streetLines) {
                address.addLine(line);
            }
        }

        String city = (String) addrData.get("city");
        if (city != null) address.setCity(city);

        String state = (String) addrData.get("state");
        if (state != null) address.setState(state);

        String postalCode = (String) addrData.get("postalCode");
        if (postalCode != null) address.setPostalCode(postalCode);

        String country = (String) addrData.get("country");
        if (country != null) address.setCountry(country);

        String use = (String) addrData.get("use");
        if (use != null) {
            address.setUse(mapAddressUse(use));
        }

        return address;
    }

    private ContactPoint convertTelecom(Map<String, String> telecomData) {
        ContactPoint telecom = new ContactPoint();

        String value = telecomData.get("value");
        if (value != null) {
            // Parse telecom value (tel:, mailto:, etc.)
            if (value.startsWith("tel:")) {
                telecom.setSystem(ContactPoint.ContactPointSystem.PHONE);
                telecom.setValue(value.substring(4));
            } else if (value.startsWith("mailto:")) {
                telecom.setSystem(ContactPoint.ContactPointSystem.EMAIL);
                telecom.setValue(value.substring(7));
            } else if (value.startsWith("fax:")) {
                telecom.setSystem(ContactPoint.ContactPointSystem.FAX);
                telecom.setValue(value.substring(4));
            } else {
                telecom.setValue(value);
            }
        }

        String use = telecomData.get("use");
        if (use != null) {
            telecom.setUse(mapTelecomUse(use));
        }

        return telecom;
    }

    private Enumerations.AdministrativeGender mapGender(String cdaGender) {
        if (cdaGender == null) return Enumerations.AdministrativeGender.UNKNOWN;
        return switch (cdaGender.toUpperCase()) {
            case "M" -> Enumerations.AdministrativeGender.MALE;
            case "F" -> Enumerations.AdministrativeGender.FEMALE;
            case "UN" -> Enumerations.AdministrativeGender.OTHER;
            default -> Enumerations.AdministrativeGender.UNKNOWN;
        };
    }

    private Address.AddressUse mapAddressUse(String cdaUse) {
        if (cdaUse == null) return Address.AddressUse.HOME;
        return switch (cdaUse.toUpperCase()) {
            case "HP", "H" -> Address.AddressUse.HOME;
            case "WP" -> Address.AddressUse.WORK;
            case "TMP" -> Address.AddressUse.TEMP;
            case "OLD" -> Address.AddressUse.OLD;
            default -> Address.AddressUse.HOME;
        };
    }

    private ContactPoint.ContactPointUse mapTelecomUse(String cdaUse) {
        if (cdaUse == null) return ContactPoint.ContactPointUse.HOME;
        return switch (cdaUse.toUpperCase()) {
            case "HP", "H" -> ContactPoint.ContactPointUse.HOME;
            case "WP" -> ContactPoint.ContactPointUse.WORK;
            case "MC" -> ContactPoint.ContactPointUse.MOBILE;
            case "TMP" -> ContactPoint.ContactPointUse.TEMP;
            case "OLD" -> ContactPoint.ContactPointUse.OLD;
            default -> ContactPoint.ContactPointUse.HOME;
        };
    }

    private String mapClinicalStatus(String cdaStatus) {
        if (cdaStatus == null) return "active";
        return switch (cdaStatus.toLowerCase()) {
            case "55561003" -> "active";
            case "73425007" -> "inactive";
            case "413322009" -> "resolved";
            default -> "active";
        };
    }

    private String mapAllergyClinicalStatus(String cdaStatus) {
        if (cdaStatus == null) return "active";
        return switch (cdaStatus.toLowerCase()) {
            case "55561003" -> "active";
            case "73425007" -> "inactive";
            case "413322009" -> "resolved";
            default -> "active";
        };
    }

    private MedicationRequest.MedicationRequestStatus mapMedicationStatus(String status) {
        if (status == null) return MedicationRequest.MedicationRequestStatus.ACTIVE;
        return switch (status.toLowerCase()) {
            case "active" -> MedicationRequest.MedicationRequestStatus.ACTIVE;
            case "completed" -> MedicationRequest.MedicationRequestStatus.COMPLETED;
            case "cancelled", "aborted" -> MedicationRequest.MedicationRequestStatus.CANCELLED;
            default -> MedicationRequest.MedicationRequestStatus.ACTIVE;
        };
    }

    private Timing.UnitsOfTime mapTimingUnit(String unit) {
        if (unit == null) return Timing.UnitsOfTime.D;
        return switch (unit.toLowerCase()) {
            case "s" -> Timing.UnitsOfTime.S;
            case "min" -> Timing.UnitsOfTime.MIN;
            case "h" -> Timing.UnitsOfTime.H;
            case "d" -> Timing.UnitsOfTime.D;
            case "wk" -> Timing.UnitsOfTime.WK;
            case "mo" -> Timing.UnitsOfTime.MO;
            case "a" -> Timing.UnitsOfTime.A;
            default -> Timing.UnitsOfTime.D;
        };
    }

    private AllergyIntolerance.AllergyIntoleranceCriticality mapCriticality(String criticality) {
        if (criticality == null) return AllergyIntolerance.AllergyIntoleranceCriticality.UNABLETOASSESS;
        return switch (criticality.toLowerCase()) {
            case "high", "399166001" -> AllergyIntolerance.AllergyIntoleranceCriticality.HIGH;
            case "low", "62482003" -> AllergyIntolerance.AllergyIntoleranceCriticality.LOW;
            default -> AllergyIntolerance.AllergyIntoleranceCriticality.UNABLETOASSESS;
        };
    }

    private AllergyIntolerance.AllergyIntoleranceSeverity mapReactionSeverity(String severity) {
        if (severity == null) return AllergyIntolerance.AllergyIntoleranceSeverity.MODERATE;
        return switch (severity.toLowerCase()) {
            case "mild", "255604002" -> AllergyIntolerance.AllergyIntoleranceSeverity.MILD;
            case "moderate", "6736007" -> AllergyIntolerance.AllergyIntoleranceSeverity.MODERATE;
            case "severe", "24484000" -> AllergyIntolerance.AllergyIntoleranceSeverity.SEVERE;
            default -> AllergyIntolerance.AllergyIntoleranceSeverity.MODERATE;
        };
    }

    private Procedure.ProcedureStatus mapProcedureStatus(String status) {
        if (status == null) return Procedure.ProcedureStatus.COMPLETED;
        return switch (status.toLowerCase()) {
            case "active" -> Procedure.ProcedureStatus.INPROGRESS;
            case "completed" -> Procedure.ProcedureStatus.COMPLETED;
            case "cancelled", "aborted" -> Procedure.ProcedureStatus.NOTDONE;
            default -> Procedure.ProcedureStatus.COMPLETED;
        };
    }

    private Observation.ObservationStatus mapObservationStatus(String status) {
        if (status == null) return Observation.ObservationStatus.FINAL;
        return switch (status.toLowerCase()) {
            case "completed" -> Observation.ObservationStatus.FINAL;
            case "active" -> Observation.ObservationStatus.PRELIMINARY;
            case "cancelled", "aborted" -> Observation.ObservationStatus.CANCELLED;
            default -> Observation.ObservationStatus.FINAL;
        };
    }

    private Encounter.EncounterStatus mapEncounterStatus(String status) {
        if (status == null) return Encounter.EncounterStatus.UNKNOWN;
        return switch (status.toLowerCase()) {
            case "in-progress" -> Encounter.EncounterStatus.INPROGRESS;
            case "finished" -> Encounter.EncounterStatus.FINISHED;
            case "cancelled" -> Encounter.EncounterStatus.CANCELLED;
            case "planned" -> Encounter.EncounterStatus.PLANNED;
            case "arrived" -> Encounter.EncounterStatus.ARRIVED;
            default -> Encounter.EncounterStatus.UNKNOWN;
        };
    }

    private Date parseHL7Date(String hl7Date) {
        if (hl7Date == null || hl7Date.isEmpty()) return null;

        try {
            // Pad to 8 characters minimum
            String normalized = hl7Date;
            if (normalized.length() < 8) {
                normalized = normalized + "01010101".substring(normalized.length());
            }
            normalized = normalized.substring(0, 8);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            return sdf.parse(normalized);
        } catch (ParseException e) {
            log.debug("Could not parse HL7 date: {}", hl7Date);
            return null;
        }
    }

    private Date parseHL7DateTime(String hl7DateTime) {
        if (hl7DateTime == null || hl7DateTime.isEmpty()) return null;

        try {
            // Remove timezone
            String normalized = hl7DateTime.replaceAll("[+-]\\d{4}$", "")
                .replaceAll("\\.\\d+", "");

            // Determine format based on length
            String format;
            if (normalized.length() <= 8) {
                while (normalized.length() < 8) normalized += "0";
                format = "yyyyMMdd";
            } else if (normalized.length() <= 12) {
                while (normalized.length() < 12) normalized += "0";
                format = "yyyyMMddHHmm";
            } else {
                while (normalized.length() < 14) normalized += "0";
                normalized = normalized.substring(0, 14);
                format = "yyyyMMddHHmmss";
            }

            SimpleDateFormat sdf = new SimpleDateFormat(format);
            return sdf.parse(normalized);
        } catch (ParseException e) {
            log.debug("Could not parse HL7 datetime: {}", hl7DateTime);
            return null;
        }
    }
}
