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
 * Documentation Assistant Agent.
 * Helps clinical staff with documentation tasks including
 * attestations, summaries, and compliance documentation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentationAssistantAgent implements AgentDefinition {

    private final AgentOrchestrator orchestrator;

    public static final String AGENT_TYPE = "documentation-assistant";

    private static final String SYSTEM_PROMPT = """
        You are a Healthcare Documentation Assistant for the HDIM platform.
        Your role is to help clinical and quality staff with documentation tasks.

        ## YOUR CAPABILITIES
        - Generate measure attestation documentation
        - Create patient summaries for transitions of care
        - Draft quality improvement documentation
        - Prepare audit response materials
        - Generate care coordination notes
        - Create patient education materials

        ## AVAILABLE TOOLS
        - fhir_query: Query patient data for documentation
        - cql_execute: Evaluate measures for attestation
        - publish_event: Trigger documentation workflows
        - send_notification: Send documentation for review

        ## DOCUMENTATION TYPES

        ### 1. Measure Attestation
        Required elements:
        - Patient identifier (de-identified in output)
        - Measure ID and description
        - Attestation statement
        - Evidence summary
        - Date and responsible party
        - Supporting data points

        ### 2. Care Transition Summary
        Required elements:
        - Patient demographics
        - Reason for transition
        - Active diagnoses
        - Current medications with dosages
        - Recent procedures/tests
        - Pending orders/referrals
        - Follow-up requirements
        - Emergency contact info
        - Care team contacts

        ### 3. Quality Improvement Documentation
        Required elements:
        - Opportunity identification
        - Root cause analysis
        - Intervention description
        - Expected outcomes
        - Measurement plan
        - Timeline
        - Responsible parties

        ### 4. Audit Response Materials
        Required elements:
        - Request reference
        - Evidence summary
        - Supporting documentation list
        - Gap explanation (if any)
        - Corrective action (if needed)

        ## FORMATTING STANDARDS
        - Use clear, professional language
        - Include all required elements
        - Date stamp all documentation
        - Use standard medical terminology
        - Format for electronic health record compatibility
        - Include appropriate headers and sections

        ## COMPLIANCE REQUIREMENTS
        - HIPAA: No unnecessary PHI exposure
        - Attestation: Accurate representation of evidence
        - Timeliness: Include relevant dates
        - Completeness: All required elements present
        - Accuracy: Cross-reference source data

        ## QUALITY CHECKS
        Before finalizing documentation:
        1. Verify all required elements are present
        2. Check for internal consistency
        3. Confirm data accuracy against sources
        4. Ensure appropriate format
        5. Flag any missing information

        Remember: Documentation must be accurate, complete, and compliant.
        """;

    @Override
    public String getAgentType() {
        return AGENT_TYPE;
    }

    @Override
    public String getDisplayName() {
        return "Documentation Assistant";
    }

    @Override
    public String getDescription() {
        return "Helps clinical staff with attestations, summaries, and compliance documentation";
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
            "temperature", 0.1,  // Very low for consistent documentation
            "model", "claude-3-5-sonnet-20241022"
        );
    }

    /**
     * Generate measure attestation documentation.
     */
    public Mono<AgentResponse> generateAttestation(String patientId, String measureId, AgentContext context) {
        String message = String.format("""
            Generate attestation documentation for:
            - Patient: %s
            - Measure: %s

            Steps:
            1. Query patient data relevant to the measure
            2. Evaluate the measure using CQL
            3. Document the evidence supporting measure compliance
            4. Generate a formal attestation statement
            5. Include all required elements

            Format as a formal attestation document ready for signature.
            """, patientId, measureId);

        context = context.toBuilder()
            .patientId(patientId)
            .agentType(AGENT_TYPE)
            .build();

        return execute(message, context);
    }

    /**
     * Generate care transition summary.
     */
    public Mono<AgentResponse> generateTransitionSummary(String patientId, String transitionType, AgentContext context) {
        String message = String.format("""
            Generate a Care Transition Summary for patient %s.
            Transition type: %s

            Include:
            1. Patient identification and demographics
            2. Reason for care transition
            3. Active problem list with ICD codes
            4. Current medication list with dosages and frequencies
            5. Recent vital signs
            6. Recent lab results (last 30 days)
            7. Recent procedures and imaging
            8. Pending orders and referrals
            9. Follow-up appointments needed
            10. Primary care and specialist contacts
            11. Emergency contacts
            12. Special instructions or precautions

            Format for receiving provider/facility.
            """, patientId, transitionType != null ? transitionType : "care coordination");

        context = context.toBuilder()
            .patientId(patientId)
            .agentType(AGENT_TYPE)
            .build();

        return execute(message, context);
    }

    /**
     * Generate patient visit summary.
     */
    public Mono<AgentResponse> generateVisitSummary(String patientId, String encounterId, AgentContext context) {
        String message = String.format("""
            Generate an After Visit Summary for patient %s, encounter %s.

            Include:
            1. Visit date and provider
            2. Chief complaint and diagnoses addressed
            3. Key findings from examination
            4. Tests ordered or performed
            5. Medications prescribed or changed
            6. Instructions provided
            7. Follow-up appointments scheduled
            8. When to seek immediate care
            9. Contact information for questions

            Format in patient-friendly language at 6th grade reading level.
            """, patientId, encounterId);

        context = context.toBuilder()
            .patientId(patientId)
            .encounterId(encounterId)
            .agentType(AGENT_TYPE)
            .build();

        return execute(message, context);
    }

    /**
     * Generate quality improvement documentation.
     */
    public Mono<AgentResponse> generateQIDocumentation(String measureId, String improvementArea, AgentContext context) {
        String message = String.format("""
            Generate Quality Improvement documentation for measure %s.
            Focus area: %s

            Include:
            1. Current Performance
               - Baseline rate
               - Benchmark comparison
               - Gap analysis

            2. Opportunity Assessment
               - Root cause analysis
               - Contributing factors
               - Population affected

            3. Intervention Plan
               - Proposed interventions
               - Evidence base
               - Expected impact

            4. Implementation
               - Timeline
               - Resources needed
               - Responsible parties
               - Training requirements

            5. Measurement
               - Success metrics
               - Monitoring frequency
               - Reporting plan

            Format as a formal QI project document.
            """, measureId, improvementArea != null ? improvementArea : "overall improvement");

        context = context.toBuilder()
            .agentType(AGENT_TYPE)
            .build();

        return execute(message, context);
    }

    /**
     * Generate audit response documentation.
     */
    public Mono<AgentResponse> generateAuditResponse(String auditId, List<String> requestedItems, AgentContext context) {
        String items = requestedItems != null && !requestedItems.isEmpty()
            ? String.join(", ", requestedItems)
            : "all requested items";

        String message = String.format("""
            Generate audit response documentation for audit %s.
            Requested items: %s

            For each requested item:
            1. Acknowledge the request
            2. Provide evidence summary
            3. List supporting documents
            4. Note any gaps or limitations
            5. Describe corrective actions if applicable

            Include:
            - Response cover letter
            - Evidence index
            - Item-by-item response
            - Supporting documentation references

            Format as formal audit response package.
            """, auditId != null ? auditId : "current", items);

        context = context.toBuilder()
            .agentType(AGENT_TYPE)
            .build();

        return execute(message, context);
    }

    /**
     * Generate care gap closure documentation.
     */
    public Mono<AgentResponse> documentGapClosure(String patientId, String measureId, String closureEvidence, AgentContext context) {
        String message = String.format("""
            Document care gap closure for:
            - Patient: %s
            - Measure: %s
            - Evidence: %s

            Steps:
            1. Verify measure denominator eligibility
            2. Confirm numerator compliance based on evidence
            3. Document the closure event
            4. Generate audit-ready documentation
            5. Include all supporting data points

            Format as care gap closure attestation.
            """, patientId, measureId, closureEvidence != null ? closureEvidence : "service completed");

        context = context.toBuilder()
            .patientId(patientId)
            .agentType(AGENT_TYPE)
            .build();

        return execute(message, context);
    }

    /**
     * Generate patient education materials.
     */
    public Mono<AgentResponse> generatePatientEducation(String topic, String patientId, AgentContext context) {
        String message = String.format("""
            Generate patient education materials on: %s
            %s

            Requirements:
            1. Written at 6th grade reading level
            2. Use simple, clear language
            3. Include visual descriptions where helpful
            4. Provide actionable steps
            5. Include when to contact healthcare provider
            6. Add reliable resources for more information

            Format as patient handout with:
            - Clear title
            - Key points bulleted
            - Step-by-step instructions
            - Warning signs section
            - Contact information
            """, topic, patientId != null ? "Personalized for patient: " + patientId : "General education");

        if (patientId != null) {
            context = context.toBuilder()
                .patientId(patientId)
                .agentType(AGENT_TYPE)
                .build();
        } else {
            context = context.toBuilder()
                .agentType(AGENT_TYPE)
                .build();
        }

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
