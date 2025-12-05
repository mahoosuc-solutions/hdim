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
