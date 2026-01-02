package com.healthdata.cdr.handler;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v25.message.PPR_PC1;
import ca.uhn.hl7v2.model.v25.segment.GOL;
import ca.uhn.hl7v2.model.v25.segment.NTE;
import ca.uhn.hl7v2.model.v25.segment.OBR;
import ca.uhn.hl7v2.model.v25.segment.OBX;
import ca.uhn.hl7v2.model.v25.segment.ORC;
import ca.uhn.hl7v2.model.v25.segment.PID;
import ca.uhn.hl7v2.model.v25.segment.PRB;
import ca.uhn.hl7v2.model.v25.segment.PTH;
import ca.uhn.hl7v2.model.v25.segment.PV1;
import ca.uhn.hl7v2.model.v25.segment.ROL;
import ca.uhn.hl7v2.model.v25.segment.VAR;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler for PPR (Patient Problem) messages.
 *
 * Supports:
 * - PPR^PC1: Problem add
 * - PPR^PC2: Problem update
 * - PPR^PC3: Problem delete
 *
 * Converts to FHIR Condition, CarePlan, and Goal resources.
 */
@Slf4j
@Component
public class PprMessageHandler {

    /**
     * Handle PPR message and extract problem data.
     *
     * @param message Parsed HL7 message
     * @return Map of extracted data
     * @throws HL7Exception If message processing fails
     */
    public Map<String, Object> handle(Message message) throws HL7Exception {
        log.debug("Processing PPR message");

        Map<String, Object> data = new HashMap<>();
        data.put("messageType", "PPR");

        if (message instanceof PPR_PC1) {
            handlePprPc1((PPR_PC1) message, data);
        } else {
            log.warn("Unsupported PPR message type: {}", message.getClass().getSimpleName());
        }

        return data;
    }

    /**
     * Handle PPR^PC1/PC2/PC3 messages.
     */
    private void handlePprPc1(PPR_PC1 ppr, Map<String, Object> data) throws HL7Exception {
        // Determine trigger event from MSH
        String triggerEvent = determineTriggerEvent(ppr);
        data.put("triggerEvent", triggerEvent);
        data.put("eventDescription", getEventDescription(triggerEvent));

        // Extract patient data
        PID pid = ppr.getPID();
        if (pid != null) {
            extractPatientData(pid, data);
        }

        // Note: Provider roles (ROL) may be available in different structures
        // depending on the HL7 v2 version and message profile

        // Extract visit data
        PV1 pv1 = ppr.getPATIENT_VISIT().getPV1();
        if (pv1 != null) {
            extractVisitData(pv1, data);
        }

        // Extract problems (PRB segments)
        int problemCount = ppr.getPROBLEMReps();
        if (problemCount > 0) {
            List<Map<String, Object>> problems = new ArrayList<>();
            for (int i = 0; i < problemCount; i++) {
                Map<String, Object> problemData = new HashMap<>();

                // PRB segment - Problem details
                PRB prb = ppr.getPROBLEM(i).getPRB();
                if (prb != null) {
                    extractProblem(prb, problemData);
                }

                // Problem Notes (NTE)
                int nteCount = ppr.getPROBLEM(i).getNTEReps();
                if (nteCount > 0) {
                    List<String> notes = new ArrayList<>();
                    for (int j = 0; j < nteCount; j++) {
                        NTE nte = ppr.getPROBLEM(i).getNTE(j);
                        if (nte.getComment(0) != null) {
                            notes.add(nte.getComment(0).getValue());
                        }
                    }
                    problemData.put("notes", notes);
                }

                // Problem Variance (VAR)
                int varCount = ppr.getPROBLEM(i).getVARReps();
                if (varCount > 0) {
                    List<Map<String, Object>> variances = new ArrayList<>();
                    for (int j = 0; j < varCount; j++) {
                        VAR var = ppr.getPROBLEM(i).getVAR(j);
                        variances.add(extractVariance(var));
                    }
                    problemData.put("variances", variances);
                }

                // Problem Roles
                int problemRolCount = ppr.getPROBLEM(i).getPROBLEM_ROLEReps();
                if (problemRolCount > 0) {
                    List<Map<String, Object>> problemRoles = new ArrayList<>();
                    for (int j = 0; j < problemRolCount; j++) {
                        ROL rol = ppr.getPROBLEM(i).getPROBLEM_ROLE(j).getROL();
                        if (rol != null) {
                            problemRoles.add(extractRole(rol));
                        }
                    }
                    problemData.put("roles", problemRoles);
                }

                // Problem Observations
                int obsCount = ppr.getPROBLEM(i).getPROBLEM_OBSERVATIONReps();
                if (obsCount > 0) {
                    List<Map<String, Object>> observations = new ArrayList<>();
                    for (int j = 0; j < obsCount; j++) {
                        OBX obx = ppr.getPROBLEM(i).getPROBLEM_OBSERVATION(j).getOBX();
                        if (obx != null) {
                            observations.add(extractObservation(obx));
                        }
                    }
                    problemData.put("observations", observations);
                }

                // Goals associated with problem
                int goalCount = ppr.getPROBLEM(i).getGOALReps();
                if (goalCount > 0) {
                    List<Map<String, Object>> goals = new ArrayList<>();
                    for (int j = 0; j < goalCount; j++) {
                        Map<String, Object> goalData = new HashMap<>();

                        GOL gol = ppr.getPROBLEM(i).getGOAL(j).getGOL();
                        if (gol != null) {
                            extractGoal(gol, goalData);
                        }

                        // Goal Roles
                        int goalRolCount = ppr.getPROBLEM(i).getGOAL(j).getGOAL_ROLEReps();
                        if (goalRolCount > 0) {
                            List<Map<String, Object>> goalRoles = new ArrayList<>();
                            for (int k = 0; k < goalRolCount; k++) {
                                ROL rol = ppr.getPROBLEM(i).getGOAL(j).getGOAL_ROLE(k).getROL();
                                if (rol != null) {
                                    goalRoles.add(extractRole(rol));
                                }
                            }
                            goalData.put("roles", goalRoles);
                        }

                        // Goal Observations
                        int goalObsCount = ppr.getPROBLEM(i).getGOAL(j).getGOAL_OBSERVATIONReps();
                        if (goalObsCount > 0) {
                            List<Map<String, Object>> goalObs = new ArrayList<>();
                            for (int k = 0; k < goalObsCount; k++) {
                                OBX obx = ppr.getPROBLEM(i).getGOAL(j).getGOAL_OBSERVATION(k).getOBX();
                                if (obx != null) {
                                    goalObs.add(extractObservation(obx));
                                }
                            }
                            goalData.put("observations", goalObs);
                        }

                        goals.add(goalData);
                    }
                    problemData.put("goals", goals);
                }

                // Pathways associated with problem
                int pathwayCount = ppr.getPROBLEM(i).getPATHWAYReps();
                if (pathwayCount > 0) {
                    List<Map<String, Object>> pathways = new ArrayList<>();
                    for (int j = 0; j < pathwayCount; j++) {
                        PTH pth = ppr.getPROBLEM(i).getPATHWAY(j).getPTH();
                        if (pth != null) {
                            pathways.add(extractPathway(pth));
                        }
                    }
                    problemData.put("pathways", pathways);
                }

                // Orders associated with problem
                int orderCount = ppr.getPROBLEM(i).getORDERReps();
                if (orderCount > 0) {
                    List<Map<String, Object>> orders = new ArrayList<>();
                    for (int j = 0; j < orderCount; j++) {
                        Map<String, Object> orderData = new HashMap<>();

                        ORC orc = ppr.getPROBLEM(i).getORDER(j).getORC();
                        if (orc != null) {
                            extractOrderControl(orc, orderData);
                        }

                        // Order Detail
                        OBR obr = ppr.getPROBLEM(i).getORDER(j).getORDER_DETAIL().getOBR();
                        if (obr != null) {
                            extractOrderDetail(obr, orderData);
                        }

                        // Order Observations
                        int orderObsCount = ppr.getPROBLEM(i).getORDER(j).getORDER_DETAIL()
                            .getORDER_OBSERVATIONReps();
                        if (orderObsCount > 0) {
                            List<Map<String, Object>> orderObs = new ArrayList<>();
                            for (int k = 0; k < orderObsCount; k++) {
                                OBX obx = ppr.getPROBLEM(i).getORDER(j).getORDER_DETAIL()
                                    .getORDER_OBSERVATION(k).getOBX();
                                if (obx != null) {
                                    orderObs.add(extractObservation(obx));
                                }
                            }
                            orderData.put("observations", orderObs);
                        }

                        orders.add(orderData);
                    }
                    problemData.put("orders", orders);
                }

                problems.add(problemData);
            }
            data.put("problems", problems);
        }
    }

    /**
     * Determine trigger event from message.
     */
    private String determineTriggerEvent(PPR_PC1 ppr) throws HL7Exception {
        try {
            String event = ppr.getMSH().getMessageType().getTriggerEvent().getValue();
            return event != null ? event : "PC1";
        } catch (Exception e) {
            return "PC1";
        }
    }

    /**
     * Get event description.
     */
    private String getEventDescription(String triggerEvent) {
        return switch (triggerEvent) {
            case "PC1" -> "Problem add";
            case "PC2" -> "Problem update";
            case "PC3" -> "Problem delete";
            default -> "Problem notification";
        };
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
     * Extract problem from PRB segment.
     */
    private void extractProblem(PRB prb, Map<String, Object> data) throws HL7Exception {
        // Action Code
        if (prb.getActionCode() != null) {
            data.put("actionCode", prb.getActionCode().getValue());
        }

        // Action Date/Time
        if (prb.getActionDateTime() != null && prb.getActionDateTime().getTime() != null) {
            data.put("actionDateTime", prb.getActionDateTime().getTime().getValue());
        }

        // Problem ID
        if (prb.getProblemID() != null) {
            Map<String, String> problemId = new HashMap<>();
            problemId.put("identifier", prb.getProblemID().getIdentifier().getValue());
            problemId.put("text", prb.getProblemID().getText().getValue());
            problemId.put("codingSystem", prb.getProblemID().getNameOfCodingSystem().getValue());
            data.put("problemId", problemId);
        }

        // Problem Instance ID
        if (prb.getProblemInstanceID() != null) {
            data.put("problemInstanceId", prb.getProblemInstanceID().getEntityIdentifier().getValue());
        }

        // Episode of Care ID
        if (prb.getEpisodeOfCareID() != null) {
            data.put("episodeOfCareId", prb.getEpisodeOfCareID().getEntityIdentifier().getValue());
        }

        // Problem List Priority
        if (prb.getProblemListPriority() != null) {
            data.put("listPriority", prb.getProblemListPriority().getValue());
        }

        // Problem Established Date/Time
        if (prb.getProblemEstablishedDateTime() != null &&
            prb.getProblemEstablishedDateTime().getTime() != null) {
            data.put("establishedDateTime", prb.getProblemEstablishedDateTime().getTime().getValue());
        }

        // Anticipated Problem Resolution Date/Time
        if (prb.getAnticipatedProblemResolutionDateTime() != null &&
            prb.getAnticipatedProblemResolutionDateTime().getTime() != null) {
            data.put("anticipatedResolutionDateTime",
                prb.getAnticipatedProblemResolutionDateTime().getTime().getValue());
        }

        // Actual Problem Resolution Date/Time
        if (prb.getActualProblemResolutionDateTime() != null &&
            prb.getActualProblemResolutionDateTime().getTime() != null) {
            data.put("actualResolutionDateTime",
                prb.getActualProblemResolutionDateTime().getTime().getValue());
        }

        // Problem Classification
        if (prb.getProblemClassification() != null) {
            data.put("classification", prb.getProblemClassification().getIdentifier().getValue());
        }

        // Problem Management Discipline
        if (prb.getProblemManagementDiscipline(0) != null) {
            data.put("managementDiscipline",
                prb.getProblemManagementDiscipline(0).getIdentifier().getValue());
        }

        // Problem Persistence
        if (prb.getProblemPersistence() != null) {
            data.put("persistence", prb.getProblemPersistence().getIdentifier().getValue());
        }

        // Problem Confirmation Status
        if (prb.getProblemConfirmationStatus() != null) {
            data.put("confirmationStatus", prb.getProblemConfirmationStatus().getIdentifier().getValue());
        }

        // Problem Life Cycle Status
        if (prb.getProblemLifeCycleStatus() != null) {
            data.put("lifeCycleStatus", prb.getProblemLifeCycleStatus().getIdentifier().getValue());
        }

        // Problem Life Cycle Status Date/Time
        if (prb.getProblemLifeCycleStatusDateTime() != null &&
            prb.getProblemLifeCycleStatusDateTime().getTime() != null) {
            data.put("lifeCycleStatusDateTime",
                prb.getProblemLifeCycleStatusDateTime().getTime().getValue());
        }

        // Problem Date of Onset
        if (prb.getProblemDateOfOnset() != null && prb.getProblemDateOfOnset().getTime() != null) {
            data.put("dateOfOnset", prb.getProblemDateOfOnset().getTime().getValue());
        }

        // Problem Onset Text
        if (prb.getProblemOnsetText() != null) {
            data.put("onsetText", prb.getProblemOnsetText().getValue());
        }

        // Problem Ranking
        if (prb.getProblemRanking() != null) {
            data.put("ranking", prb.getProblemRanking().getIdentifier().getValue());
        }

        // Certainty of Problem
        if (prb.getCertaintyOfProblem() != null) {
            data.put("certainty", prb.getCertaintyOfProblem().getIdentifier().getValue());
        }

        // Probability of Problem
        if (prb.getProbabilityOfProblem() != null) {
            data.put("probability", prb.getProbabilityOfProblem().getValue());
        }

        // Individual Awareness of Problem
        if (prb.getIndividualAwarenessOfProblem() != null) {
            data.put("individualAwareness", prb.getIndividualAwarenessOfProblem().getIdentifier().getValue());
        }

        // Problem Prognosis
        if (prb.getProblemPrognosis() != null) {
            data.put("prognosis", prb.getProblemPrognosis().getIdentifier().getValue());
        }

        // Individual Awareness of Prognosis
        if (prb.getIndividualAwarenessOfPrognosis() != null) {
            data.put("prognosisAwareness",
                prb.getIndividualAwarenessOfPrognosis().getIdentifier().getValue());
        }

        // Family/Significant Other Awareness of Problem/Prognosis
        if (prb.getFamilySignificantOtherAwarenessOfProblemPrognosis() != null) {
            data.put("familyAwareness",
                prb.getFamilySignificantOtherAwarenessOfProblemPrognosis().getValue());
        }

        // Security/Sensitivity
        if (prb.getSecuritySensitivity() != null) {
            data.put("securitySensitivity", prb.getSecuritySensitivity().getIdentifier().getValue());
        }
    }

    /**
     * Extract goal from GOL segment.
     */
    private void extractGoal(GOL gol, Map<String, Object> data) throws HL7Exception {
        // Action Code
        if (gol.getActionCode() != null) {
            data.put("actionCode", gol.getActionCode().getValue());
        }

        // Action Date/Time
        if (gol.getActionDateTime() != null && gol.getActionDateTime().getTime() != null) {
            data.put("actionDateTime", gol.getActionDateTime().getTime().getValue());
        }

        // Goal ID
        if (gol.getGoalID() != null) {
            Map<String, String> goalId = new HashMap<>();
            goalId.put("identifier", gol.getGoalID().getIdentifier().getValue());
            goalId.put("text", gol.getGoalID().getText().getValue());
            goalId.put("codingSystem", gol.getGoalID().getNameOfCodingSystem().getValue());
            data.put("goalId", goalId);
        }

        // Goal Instance ID
        if (gol.getGoalInstanceID() != null) {
            data.put("goalInstanceId", gol.getGoalInstanceID().getEntityIdentifier().getValue());
        }

        // Episode of Care ID
        if (gol.getEpisodeOfCareID() != null) {
            data.put("episodeOfCareId", gol.getEpisodeOfCareID().getEntityIdentifier().getValue());
        }

        // Goal List Priority
        if (gol.getGoalListPriority() != null) {
            data.put("listPriority", gol.getGoalListPriority().getValue());
        }

        // Goal Established Date/Time
        if (gol.getGoalEstablishedDateTime() != null &&
            gol.getGoalEstablishedDateTime().getTime() != null) {
            data.put("establishedDateTime", gol.getGoalEstablishedDateTime().getTime().getValue());
        }

        // Expected Goal Achieve Date/Time
        if (gol.getExpectedGoalAchieveDateTime() != null &&
            gol.getExpectedGoalAchieveDateTime().getTime() != null) {
            data.put("expectedAchieveDateTime",
                gol.getExpectedGoalAchieveDateTime().getTime().getValue());
        }

        // Goal Classification
        if (gol.getGoalClassification() != null) {
            data.put("classification", gol.getGoalClassification().getIdentifier().getValue());
        }

        // Goal Management Discipline
        if (gol.getGoalManagementDiscipline() != null) {
            data.put("managementDiscipline", gol.getGoalManagementDiscipline().getIdentifier().getValue());
        }

        // Current Goal Review Status
        if (gol.getCurrentGoalReviewStatus() != null) {
            data.put("currentReviewStatus", gol.getCurrentGoalReviewStatus().getIdentifier().getValue());
        }

        // Current Goal Review Date/Time
        if (gol.getCurrentGoalReviewDateTime() != null &&
            gol.getCurrentGoalReviewDateTime().getTime() != null) {
            data.put("currentReviewDateTime",
                gol.getCurrentGoalReviewDateTime().getTime().getValue());
        }

        // Next Goal Review Date/Time
        if (gol.getNextGoalReviewDateTime() != null &&
            gol.getNextGoalReviewDateTime().getTime() != null) {
            data.put("nextReviewDateTime", gol.getNextGoalReviewDateTime().getTime().getValue());
        }

        // Previous Goal Review Date/Time
        if (gol.getPreviousGoalReviewDateTime() != null &&
            gol.getPreviousGoalReviewDateTime().getTime() != null) {
            data.put("previousReviewDateTime",
                gol.getPreviousGoalReviewDateTime().getTime().getValue());
        }

        // Goal Life Cycle Status
        if (gol.getGoalLifeCycleStatus() != null) {
            data.put("lifeCycleStatus", gol.getGoalLifeCycleStatus().getIdentifier().getValue());
        }

        // Goal Life Cycle Status Date/Time
        if (gol.getGoalLifeCycleStatusDateTime() != null &&
            gol.getGoalLifeCycleStatusDateTime().getTime() != null) {
            data.put("lifeCycleStatusDateTime",
                gol.getGoalLifeCycleStatusDateTime().getTime().getValue());
        }

        // Goal Target Type
        if (gol.getGoalTargetType(0) != null) {
            data.put("targetType", gol.getGoalTargetType(0).getIdentifier().getValue());
        }

        // Goal Target Name
        if (gol.getGoalTargetName(0) != null) {
            data.put("targetName", gol.getGoalTargetName(0).getFamilyName().getSurname().getValue());
        }
    }

    /**
     * Extract pathway from PTH segment.
     */
    private Map<String, Object> extractPathway(PTH pth) throws HL7Exception {
        Map<String, Object> pathway = new HashMap<>();

        // Action Code
        if (pth.getActionCode() != null) {
            pathway.put("actionCode", pth.getActionCode().getValue());
        }

        // Pathway ID
        if (pth.getPathwayID() != null) {
            Map<String, String> pathwayId = new HashMap<>();
            pathwayId.put("identifier", pth.getPathwayID().getIdentifier().getValue());
            pathwayId.put("text", pth.getPathwayID().getText().getValue());
            pathway.put("pathwayId", pathwayId);
        }

        // Pathway Instance ID
        if (pth.getPathwayInstanceID() != null) {
            pathway.put("pathwayInstanceId", pth.getPathwayInstanceID().getEntityIdentifier().getValue());
        }

        // Pathway Established Date/Time
        if (pth.getPathwayEstablishedDateTime() != null &&
            pth.getPathwayEstablishedDateTime().getTime() != null) {
            pathway.put("establishedDateTime",
                pth.getPathwayEstablishedDateTime().getTime().getValue());
        }

        // Pathway Life Cycle Status
        if (pth.getPathwayLifeCycleStatus() != null) {
            pathway.put("lifeCycleStatus", pth.getPathwayLifeCycleStatus().getIdentifier().getValue());
        }

        // Change Pathway Life Cycle Status Date/Time
        if (pth.getChangePathwayLifeCycleStatusDateTime() != null &&
            pth.getChangePathwayLifeCycleStatusDateTime().getTime() != null) {
            pathway.put("lifeCycleStatusChangeDateTime",
                pth.getChangePathwayLifeCycleStatusDateTime().getTime().getValue());
        }

        return pathway;
    }

    /**
     * Extract role from ROL segment.
     */
    private Map<String, Object> extractRole(ROL rol) throws HL7Exception {
        Map<String, Object> role = new HashMap<>();

        // Role Instance ID
        if (rol.getRoleInstanceID() != null) {
            role.put("roleInstanceId", rol.getRoleInstanceID().getEntityIdentifier().getValue());
        }

        // Action Code
        if (rol.getActionCode() != null) {
            role.put("actionCode", rol.getActionCode().getValue());
        }

        // Role-ROL
        if (rol.getRoleROL() != null) {
            Map<String, String> roleCode = new HashMap<>();
            roleCode.put("identifier", rol.getRoleROL().getIdentifier().getValue());
            roleCode.put("text", rol.getRoleROL().getText().getValue());
            role.put("role", roleCode);
        }

        // Role Person
        if (rol.getRolePerson(0) != null) {
            Map<String, String> person = new HashMap<>();
            person.put("id", rol.getRolePerson(0).getIDNumber().getValue());
            person.put("familyName", rol.getRolePerson(0).getFamilyName().getSurname().getValue());
            person.put("givenName", rol.getRolePerson(0).getGivenName().getValue());
            role.put("person", person);
        }

        // Role Begin Date/Time
        if (rol.getRoleBeginDateTime() != null && rol.getRoleBeginDateTime().getTime() != null) {
            role.put("beginDateTime", rol.getRoleBeginDateTime().getTime().getValue());
        }

        // Role End Date/Time
        if (rol.getRoleEndDateTime() != null && rol.getRoleEndDateTime().getTime() != null) {
            role.put("endDateTime", rol.getRoleEndDateTime().getTime().getValue());
        }

        // Role Duration
        if (rol.getRoleDuration() != null) {
            role.put("duration", rol.getRoleDuration().getIdentifier().getValue());
        }

        // Role Action Reason
        if (rol.getRoleActionReason() != null) {
            role.put("actionReason", rol.getRoleActionReason().getIdentifier().getValue());
        }

        // Provider Type
        if (rol.getProviderType(0) != null) {
            role.put("providerType", rol.getProviderType(0).getIdentifier().getValue());
        }

        // Organization Unit Type
        if (rol.getOrganizationUnitType() != null) {
            role.put("organizationUnitType", rol.getOrganizationUnitType().getIdentifier().getValue());
        }

        return role;
    }

    /**
     * Extract variance from VAR segment.
     */
    private Map<String, Object> extractVariance(VAR var) throws HL7Exception {
        Map<String, Object> variance = new HashMap<>();

        // Variance Instance ID
        if (var.getVarianceInstanceID() != null) {
            variance.put("varianceInstanceId", var.getVarianceInstanceID().getEntityIdentifier().getValue());
        }

        // Documented Date/Time
        if (var.getDocumentedDateTime() != null && var.getDocumentedDateTime().getTime() != null) {
            variance.put("documentedDateTime", var.getDocumentedDateTime().getTime().getValue());
        }

        // Stated Variance Date/Time
        if (var.getStatedVarianceDateTime() != null && var.getStatedVarianceDateTime().getTime() != null) {
            variance.put("statedVarianceDateTime", var.getStatedVarianceDateTime().getTime().getValue());
        }

        // Variance Originator
        if (var.getVarianceOriginator(0) != null) {
            Map<String, String> originator = new HashMap<>();
            originator.put("id", var.getVarianceOriginator(0).getIDNumber().getValue());
            originator.put("familyName", var.getVarianceOriginator(0).getFamilyName().getSurname().getValue());
            originator.put("givenName", var.getVarianceOriginator(0).getGivenName().getValue());
            variance.put("originator", originator);
        }

        // Variance Classification
        if (var.getVarianceClassification() != null) {
            variance.put("classification", var.getVarianceClassification().getIdentifier().getValue());
        }

        // Variance Description
        if (var.getVarianceDescription(0) != null) {
            variance.put("description", var.getVarianceDescription(0).getValue());
        }

        return variance;
    }

    /**
     * Extract observation from OBX segment.
     */
    private Map<String, Object> extractObservation(OBX obx) throws HL7Exception {
        Map<String, Object> obs = new HashMap<>();

        if (obx.getSetIDOBX() != null) {
            obs.put("setId", obx.getSetIDOBX().getValue());
        }

        if (obx.getValueType() != null) {
            obs.put("valueType", obx.getValueType().getValue());
        }

        if (obx.getObservationIdentifier() != null) {
            Map<String, String> id = new HashMap<>();
            id.put("identifier", obx.getObservationIdentifier().getIdentifier().getValue());
            id.put("text", obx.getObservationIdentifier().getText().getValue());
            id.put("codingSystem", obx.getObservationIdentifier().getNameOfCodingSystem().getValue());
            obs.put("observationId", id);
        }

        if (obx.getObservationValue(0) != null) {
            obs.put("value", obx.getObservationValue(0).getData().toString());
        }

        if (obx.getUnits() != null) {
            obs.put("units", obx.getUnits().getIdentifier().getValue());
        }

        if (obx.getObservationResultStatus() != null) {
            obs.put("resultStatus", obx.getObservationResultStatus().getValue());
        }

        if (obx.getDateTimeOfTheObservation() != null &&
            obx.getDateTimeOfTheObservation().getTime() != null) {
            obs.put("observationDateTime", obx.getDateTimeOfTheObservation().getTime().getValue());
        }

        return obs;
    }

    /**
     * Extract order control from ORC segment.
     */
    private void extractOrderControl(ORC orc, Map<String, Object> data) throws HL7Exception {
        // Order Control
        if (orc.getOrderControl() != null) {
            data.put("orderControl", orc.getOrderControl().getValue());
        }

        // Placer Order Number
        if (orc.getPlacerOrderNumber() != null) {
            data.put("placerOrderNumber", orc.getPlacerOrderNumber().getEntityIdentifier().getValue());
        }

        // Filler Order Number
        if (orc.getFillerOrderNumber() != null) {
            data.put("fillerOrderNumber", orc.getFillerOrderNumber().getEntityIdentifier().getValue());
        }

        // Order Status
        if (orc.getOrderStatus() != null) {
            data.put("orderStatus", orc.getOrderStatus().getValue());
        }

        // Ordering Provider
        if (orc.getOrderingProvider(0) != null) {
            Map<String, String> provider = new HashMap<>();
            provider.put("id", orc.getOrderingProvider(0).getIDNumber().getValue());
            provider.put("familyName", orc.getOrderingProvider(0).getFamilyName().getSurname().getValue());
            provider.put("givenName", orc.getOrderingProvider(0).getGivenName().getValue());
            data.put("orderingProvider", provider);
        }
    }

    /**
     * Extract order detail from OBR segment.
     */
    private void extractOrderDetail(OBR obr, Map<String, Object> data) throws HL7Exception {
        if (obr.getSetIDOBR() != null) {
            data.put("setId", obr.getSetIDOBR().getValue());
        }

        if (obr.getUniversalServiceIdentifier() != null) {
            Map<String, String> serviceId = new HashMap<>();
            serviceId.put("identifier", obr.getUniversalServiceIdentifier().getIdentifier().getValue());
            serviceId.put("text", obr.getUniversalServiceIdentifier().getText().getValue());
            serviceId.put("codingSystem",
                obr.getUniversalServiceIdentifier().getNameOfCodingSystem().getValue());
            data.put("serviceId", serviceId);
        }

        if (obr.getRequestedDateTime() != null && obr.getRequestedDateTime().getTime() != null) {
            data.put("requestedDateTime", obr.getRequestedDateTime().getTime().getValue());
        }

        if (obr.getObservationDateTime() != null && obr.getObservationDateTime().getTime() != null) {
            data.put("observationDateTime", obr.getObservationDateTime().getTime().getValue());
        }

        if (obr.getPriorityOBR() != null) {
            data.put("priority", obr.getPriorityOBR().getValue());
        }
    }
}
