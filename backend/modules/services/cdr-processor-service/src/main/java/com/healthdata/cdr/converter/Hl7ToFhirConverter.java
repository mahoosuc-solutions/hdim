package com.healthdata.cdr.converter;

import com.healthdata.cdr.dto.Hl7v2Message;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Converter for transforming HL7 v2 messages to FHIR R4 resources.
 *
 * Converts:
 * - ADT messages to Patient and Encounter resources
 * - ORU messages to Observation resources
 * - ORM messages to ServiceRequest resources
 * - RDE messages to MedicationRequest resources
 * - RAS messages to MedicationAdministration resources
 * - VXU messages to Immunization resources
 */
@Slf4j
@Component
public class Hl7ToFhirConverter {

    private static final DateTimeFormatter HL7_DATE_FORMAT =
        DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * Convert HL7 v2 message to FHIR resources.
     *
     * @param hl7Message Parsed HL7 v2 message
     * @return Bundle containing FHIR resources
     */
    public Bundle convertToFhir(Hl7v2Message hl7Message) {
        log.debug("Converting HL7 message to FHIR: type={}", hl7Message.getMessageType());

        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.TRANSACTION);
        bundle.setTimestamp(new Date());

        try {
            switch (hl7Message.getMessageType()) {
                case "ADT" -> convertAdtToFhir(hl7Message, bundle);
                case "ORU" -> convertOruToFhir(hl7Message, bundle);
                case "ORM" -> convertOrmToFhir(hl7Message, bundle);
                case "RDE" -> convertRdeToFhir(hl7Message, bundle);
                case "RAS" -> convertRasToFhir(hl7Message, bundle);
                case "VXU" -> convertVxuToFhir(hl7Message, bundle);
                default -> log.warn("Unsupported message type for FHIR conversion: {}",
                    hl7Message.getMessageType());
            }

            log.info("Successfully converted HL7 message to FHIR bundle with {} entries",
                bundle.getEntry().size());

        } catch (Exception e) {
            log.error("Error converting HL7 to FHIR: {}", e.getMessage(), e);
        }

        return bundle;
    }

    /**
     * Convert ADT message to FHIR Patient and Encounter resources.
     */
    @SuppressWarnings("unchecked")
    private void convertAdtToFhir(Hl7v2Message hl7Message, Bundle bundle) {
        Map<String, Object> parsedData = hl7Message.getParsedData();
        if (parsedData == null) {
            return;
        }

        // Convert patient data
        Map<String, Object> patientData = (Map<String, Object>) parsedData.get("patient");
        if (patientData != null) {
            Patient patient = convertToPatient(patientData);
            addResourceToBundle(bundle, patient, "Patient");
        }

        // Convert visit/encounter data
        Map<String, Object> visitData = (Map<String, Object>) parsedData.get("visit");
        if (visitData != null && patientData != null) {
            Encounter encounter = convertToEncounter(visitData, patientData);
            addResourceToBundle(bundle, encounter, "Encounter");
        }
    }

    /**
     * Convert ORU message to FHIR Observation resources.
     */
    @SuppressWarnings("unchecked")
    private void convertOruToFhir(Hl7v2Message hl7Message, Bundle bundle) {
        Map<String, Object> parsedData = hl7Message.getParsedData();
        if (parsedData == null) {
            return;
        }

        Map<String, Object> patientData = (Map<String, Object>) parsedData.get("patient");
        List<Map<String, Object>> orderObservations =
            (List<Map<String, Object>>) parsedData.get("orderObservations");

        if (orderObservations != null) {
            for (Map<String, Object> orderObs : orderObservations) {
                List<Map<String, Object>> observations =
                    (List<Map<String, Object>>) orderObs.get("observations");

                if (observations != null) {
                    for (Map<String, Object> obs : observations) {
                        Observation observation = convertToObservation(obs, patientData);
                        addResourceToBundle(bundle, observation, "Observation");
                    }
                }
            }
        }
    }

    /**
     * Convert ORM message to FHIR ServiceRequest resource.
     */
    @SuppressWarnings("unchecked")
    private void convertOrmToFhir(Hl7v2Message hl7Message, Bundle bundle) {
        Map<String, Object> parsedData = hl7Message.getParsedData();
        if (parsedData == null) {
            return;
        }

        Map<String, Object> patientData = (Map<String, Object>) parsedData.get("patient");
        Map<String, Object> observationRequest =
            (Map<String, Object>) parsedData.get("observationRequest");

        if (observationRequest != null) {
            ServiceRequest serviceRequest =
                convertToServiceRequest(observationRequest, patientData);
            addResourceToBundle(bundle, serviceRequest, "ServiceRequest");
        }
    }

    /**
     * Convert RDE (Pharmacy/Treatment Encoded Order) message to FHIR MedicationRequest resource.
     */
    @SuppressWarnings("unchecked")
    private void convertRdeToFhir(Hl7v2Message hl7Message, Bundle bundle) {
        Map<String, Object> parsedData = hl7Message.getParsedData();
        if (parsedData == null) {
            return;
        }

        Map<String, Object> patientData = (Map<String, Object>) parsedData.get("patient");
        List<Map<String, Object>> orders = (List<Map<String, Object>>) parsedData.get("orders");

        if (orders != null) {
            for (Map<String, Object> order : orders) {
                MedicationRequest medicationRequest = convertToMedicationRequest(order, patientData);
                addResourceToBundle(bundle, medicationRequest, "MedicationRequest");
            }
        }
    }

    /**
     * Convert RAS (Pharmacy/Treatment Administration) message to FHIR MedicationAdministration resource.
     */
    @SuppressWarnings("unchecked")
    private void convertRasToFhir(Hl7v2Message hl7Message, Bundle bundle) {
        Map<String, Object> parsedData = hl7Message.getParsedData();
        if (parsedData == null) {
            return;
        }

        Map<String, Object> patientData = (Map<String, Object>) parsedData.get("patient");
        List<Map<String, Object>> medAdmins =
            (List<Map<String, Object>>) parsedData.get("medicationAdministrations");

        if (medAdmins != null) {
            for (Map<String, Object> adminData : medAdmins) {
                List<Map<String, Object>> administrations =
                    (List<Map<String, Object>>) adminData.get("administrations");

                if (administrations != null) {
                    for (Map<String, Object> admin : administrations) {
                        MedicationAdministration medicationAdministration =
                            convertToMedicationAdministration(admin, patientData);
                        addResourceToBundle(bundle, medicationAdministration, "MedicationAdministration");
                    }
                }
            }
        }
    }

    /**
     * Convert VXU (Vaccination Update) message to FHIR Immunization resource.
     */
    @SuppressWarnings("unchecked")
    private void convertVxuToFhir(Hl7v2Message hl7Message, Bundle bundle) {
        Map<String, Object> parsedData = hl7Message.getParsedData();
        if (parsedData == null) {
            return;
        }

        Map<String, Object> patientData = (Map<String, Object>) parsedData.get("patient");
        List<Map<String, Object>> immunizations =
            (List<Map<String, Object>>) parsedData.get("immunizations");

        if (immunizations != null) {
            for (Map<String, Object> immunization : immunizations) {
                Immunization fhirImmunization = convertToImmunization(immunization, patientData);
                addResourceToBundle(bundle, fhirImmunization, "Immunization");
            }
        }
    }

    /**
     * Convert order data to FHIR MedicationRequest resource.
     */
    @SuppressWarnings("unchecked")
    private MedicationRequest convertToMedicationRequest(Map<String, Object> orderData,
                                                          Map<String, Object> patientData) {
        MedicationRequest medicationRequest = new MedicationRequest();

        // Status
        medicationRequest.setStatus(MedicationRequest.MedicationRequestStatus.ACTIVE);

        // Intent
        medicationRequest.setIntent(MedicationRequest.MedicationRequestIntent.ORDER);

        // Subject (Patient reference)
        if (patientData != null && patientData.containsKey("patientId")) {
            Reference patientRef = new Reference();
            patientRef.setReference("Patient/" + patientData.get("patientId"));
            medicationRequest.setSubject(patientRef);
        }

        // Identifier (Placer Order Number)
        if (orderData.containsKey("placerOrderNumber")) {
            Identifier identifier = new Identifier();
            identifier.setValue((String) orderData.get("placerOrderNumber"));
            identifier.setSystem("http://hospital.example.org/placer-orders");
            medicationRequest.addIdentifier(identifier);
        }

        // Medication from RXE segment
        Map<String, Object> medication = (Map<String, Object>) orderData.get("medication");
        if (medication != null) {
            Map<String, String> drugCode = (Map<String, String>) medication.get("drugCode");
            if (drugCode != null) {
                CodeableConcept medicationCode = new CodeableConcept();
                Coding coding = new Coding();
                coding.setCode(drugCode.get("code"));
                coding.setDisplay(drugCode.get("display"));
                coding.setSystem(mapCodingSystem(drugCode.get("system")));
                medicationCode.addCoding(coding);
                medicationRequest.setMedication(medicationCode);
            }

            // Dosage
            Dosage dosage = new Dosage();
            if (medication.containsKey("administrationInstructions")) {
                dosage.setText((String) medication.get("administrationInstructions"));
            }

            // Dose Quantity
            if (medication.containsKey("giveAmountMin")) {
                Dosage.DosageDoseAndRateComponent doseAndRate = new Dosage.DosageDoseAndRateComponent();
                SimpleQuantity doseQuantity = new SimpleQuantity();
                try {
                    doseQuantity.setValue(Double.parseDouble((String) medication.get("giveAmountMin")));
                } catch (Exception e) {
                    log.debug("Could not parse dose amount");
                }
                if (medication.containsKey("giveUnitsDisplay")) {
                    doseQuantity.setUnit((String) medication.get("giveUnitsDisplay"));
                }
                doseAndRate.setDose(doseQuantity);
                dosage.addDoseAndRate(doseAndRate);
            }

            medicationRequest.addDosageInstruction(dosage);

            // Dispense Request
            MedicationRequest.MedicationRequestDispenseRequestComponent dispenseRequest =
                new MedicationRequest.MedicationRequestDispenseRequestComponent();

            if (medication.containsKey("dispenseAmount")) {
                SimpleQuantity quantity = new SimpleQuantity();
                try {
                    quantity.setValue(Double.parseDouble((String) medication.get("dispenseAmount")));
                } catch (Exception e) {
                    log.debug("Could not parse dispense amount");
                }
                if (medication.containsKey("dispenseUnits")) {
                    quantity.setUnit((String) medication.get("dispenseUnits"));
                }
                dispenseRequest.setQuantity(quantity);
            }

            if (medication.containsKey("numberOfRefills")) {
                try {
                    int refills = Integer.parseInt((String) medication.get("numberOfRefills"));
                    dispenseRequest.setNumberOfRepeatsAllowed(refills);
                } catch (Exception e) {
                    log.debug("Could not parse refills");
                }
            }

            medicationRequest.setDispenseRequest(dispenseRequest);
        }

        // Routes
        List<Map<String, String>> routes = (List<Map<String, String>>) orderData.get("routes");
        if (routes != null && !routes.isEmpty()) {
            Map<String, String> route = routes.get(0);
            if (medicationRequest.hasDosageInstruction()) {
                CodeableConcept routeCode = new CodeableConcept();
                Coding routeCoding = new Coding();
                routeCoding.setCode(route.get("code"));
                routeCoding.setDisplay(route.get("display"));
                routeCoding.setSystem(mapCodingSystem(route.get("system")));
                routeCode.addCoding(routeCoding);
                medicationRequest.getDosageInstructionFirstRep().setRoute(routeCode);
            }
        }

        // Requester from ORC
        if (orderData.containsKey("orderingProvider")) {
            Map<String, String> provider = (Map<String, String>) orderData.get("orderingProvider");
            Reference requesterRef = new Reference();
            requesterRef.setReference("Practitioner/" + provider.get("id"));
            requesterRef.setDisplay(provider.get("givenName") + " " + provider.get("familyName"));
            medicationRequest.setRequester(requesterRef);
        }

        return medicationRequest;
    }

    /**
     * Convert administration data to FHIR MedicationAdministration resource.
     */
    @SuppressWarnings("unchecked")
    private MedicationAdministration convertToMedicationAdministration(Map<String, Object> adminData,
                                                                        Map<String, Object> patientData) {
        MedicationAdministration medicationAdministration = new MedicationAdministration();

        // Status
        if (adminData.containsKey("fhirStatus")) {
            String status = (String) adminData.get("fhirStatus");
            medicationAdministration.setStatus(mapMedicationAdministrationStatus(status));
        } else {
            medicationAdministration.setStatus(MedicationAdministration.MedicationAdministrationStatus.COMPLETED);
        }

        // Subject (Patient reference)
        if (patientData != null && patientData.containsKey("patientId")) {
            Reference patientRef = new Reference();
            patientRef.setReference("Patient/" + patientData.get("patientId"));
            medicationAdministration.setSubject(patientRef);
        }

        // Medication
        Map<String, String> administeredCode = (Map<String, String>) adminData.get("administeredCode");
        if (administeredCode != null) {
            CodeableConcept medicationCode = new CodeableConcept();
            Coding coding = new Coding();
            coding.setCode(administeredCode.get("code"));
            coding.setDisplay(administeredCode.get("display"));
            coding.setSystem(mapCodingSystem(administeredCode.get("system")));
            medicationCode.addCoding(coding);
            medicationAdministration.setMedication(medicationCode);
        }

        // Effective DateTime
        if (adminData.containsKey("startDateTime")) {
            try {
                Date effectiveDate = parseHl7Date((String) adminData.get("startDateTime"));
                medicationAdministration.setEffective(new DateTimeType(effectiveDate));
            } catch (Exception e) {
                log.warn("Failed to parse administration start date: {}", e.getMessage());
            }
        }

        // Dosage
        MedicationAdministration.MedicationAdministrationDosageComponent dosage =
            new MedicationAdministration.MedicationAdministrationDosageComponent();

        if (adminData.containsKey("administeredAmount")) {
            SimpleQuantity doseQuantity = new SimpleQuantity();
            try {
                doseQuantity.setValue(Double.parseDouble((String) adminData.get("administeredAmount")));
            } catch (Exception e) {
                log.debug("Could not parse administered amount");
            }
            if (adminData.containsKey("administeredUnitsDisplay")) {
                doseQuantity.setUnit((String) adminData.get("administeredUnitsDisplay"));
            }
            dosage.setDose(doseQuantity);
        }

        // Route
        Map<String, String> route = (Map<String, String>) adminData.get("route");
        if (route != null) {
            CodeableConcept routeCode = new CodeableConcept();
            Coding routeCoding = new Coding();
            routeCoding.setCode(route.get("code"));
            routeCoding.setDisplay(route.get("display"));
            routeCoding.setSystem(mapCodingSystem(route.get("system")));
            routeCode.addCoding(routeCoding);
            dosage.setRoute(routeCode);

            // Site
            if (route.containsKey("siteCode")) {
                CodeableConcept siteCode = new CodeableConcept();
                Coding siteCoding = new Coding();
                siteCoding.setCode(route.get("siteCode"));
                siteCoding.setDisplay(route.get("siteDisplay"));
                siteCode.addCoding(siteCoding);
                dosage.setSite(siteCode);
            }
        }

        medicationAdministration.setDosage(dosage);

        // Performer
        if (adminData.containsKey("administeringProvider")) {
            Map<String, String> provider = (Map<String, String>) adminData.get("administeringProvider");
            MedicationAdministration.MedicationAdministrationPerformerComponent performer =
                new MedicationAdministration.MedicationAdministrationPerformerComponent();
            Reference actorRef = new Reference();
            actorRef.setReference("Practitioner/" + provider.get("id"));
            actorRef.setDisplay(provider.get("givenName") + " " + provider.get("familyName"));
            performer.setActor(actorRef);
            medicationAdministration.addPerformer(performer);
        }

        return medicationAdministration;
    }

    /**
     * Convert immunization data to FHIR Immunization resource.
     */
    @SuppressWarnings("unchecked")
    private Immunization convertToImmunization(Map<String, Object> immunizationData,
                                                Map<String, Object> patientData) {
        Immunization immunization = new Immunization();

        // Status
        if (immunizationData.containsKey("fhirStatus")) {
            String status = (String) immunizationData.get("fhirStatus");
            immunization.setStatus(mapImmunizationStatus(status));
        } else {
            immunization.setStatus(Immunization.ImmunizationStatus.COMPLETED);
        }

        // Subject (Patient reference)
        if (patientData != null && patientData.containsKey("patientId")) {
            Reference patientRef = new Reference();
            patientRef.setReference("Patient/" + patientData.get("patientId"));
            immunization.setPatient(patientRef);
        }

        // Vaccine Code
        Map<String, String> vaccineCode = (Map<String, String>) immunizationData.get("vaccineCode");
        if (vaccineCode != null) {
            CodeableConcept vaccine = new CodeableConcept();
            Coding coding = new Coding();
            coding.setCode(vaccineCode.get("code"));
            coding.setDisplay(vaccineCode.get("display"));
            coding.setSystem(mapCodingSystem(vaccineCode.get("system")));
            vaccine.addCoding(coding);
            immunization.setVaccineCode(vaccine);
        }

        // Occurrence DateTime
        if (immunizationData.containsKey("administrationDateTime")) {
            try {
                Date occurrenceDate = parseHl7Date((String) immunizationData.get("administrationDateTime"));
                immunization.setOccurrence(new DateTimeType(occurrenceDate));
            } catch (Exception e) {
                log.warn("Failed to parse immunization date: {}", e.getMessage());
            }
        }

        // Dose Quantity
        if (immunizationData.containsKey("administeredAmount")) {
            SimpleQuantity doseQuantity = new SimpleQuantity();
            try {
                doseQuantity.setValue(Double.parseDouble((String) immunizationData.get("administeredAmount")));
            } catch (Exception e) {
                log.debug("Could not parse dose amount");
            }
            if (immunizationData.containsKey("administeredUnitsDisplay")) {
                doseQuantity.setUnit((String) immunizationData.get("administeredUnitsDisplay"));
            }
            immunization.setDoseQuantity(doseQuantity);
        }

        // Lot Number
        if (immunizationData.containsKey("lotNumber")) {
            immunization.setLotNumber((String) immunizationData.get("lotNumber"));
        }

        // Expiration Date
        if (immunizationData.containsKey("expirationDate")) {
            try {
                Date expirationDate = parseHl7Date((String) immunizationData.get("expirationDate"));
                immunization.setExpirationDate(expirationDate);
            } catch (Exception e) {
                log.warn("Failed to parse expiration date: {}", e.getMessage());
            }
        }

        // Manufacturer
        if (immunizationData.containsKey("manufacturer")) {
            Reference manufacturerRef = new Reference();
            manufacturerRef.setDisplay((String) immunizationData.get("manufacturer"));
            immunization.setManufacturer(manufacturerRef);
        }

        // Route
        Map<String, String> route = (Map<String, String>) immunizationData.get("route");
        if (route != null) {
            CodeableConcept routeCode = new CodeableConcept();
            Coding routeCoding = new Coding();
            routeCoding.setCode(route.get("code"));
            routeCoding.setDisplay(route.get("display"));
            routeCoding.setSystem(mapCodingSystem(route.get("system")));
            routeCode.addCoding(routeCoding);
            immunization.setRoute(routeCode);

            // Site
            if (route.containsKey("siteCode")) {
                CodeableConcept siteCode = new CodeableConcept();
                Coding siteCoding = new Coding();
                siteCoding.setCode(route.get("siteCode"));
                siteCoding.setDisplay(route.get("siteDisplay"));
                siteCode.addCoding(siteCoding);
                immunization.setSite(siteCode);
            }
        }

        // Performer
        if (immunizationData.containsKey("administeringProvider")) {
            Map<String, String> provider = (Map<String, String>) immunizationData.get("administeringProvider");
            Immunization.ImmunizationPerformerComponent performer =
                new Immunization.ImmunizationPerformerComponent();
            Reference actorRef = new Reference();
            actorRef.setReference("Practitioner/" + provider.get("id"));
            actorRef.setDisplay(provider.get("givenName") + " " + provider.get("familyName"));
            performer.setActor(actorRef);
            immunization.addPerformer(performer);
        }

        // Primary Source - default to true if from healthcare provider
        immunization.setPrimarySource(true);

        return immunization;
    }

    /**
     * Map status to FHIR MedicationAdministrationStatus.
     */
    private MedicationAdministration.MedicationAdministrationStatus mapMedicationAdministrationStatus(String status) {
        return switch (status) {
            case "completed" -> MedicationAdministration.MedicationAdministrationStatus.COMPLETED;
            case "in-progress" -> MedicationAdministration.MedicationAdministrationStatus.INPROGRESS;
            case "not-done" -> MedicationAdministration.MedicationAdministrationStatus.NOTDONE;
            case "stopped" -> MedicationAdministration.MedicationAdministrationStatus.STOPPED;
            case "on-hold" -> MedicationAdministration.MedicationAdministrationStatus.ONHOLD;
            default -> MedicationAdministration.MedicationAdministrationStatus.COMPLETED;
        };
    }

    /**
     * Map status to FHIR ImmunizationStatus.
     */
    private Immunization.ImmunizationStatus mapImmunizationStatus(String status) {
        return switch (status) {
            case "completed" -> Immunization.ImmunizationStatus.COMPLETED;
            case "not-done" -> Immunization.ImmunizationStatus.NOTDONE;
            case "entered-in-error" -> Immunization.ImmunizationStatus.ENTEREDINERROR;
            default -> Immunization.ImmunizationStatus.COMPLETED;
        };
    }

    /**
     * Convert patient data to FHIR Patient resource.
     */
    @SuppressWarnings("unchecked")
    private Patient convertToPatient(Map<String, Object> patientData) {
        Patient patient = new Patient();

        // Identifier
        if (patientData.containsKey("patientId")) {
            Identifier identifier = new Identifier();
            identifier.setValue((String) patientData.get("patientId"));
            identifier.setSystem("urn:oid:2.16.840.1.113883.4.1"); // SSN OID as example
            patient.addIdentifier(identifier);
        }

        // Name
        if (patientData.containsKey("familyName") && patientData.containsKey("givenName")) {
            HumanName name = new HumanName();
            name.setFamily((String) patientData.get("familyName"));
            name.addGiven((String) patientData.get("givenName"));
            if (patientData.containsKey("middleName")) {
                name.addGiven((String) patientData.get("middleName"));
            }
            patient.addName(name);
        }

        // Gender
        if (patientData.containsKey("gender")) {
            String gender = (String) patientData.get("gender");
            patient.setGender(mapGender(gender));
        }

        // Birth Date
        if (patientData.containsKey("dateOfBirth")) {
            try {
                String dob = (String) patientData.get("dateOfBirth");
                Date birthDate = parseHl7Date(dob);
                patient.setBirthDate(birthDate);
            } catch (Exception e) {
                log.warn("Failed to parse birth date: {}", e.getMessage());
            }
        }

        // Address
        if (patientData.containsKey("address")) {
            Map<String, String> addressData = (Map<String, String>) patientData.get("address");
            Address address = new Address();
            address.addLine(addressData.get("street"));
            address.setCity(addressData.get("city"));
            address.setState(addressData.get("state"));
            address.setPostalCode(addressData.get("zip"));
            address.setCountry(addressData.get("country"));
            patient.addAddress(address);
        }

        // Phone
        if (patientData.containsKey("phoneHome")) {
            ContactPoint phone = new ContactPoint();
            phone.setSystem(ContactPoint.ContactPointSystem.PHONE);
            phone.setValue((String) patientData.get("phoneHome"));
            phone.setUse(ContactPoint.ContactPointUse.HOME);
            patient.addTelecom(phone);
        }

        return patient;
    }

    /**
     * Convert visit data to FHIR Encounter resource.
     */
    @SuppressWarnings("unchecked")
    private Encounter convertToEncounter(Map<String, Object> visitData,
                                          Map<String, Object> patientData) {
        Encounter encounter = new Encounter();

        // Status
        encounter.setStatus(Encounter.EncounterStatus.INPROGRESS);

        // Class
        if (visitData.containsKey("patientClass")) {
            String patientClass = (String) visitData.get("patientClass");
            Coding classCoding = new Coding();
            classCoding.setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode");
            classCoding.setCode(mapEncounterClass(patientClass));
            encounter.setClass_(classCoding);
        }

        // Subject (Patient reference)
        if (patientData != null && patientData.containsKey("patientId")) {
            Reference patientRef = new Reference();
            patientRef.setReference("Patient/" + patientData.get("patientId"));
            encounter.setSubject(patientRef);
        }

        // Identifier (Visit Number)
        if (visitData.containsKey("visitNumber")) {
            Identifier identifier = new Identifier();
            identifier.setValue((String) visitData.get("visitNumber"));
            identifier.setSystem("http://hospital.example.org/visits");
            encounter.addIdentifier(identifier);
        }

        // Period
        Period period = new Period();
        if (visitData.containsKey("admitDateTime")) {
            try {
                Date admitDate = parseHl7Date((String) visitData.get("admitDateTime"));
                period.setStart(admitDate);
            } catch (Exception e) {
                log.warn("Failed to parse admit date: {}", e.getMessage());
            }
        }
        if (visitData.containsKey("dischargeDateTime")) {
            try {
                Date dischargeDate = parseHl7Date((String) visitData.get("dischargeDateTime"));
                period.setEnd(dischargeDate);
            } catch (Exception e) {
                log.warn("Failed to parse discharge date: {}", e.getMessage());
            }
        }
        if (period.hasStart() || period.hasEnd()) {
            encounter.setPeriod(period);
        }

        // Location
        if (visitData.containsKey("location")) {
            Map<String, String> locationData = (Map<String, String>) visitData.get("location");
            Encounter.EncounterLocationComponent location =
                new Encounter.EncounterLocationComponent();
            Reference locationRef = new Reference();
            locationRef.setDisplay(String.format("%s-%s-%s",
                locationData.get("pointOfCare"),
                locationData.get("room"),
                locationData.get("bed")));
            location.setLocation(locationRef);
            encounter.addLocation(location);
        }

        return encounter;
    }

    /**
     * Convert observation data to FHIR Observation resource.
     */
    @SuppressWarnings("unchecked")
    private Observation convertToObservation(Map<String, Object> obsData,
                                              Map<String, Object> patientData) {
        Observation observation = new Observation();

        // Status
        if (obsData.containsKey("resultStatus")) {
            String status = (String) obsData.get("resultStatus");
            observation.setStatus(mapObservationStatus(status));
        } else {
            observation.setStatus(Observation.ObservationStatus.FINAL);
        }

        // Code
        if (obsData.containsKey("identifier")) {
            Map<String, String> identifier = (Map<String, String>) obsData.get("identifier");
            CodeableConcept code = new CodeableConcept();
            Coding coding = new Coding();
            coding.setCode(identifier.get("code"));
            coding.setDisplay(identifier.get("text"));
            coding.setSystem(mapCodingSystem(identifier.get("codingSystem")));
            code.addCoding(coding);
            observation.setCode(code);
        }

        // Subject (Patient reference)
        if (patientData != null && patientData.containsKey("patientId")) {
            Reference patientRef = new Reference();
            patientRef.setReference("Patient/" + patientData.get("patientId"));
            observation.setSubject(patientRef);
        }

        // Value
        if (obsData.containsKey("value")) {
            String value = (String) obsData.get("value");
            if (obsData.containsKey("units")) {
                Map<String, String> units = (Map<String, String>) obsData.get("units");
                Quantity quantity = new Quantity();
                try {
                    quantity.setValue(Double.parseDouble(value));
                } catch (NumberFormatException e) {
                    quantity.setValue(0);
                }
                quantity.setUnit(units.get("text"));
                quantity.setCode(units.get("identifier"));
                observation.setValue(quantity);
            } else {
                observation.setValue(new StringType(value));
            }
        }

        // Reference Range
        if (obsData.containsKey("referenceRange")) {
            Observation.ObservationReferenceRangeComponent range =
                new Observation.ObservationReferenceRangeComponent();
            range.setText((String) obsData.get("referenceRange"));
            observation.addReferenceRange(range);
        }

        // Effective Date/Time
        if (obsData.containsKey("observationDateTime")) {
            try {
                Date effectiveDate = parseHl7Date((String) obsData.get("observationDateTime"));
                observation.setEffective(new DateTimeType(effectiveDate));
            } catch (Exception e) {
                log.warn("Failed to parse observation date: {}", e.getMessage());
            }
        }

        return observation;
    }

    /**
     * Convert service request data to FHIR ServiceRequest resource.
     */
    @SuppressWarnings("unchecked")
    private ServiceRequest convertToServiceRequest(Map<String, Object> requestData,
                                                     Map<String, Object> patientData) {
        ServiceRequest serviceRequest = new ServiceRequest();

        // Status
        serviceRequest.setStatus(ServiceRequest.ServiceRequestStatus.ACTIVE);
        serviceRequest.setIntent(ServiceRequest.ServiceRequestIntent.ORDER);

        // Code
        if (requestData.containsKey("serviceId")) {
            Map<String, String> serviceId = (Map<String, String>) requestData.get("serviceId");
            CodeableConcept code = new CodeableConcept();
            Coding coding = new Coding();
            coding.setCode(serviceId.get("identifier"));
            coding.setDisplay(serviceId.get("text"));
            coding.setSystem(mapCodingSystem(serviceId.get("codingSystem")));
            code.addCoding(coding);
            serviceRequest.setCode(code);
        }

        // Subject (Patient reference)
        if (patientData != null && patientData.containsKey("patientId")) {
            Reference patientRef = new Reference();
            patientRef.setReference("Patient/" + patientData.get("patientId"));
            serviceRequest.setSubject(patientRef);
        }

        // Priority
        if (requestData.containsKey("priority")) {
            String priority = (String) requestData.get("priority");
            serviceRequest.setPriority(mapPriority(priority));
        }

        // Authored On
        if (requestData.containsKey("requestedDateTime")) {
            try {
                Date authoredOn = parseHl7Date((String) requestData.get("requestedDateTime"));
                serviceRequest.setAuthoredOn(authoredOn);
            } catch (Exception e) {
                log.warn("Failed to parse requested date: {}", e.getMessage());
            }
        }

        return serviceRequest;
    }

    /**
     * Add resource to bundle with transaction request.
     */
    private void addResourceToBundle(Bundle bundle, Resource resource, String resourceType) {
        Bundle.BundleEntryComponent entry = bundle.addEntry();
        entry.setResource(resource);

        Bundle.BundleEntryRequestComponent request = new Bundle.BundleEntryRequestComponent();
        request.setMethod(Bundle.HTTPVerb.POST);
        request.setUrl(resourceType);
        entry.setRequest(request);
    }

    /**
     * Map HL7 gender to FHIR gender.
     */
    private Enumerations.AdministrativeGender mapGender(String hl7Gender) {
        return switch (hl7Gender.toUpperCase()) {
            case "M" -> Enumerations.AdministrativeGender.MALE;
            case "F" -> Enumerations.AdministrativeGender.FEMALE;
            case "O" -> Enumerations.AdministrativeGender.OTHER;
            default -> Enumerations.AdministrativeGender.UNKNOWN;
        };
    }

    /**
     * Map HL7 patient class to FHIR encounter class.
     */
    private String mapEncounterClass(String patientClass) {
        return switch (patientClass.toUpperCase()) {
            case "I" -> "IMP"; // Inpatient
            case "O" -> "AMB"; // Outpatient/Ambulatory
            case "E" -> "EMER"; // Emergency
            default -> "AMB";
        };
    }

    /**
     * Map HL7 observation status to FHIR observation status.
     */
    private Observation.ObservationStatus mapObservationStatus(String hl7Status) {
        return switch (hl7Status.toUpperCase()) {
            case "F" -> Observation.ObservationStatus.FINAL;
            case "P" -> Observation.ObservationStatus.PRELIMINARY;
            case "C" -> Observation.ObservationStatus.CORRECTED;
            case "X" -> Observation.ObservationStatus.CANCELLED;
            default -> Observation.ObservationStatus.UNKNOWN;
        };
    }

    /**
     * Map HL7 priority to FHIR priority.
     */
    private ServiceRequest.ServiceRequestPriority mapPriority(String hl7Priority) {
        return switch (hl7Priority.toUpperCase()) {
            case "S" -> ServiceRequest.ServiceRequestPriority.STAT;
            case "A" -> ServiceRequest.ServiceRequestPriority.ASAP;
            case "R" -> ServiceRequest.ServiceRequestPriority.ROUTINE;
            case "U" -> ServiceRequest.ServiceRequestPriority.URGENT;
            default -> ServiceRequest.ServiceRequestPriority.ROUTINE;
        };
    }

    /**
     * Map HL7 coding system to FHIR system URI.
     */
    private String mapCodingSystem(String hl7System) {
        return switch (hl7System) {
            case "LN" -> "http://loinc.org";
            case "SNM", "SCT" -> "http://snomed.info/sct";
            case "CPT" -> "http://www.ama-assn.org/go/cpt";
            case "ICD10" -> "http://hl7.org/fhir/sid/icd-10";
            default -> "urn:oid:" + hl7System;
        };
    }

    /**
     * Parse HL7 date/time string to Java Date.
     */
    private Date parseHl7Date(String hl7Date) {
        if (hl7Date == null || hl7Date.isEmpty()) {
            return new Date();
        }

        try {
            String normalized = hl7Date;
            if (normalized.length() < 14) {
                normalized = String.format("%-14s", normalized).replace(' ', '0');
            }
            LocalDateTime localDateTime = LocalDateTime.parse(
                normalized.substring(0, 14), HL7_DATE_FORMAT);
            return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
        } catch (Exception e) {
            log.warn("Failed to parse HL7 date: {}", hl7Date);
            return new Date();
        }
    }
}
