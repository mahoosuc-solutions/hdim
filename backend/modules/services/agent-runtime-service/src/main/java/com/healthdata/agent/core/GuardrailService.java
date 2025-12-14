package com.healthdata.agent.core;

import com.healthdata.agent.approval.ApprovalIntegration;
import com.healthdata.agent.approval.ApprovalIntegration.ApprovalResult;
import com.healthdata.agent.llm.model.LLMResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Clinical safety guardrails for AI agent responses.
 * Filters unsafe medical advice, diagnoses, and prescription language.
 * Integrates with the approval service for human review of flagged content.
 */
@Slf4j
@Service
public class GuardrailService {

    private final ApprovalIntegration approvalIntegration;

    @Value("${hdim.agent.guardrails.enabled:true}")
    private boolean guardrailsEnabled;

    @Value("${hdim.agent.guardrails.strict-mode:true}")
    private boolean strictMode;

    @Value("${hdim.agent.guardrails.approval-integration:true}")
    private boolean approvalIntegrationEnabled;

    public GuardrailService(ApprovalIntegration approvalIntegration) {
        this.approvalIntegration = approvalIntegration;
    }

    // Patterns for unsafe medical language
    private static final List<GuardrailPattern> CRITICAL_PATTERNS = List.of(
        // Definitive diagnoses
        new GuardrailPattern(
            Pattern.compile("(?i)\\b(you have|you've got|you are diagnosed with|diagnosis is)\\b.*\\b(cancer|tumor|diabetes|heart disease|stroke|HIV|AIDS|Alzheimer's|Parkinson's|MS|ALS|leukemia)\\b", Pattern.MULTILINE),
            "CRITICAL",
            "Definitive diagnosis of serious condition"
        ),
        // Prescription language
        new GuardrailPattern(
            Pattern.compile("(?i)\\b(take|prescribe|prescription|dosage|dose)\\b.*\\b(\\d+\\s*(mg|ml|mcg|units|tablets|capsules|pills))\\b", Pattern.MULTILINE),
            "CRITICAL",
            "Specific prescription or dosage"
        ),
        // Stop medication advice
        new GuardrailPattern(
            Pattern.compile("(?i)\\b(stop taking|discontinue|don't take|quit|cease)\\b.*\\b(medication|medicine|drug|prescription|insulin|blood thinner|antibiotic)\\b", Pattern.MULTILINE),
            "CRITICAL",
            "Advice to stop medication"
        ),
        // Dangerous emergency advice
        new GuardrailPattern(
            Pattern.compile("(?i)\\b(don't worry about|ignore|it's fine|not serious)\\b.*\\b(chest pain|breathing|stroke|heart attack|seizure|bleeding)\\b", Pattern.MULTILINE),
            "CRITICAL",
            "Dismissing emergency symptoms"
        )
    );

    private static final List<GuardrailPattern> HIGH_PATTERNS = List.of(
        // Treatment recommendations without context
        new GuardrailPattern(
            Pattern.compile("(?i)\\b(you should|you need to|must|have to)\\b.*\\b(surgery|operation|chemotherapy|radiation|transplant)\\b", Pattern.MULTILINE),
            "HIGH",
            "Direct treatment recommendation"
        ),
        // Definitive test interpretation
        new GuardrailPattern(
            Pattern.compile("(?i)\\b(results? (show|indicate|confirm|prove)|this means you have)\\b", Pattern.MULTILINE),
            "HIGH",
            "Definitive test interpretation"
        ),
        // Prognosis statements
        new GuardrailPattern(
            Pattern.compile("(?i)\\b(you (will|won't) (survive|live|die)|life expectancy|terminal|incurable)\\b", Pattern.MULTILINE),
            "HIGH",
            "Prognosis statement"
        )
    );

    private static final List<GuardrailPattern> MEDIUM_PATTERNS = List.of(
        // Over-the-counter recommendations
        new GuardrailPattern(
            Pattern.compile("(?i)\\b(take|use|try)\\b.*\\b(ibuprofen|acetaminophen|aspirin|tylenol|advil|benadryl|antacid)\\b", Pattern.MULTILINE),
            "MEDIUM",
            "OTC medication recommendation"
        )
    );

    /**
     * Check response against guardrails.
     */
    public GuardrailResult check(LLMResponse response, AgentContext context) {
        if (!guardrailsEnabled) {
            return GuardrailResult.allowed();
        }

        String content = response.getContent();
        if (content == null || content.isBlank()) {
            return GuardrailResult.allowed();
        }

        List<GuardrailViolation> violations = new ArrayList<>();

        // Check critical patterns
        for (GuardrailPattern pattern : CRITICAL_PATTERNS) {
            if (pattern.pattern().matcher(content).find()) {
                violations.add(new GuardrailViolation(pattern.severity(), pattern.description()));
            }
        }

        // Check high patterns
        for (GuardrailPattern pattern : HIGH_PATTERNS) {
            if (pattern.pattern().matcher(content).find()) {
                violations.add(new GuardrailViolation(pattern.severity(), pattern.description()));
            }
        }

        // Check medium patterns (only in strict mode)
        if (strictMode) {
            for (GuardrailPattern pattern : MEDIUM_PATTERNS) {
                if (pattern.pattern().matcher(content).find()) {
                    violations.add(new GuardrailViolation(pattern.severity(), pattern.description()));
                }
            }
        }

        if (violations.isEmpty()) {
            return GuardrailResult.allowed();
        }

        // Determine action based on severity
        boolean hasCritical = violations.stream().anyMatch(v -> "CRITICAL".equals(v.severity()));
        boolean hasHigh = violations.stream().anyMatch(v -> "HIGH".equals(v.severity()));

        if (hasCritical) {
            log.warn("Guardrail BLOCKED response - CRITICAL violation: tenant={}, violations={}",
                context.getTenantId(), violations);
            return GuardrailResult.blocked(
                "Response blocked for patient safety: " + violations.get(0).description(),
                violations
            );
        }

        if (hasHigh && strictMode) {
            log.warn("Guardrail BLOCKED response - HIGH violation in strict mode: tenant={}, violations={}",
                context.getTenantId(), violations);
            return GuardrailResult.blocked(
                "Response requires clinical review: " + violations.get(0).description(),
                violations
            );
        }

        // Flag for review - create approval request for human review
        log.info("Guardrail flagged response: tenant={}, violations={}", context.getTenantId(), violations);

        // Create approval request if integration is enabled
        if (approvalIntegrationEnabled) {
            String violationsStr = violations.stream()
                .map(v -> v.severity() + ": " + v.description())
                .collect(Collectors.joining("; "));

            ApprovalResult approvalResult = approvalIntegration.createGuardrailApprovalRequest(
                content,
                violationsStr,
                context
            );

            if (approvalResult.blocked()) {
                // Approval service blocked the content
                return GuardrailResult.blocked(approvalResult.message(), violations);
            }

            if (approvalResult.required()) {
                // Created approval request - return flagged with approval ID
                log.info("Created guardrail approval request: approvalId={}", approvalResult.approvalId());
                return GuardrailResult.flaggedWithApproval(violations, approvalResult.approvalId());
            }
        }

        return GuardrailResult.flagged(violations);
    }

    /**
     * Sanitize response by adding disclaimers.
     */
    public String sanitize(String content, List<GuardrailViolation> violations) {
        if (violations.isEmpty()) {
            return content;
        }

        StringBuilder sanitized = new StringBuilder();
        sanitized.append("**Important Notice:** This information is for educational purposes only ");
        sanitized.append("and should not replace professional medical advice. ");
        sanitized.append("Please consult your healthcare provider for personalized guidance.\n\n");
        sanitized.append(content);

        return sanitized.toString();
    }

    /**
     * Guardrail pattern definition.
     */
    private record GuardrailPattern(Pattern pattern, String severity, String description) {}

    /**
     * Guardrail violation.
     */
    public record GuardrailViolation(String severity, String description) {}

    /**
     * Guardrail check result.
     */
    public record GuardrailResult(
        boolean blocked,
        boolean flagged,
        String reason,
        List<GuardrailViolation> violations,
        java.util.UUID approvalId
    ) {
        public static GuardrailResult allowed() {
            return new GuardrailResult(false, false, null, List.of(), null);
        }

        public static GuardrailResult blocked(String reason, List<GuardrailViolation> violations) {
            return new GuardrailResult(true, false, reason, violations, null);
        }

        public static GuardrailResult flagged(List<GuardrailViolation> violations) {
            return new GuardrailResult(false, true, null, violations, null);
        }

        public static GuardrailResult flaggedWithApproval(List<GuardrailViolation> violations, java.util.UUID approvalId) {
            return new GuardrailResult(false, true, null, violations, approvalId);
        }

        /**
         * Check if the result has a pending approval request.
         */
        public boolean hasPendingApproval() {
            return approvalId != null;
        }
    }
}
