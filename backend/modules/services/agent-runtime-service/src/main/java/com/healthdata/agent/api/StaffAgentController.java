package com.healthdata.agent.api;

import com.healthdata.agent.agents.AgentRegistry;
import com.healthdata.agent.agents.staff.CareGapOptimizerAgent;
import com.healthdata.agent.agents.staff.ClinicalDecisionAgent;
import com.healthdata.agent.agents.staff.DocumentationAssistantAgent;
import com.healthdata.agent.agents.staff.ReportGeneratorAgent;
import com.healthdata.agent.core.AgentContext;
import com.healthdata.agent.core.AgentOrchestrator.AgentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * REST API for Staff AI Agents.
 * Provides specialized endpoints for clinical and quality staff agents.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/staff-agents")
@RequiredArgsConstructor
@Tag(name = "Staff AI Agents", description = "Specialized AI agents for clinical and quality staff")
public class StaffAgentController {

    private final ClinicalDecisionAgent clinicalDecisionAgent;
    private final CareGapOptimizerAgent careGapOptimizerAgent;
    private final ReportGeneratorAgent reportGeneratorAgent;
    private final DocumentationAssistantAgent documentationAssistantAgent;
    private final AgentRegistry agentRegistry;

    // ==================== Clinical Decision Agent ====================

    @PostMapping("/clinical/patient-summary/{patientId}")
    @Operation(summary = "Get patient clinical summary", description = "Generate comprehensive clinical summary for a patient")
    public Mono<ResponseEntity<AgentResponseDTO>> getPatientSummary(
            @PathVariable String patientId,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @AuthenticationPrincipal Jwt jwt) {

        AgentContext context = buildContext(tenantId, jwt, ClinicalDecisionAgent.AGENT_TYPE);
        return clinicalDecisionAgent.getPatientSummary(patientId, context)
            .map(this::mapToResponseEntity);
    }

    @PostMapping("/clinical/care-gaps/{patientId}")
    @Operation(summary = "Evaluate patient care gaps", description = "Analyze and prioritize care gaps for a patient")
    public Mono<ResponseEntity<AgentResponseDTO>> evaluateCareGaps(
            @PathVariable String patientId,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @AuthenticationPrincipal Jwt jwt) {

        AgentContext context = buildContext(tenantId, jwt, ClinicalDecisionAgent.AGENT_TYPE);
        return clinicalDecisionAgent.evaluateCareGaps(patientId, context)
            .map(this::mapToResponseEntity);
    }

    @PostMapping("/clinical/medications/{patientId}")
    @Operation(summary = "Review patient medications", description = "Perform medication review and identify concerns")
    public Mono<ResponseEntity<AgentResponseDTO>> reviewMedications(
            @PathVariable String patientId,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @AuthenticationPrincipal Jwt jwt) {

        AgentContext context = buildContext(tenantId, jwt, ClinicalDecisionAgent.AGENT_TYPE);
        return clinicalDecisionAgent.reviewMedications(patientId, context)
            .map(this::mapToResponseEntity);
    }

    @PostMapping("/clinical/labs/{patientId}")
    @Operation(summary = "Interpret lab results", description = "Analyze and interpret patient lab results")
    public Mono<ResponseEntity<AgentResponseDTO>> interpretLabs(
            @PathVariable String patientId,
            @RequestParam(required = false) String labType,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @AuthenticationPrincipal Jwt jwt) {

        AgentContext context = buildContext(tenantId, jwt, ClinicalDecisionAgent.AGENT_TYPE);
        return clinicalDecisionAgent.interpretLabResults(patientId, labType, context)
            .map(this::mapToResponseEntity);
    }

    @PostMapping("/clinical/ask")
    @Operation(summary = "Ask clinical question", description = "Ask a clinical decision support question")
    public Mono<ResponseEntity<AgentResponseDTO>> askClinicalQuestion(
            @Valid @RequestBody ClinicalQuestionRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @AuthenticationPrincipal Jwt jwt) {

        AgentContext context = buildContext(tenantId, jwt, ClinicalDecisionAgent.AGENT_TYPE);
        return clinicalDecisionAgent.analyzeClinicalQuestion(request.question(), request.patientId(), context)
            .map(this::mapToResponseEntity);
    }

    // ==================== Care Gap Optimizer Agent ====================

    @PostMapping("/care-gaps/worklist")
    @Operation(summary = "Get prioritized care gap worklist", description = "Generate prioritized care gap worklist for a patient panel")
    public Mono<ResponseEntity<AgentResponseDTO>> getPrioritizedWorklist(
            @Valid @RequestBody WorklistRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @AuthenticationPrincipal Jwt jwt) {

        AgentContext context = buildContext(tenantId, jwt, CareGapOptimizerAgent.AGENT_TYPE);
        return careGapOptimizerAgent.getPrioritizedWorklist(request.patientIds(), context)
            .map(this::mapToResponseEntity);
    }

    @PostMapping("/care-gaps/outreach/{patientId}")
    @Operation(summary = "Optimize patient outreach", description = "Create optimized outreach plan for a patient")
    public Mono<ResponseEntity<AgentResponseDTO>> optimizeOutreach(
            @PathVariable String patientId,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @AuthenticationPrincipal Jwt jwt) {

        AgentContext context = buildContext(tenantId, jwt, CareGapOptimizerAgent.AGENT_TYPE);
        return careGapOptimizerAgent.optimizePatientOutreach(patientId, context)
            .map(this::mapToResponseEntity);
    }

    @PostMapping("/care-gaps/appointments/{patientId}")
    @Operation(summary = "Analyze appointment opportunities", description = "Find care gap closure opportunities at upcoming appointments")
    public Mono<ResponseEntity<AgentResponseDTO>> analyzeAppointments(
            @PathVariable String patientId,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @AuthenticationPrincipal Jwt jwt) {

        AgentContext context = buildContext(tenantId, jwt, CareGapOptimizerAgent.AGENT_TYPE);
        return careGapOptimizerAgent.analyzeAppointmentOpportunities(patientId, context)
            .map(this::mapToResponseEntity);
    }

    @PostMapping("/care-gaps/campaign/{measureId}")
    @Operation(summary = "Generate measure campaign", description = "Generate outreach campaign for a quality measure")
    public Mono<ResponseEntity<AgentResponseDTO>> generateCampaign(
            @PathVariable String measureId,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @AuthenticationPrincipal Jwt jwt) {

        AgentContext context = buildContext(tenantId, jwt, CareGapOptimizerAgent.AGENT_TYPE);
        return careGapOptimizerAgent.generateMeasureCampaign(measureId, context)
            .map(this::mapToResponseEntity);
    }

    @PostMapping("/care-gaps/barriers/{patientId}")
    @Operation(summary = "Analyze patient barriers", description = "Identify barriers preventing care gap closure")
    public Mono<ResponseEntity<AgentResponseDTO>> analyzeBarriers(
            @PathVariable String patientId,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @AuthenticationPrincipal Jwt jwt) {

        AgentContext context = buildContext(tenantId, jwt, CareGapOptimizerAgent.AGENT_TYPE);
        return careGapOptimizerAgent.analyzeBarriers(patientId, context)
            .map(this::mapToResponseEntity);
    }

    // ==================== Report Generator Agent ====================

    @PostMapping("/reports/hedis")
    @Operation(summary = "Generate HEDIS report", description = "Generate comprehensive HEDIS performance report")
    public Mono<ResponseEntity<AgentResponseDTO>> generateHEDISReport(
            @Valid @RequestBody HEDISReportRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @AuthenticationPrincipal Jwt jwt) {

        AgentContext context = buildContext(tenantId, jwt, ReportGeneratorAgent.AGENT_TYPE);
        return reportGeneratorAgent.generateHEDISReport(request.measureYear(), request.measureIds(), context)
            .map(this::mapToResponseEntity);
    }

    @PostMapping("/reports/star-ratings")
    @Operation(summary = "Generate Star Ratings report", description = "Generate Star Ratings projection and analysis")
    public Mono<ResponseEntity<AgentResponseDTO>> generateStarRatingsReport(
            @RequestParam(required = false) String contractId,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @AuthenticationPrincipal Jwt jwt) {

        AgentContext context = buildContext(tenantId, jwt, ReportGeneratorAgent.AGENT_TYPE);
        return reportGeneratorAgent.generateStarRatingsReport(contractId, context)
            .map(this::mapToResponseEntity);
    }

    @PostMapping("/reports/care-gaps")
    @Operation(summary = "Generate care gap report", description = "Generate care gap summary report")
    public Mono<ResponseEntity<AgentResponseDTO>> generateCareGapReport(
            @RequestParam(required = false) String reportPeriod,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @AuthenticationPrincipal Jwt jwt) {

        AgentContext context = buildContext(tenantId, jwt, ReportGeneratorAgent.AGENT_TYPE);
        return reportGeneratorAgent.generateCareGapReport(reportPeriod, context)
            .map(this::mapToResponseEntity);
    }

    @PostMapping("/reports/executive-summary")
    @Operation(summary = "Generate executive summary", description = "Generate executive dashboard summary")
    public Mono<ResponseEntity<AgentResponseDTO>> generateExecutiveSummary(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @AuthenticationPrincipal Jwt jwt) {

        AgentContext context = buildContext(tenantId, jwt, ReportGeneratorAgent.AGENT_TYPE);
        return reportGeneratorAgent.generateExecutiveSummary(context)
            .map(this::mapToResponseEntity);
    }

    @PostMapping("/reports/measure/{measureId}")
    @Operation(summary = "Generate measure deep-dive", description = "Generate detailed analysis for a specific measure")
    public Mono<ResponseEntity<AgentResponseDTO>> generateMeasureDeepDive(
            @PathVariable String measureId,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @AuthenticationPrincipal Jwt jwt) {

        AgentContext context = buildContext(tenantId, jwt, ReportGeneratorAgent.AGENT_TYPE);
        return reportGeneratorAgent.generateMeasureDeepDive(measureId, context)
            .map(this::mapToResponseEntity);
    }

    // ==================== Documentation Assistant Agent ====================

    @PostMapping("/docs/attestation")
    @Operation(summary = "Generate measure attestation", description = "Generate attestation documentation for a measure")
    public Mono<ResponseEntity<AgentResponseDTO>> generateAttestation(
            @Valid @RequestBody AttestationRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @AuthenticationPrincipal Jwt jwt) {

        AgentContext context = buildContext(tenantId, jwt, DocumentationAssistantAgent.AGENT_TYPE);
        return documentationAssistantAgent.generateAttestation(request.patientId(), request.measureId(), context)
            .map(this::mapToResponseEntity);
    }

    @PostMapping("/docs/transition-summary/{patientId}")
    @Operation(summary = "Generate care transition summary", description = "Generate care transition documentation")
    public Mono<ResponseEntity<AgentResponseDTO>> generateTransitionSummary(
            @PathVariable String patientId,
            @RequestParam(required = false) String transitionType,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @AuthenticationPrincipal Jwt jwt) {

        AgentContext context = buildContext(tenantId, jwt, DocumentationAssistantAgent.AGENT_TYPE);
        return documentationAssistantAgent.generateTransitionSummary(patientId, transitionType, context)
            .map(this::mapToResponseEntity);
    }

    @PostMapping("/docs/visit-summary")
    @Operation(summary = "Generate visit summary", description = "Generate after visit summary for patient")
    public Mono<ResponseEntity<AgentResponseDTO>> generateVisitSummary(
            @Valid @RequestBody VisitSummaryRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @AuthenticationPrincipal Jwt jwt) {

        AgentContext context = buildContext(tenantId, jwt, DocumentationAssistantAgent.AGENT_TYPE);
        return documentationAssistantAgent.generateVisitSummary(request.patientId(), request.encounterId(), context)
            .map(this::mapToResponseEntity);
    }

    @PostMapping("/docs/gap-closure")
    @Operation(summary = "Document gap closure", description = "Generate care gap closure documentation")
    public Mono<ResponseEntity<AgentResponseDTO>> documentGapClosure(
            @Valid @RequestBody GapClosureRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @AuthenticationPrincipal Jwt jwt) {

        AgentContext context = buildContext(tenantId, jwt, DocumentationAssistantAgent.AGENT_TYPE);
        return documentationAssistantAgent.documentGapClosure(
            request.patientId(), request.measureId(), request.closureEvidence(), context)
            .map(this::mapToResponseEntity);
    }

    @PostMapping("/docs/patient-education")
    @Operation(summary = "Generate patient education", description = "Generate patient education materials")
    public Mono<ResponseEntity<AgentResponseDTO>> generatePatientEducation(
            @Valid @RequestBody PatientEducationRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @AuthenticationPrincipal Jwt jwt) {

        AgentContext context = buildContext(tenantId, jwt, DocumentationAssistantAgent.AGENT_TYPE);
        return documentationAssistantAgent.generatePatientEducation(request.topic(), request.patientId(), context)
            .map(this::mapToResponseEntity);
    }

    // ==================== Agent Discovery ====================

    @GetMapping
    @Operation(summary = "List available staff agents", description = "Get list of available staff AI agents")
    public ResponseEntity<List<AgentRegistry.AgentInfo>> listAgents(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(agentRegistry.getAgentInfoList());
    }

    // ==================== Helper Methods ====================

    private AgentContext buildContext(String tenantId, Jwt jwt, String agentType) {
        String userId = jwt != null ? jwt.getSubject() : "anonymous";
        @SuppressWarnings("unchecked")
        List<String> roles = jwt != null ? jwt.getClaimAsStringList("roles") : List.of();

        return AgentContext.builder()
            .tenantId(tenantId)
            .userId(userId)
            .sessionId(UUID.randomUUID().toString())
            .correlationId(UUID.randomUUID().toString())
            .roles(Set.copyOf(roles != null ? roles : List.of()))
            .agentType(agentType)
            .origin("api")
            .build();
    }

    private ResponseEntity<AgentResponseDTO> mapToResponseEntity(AgentResponse response) {
        AgentResponseDTO dto = new AgentResponseDTO(
            response.success(),
            response.content(),
            response.error(),
            response.blocked(),
            response.blockReason()
        );
        return response.success()
            ? ResponseEntity.ok(dto)
            : ResponseEntity.badRequest().body(dto);
    }

    // ==================== Request DTOs ====================

    public record ClinicalQuestionRequest(
        @NotBlank String question,
        String patientId
    ) {}

    public record WorklistRequest(
        List<String> patientIds
    ) {}

    public record HEDISReportRequest(
        @NotBlank String measureYear,
        List<String> measureIds
    ) {}

    public record AttestationRequest(
        @NotBlank String patientId,
        @NotBlank String measureId
    ) {}

    public record VisitSummaryRequest(
        @NotBlank String patientId,
        @NotBlank String encounterId
    ) {}

    public record GapClosureRequest(
        @NotBlank String patientId,
        @NotBlank String measureId,
        String closureEvidence
    ) {}

    public record PatientEducationRequest(
        @NotBlank String topic,
        String patientId
    ) {}

    // ==================== Response DTO ====================

    public record AgentResponseDTO(
        boolean success,
        String content,
        String error,
        boolean blocked,
        String blockReason
    ) {}
}
