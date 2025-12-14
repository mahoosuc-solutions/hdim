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
 * Care Gap Optimizer Agent.
 * Helps care coordinators prioritize and close care gaps
 * with intelligent outreach recommendations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CareGapOptimizerAgent implements AgentDefinition {

    private final AgentOrchestrator orchestrator;

    public static final String AGENT_TYPE = "care-gap-optimizer";

    private static final String SYSTEM_PROMPT = """
        You are a Care Gap Optimization Assistant for the HDIM healthcare platform.
        Your role is to help care coordinators prioritize and close care gaps effectively.

        ## YOUR CAPABILITIES
        - Query patient care gaps and quality measure status
        - Prioritize care gaps based on clinical urgency and impact
        - Recommend optimal outreach strategies
        - Identify barriers to care gap closure
        - Suggest scheduling and coordination actions
        - Track care gap closure trends

        ## AVAILABLE TOOLS
        - fhir_query: Query patient data, care plans, and appointments
        - cql_execute: Evaluate quality measures and care gap criteria
        - publish_event: Trigger outreach workflows and care gap alerts
        - send_notification: Send reminders to patients or care team

        ## PRIORITIZATION FRAMEWORK

        ### Clinical Urgency (Weight: 40%)
        - HIGH: Overdue cancer screenings, uncontrolled chronic conditions, missed immunizations
        - MEDIUM: Routine preventive care, wellness visits
        - LOW: Supplemental measures, optional screenings

        ### Quality Impact (Weight: 30%)
        - Star Ratings impact (4-5 star measures higher priority)
        - HEDIS measure weight
        - Contractual incentive alignment

        ### Closure Probability (Weight: 20%)
        - Patient engagement history
        - Previous outreach success
        - Barrier complexity

        ### Time Sensitivity (Weight: 10%)
        - Measurement year deadlines
        - Seasonal appropriateness (flu shots, etc.)

        ## OUTREACH STRATEGIES
        Based on patient preferences and gap type:
        1. Phone outreach - Best for complex gaps, high-risk patients
        2. Text/SMS - Best for appointment reminders, simple actions
        3. Patient portal - Best for tech-savvy patients, educational content
        4. Mail - Best for patients without digital access
        5. In-person at next visit - Best for bundling opportunities

        ## BARRIER IDENTIFICATION
        Common barriers to address:
        - Transportation issues
        - Financial/coverage concerns
        - Language barriers
        - Health literacy
        - Provider availability
        - Patient reluctance/fear

        ## RESPONSE FORMAT
        When analyzing care gaps, provide:
        1. Prioritized list with scores
        2. Recommended action for each gap
        3. Suggested outreach method
        4. Talking points for patient contact
        5. Scheduling recommendations
        6. Barrier mitigation strategies

        Remember: Your goal is to help close care gaps efficiently while respecting patient preferences.
        """;

    @Override
    public String getAgentType() {
        return AGENT_TYPE;
    }

    @Override
    public String getDisplayName() {
        return "Care Gap Optimizer";
    }

    @Override
    public String getDescription() {
        return "Helps care coordinators prioritize and close care gaps with intelligent outreach recommendations";
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
            "temperature", 0.3,
            "model", "claude-3-5-sonnet-20241022"
        );
    }

    /**
     * Get prioritized care gap worklist for a patient panel.
     */
    public Mono<AgentResponse> getPrioritizedWorklist(List<String> patientIds, AgentContext context) {
        String message = String.format("""
            Analyze and prioritize care gaps for the following patient panel:
            Patient IDs: %s

            For each patient with open gaps:
            1. List all open care gaps
            2. Calculate priority score using the prioritization framework
            3. Recommend specific outreach actions
            4. Suggest optimal contact method
            5. Note any known barriers

            Provide a consolidated worklist sorted by priority.
            """, String.join(", ", patientIds));

        context = context.toBuilder()
            .agentType(AGENT_TYPE)
            .build();

        return execute(message, context);
    }

    /**
     * Optimize outreach for a specific patient.
     */
    public Mono<AgentResponse> optimizePatientOutreach(String patientId, AgentContext context) {
        String message = String.format("""
            Create an optimized outreach plan for patient %s.

            1. Identify all open care gaps
            2. Check patient contact preferences
            3. Review previous outreach history and outcomes
            4. Identify potential barriers
            5. Create a personalized outreach script
            6. Recommend best time and method to contact
            7. Suggest bundling opportunities (address multiple gaps in one contact)
            8. Provide backup strategies if initial outreach fails
            """, patientId);

        context = context.toBuilder()
            .patientId(patientId)
            .agentType(AGENT_TYPE)
            .build();

        return execute(message, context);
    }

    /**
     * Analyze care gap closure opportunities at upcoming appointments.
     */
    public Mono<AgentResponse> analyzeAppointmentOpportunities(String patientId, AgentContext context) {
        String message = String.format("""
            Analyze care gap closure opportunities for patient %s at their upcoming appointments.

            1. Check upcoming scheduled appointments
            2. Match open care gaps to appointment types
            3. Identify gaps that can be addressed at each visit
            4. Recommend pre-visit preparation (orders, referrals)
            5. Create a visit checklist for the care team
            6. Note any gaps requiring separate scheduling
            """, patientId);

        context = context.toBuilder()
            .patientId(patientId)
            .agentType(AGENT_TYPE)
            .build();

        return execute(message, context);
    }

    /**
     * Generate outreach campaign for a specific measure.
     */
    public Mono<AgentResponse> generateMeasureCampaign(String measureId, AgentContext context) {
        String message = String.format("""
            Generate an outreach campaign for quality measure %s.

            1. Identify all patients with open gaps for this measure
            2. Segment patients by:
               - Urgency level
               - Preferred contact method
               - Barrier type
            3. Create targeted messaging for each segment
            4. Recommend campaign timeline
            5. Set follow-up triggers
            6. Define success metrics
            """, measureId);

        context = context.toBuilder()
            .agentType(AGENT_TYPE)
            .build();

        return execute(message, context);
    }

    /**
     * Identify high-value patients for care gap closure.
     */
    public Mono<AgentResponse> identifyHighValuePatients(String measureId, int limit, AgentContext context) {
        String message = String.format("""
            Identify the top %d patients to target for care gap closure on measure %s.

            Prioritization criteria:
            1. High probability of successful closure
            2. Positive previous engagement
            3. Multiple gaps that can be bundled
            4. Approaching care opportunities (scheduled visits)
            5. Minimal barriers identified

            For each patient, provide:
            - Priority score and rationale
            - Recommended action
            - Best contact approach
            - Talking points
            """, limit, measureId);

        context = context.toBuilder()
            .agentType(AGENT_TYPE)
            .build();

        return execute(message, context);
    }

    /**
     * Analyze barriers preventing care gap closure.
     */
    public Mono<AgentResponse> analyzeBarriers(String patientId, AgentContext context) {
        String message = String.format("""
            Analyze barriers preventing care gap closure for patient %s.

            1. Review patient history and demographics
            2. Check previous outreach attempts and outcomes
            3. Identify potential SDOH factors
            4. Assess access barriers (transportation, coverage)
            5. Evaluate engagement barriers (health literacy, language)
            6. Recommend barrier mitigation strategies
            7. Suggest community resources if applicable
            """, patientId);

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
