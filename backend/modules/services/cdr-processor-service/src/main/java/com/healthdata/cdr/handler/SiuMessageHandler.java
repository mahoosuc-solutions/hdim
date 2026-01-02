package com.healthdata.cdr.handler;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v25.message.SIU_S12;
import ca.uhn.hl7v2.model.v25.segment.AIG;
import ca.uhn.hl7v2.model.v25.segment.AIL;
import ca.uhn.hl7v2.model.v25.segment.AIP;
import ca.uhn.hl7v2.model.v25.segment.AIS;
import ca.uhn.hl7v2.model.v25.segment.PID;
import ca.uhn.hl7v2.model.v25.segment.PV1;
import ca.uhn.hl7v2.model.v25.segment.SCH;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler for SIU (Scheduling Information Unsolicited) messages.
 *
 * Supports:
 * - SIU^S12: Notification of new appointment booking
 * - SIU^S13: Notification of appointment rescheduling
 * - SIU^S14: Notification of appointment modification
 * - SIU^S15: Notification of appointment cancellation
 * - SIU^S17: Notification of appointment deletion
 * - SIU^S26: Notification of patient no-show
 *
 * Converts to FHIR Appointment resources.
 */
@Slf4j
@Component
public class SiuMessageHandler {

    /**
     * Handle SIU message and extract scheduling data.
     *
     * @param message Parsed HL7 message
     * @return Map of extracted data
     * @throws HL7Exception If message processing fails
     */
    public Map<String, Object> handle(Message message) throws HL7Exception {
        log.debug("Processing SIU message");

        Map<String, Object> data = new HashMap<>();
        data.put("messageType", "SIU");

        if (!(message instanceof SIU_S12)) {
            log.warn("Message is not SIU_S12 type: {}", message.getClass().getSimpleName());
            return data;
        }

        SIU_S12 siu = (SIU_S12) message;

        // Determine trigger event
        String triggerEvent = determineTriggerEvent(siu);
        data.put("triggerEvent", triggerEvent);
        data.put("eventDescription", getEventDescription(triggerEvent));

        // Extract scheduling data from SCH segment
        SCH sch = siu.getSCH();
        if (sch != null) {
            extractSchedulingData(sch, data);
        }

        // Extract patient data
        PID pid = siu.getPATIENT().getPID();
        if (pid != null) {
            extractPatientData(pid, data);
        }

        // Extract visit data
        PV1 pv1 = siu.getPATIENT().getPV1();
        if (pv1 != null) {
            extractVisitData(pv1, data);
        }

        // Extract resource information
        extractResources(siu, data);

        return data;
    }

    /**
     * Determine the trigger event from message structure.
     */
    private String determineTriggerEvent(SIU_S12 siu) throws HL7Exception {
        try {
            String msgType = siu.getMSH().getMessageType().getTriggerEvent().getValue();
            return msgType != null ? msgType : "S12";
        } catch (Exception e) {
            return "S12";
        }
    }

    /**
     * Get description for trigger event.
     */
    private String getEventDescription(String triggerEvent) {
        return switch (triggerEvent) {
            case "S12" -> "Notification of new appointment booking";
            case "S13" -> "Notification of appointment rescheduling";
            case "S14" -> "Notification of appointment modification";
            case "S15" -> "Notification of appointment cancellation";
            case "S17" -> "Notification of appointment deletion";
            case "S26" -> "Notification of patient no-show";
            default -> "Scheduling notification";
        };
    }

    /**
     * Extract scheduling data from SCH segment.
     */
    private void extractSchedulingData(SCH sch, Map<String, Object> data) throws HL7Exception {
        Map<String, Object> scheduleData = new HashMap<>();

        // Placer Appointment ID
        if (sch.getPlacerAppointmentID() != null) {
            scheduleData.put("placerAppointmentId",
                sch.getPlacerAppointmentID().getEntityIdentifier().getValue());
        }

        // Filler Appointment ID
        if (sch.getFillerAppointmentID() != null) {
            scheduleData.put("fillerAppointmentId",
                sch.getFillerAppointmentID().getEntityIdentifier().getValue());
        }

        // Occurrence Number
        if (sch.getOccurrenceNumber() != null) {
            scheduleData.put("occurrenceNumber", sch.getOccurrenceNumber().getValue());
        }

        // Placer Group Number
        if (sch.getPlacerGroupNumber() != null) {
            scheduleData.put("placerGroupNumber",
                sch.getPlacerGroupNumber().getEntityIdentifier().getValue());
        }

        // Schedule ID
        if (sch.getScheduleID() != null) {
            Map<String, String> scheduleId = new HashMap<>();
            scheduleId.put("identifier", sch.getScheduleID().getIdentifier().getValue());
            scheduleId.put("text", sch.getScheduleID().getText().getValue());
            scheduleData.put("scheduleId", scheduleId);
        }

        // Event Reason
        if (sch.getEventReason() != null) {
            Map<String, String> eventReason = new HashMap<>();
            eventReason.put("identifier", sch.getEventReason().getIdentifier().getValue());
            eventReason.put("text", sch.getEventReason().getText().getValue());
            scheduleData.put("eventReason", eventReason);
        }

        // Appointment Reason
        if (sch.getAppointmentReason() != null) {
            Map<String, String> appointmentReason = new HashMap<>();
            appointmentReason.put("identifier", sch.getAppointmentReason().getIdentifier().getValue());
            appointmentReason.put("text", sch.getAppointmentReason().getText().getValue());
            scheduleData.put("appointmentReason", appointmentReason);
        }

        // Appointment Type
        if (sch.getAppointmentType() != null) {
            Map<String, String> appointmentType = new HashMap<>();
            appointmentType.put("identifier", sch.getAppointmentType().getIdentifier().getValue());
            appointmentType.put("text", sch.getAppointmentType().getText().getValue());
            scheduleData.put("appointmentType", appointmentType);
        }

        // Appointment Duration
        if (sch.getAppointmentDuration() != null) {
            scheduleData.put("appointmentDuration", sch.getAppointmentDuration().getValue());
        }

        // Appointment Duration Units
        if (sch.getAppointmentDurationUnits() != null) {
            scheduleData.put("appointmentDurationUnits",
                sch.getAppointmentDurationUnits().getIdentifier().getValue());
        }

        // Appointment Timing Quantity
        if (sch.getAppointmentTimingQuantity(0) != null) {
            Map<String, Object> timing = new HashMap<>();
            if (sch.getAppointmentTimingQuantity(0).getStartDateTime() != null &&
                sch.getAppointmentTimingQuantity(0).getStartDateTime().getTime() != null) {
                timing.put("startDateTime",
                    sch.getAppointmentTimingQuantity(0).getStartDateTime().getTime().getValue());
            }
            if (sch.getAppointmentTimingQuantity(0).getEndDateTime() != null &&
                sch.getAppointmentTimingQuantity(0).getEndDateTime().getTime() != null) {
                timing.put("endDateTime",
                    sch.getAppointmentTimingQuantity(0).getEndDateTime().getTime().getValue());
            }
            scheduleData.put("timing", timing);
        }

        // Entered By Person
        if (sch.getEnteredByPerson(0) != null) {
            Map<String, String> enteredBy = new HashMap<>();
            enteredBy.put("id", sch.getEnteredByPerson(0).getIDNumber().getValue());
            enteredBy.put("familyName",
                sch.getEnteredByPerson(0).getFamilyName().getSurname().getValue());
            enteredBy.put("givenName", sch.getEnteredByPerson(0).getGivenName().getValue());
            scheduleData.put("enteredBy", enteredBy);
        }

        // Entered By Phone Number
        if (sch.getEnteredByPhoneNumber(0) != null) {
            scheduleData.put("enteredByPhone",
                sch.getEnteredByPhoneNumber(0).getTelephoneNumber().getValue());
        }

        // Filler Status Code
        if (sch.getFillerStatusCode() != null) {
            Map<String, String> status = new HashMap<>();
            status.put("identifier", sch.getFillerStatusCode().getIdentifier().getValue());
            status.put("text", sch.getFillerStatusCode().getText().getValue());
            scheduleData.put("fillerStatus", status);
        }

        data.put("schedule", scheduleData);
    }

    /**
     * Extract patient data from PID segment.
     */
    private void extractPatientData(PID pid, Map<String, Object> data) throws HL7Exception {
        Map<String, Object> patientData = new HashMap<>();

        // Patient ID
        if (pid.getPatientIdentifierList(0) != null) {
            patientData.put("patientId",
                pid.getPatientIdentifierList(0).getIDNumber().getValue());
            patientData.put("patientIdType",
                pid.getPatientIdentifierList(0).getIdentifierTypeCode().getValue());
        }

        // Patient Name
        if (pid.getPatientName(0) != null) {
            String familyName = pid.getPatientName(0).getFamilyName().getSurname().getValue();
            String givenName = pid.getPatientName(0).getGivenName().getValue();
            patientData.put("familyName", familyName);
            patientData.put("givenName", givenName);
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

        // Phone Number
        if (pid.getPhoneNumberHome(0) != null) {
            patientData.put("phoneHome",
                pid.getPhoneNumberHome(0).getTelephoneNumber().getValue());
        }

        data.put("patient", patientData);
    }

    /**
     * Extract visit data from PV1 segment.
     */
    private void extractVisitData(PV1 pv1, Map<String, Object> data) throws HL7Exception {
        Map<String, Object> visitData = new HashMap<>();

        // Visit Number
        if (pv1.getVisitNumber() != null) {
            visitData.put("visitNumber", pv1.getVisitNumber().getIDNumber().getValue());
        }

        // Patient Class
        if (pv1.getPatientClass() != null) {
            visitData.put("patientClass", pv1.getPatientClass().getValue());
        }

        // Assigned Location
        if (pv1.getAssignedPatientLocation() != null) {
            Map<String, String> location = new HashMap<>();
            location.put("pointOfCare",
                pv1.getAssignedPatientLocation().getPointOfCare().getValue());
            location.put("room", pv1.getAssignedPatientLocation().getRoom().getValue());
            location.put("bed", pv1.getAssignedPatientLocation().getBed().getValue());
            location.put("facility",
                pv1.getAssignedPatientLocation().getFacility().getNamespaceID().getValue());
            visitData.put("location", location);
        }

        // Attending Doctor
        if (pv1.getAttendingDoctor(0) != null) {
            Map<String, String> doctor = new HashMap<>();
            doctor.put("id", pv1.getAttendingDoctor(0).getIDNumber().getValue());
            doctor.put("familyName",
                pv1.getAttendingDoctor(0).getFamilyName().getSurname().getValue());
            doctor.put("givenName", pv1.getAttendingDoctor(0).getGivenName().getValue());
            visitData.put("attendingDoctor", doctor);
        }

        data.put("visit", visitData);
    }

    /**
     * Extract resource information (AIS, AIG, AIL, AIP segments).
     */
    private void extractResources(SIU_S12 siu, Map<String, Object> data) throws HL7Exception {
        List<Map<String, Object>> resources = new ArrayList<>();

        // Extract service information (AIS)
        int aisCount = siu.getRESOURCESReps();
        for (int i = 0; i < aisCount; i++) {
            try {
                AIS ais = siu.getRESOURCES(i).getSERVICE().getAIS();
                if (ais != null) {
                    Map<String, Object> service = new HashMap<>();
                    service.put("type", "SERVICE");

                    if (ais.getSetIDAIS() != null) {
                        service.put("setId", ais.getSetIDAIS().getValue());
                    }

                    if (ais.getUniversalServiceIdentifier() != null) {
                        Map<String, String> serviceId = new HashMap<>();
                        serviceId.put("identifier",
                            ais.getUniversalServiceIdentifier().getIdentifier().getValue());
                        serviceId.put("text",
                            ais.getUniversalServiceIdentifier().getText().getValue());
                        service.put("serviceIdentifier", serviceId);
                    }

                    if (ais.getStartDateTime() != null && ais.getStartDateTime().getTime() != null) {
                        service.put("startDateTime", ais.getStartDateTime().getTime().getValue());
                    }

                    if (ais.getDuration() != null) {
                        service.put("duration", ais.getDuration().getValue());
                    }

                    resources.add(service);
                }
            } catch (Exception e) {
                log.debug("No AIS segment at index {}", i);
            }
        }

        // Extract general resource (AIG)
        for (int i = 0; i < aisCount; i++) {
            try {
                int aigCount = siu.getRESOURCES(i).getGENERAL_RESOURCEReps();
                for (int j = 0; j < aigCount; j++) {
                    AIG aig = siu.getRESOURCES(i).getGENERAL_RESOURCE(j).getAIG();
                    if (aig != null) {
                        Map<String, Object> generalResource = new HashMap<>();
                        generalResource.put("type", "GENERAL_RESOURCE");

                        if (aig.getSetIDAIG() != null) {
                            generalResource.put("setId", aig.getSetIDAIG().getValue());
                        }

                        if (aig.getResourceID() != null) {
                            Map<String, String> resourceId = new HashMap<>();
                            resourceId.put("identifier", aig.getResourceID().getIdentifier().getValue());
                            resourceId.put("text", aig.getResourceID().getText().getValue());
                            generalResource.put("resourceId", resourceId);
                        }

                        if (aig.getResourceType() != null) {
                            generalResource.put("resourceType", aig.getResourceType().getIdentifier().getValue());
                        }

                        if (aig.getResourceGroup(0) != null) {
                            generalResource.put("resourceGroup",
                                aig.getResourceGroup(0).getIdentifier().getValue());
                        }

                        resources.add(generalResource);
                    }
                }
            } catch (Exception e) {
                log.debug("No AIG segment at index {}", i);
            }
        }

        // Extract location resource (AIL)
        for (int i = 0; i < aisCount; i++) {
            try {
                int ailCount = siu.getRESOURCES(i).getLOCATION_RESOURCEReps();
                for (int j = 0; j < ailCount; j++) {
                    AIL ail = siu.getRESOURCES(i).getLOCATION_RESOURCE(j).getAIL();
                    if (ail != null) {
                        Map<String, Object> locationResource = new HashMap<>();
                        locationResource.put("type", "LOCATION_RESOURCE");

                        if (ail.getSetIDAIL() != null) {
                            locationResource.put("setId", ail.getSetIDAIL().getValue());
                        }

                        if (ail.getLocationResourceID(0) != null) {
                            Map<String, String> location = new HashMap<>();
                            location.put("pointOfCare",
                                ail.getLocationResourceID(0).getPointOfCare().getValue());
                            location.put("room", ail.getLocationResourceID(0).getRoom().getValue());
                            location.put("bed", ail.getLocationResourceID(0).getBed().getValue());
                            locationResource.put("location", location);
                        }

                        if (ail.getLocationTypeAIL() != null) {
                            locationResource.put("locationType",
                                ail.getLocationTypeAIL().getIdentifier().getValue());
                        }

                        resources.add(locationResource);
                    }
                }
            } catch (Exception e) {
                log.debug("No AIL segment at index {}", i);
            }
        }

        // Extract personnel resource (AIP)
        for (int i = 0; i < aisCount; i++) {
            try {
                int aipCount = siu.getRESOURCES(i).getPERSONNEL_RESOURCEReps();
                for (int j = 0; j < aipCount; j++) {
                    AIP aip = siu.getRESOURCES(i).getPERSONNEL_RESOURCE(j).getAIP();
                    if (aip != null) {
                        Map<String, Object> personnelResource = new HashMap<>();
                        personnelResource.put("type", "PERSONNEL_RESOURCE");

                        if (aip.getSetIDAIP() != null) {
                            personnelResource.put("setId", aip.getSetIDAIP().getValue());
                        }

                        if (aip.getPersonnelResourceID(0) != null) {
                            Map<String, String> personnel = new HashMap<>();
                            personnel.put("id", aip.getPersonnelResourceID(0).getIDNumber().getValue());
                            personnel.put("familyName",
                                aip.getPersonnelResourceID(0).getFamilyName().getSurname().getValue());
                            personnel.put("givenName",
                                aip.getPersonnelResourceID(0).getGivenName().getValue());
                            personnelResource.put("personnel", personnel);
                        }

                        if (aip.getResourceType() != null) {
                            personnelResource.put("resourceType",
                                aip.getResourceType().getIdentifier().getValue());
                        }

                        resources.add(personnelResource);
                    }
                }
            } catch (Exception e) {
                log.debug("No AIP segment at index {}", i);
            }
        }

        if (!resources.isEmpty()) {
            data.put("resources", resources);
        }
    }
}
