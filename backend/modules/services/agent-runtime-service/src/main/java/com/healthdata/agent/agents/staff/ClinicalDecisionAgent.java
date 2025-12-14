package com.healthdata.agent.agents.staff;

import com.healthdata.agent.agents.AgentDefinition;
import com.healthdata.agent.core.AgentContext;
import com.healthdata.agent.core.AgentOrchestrator;
import com.healthdata.agent.core.AgentOrchestrator.AgentRequest;
import com.healthdata.agent.core.AgentOrchestrator.AgentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Clinical Decision Support Agent.
 * Assists clinical staff with evidence-based decision support,
 * patient data analysis, and care recommendations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClinicalDecisionAgent implements AgentDefinition {

    private final AgentOrchestrator orchestrator;

    public static final String AGENT_TYPE = "clinical-decision";

    private static final String SYSTEM_PROMPT = """
        You are a Clinical Decision Support Assistant for the HDIM healthcare platform.
        Your role is to help clinical staff with evidence-based decision support.

        ## YOUR CAPABILITIES
        - Query patient health records via FHIR (demographics, conditions, medications, labs, vitals)
        - Evaluate clinical quality measures using CQL
        - Analyze patient risk factors and care gaps
        - Provide evidence-based clinical insights
        - Summarize patient health status

        ## AVAILABLE TOOLS
        - fhir_query: Query patient FHIR resources (Patient, Observation, Condition, MedicationRequest, etc.)
        - cql_execute: Evaluate CQL quality measures and clinical logic
        - publish_event: Trigger care gap alerts or follow-up workflows
        - send_notification: Alert care team members

        ## CRITICAL SAFETY GUIDELINES
        1. NEVER provide definitive diagnoses. Use phrases like "findings suggest" or "consider evaluating for"
        2. NEVER prescribe medications or specific dosages. Recommend "discuss with prescriber"
        3. NEVER advise stopping medications. Say "consult with the prescribing provider"
        4. ALWAYS recommend clinical review for high-risk findings
        5. ALWAYS cite evidence sources when possible (clinical guidelines, studies)
        6. NEVER dismiss emergency symptoms - escalate immediately

        ## RESPONSE STYLE
        - Be concise and clinically focused
        - Use standard medical terminology
        - Structure responses with clear sections (Assessment, Findings, Recommendations)
        - Include relevant lab values and vitals with dates
        - Flag critical or abnormal values

        ## WHEN UNCERTAIN
        - Acknowledge limitations clearly
        - Recommend specialist consultation
        - Suggest additional diagnostic tests if appropriate
        - Never fabricate clinical data

        Remember: You are a decision SUPPORT tool. All clinical decisions must be made by qualified healthcare providers.
        """;

    @Override
    public String getAgentType() {
        return AGENT_TYPE;
    }

    @Override
    public String getDisplayName() {
        return "Clinical Decision Support";
    }

    @Override
    public String getDescription() {
        return "Assists clinical staff with evidence-based decision support, patient data analysis, and care recommendations";
    }

    @Override
    public String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    @Override
    public List<String> getEnabledTools() {
        return List.of("fhir_query", "cql_execute", "publish_event", "send_notification");
    }

    @Override
    public Map<String, Object> getDefaultParameters() {
        return Map.of(
            "maxTokens", 4096,
            "temperature", 0.2,  // Lower for more consistent clinical responses
            "model", "claude-3-5-sonnet-20241022"
        );
    }

    /**
     * Get patient summary with clinical decision support.
     */
    public Mono<AgentResponse> getPatientSummary(String patientId, AgentContext context) {
        String message = String.format("""
            Please provide a comprehensive clinical summary for patient %s.
            Include:
            1. Demographics and key identifiers
            2. Active conditions and problem list
            3. Current medications
            4. Recent vital signs and trends
            5. Recent lab results with any concerning values
            6. Open care gaps
            7. Risk factors and clinical concerns
            8. Recommended follow-up actions
            """, patientId);

        context = context.toBuilder()
            .patientId(patientId)
            .agentType(AGENT_TYPE)
            .build();

        return execute(message, context);
    }

    /**
     * Analyze specific clinical question.
     */
    public Mono<AgentResponse> analyzeClinicalQuestion(String question, String patientId, AgentContext context) {
        context = context.toBuilder()
            .patientId(patientId)
            .agentType(AGENT_TYPE)
            .build();

        return execute(question, context);
    }

    /**
     * Evaluate care gaps for a patient.
     */
    public Mono<AgentResponse> evaluateCareGaps(String patientId, AgentContext context) {
        String message = String.format("""
            Evaluate care gaps for patient %s.
            1. Check all applicable quality measures (HEDIS, Star Ratings)
            2. Identify any open care gaps
            3. Prioritize gaps by clinical urgency
            4. Recommend specific interventions to close each gap
            5. Suggest appropriate outreach or scheduling
            """, patientId);

        context = context.toBuilder()
            .patientId(patientId)
            .agentType(AGENT_TYPE)
            .build();

        return execute(message, context);
    }

    /**
     * Medication review and reconciliation support.
     */
    public Mono<AgentResponse> reviewMedications(String patientId, AgentContext context) {
        String message = String.format("""
            Perform a medication review for patient %s.
            1. List all current medications with dosages
            2. Check for potential drug interactions
            3. Identify high-risk medications
            4. Review adherence concerns (if data available)
            5. Note any medications requiring monitoring
            6. Flag any polypharmacy concerns

            Note: Do NOT recommend specific medication changes. Flag concerns for prescriber review.
            """, patientId);

        context = context.toBuilder()
            .patientId(patientId)
            .agentType(AGENT_TYPE)
            .build();

        return execute(message, context);
    }

    /**
     * Lab result interpretation support.
     */
    public Mono<AgentResponse> interpretLabResults(String patientId, String labType, AgentContext context) {
        String message = String.format("""
            Analyze recent %s lab results for patient %s.
            1. List relevant lab values with reference ranges
            2. Identify any abnormal values
            3. Compare to previous values (trends)
            4. Provide clinical context for findings
            5. Suggest follow-up if indicated

            Note: Interpretation is for clinical decision support only.
            """, labType != null ? labType : "laboratory", patientId);

        context = context.toBuilder()
            .patientId(patientId)
            .agentType(AGENT_TYPE)
            .build();

        return execute(message, context);
    }

    @Override
    public Mono<AgentResponse> execute(String message, AgentContext context) {
        AgentRequest request = new AgentRequest(
            message,
            SYSTEM_PROMPT,
            (String) getDefaultParameters().get("model"),
            (Integer) getDefaultParameters().get("maxTokens"),
            (Double) getDefaultParameters().get("temperature"),
            getEnabledTools(),
            Map.of("agentType", AGENT_TYPE)
        );

        return orchestrator.execute(request, context);
    }

    @Override
    public Flux<AgentOrchestrator.AgentStreamEvent> executeStreaming(String message, AgentContext context) {
        AgentRequest request = new AgentRequest(
            message,
            SYSTEM_PROMPT,
            (String) getDefaultParameters().get("model"),
            (Integer) getDefaultParameters().get("maxTokens"),
            (Double) getDefaultParameters().get("temperature"),
            getEnabledTools(),
            Map.of("agentType", AGENT_TYPE)
        );

        return orchestrator.executeStreaming(request, context);
    }
}
