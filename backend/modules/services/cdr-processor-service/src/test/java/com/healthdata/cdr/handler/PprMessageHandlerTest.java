package com.healthdata.cdr.handler;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v25.datatype.*;
import ca.uhn.hl7v2.model.v25.message.PPR_PC1;
import ca.uhn.hl7v2.model.v25.segment.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PprMessageHandler.
 * Tests PPR (Patient Problem) message processing.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PPR Message Handler Tests")
class PprMessageHandlerTest {

    @InjectMocks
    private PprMessageHandler handler;

    @Nested
    @DisplayName("PPR^PC1 - Problem Add")
    class PprPc1Tests {

        @Test
        @DisplayName("Should extract trigger event PC1")
        void handle_withPprPc1_extractsTriggerEvent() throws HL7Exception {
            PPR_PC1 ppr = mock(PPR_PC1.class);
            setupMinimalPpr(ppr, "PC1");

            Map<String, Object> result = handler.handle(ppr);

            assertThat(result).containsEntry("messageType", "PPR");
            assertThat(result).containsEntry("triggerEvent", "PC1");
            assertThat(result).containsEntry("eventDescription", "Problem add");
        }

        @Test
        @DisplayName("Should extract trigger event PC2 for problem update")
        void handle_withPprPc2_extractsTriggerEvent() throws HL7Exception {
            PPR_PC1 ppr = mock(PPR_PC1.class);
            setupMinimalPpr(ppr, "PC2");

            Map<String, Object> result = handler.handle(ppr);

            assertThat(result).containsEntry("triggerEvent", "PC2");
            assertThat(result).containsEntry("eventDescription", "Problem update");
        }

        @Test
        @DisplayName("Should extract trigger event PC3 for problem delete")
        void handle_withPprPc3_extractsTriggerEvent() throws HL7Exception {
            PPR_PC1 ppr = mock(PPR_PC1.class);
            setupMinimalPpr(ppr, "PC3");

            Map<String, Object> result = handler.handle(ppr);

            assertThat(result).containsEntry("triggerEvent", "PC3");
            assertThat(result).containsEntry("eventDescription", "Problem delete");
        }

        @Test
        @DisplayName("Should extract patient data")
        void handle_withPprPc1_extractsPatientData() throws HL7Exception {
            PPR_PC1 ppr = mock(PPR_PC1.class);
            setupPprWithPatient(ppr);

            Map<String, Object> result = handler.handle(ppr);

            assertThat(result).containsKey("patient");
            @SuppressWarnings("unchecked")
            Map<String, Object> patient = (Map<String, Object>) result.get("patient");
            assertThat(patient).containsEntry("patientId", "12345");
            assertThat(patient).containsEntry("familyName", "Smith");
        }
    }

    @Nested
    @DisplayName("Problem (PRB) Extraction")
    class ProblemTests {

        @Test
        @DisplayName("Should extract problem ID")
        void extractProblem_withProblemId_extractsId() throws HL7Exception {
            PPR_PC1 ppr = mock(PPR_PC1.class);
            setupPprWithProblem(ppr, "E11.9", "Type 2 diabetes");

            Map<String, Object> result = handler.handle(ppr);

            assertThat(result).containsKey("problems");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> problems = (List<Map<String, Object>>) result.get("problems");
            assertThat(problems).hasSize(1);
            @SuppressWarnings("unchecked")
            Map<String, String> problemId = (Map<String, String>) problems.get(0).get("problemId");
            assertThat(problemId).containsEntry("identifier", "E11.9");
        }

        @Test
        @DisplayName("Should extract problem action code")
        void extractProblem_withActionCode_extractsAction() throws HL7Exception {
            PPR_PC1 ppr = mock(PPR_PC1.class);
            setupPprWithProblemAction(ppr, "AD");

            Map<String, Object> result = handler.handle(ppr);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> problems = (List<Map<String, Object>>) result.get("problems");
            assertThat(problems.get(0)).containsEntry("actionCode", "AD");
        }

        @Test
        @DisplayName("Should extract problem confirmation status")
        void extractProblem_withConfirmationStatus_extractsStatus() throws HL7Exception {
            PPR_PC1 ppr = mock(PPR_PC1.class);
            setupPprWithProblemStatus(ppr, "C");

            Map<String, Object> result = handler.handle(ppr);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> problems = (List<Map<String, Object>>) result.get("problems");
            assertThat(problems.get(0)).containsEntry("confirmationStatus", "C");
        }

        @Test
        @DisplayName("Should extract problem notes")
        void extractProblem_withNotes_extractsNotes() throws HL7Exception {
            PPR_PC1 ppr = mock(PPR_PC1.class);
            setupPprWithProblemNotes(ppr, "Patient monitoring required");

            Map<String, Object> result = handler.handle(ppr);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> problems = (List<Map<String, Object>>) result.get("problems");
            @SuppressWarnings("unchecked")
            List<String> notes = (List<String>) problems.get(0).get("notes");
            assertThat(notes).contains("Patient monitoring required");
        }
    }

    @Nested
    @DisplayName("Goal (GOL) Extraction")
    class GoalTests {

        @Test
        @DisplayName("Should extract goal associated with problem")
        void extractGoal_withProblemGoal_extractsGoal() throws HL7Exception {
            PPR_PC1 ppr = mock(PPR_PC1.class);
            setupPprWithGoal(ppr, "GOAL001", "Reduce A1C");

            Map<String, Object> result = handler.handle(ppr);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> problems = (List<Map<String, Object>>) result.get("problems");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> goals = (List<Map<String, Object>>) problems.get(0).get("goals");
            assertThat(goals).hasSize(1);
        }

        @Test
        @DisplayName("Should extract goal action code")
        void extractGoal_withActionCode_extractsAction() throws HL7Exception {
            PPR_PC1 ppr = mock(PPR_PC1.class);
            setupPprWithGoalAction(ppr, "AD");

            Map<String, Object> result = handler.handle(ppr);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> problems = (List<Map<String, Object>>) result.get("problems");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> goals = (List<Map<String, Object>>) problems.get(0).get("goals");
            assertThat(goals.get(0)).containsEntry("actionCode", "AD");
        }
    }

    @Nested
    @DisplayName("Pathway (PTH) Extraction")
    class PathwayTests {

        @Test
        @DisplayName("Should extract pathway associated with problem")
        void extractPathway_withProblemPathway_extractsPathway() throws HL7Exception {
            PPR_PC1 ppr = mock(PPR_PC1.class);
            setupPprWithPathway(ppr, "PATH001", "Diabetes Care Pathway");

            Map<String, Object> result = handler.handle(ppr);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> problems = (List<Map<String, Object>>) result.get("problems");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> pathways = (List<Map<String, Object>>) problems.get(0).get("pathways");
            assertThat(pathways).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Unsupported Message Types")
    class UnsupportedMessageTests {

        @Test
        @DisplayName("Should handle unsupported PPR message type")
        void handle_withUnsupportedType_returnsBasicData() throws HL7Exception {
            ca.uhn.hl7v2.model.Message unknownMessage = mock(ca.uhn.hl7v2.model.Message.class);

            Map<String, Object> result = handler.handle(unknownMessage);

            assertThat(result).containsEntry("messageType", "PPR");
            assertThat(result).doesNotContainKey("triggerEvent");
        }
    }

    // Helper methods
    private void setupMinimalPpr(PPR_PC1 ppr, String triggerEvent) throws HL7Exception {
        MSH msh = mock(MSH.class);
        MSG messageType = mock(MSG.class);
        ID trigger = mock(ID.class);
        when(trigger.getValue()).thenReturn(triggerEvent);
        when(messageType.getTriggerEvent()).thenReturn(trigger);
        when(msh.getMessageType()).thenReturn(messageType);
        when(ppr.getMSH()).thenReturn(msh);

        when(ppr.getPID()).thenReturn(null);

        PPR_PC1.PATIENT_VISIT patientVisit = mock(PPR_PC1.PATIENT_VISIT.class);
        when(patientVisit.getPV1()).thenReturn(null);
        when(ppr.getPATIENT_VISIT()).thenReturn(patientVisit);

        when(ppr.getPROBLEMReps()).thenReturn(0);
    }

    private void setupPprWithPatient(PPR_PC1 ppr) throws HL7Exception {
        setupMinimalPpr(ppr, "PC1");

        PID pid = createMockPid();
        when(ppr.getPID()).thenReturn(pid);
    }

    private void setupPprWithProblem(PPR_PC1 ppr, String code, String text) throws HL7Exception {
        setupMinimalPpr(ppr, "PC1");
        when(ppr.getPROBLEMReps()).thenReturn(1);

        PPR_PC1.PROBLEM problem = mock(PPR_PC1.PROBLEM.class);
        when(ppr.getPROBLEM(0)).thenReturn(problem);

        PRB prb = createMockPrb(code, text);
        when(problem.getPRB()).thenReturn(prb);

        when(problem.getNTEReps()).thenReturn(0);
        when(problem.getVARReps()).thenReturn(0);
        when(problem.getPROBLEM_ROLEReps()).thenReturn(0);
        when(problem.getPROBLEM_OBSERVATIONReps()).thenReturn(0);
        when(problem.getGOALReps()).thenReturn(0);
        when(problem.getPATHWAYReps()).thenReturn(0);
        when(problem.getORDERReps()).thenReturn(0);
    }

    private void setupPprWithProblemAction(PPR_PC1 ppr, String action) throws HL7Exception {
        setupPprWithProblem(ppr, "E11.9", "Diabetes");

        PRB prb = ppr.getPROBLEM(0).getPRB();
        ID actionCode = mock(ID.class);
        when(actionCode.getValue()).thenReturn(action);
        when(prb.getActionCode()).thenReturn(actionCode);
    }

    private void setupPprWithProblemStatus(PPR_PC1 ppr, String status) throws HL7Exception {
        setupPprWithProblem(ppr, "E11.9", "Diabetes");

        PRB prb = ppr.getPROBLEM(0).getPRB();
        CE confirmationStatus = mock(CE.class);
        ST identifier = mock(ST.class);
        when(identifier.getValue()).thenReturn(status);
        when(confirmationStatus.getIdentifier()).thenReturn(identifier);
        when(prb.getProblemConfirmationStatus()).thenReturn(confirmationStatus);
    }

    private void setupPprWithProblemNotes(PPR_PC1 ppr, String noteText) throws HL7Exception {
        setupPprWithProblem(ppr, "E11.9", "Diabetes");

        PPR_PC1.PROBLEM problem = ppr.getPROBLEM(0);
        when(problem.getNTEReps()).thenReturn(1);

        NTE nte = mock(NTE.class);
        when(problem.getNTE(0)).thenReturn(nte);

        FT comment = mock(FT.class);
        when(comment.getValue()).thenReturn(noteText);
        when(nte.getComment(0)).thenReturn(comment);
    }

    private void setupPprWithGoal(PPR_PC1 ppr, String goalId, String goalText) throws HL7Exception {
        setupPprWithProblem(ppr, "E11.9", "Diabetes");

        PPR_PC1.PROBLEM problem = ppr.getPROBLEM(0);
        when(problem.getGOALReps()).thenReturn(1);

        PPR_PC1.PROBLEM.GOAL goal = mock(PPR_PC1.PROBLEM.GOAL.class);
        when(problem.getGOAL(0)).thenReturn(goal);

        GOL gol = createMockGol(goalId, goalText);
        when(goal.getGOL()).thenReturn(gol);
        when(goal.getGOAL_ROLEReps()).thenReturn(0);
        when(goal.getGOAL_OBSERVATIONReps()).thenReturn(0);
    }

    private void setupPprWithGoalAction(PPR_PC1 ppr, String action) throws HL7Exception {
        setupPprWithGoal(ppr, "GOAL001", "Test Goal");

        GOL gol = ppr.getPROBLEM(0).getGOAL(0).getGOL();
        ID actionCode = mock(ID.class);
        when(actionCode.getValue()).thenReturn(action);
        when(gol.getActionCode()).thenReturn(actionCode);
    }

    private void setupPprWithPathway(PPR_PC1 ppr, String pathwayId, String pathwayText) throws HL7Exception {
        setupPprWithProblem(ppr, "E11.9", "Diabetes");

        PPR_PC1.PROBLEM problem = ppr.getPROBLEM(0);
        when(problem.getPATHWAYReps()).thenReturn(1);

        PPR_PC1.PROBLEM.PATHWAY pathway = mock(PPR_PC1.PROBLEM.PATHWAY.class);
        when(problem.getPATHWAY(0)).thenReturn(pathway);

        PTH pth = createMockPth(pathwayId, pathwayText);
        when(pathway.getPTH()).thenReturn(pth);
    }

    private PID createMockPid() throws HL7Exception {
        PID pid = mock(PID.class);

        CX patientId = mock(CX.class);
        ST idNumber = mock(ST.class);
        when(idNumber.getValue()).thenReturn("12345");
        when(patientId.getIDNumber()).thenReturn(idNumber);

        ID idType = mock(ID.class);
        when(idType.getValue()).thenReturn("MR");
        when(patientId.getIdentifierTypeCode()).thenReturn(idType);
        when(pid.getPatientIdentifierList(0)).thenReturn(patientId);

        XPN patientName = mock(XPN.class);
        FN familyName = mock(FN.class);
        ST surname = mock(ST.class);
        when(surname.getValue()).thenReturn("Smith");
        when(familyName.getSurname()).thenReturn(surname);
        when(patientName.getFamilyName()).thenReturn(familyName);

        ST givenName = mock(ST.class);
        when(givenName.getValue()).thenReturn("John");
        when(patientName.getGivenName()).thenReturn(givenName);
        when(pid.getPatientName(0)).thenReturn(patientName);

        return pid;
    }

    private PRB createMockPrb(String code, String text) throws HL7Exception {
        PRB prb = mock(PRB.class);

        CE problemId = mock(CE.class);
        ST identifier = mock(ST.class);
        when(identifier.getValue()).thenReturn(code);
        when(problemId.getIdentifier()).thenReturn(identifier);

        ST codeText = mock(ST.class);
        when(codeText.getValue()).thenReturn(text);
        when(problemId.getText()).thenReturn(codeText);

        ID codingSystem = mock(ID.class);
        when(codingSystem.getValue()).thenReturn("ICD10");
        when(problemId.getNameOfCodingSystem()).thenReturn(codingSystem);

        when(prb.getProblemID()).thenReturn(problemId);

        return prb;
    }

    private GOL createMockGol(String goalId, String goalText) throws HL7Exception {
        GOL gol = mock(GOL.class);

        CE goalIdCe = mock(CE.class);
        ST identifier = mock(ST.class);
        when(identifier.getValue()).thenReturn(goalId);
        when(goalIdCe.getIdentifier()).thenReturn(identifier);

        ST codeText = mock(ST.class);
        when(codeText.getValue()).thenReturn(goalText);
        when(goalIdCe.getText()).thenReturn(codeText);

        ID codingSystem = mock(ID.class);
        when(codingSystem.getValue()).thenReturn("LOCAL");
        when(goalIdCe.getNameOfCodingSystem()).thenReturn(codingSystem);

        when(gol.getGoalID()).thenReturn(goalIdCe);

        return gol;
    }

    private PTH createMockPth(String pathwayId, String pathwayText) throws HL7Exception {
        PTH pth = mock(PTH.class);

        CE pathwayIdCe = mock(CE.class);
        ST identifier = mock(ST.class);
        when(identifier.getValue()).thenReturn(pathwayId);
        when(pathwayIdCe.getIdentifier()).thenReturn(identifier);

        ST codeText = mock(ST.class);
        when(codeText.getValue()).thenReturn(pathwayText);
        when(pathwayIdCe.getText()).thenReturn(codeText);

        when(pth.getPathwayID()).thenReturn(pathwayIdCe);

        return pth;
    }
}
