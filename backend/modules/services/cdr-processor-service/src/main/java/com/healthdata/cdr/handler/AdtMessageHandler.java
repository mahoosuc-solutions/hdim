package com.healthdata.cdr.handler;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v25.message.*;
import ca.uhn.hl7v2.model.v25.segment.EVN;
import ca.uhn.hl7v2.model.v25.segment.PID;
import ca.uhn.hl7v2.model.v25.segment.PV1;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Handler for ADT (Admit/Discharge/Transfer) messages.
 *
 * Supports:
 * - ADT^A01: Admit/Visit notification
 * - ADT^A02: Transfer
 * - ADT^A03: Discharge
 * - ADT^A04: Register patient
 * - ADT^A05: Pre-admit a patient
 * - ADT^A06: Change outpatient to inpatient
 * - ADT^A07: Change inpatient to outpatient
 * - ADT^A08: Update patient information
 * - ADT^A11: Cancel admit/visit notification
 */
@Slf4j
@Component
public class AdtMessageHandler {

    /**
     * Handle ADT message and extract relevant data.
     *
     * @param message Parsed HL7 message
     * @return Map of extracted data
     * @throws HL7Exception If message processing fails
     */
    public Map<String, Object> handle(Message message) throws HL7Exception {
        log.debug("Processing ADT message");

        Map<String, Object> data = new HashMap<>();

        // Extract common segments
        PID pid = getPidSegment(message);
        PV1 pv1 = getPv1Segment(message);
        EVN evn = getEvnSegment(message);

        if (pid != null) {
            extractPatientData(pid, data);
        }

        if (pv1 != null) {
            extractVisitData(pv1, data);
        }

        if (evn != null) {
            extractEventData(evn, data);

            // Add FHIR-specific encounter status based on event type
            String eventTypeCode = evn.getEventTypeCode() != null ? evn.getEventTypeCode().getValue() : null;
            if (eventTypeCode != null) {
                data.put("fhirEncounterStatus", mapEventToEncounterStatus(eventTypeCode));
                data.put("isPatientClassChange", isPatientClassChangeEvent(eventTypeCode));
                data.put("isCancellation", isCancellationEvent(eventTypeCode));

                // For A06/A07 events, include the target patient class
                if (isPatientClassChangeEvent(eventTypeCode)) {
                    data.put("targetPatientClass", getTargetPatientClass(eventTypeCode));
                }
            }
        }

        return data;
    }

    /**
     * Get PID segment from message.
     * Note: ADT_A04 and ADT_A08 use ADT_A01 structure in HAPI HL7 library
     * ADT_A05 for A05, A14, A28, A31
     * ADT_A06 for A06, A07
     * ADT_A09 for A09, A10, A11
     */
    private PID getPidSegment(Message message) throws HL7Exception {
        if (message instanceof ADT_A01) {
            return ((ADT_A01) message).getPID();
        } else if (message instanceof ADT_A02) {
            return ((ADT_A02) message).getPID();
        } else if (message instanceof ADT_A03) {
            return ((ADT_A03) message).getPID();
        } else if (message instanceof ADT_A05) {
            return ((ADT_A05) message).getPID();
        } else if (message instanceof ADT_A06) {
            return ((ADT_A06) message).getPID();
        } else if (message instanceof ADT_A09) {
            return ((ADT_A09) message).getPID();
        }
        return null;
    }

    /**
     * Get PV1 segment from message.
     */
    private PV1 getPv1Segment(Message message) throws HL7Exception {
        if (message instanceof ADT_A01) {
            return ((ADT_A01) message).getPV1();
        } else if (message instanceof ADT_A02) {
            return ((ADT_A02) message).getPV1();
        } else if (message instanceof ADT_A03) {
            return ((ADT_A03) message).getPV1();
        } else if (message instanceof ADT_A05) {
            return ((ADT_A05) message).getPV1();
        } else if (message instanceof ADT_A06) {
            return ((ADT_A06) message).getPV1();
        } else if (message instanceof ADT_A09) {
            return ((ADT_A09) message).getPV1();
        }
        return null;
    }

    /**
     * Get EVN segment from message.
     */
    private EVN getEvnSegment(Message message) throws HL7Exception {
        if (message instanceof ADT_A01) {
            return ((ADT_A01) message).getEVN();
        } else if (message instanceof ADT_A02) {
            return ((ADT_A02) message).getEVN();
        } else if (message instanceof ADT_A03) {
            return ((ADT_A03) message).getEVN();
        } else if (message instanceof ADT_A05) {
            return ((ADT_A05) message).getEVN();
        } else if (message instanceof ADT_A06) {
            return ((ADT_A06) message).getEVN();
        } else if (message instanceof ADT_A09) {
            return ((ADT_A09) message).getEVN();
        }
        return null;
    }

    /**
     * Map ADT event type to FHIR Encounter status.
     *
     * @param eventTypeCode The HL7 event type code (A01, A02, etc.)
     * @return FHIR Encounter status string
     */
    public String mapEventToEncounterStatus(String eventTypeCode) {
        if (eventTypeCode == null) {
            return "unknown";
        }

        return switch (eventTypeCode) {
            case "A01" -> "in-progress";      // Admit - patient is admitted
            case "A02" -> "in-progress";      // Transfer - still in progress
            case "A03" -> "finished";         // Discharge - encounter finished
            case "A04" -> "in-progress";      // Register - outpatient in progress
            case "A05" -> "planned";          // Pre-admit - planned encounter
            case "A06" -> "in-progress";      // Outpatient → Inpatient - still in progress
            case "A07" -> "in-progress";      // Inpatient → Outpatient - still in progress
            case "A08" -> "in-progress";      // Update - no status change
            case "A11" -> "cancelled";        // Cancel admit - cancelled
            default -> "unknown";
        };
    }

    /**
     * Determine if the event represents a class change (outpatient/inpatient).
     *
     * @param eventTypeCode The HL7 event type code
     * @return true if this is a patient class change event
     */
    public boolean isPatientClassChangeEvent(String eventTypeCode) {
        return "A06".equals(eventTypeCode) || "A07".equals(eventTypeCode);
    }

    /**
     * Determine if the event is a cancellation event.
     *
     * @param eventTypeCode The HL7 event type code
     * @return true if this is a cancellation event
     */
    public boolean isCancellationEvent(String eventTypeCode) {
        return "A11".equals(eventTypeCode);
    }

    /**
     * Get the target patient class for A06/A07 events.
     *
     * @param eventTypeCode The HL7 event type code
     * @return Target patient class (I for inpatient, O for outpatient)
     */
    public String getTargetPatientClass(String eventTypeCode) {
        return switch (eventTypeCode) {
            case "A06" -> "I";  // Outpatient → Inpatient
            case "A07" -> "O";  // Inpatient → Outpatient
            default -> null;
        };
    }

    /**
     * Extract patient demographic data from PID segment.
     */
    private void extractPatientData(PID pid, Map<String, Object> data) throws HL7Exception {
        Map<String, Object> patientData = new HashMap<>();

        // Patient ID
        if (pid.getPatientID() != null && pid.getPatientID().getIDNumber() != null) {
            patientData.put("patientId", pid.getPatientID().getIDNumber().getValue());
        }

        // Patient Identifier List
        if (pid.getPatientIdentifierList(0) != null) {
            patientData.put("patientIdentifier",
                pid.getPatientIdentifierList(0).getIDNumber().getValue());
        }

        // Patient Name
        if (pid.getPatientName(0) != null) {
            String familyName = pid.getPatientName(0).getFamilyName().getSurname().getValue();
            String givenName = pid.getPatientName(0).getGivenName().getValue();
            String middleName = pid.getPatientName(0).getSecondAndFurtherGivenNamesOrInitialsThereof().getValue();

            patientData.put("familyName", familyName);
            patientData.put("givenName", givenName);
            if (middleName != null && !middleName.isEmpty()) {
                patientData.put("middleName", middleName);
            }
            patientData.put("fullName", givenName + " " + familyName);
        }

        // Date of Birth
        if (pid.getDateTimeOfBirth() != null && pid.getDateTimeOfBirth().getTime() != null) {
            patientData.put("dateOfBirth", pid.getDateTimeOfBirth().getTime().getValue());
        }

        // Gender
        if (pid.getAdministrativeSex() != null) {
            patientData.put("gender", pid.getAdministrativeSex().getValue());
        }

        // Address
        if (pid.getPatientAddress(0) != null) {
            Map<String, String> address = new HashMap<>();
            address.put("street", pid.getPatientAddress(0).getStreetAddress().getStreetOrMailingAddress().getValue());
            address.put("city", pid.getPatientAddress(0).getCity().getValue());
            address.put("state", pid.getPatientAddress(0).getStateOrProvince().getValue());
            address.put("zip", pid.getPatientAddress(0).getZipOrPostalCode().getValue());
            address.put("country", pid.getPatientAddress(0).getCountry().getValue());
            patientData.put("address", address);
        }

        // Phone
        if (pid.getPhoneNumberHome(0) != null) {
            patientData.put("phoneHome", pid.getPhoneNumberHome(0).getTelephoneNumber().getValue());
        }

        // SSN
        if (pid.getSSNNumberPatient() != null) {
            patientData.put("ssn", pid.getSSNNumberPatient().getValue());
        }

        data.put("patient", patientData);
    }

    /**
     * Extract visit/encounter data from PV1 segment.
     */
    private void extractVisitData(PV1 pv1, Map<String, Object> data) throws HL7Exception {
        Map<String, Object> visitData = new HashMap<>();

        // Patient Class (Inpatient, Outpatient, Emergency, etc.)
        if (pv1.getPatientClass() != null) {
            visitData.put("patientClass", pv1.getPatientClass().getValue());
        }

        // Assigned Patient Location
        if (pv1.getAssignedPatientLocation() != null) {
            Map<String, String> location = new HashMap<>();
            location.put("pointOfCare", pv1.getAssignedPatientLocation().getPointOfCare().getValue());
            location.put("room", pv1.getAssignedPatientLocation().getRoom().getValue());
            location.put("bed", pv1.getAssignedPatientLocation().getBed().getValue());
            location.put("facility", pv1.getAssignedPatientLocation().getFacility().getNamespaceID().getValue());
            visitData.put("location", location);
        }

        // Attending Doctor
        if (pv1.getAttendingDoctor(0) != null) {
            Map<String, String> doctor = new HashMap<>();
            doctor.put("id", pv1.getAttendingDoctor(0).getIDNumber().getValue());
            doctor.put("familyName", pv1.getAttendingDoctor(0).getFamilyName().getSurname().getValue());
            doctor.put("givenName", pv1.getAttendingDoctor(0).getGivenName().getValue());
            visitData.put("attendingDoctor", doctor);
        }

        // Visit Number
        if (pv1.getVisitNumber() != null && pv1.getVisitNumber().getIDNumber() != null) {
            visitData.put("visitNumber", pv1.getVisitNumber().getIDNumber().getValue());
        }

        // Admission Type
        if (pv1.getAdmissionType() != null) {
            visitData.put("admissionType", pv1.getAdmissionType().getValue());
        }

        // Admit Date/Time
        if (pv1.getAdmitDateTime() != null && pv1.getAdmitDateTime().getTime() != null) {
            visitData.put("admitDateTime", pv1.getAdmitDateTime().getTime().getValue());
        }

        // Discharge Date/Time
        if (pv1.getDischargeDateTime(0) != null && pv1.getDischargeDateTime(0).getTime() != null) {
            visitData.put("dischargeDateTime", pv1.getDischargeDateTime(0).getTime().getValue());
        }

        data.put("visit", visitData);
    }

    /**
     * Extract event data from EVN segment.
     */
    private void extractEventData(EVN evn, Map<String, Object> data) throws HL7Exception {
        Map<String, Object> eventData = new HashMap<>();

        // Event Type Code
        if (evn.getEventTypeCode() != null) {
            eventData.put("eventTypeCode", evn.getEventTypeCode().getValue());
        }

        // Recorded Date/Time
        if (evn.getRecordedDateTime() != null && evn.getRecordedDateTime().getTime() != null) {
            eventData.put("recordedDateTime", evn.getRecordedDateTime().getTime().getValue());
        }

        // Event Occurred
        if (evn.getEventOccurred() != null && evn.getEventOccurred().getTime() != null) {
            eventData.put("eventOccurred", evn.getEventOccurred().getTime().getValue());
        }

        data.put("event", eventData);
    }
}
