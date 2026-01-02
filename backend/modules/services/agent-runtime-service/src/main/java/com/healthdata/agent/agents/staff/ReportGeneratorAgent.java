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
 * Report Generator Agent.
 * Creates quality measure reports, HEDIS summaries, Star Ratings analysis,
 * and compliance documentation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportGeneratorAgent implements AgentDefinition {

    private final AgentOrchestrator orchestrator;

    public static final String AGENT_TYPE = "report-generator";

    private static final String SYSTEM_PROMPT = """
        You are a Healthcare Quality Report Generator for the HDIM platform.
        Your role is to create comprehensive, accurate reports on quality measures,
        compliance status, and performance metrics.

        ## YOUR CAPABILITIES
        - Generate HEDIS measure performance reports
        - Create Star Ratings analysis and projections
        - Produce care gap summary reports
        - Build compliance documentation
        - Create executive dashboards and summaries
        - Analyze trends and provide insights

        ## AVAILABLE TOOLS
        - fhir_query: Query patient data and aggregate statistics
        - cql_execute: Evaluate quality measures and calculate rates

        ## REPORT TYPES

        ### 1. HEDIS Performance Report
        Structure:
        - Executive Summary
        - Measure-by-Measure Performance
        - Numerator/Denominator Counts
        - Rate Calculations
        - Comparison to Benchmarks (50th, 75th, 90th percentiles)
        - Trend Analysis (prior periods)
        - Gap Analysis
        - Recommendations

        ### 2. Star Ratings Analysis
        Structure:
        - Current Star Rating Projection
        - Domain Breakdown (Part C, Part D)
        - Cut Point Analysis
        - Measures at Risk
        - Improvement Opportunities
        - Impact Modeling

        ### 3. Care Gap Summary
        Structure:
        - Total Open Gaps by Measure
        - Patient Distribution
        - Urgency Classification
        - Closure Rate Trends
        - Projected Impact
        - Action Plan

        ### 4. Regulatory Compliance Report
        Structure:
        - Compliance Status by Requirement
        - Evidence Documentation
        - Gap Identification
        - Remediation Status
        - Audit Readiness Score

        ## FORMATTING GUIDELINES
        - Use clear section headers
        - Include data tables with proper alignment
        - Provide visual indicators for status (✓ ✗ ⚠)
        - Round percentages to 2 decimal places
        - Include date ranges and data freshness
        - Add footnotes for methodology

        ## DATA ACCURACY
        - Always verify data freshness
        - Note any data quality issues
        - Indicate confidence levels
        - Document exclusions
        - Show sample sizes

        ## BENCHMARK SOURCES
        Reference these when available:
        - NCQA HEDIS Benchmarks
        - CMS Star Ratings Cut Points
        - State Medicaid Thresholds
        - Internal Historical Performance

        Remember: Reports must be accurate, well-structured, and actionable.
        """;

    @Override
    public String getAgentType() {
        return AGENT_TYPE;
    }

    @Override
    public String getDisplayName() {
        return "Report Generator";
    }

    @Override
    public String getDescription() {
        return "Creates quality measure reports, HEDIS summaries, Star Ratings analysis, and compliance documentation";
    }

    @Override
    public String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    @Override
    public List<String> getEnabledTools() {
        return List.of("fhir_query", "cql_execute");
    }

    @Override
    public Map<String, Object> getDefaultParameters() {
        return Map.of(
            "maxTokens", 8192,  // Higher for detailed reports
            "temperature", 0.1,  // Very low for consistent formatting
            "model", "claude-3-5-sonnet-20241022"
        );
    }

    /**
     * Generate HEDIS performance report.
     */
    public Mono<AgentResponse> generateHEDISReport(String measureYear, List<String> measureIds, AgentContext context) {
        String measures = measureIds != null && !measureIds.isEmpty()
            ? String.join(", ", measureIds)
            : "all applicable measures";

        String message = String.format("""
            Generate a comprehensive HEDIS Performance Report for measurement year %s.
            Measures to include: %s

            The report should include:
            1. Executive Summary with overall performance
            2. Detailed measure-by-measure analysis
            3. Performance rates with numerator/denominator
            4. Comparison to NCQA benchmarks
            5. Year-over-year trend analysis
            6. Identified gaps and improvement opportunities
            7. Specific action recommendations

            Format as a professional report suitable for leadership review.
            """, measureYear, measures);

        context = context.toBuilder()
            .agentType(AGENT_TYPE)
            .build();

        return execute(message, context);
    }

    /**
     * Generate Star Ratings projection report.
     */
    public Mono<AgentResponse> generateStarRatingsReport(String contractId, AgentContext context) {
        String message = String.format("""
            Generate a Star Ratings Projection Report for contract %s.

            Include:
            1. Current projected overall Star Rating
            2. Part C and Part D domain breakdowns
            3. Individual measure performance vs. cut points
            4. Measures within improvement range
            5. Measures at risk of decline
            6. Cut point gap analysis
            7. Recommended focus areas for improvement
            8. Projected impact of closing care gaps

            Provide specific, actionable recommendations for each measure
            that could move the needle on Star Ratings.
            """, contractId != null ? contractId : "primary");

        context = context.toBuilder()
            .agentType(AGENT_TYPE)
            .build();

        return execute(message, context);
    }

    /**
     * Generate care gap closure report.
     */
    public Mono<AgentResponse> generateCareGapReport(String reportPeriod, AgentContext context) {
        String message = String.format("""
            Generate a Care Gap Summary Report for period: %s.

            Include:
            1. Total open care gaps by measure
            2. Gap distribution by urgency level
            3. Patient population breakdown
            4. Closure rate trends over time
            5. Outreach success metrics
            6. Barriers analysis
            7. Projected closure by year-end
            8. Resource requirements for gap closure
            9. Recommended prioritization

            Format with clear tables and visualizable data.
            """, reportPeriod != null ? reportPeriod : "current measurement year");

        context = context.toBuilder()
            .agentType(AGENT_TYPE)
            .build();

        return execute(message, context);
    }

    /**
     * Generate compliance status report.
     */
    public Mono<AgentResponse> generateComplianceReport(String regulatoryBody, AgentContext context) {
        String message = String.format("""
            Generate a Regulatory Compliance Report for %s requirements.

            Include:
            1. Compliance status overview (compliant/non-compliant/partial)
            2. Requirement-by-requirement status
            3. Evidence documentation summary
            4. Identified compliance gaps
            5. Remediation plan status
            6. Audit readiness assessment
            7. Risk areas requiring attention
            8. Timeline for full compliance

            Flag any critical non-compliance issues requiring immediate action.
            """, regulatoryBody != null ? regulatoryBody : "CMS/NCQA");

        context = context.toBuilder()
            .agentType(AGENT_TYPE)
            .build();

        return execute(message, context);
    }

    /**
     * Generate executive dashboard summary.
     */
    public Mono<AgentResponse> generateExecutiveSummary(AgentContext context) {
        String message = """
            Generate an Executive Dashboard Summary with key metrics.

            Include:
            1. Overall Quality Score / Star Rating projection
            2. Top 5 performing measures
            3. Top 5 measures needing improvement
            4. Care gap closure progress (% closed YTD)
            5. Member engagement metrics
            6. Key risks and concerns
            7. Quick wins / immediate opportunities
            8. Month-over-month trends

            Format as a one-page executive briefing with key metrics highlighted.
            """;

        context = context.toBuilder()
            .agentType(AGENT_TYPE)
            .build();

        return execute(message, context);
    }

    /**
     * Generate provider performance report.
     */
    public Mono<AgentResponse> generateProviderReport(String providerId, AgentContext context) {
        String message = String.format("""
            Generate a Provider Performance Report for provider %s.

            Include:
            1. Provider demographic and panel information
            2. Quality measure performance summary
            3. Comparison to peer group
            4. Care gap status for attributed patients
            5. Trending performance (quarterly)
            6. Specific improvement opportunities
            7. Best practice recommendations
            8. Patient attribution details

            Format for provider-facing communication.
            """, providerId);

        context = context.toBuilder()
            .agentType(AGENT_TYPE)
            .build();

        return execute(message, context);
    }

    /**
     * Generate measure deep-dive analysis.
     */
    public Mono<AgentResponse> generateMeasureDeepDive(String measureId, AgentContext context) {
        String message = String.format("""
            Generate a deep-dive analysis for quality measure %s.

            Include:
            1. Measure specification summary
            2. Current performance rate with confidence interval
            3. Denominator and numerator breakdown
            4. Exclusion analysis
            5. Sub-population performance (age, gender, risk)
            6. Provider-level variation
            7. Root cause analysis for gaps
            8. Evidence-based improvement strategies
            9. Implementation recommendations
            10. Projected impact of interventions

            Provide actionable, specific recommendations.
            """, measureId);

        context = context.toBuilder()
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
