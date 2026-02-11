package com.healthdata.audit.service.qa;

import com.healthdata.audit.dto.qa.AgentPerformance;
import com.healthdata.audit.dto.qa.AgentStats;
import com.healthdata.audit.dto.qa.QAMetrics;
import com.healthdata.audit.dto.qa.QATrendData;
import com.healthdata.audit.entity.ai.AIAgentDecisionEventEntity;
import com.healthdata.audit.entity.QAReviewEntity;
import com.healthdata.audit.models.ai.AIAgentDecisionEvent;
import com.healthdata.audit.repository.ai.AIAgentDecisionEventRepository;
import com.healthdata.audit.repository.QAReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for QA Review Service per-agent statistics.
 * 
 * Tests cover:
 * - Per-agent metrics calculation
 * - Per-agent trends calculation
 * - Filtering by agent type
 * - Edge cases (empty data, single agent, multiple agents)
 * - Accuracy calculations
 * - Confidence distributions
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("QA Review Service - Per-Agent Statistics Tests")
class QAReviewServicePerAgentTest {

    @Mock
    private AIAgentDecisionEventRepository auditEventRepository;

    @Mock
    private QAReviewRepository qaReviewRepository;

    private QAReviewService qaReviewService;

    @BeforeEach
    void setUp() {
        qaReviewService = new QAReviewService(auditEventRepository, qaReviewRepository);
    }

    // ==================== Per-Agent Metrics Tests ====================

    @Test
    @DisplayName("Should calculate per-agent statistics for multiple agent types")
    void testGetMetrics_PerAgentStatistics_MultipleAgents() {
        // Given
        String tenantId = "tenant-1";
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        Instant start = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();

        // Create events for different agent types
        List<AIAgentDecisionEventEntity> events = Arrays.asList(
            createEvent("event-1", AIAgentDecisionEvent.AgentType.CLINICAL_WORKFLOW, 0.9),
            createEvent("event-2", AIAgentDecisionEvent.AgentType.CLINICAL_WORKFLOW, 0.85),
            createEvent("event-3", AIAgentDecisionEvent.AgentType.CARE_GAP_IDENTIFIER, 0.8),
            createEvent("event-4", AIAgentDecisionEvent.AgentType.CARE_GAP_IDENTIFIER, 0.75),
            createEvent("event-5", AIAgentDecisionEvent.AgentType.AI_AGENT, 0.95)
        );

        // Create reviews
        List<QAReviewEntity> reviews = Arrays.asList(
            createReview("event-1", "APPROVED", false, false),
            createReview("event-2", "APPROVED", false, false),
            createReview("event-3", "REJECTED", true, false), // False positive
            createReview("event-4", "APPROVED", false, false),
            createReview("event-5", "APPROVED", false, false)
        );

        when(qaReviewRepository.findByTenantIdAndDateRange(tenantId, start, end))
            .thenReturn(reviews);
        stubEventLookups(events);

        // When
        QAMetrics metrics = qaReviewService.getMetrics(tenantId, null, startDate, endDate);

        // Then
        assertNotNull(metrics);
        assertNotNull(metrics.getAgentPerformance());
        assertNotNull(metrics.getAgentPerformance().getByAgentType());
        
        Map<String, AgentStats> agentStats = metrics.getAgentPerformance().getByAgentType();
        
        // Should have stats for all 3 agent types
        assertTrue(agentStats.containsKey("CLINICAL_WORKFLOW"));
        assertTrue(agentStats.containsKey("CARE_GAP_IDENTIFIER"));
        assertTrue(agentStats.containsKey("AI_AGENT"));

        // Verify CLINICAL_WORKFLOW stats
        AgentStats clinicalStats = agentStats.get("CLINICAL_WORKFLOW");
        assertEquals(2, clinicalStats.getTotalDecisions());
        assertEquals(2, clinicalStats.getApproved());
        assertEquals(0, clinicalStats.getRejected());
        assertEquals(1.0, clinicalStats.getApprovalRate(), 0.01);
        assertEquals(0.875, clinicalStats.getAverageConfidence(), 0.01);
        assertEquals(1.0, clinicalStats.getAccuracy(), 0.01); // No false positives/negatives

        // Verify CARE_GAP_IDENTIFIER stats
        AgentStats careGapStats = agentStats.get("CARE_GAP_IDENTIFIER");
        assertEquals(2, careGapStats.getTotalDecisions());
        assertEquals(1, careGapStats.getApproved());
        assertEquals(1, careGapStats.getRejected());
        assertEquals(0.5, careGapStats.getApprovalRate(), 0.01);
        assertEquals(0.775, careGapStats.getAverageConfidence(), 0.01);
        assertEquals(0.5, careGapStats.getAccuracy(), 0.01); // 1 false positive out of 2

        // Verify AI_AGENT stats
        AgentStats docStats = agentStats.get("AI_AGENT");
        assertEquals(1, docStats.getTotalDecisions());
        assertEquals(1, docStats.getApproved());
        assertEquals(0, docStats.getRejected());
        assertEquals(1.0, docStats.getApprovalRate(), 0.01);
        assertEquals(0.95, docStats.getAverageConfidence(), 0.01);
        assertEquals(1.0, docStats.getAccuracy(), 0.01);
    }

    @Test
    @DisplayName("Should filter per-agent statistics by agent type")
    void testGetMetrics_PerAgentStatistics_FilteredByAgentType() {
        // Given
        String tenantId = "tenant-1";
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();
        String filterAgentType = "CLINICAL_WORKFLOW";

        Instant start = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();

        List<AIAgentDecisionEventEntity> allEvents = Arrays.asList(
            createEvent("event-1", AIAgentDecisionEvent.AgentType.CLINICAL_WORKFLOW, 0.9),
            createEvent("event-2", AIAgentDecisionEvent.AgentType.CLINICAL_WORKFLOW, 0.85),
            createEvent("event-3", AIAgentDecisionEvent.AgentType.CARE_GAP_IDENTIFIER, 0.8)
        );

        List<QAReviewEntity> allReviews = Arrays.asList(
            createReview("event-1", "APPROVED", false, false),
            createReview("event-2", "APPROVED", false, false),
            createReview("event-3", "APPROVED", false, false)
        );

        when(qaReviewRepository.findByTenantIdAndDateRange(tenantId, start, end))
            .thenReturn(allReviews);
        stubEventLookups(allEvents);

        // When
        QAMetrics metrics = qaReviewService.getMetrics(tenantId, filterAgentType, startDate, endDate);

        // Then
        assertNotNull(metrics);
        Map<String, AgentStats> agentStats = metrics.getAgentPerformance().getByAgentType();
        
        // Should only have stats for CLINICAL_WORKFLOW
        assertTrue(agentStats.containsKey("CLINICAL_WORKFLOW"));
        assertFalse(agentStats.containsKey("CARE_GAP_IDENTIFIER"));
        
        AgentStats stats = agentStats.get("CLINICAL_WORKFLOW");
        assertEquals(2, stats.getTotalDecisions());
    }

    @Test
    @DisplayName("Should handle empty agent statistics gracefully")
    void testGetMetrics_PerAgentStatistics_NoEvents() {
        // Given
        String tenantId = "tenant-1";
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        Instant start = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();

        when(qaReviewRepository.findByTenantIdAndDateRange(tenantId, start, end))
            .thenReturn(Collections.emptyList());
        stubEventLookups(Collections.emptyList());

        // When
        QAMetrics metrics = qaReviewService.getMetrics(tenantId, null, startDate, endDate);

        // Then
        assertNotNull(metrics);
        assertNotNull(metrics.getAgentPerformance());
        assertNotNull(metrics.getAgentPerformance().getByAgentType());
        assertTrue(metrics.getAgentPerformance().getByAgentType().isEmpty());
    }

    @Test
    @DisplayName("Should calculate accuracy correctly with false positives and negatives")
    void testGetMetrics_PerAgentStatistics_AccuracyCalculation() {
        // Given
        String tenantId = "tenant-1";
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        Instant start = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();

        List<AIAgentDecisionEventEntity> events = Arrays.asList(
            createEvent("event-1", AIAgentDecisionEvent.AgentType.CLINICAL_WORKFLOW, 0.9),
            createEvent("event-2", AIAgentDecisionEvent.AgentType.CLINICAL_WORKFLOW, 0.85),
            createEvent("event-3", AIAgentDecisionEvent.AgentType.CLINICAL_WORKFLOW, 0.8),
            createEvent("event-4", AIAgentDecisionEvent.AgentType.CLINICAL_WORKFLOW, 0.75)
        );

        List<QAReviewEntity> reviews = Arrays.asList(
            createReview("event-1", "APPROVED", false, false), // Correct
            createReview("event-2", "APPROVED", true, false),  // False positive
            createReview("event-3", "REJECTED", false, true),  // False negative
            createReview("event-4", "APPROVED", false, false)  // Correct
        );

        when(qaReviewRepository.findByTenantIdAndDateRange(tenantId, start, end))
            .thenReturn(reviews);
        stubEventLookups(events);

        // When
        QAMetrics metrics = qaReviewService.getMetrics(tenantId, null, startDate, endDate);

        // Then
        assertNotNull(metrics);
        Map<String, AgentStats> agentStats = metrics.getAgentPerformance().getByAgentType();
        AgentStats stats = agentStats.get("CLINICAL_WORKFLOW");
        
        // Accuracy = (total - false positives - false negatives) / total
        // = (4 - 1 - 1) / 4 = 0.5
        assertEquals(0.5, stats.getAccuracy(), 0.01);
        assertEquals(4, stats.getTotalDecisions());
    }

    // ==================== Per-Agent Trends Tests ====================

    @Test
    @DisplayName("Should calculate per-agent trends over time")
    void testGetAccuracyTrends_PerAgentTrends() {
        // Given
        String tenantId = "tenant-1";
        int days = 7;

        Instant start = Instant.now().minus(days, java.time.temporal.ChronoUnit.DAYS);
        LocalDate date1 = LocalDate.now().minusDays(2);
        LocalDate date2 = LocalDate.now().minusDays(1);

        List<AIAgentDecisionEventEntity> events = Arrays.asList(
            createEventWithDate("event-1", AIAgentDecisionEvent.AgentType.CLINICAL_WORKFLOW, 
                0.9, date1),
            createEventWithDate("event-2", AIAgentDecisionEvent.AgentType.CLINICAL_WORKFLOW, 
                0.85, date1),
            createEventWithDate("event-3", AIAgentDecisionEvent.AgentType.CARE_GAP_IDENTIFIER, 
                0.8, date2)
        );

        List<QAReviewEntity> reviews = Arrays.asList(
            createReviewWithDate("event-1", "APPROVED", date1),
            createReviewWithDate("event-2", "APPROVED", date1),
            createReviewWithDate("event-3", "APPROVED", date2)
        );

        when(qaReviewRepository.findByTenantIdAndDateRange(eq(tenantId), any(Instant.class), any(Instant.class)))
            .thenReturn(reviews);
        stubEventLookups(events);

        // When
        QATrendData trends = qaReviewService.getAccuracyTrends(tenantId, null, days);

        // Then
        assertNotNull(trends);
        assertNotNull(trends.getByAgentType());
        
        Map<String, List<com.healthdata.audit.dto.qa.DailyTrendPoint>> agentTrends = 
            trends.getByAgentType();
        
        // Should have trends for both agent types
        assertTrue(agentTrends.containsKey("CLINICAL_WORKFLOW"));
        assertTrue(agentTrends.containsKey("CARE_GAP_IDENTIFIER"));
        
        // Verify CLINICAL_WORKFLOW has trend for date1
        List<com.healthdata.audit.dto.qa.DailyTrendPoint> clinicalTrends = 
            agentTrends.get("CLINICAL_WORKFLOW");
        assertEquals(1, clinicalTrends.size());
        assertEquals(date1, clinicalTrends.get(0).getDate());
        assertEquals(2, clinicalTrends.get(0).getTotalDecisions());
    }

    @Test
    @DisplayName("Should filter per-agent trends by agent type")
    void testGetAccuracyTrends_PerAgentTrends_Filtered() {
        // Given
        String tenantId = "tenant-1";
        String filterAgentType = "CLINICAL_WORKFLOW";
        int days = 7;

        LocalDate date1 = LocalDate.now().minusDays(1);

        List<AIAgentDecisionEventEntity> events = Arrays.asList(
            createEventWithDate("event-1", AIAgentDecisionEvent.AgentType.CLINICAL_WORKFLOW, 
                0.9, date1),
            createEventWithDate("event-2", AIAgentDecisionEvent.AgentType.CARE_GAP_IDENTIFIER, 
                0.8, date1)
        );

        List<QAReviewEntity> reviews = Arrays.asList(
            createReviewWithDate("event-1", "APPROVED", date1),
            createReviewWithDate("event-2", "APPROVED", date1)
        );

        when(qaReviewRepository.findByTenantIdAndDateRange(eq(tenantId), any(Instant.class), any(Instant.class)))
            .thenReturn(reviews);
        stubEventLookups(events);

        // When
        QATrendData trends = qaReviewService.getAccuracyTrends(tenantId, filterAgentType, days);

        // Then
        assertNotNull(trends);
        Map<String, List<com.healthdata.audit.dto.qa.DailyTrendPoint>> agentTrends = 
            trends.getByAgentType();
        
        // Should only have trends for CLINICAL_WORKFLOW
        assertTrue(agentTrends.containsKey("CLINICAL_WORKFLOW"));
        assertFalse(agentTrends.containsKey("CARE_GAP_IDENTIFIER"));
    }

    // ==================== Helper Methods ====================

    private void stubEventLookups(List<AIAgentDecisionEventEntity> events) {
        when(auditEventRepository.findByDecisionIdIn(anyList()))
            .thenAnswer(invocation -> {
                List<String> ids = invocation.getArgument(0);
                Set<UUID> requested = ids.stream()
                    .map(UUID::fromString)
                    .collect(java.util.stream.Collectors.toSet());
                return events.stream()
                        .filter(e -> requested.contains(e.getEventId()))
                        .collect(java.util.stream.Collectors.toList());
            });
    }

    private AIAgentDecisionEventEntity createEvent(String eventId, 
                                                   AIAgentDecisionEvent.AgentType agentType, 
                                                   double confidence) {
        AIAgentDecisionEventEntity entity = new AIAgentDecisionEventEntity();
        // Generate deterministic UUID from eventId
        UUID uuid = UUID.nameUUIDFromBytes(eventId.getBytes());
        entity.setEventId(uuid);
        entity.setTimestamp(Instant.now());
        entity.setAgentType(agentType);
        entity.setConfidenceScore(confidence);
        // Note: AIAgentDecisionEventEntity doesn't have setDecisionId, decisionId is derived from eventId
        return entity;
    }

    private AIAgentDecisionEventEntity createEventWithDate(String eventId, 
                                                           AIAgentDecisionEvent.AgentType agentType, 
                                                           double confidence,
                                                           LocalDate date) {
        AIAgentDecisionEventEntity entity = createEvent(eventId, agentType, confidence);
        entity.setTimestamp(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
        return entity;
    }

    private QAReviewEntity createReview(String decisionId, String status, 
                                       boolean falsePositive, boolean falseNegative) {
        QAReviewEntity review = new QAReviewEntity();
        // Convert eventId to decisionId (UUID string)
        UUID uuid = UUID.nameUUIDFromBytes(decisionId.getBytes());
        review.setDecisionId(uuid.toString());
        review.setReviewStatus(status);
        review.setReviewedAt(Instant.now());
        review.setIsFalsePositive(falsePositive);
        review.setIsFalseNegative(falseNegative);
        return review;
    }

    private QAReviewEntity createReviewWithDate(String decisionId, String status, LocalDate date) {
        QAReviewEntity review = createReview(decisionId, status, false, false);
        review.setReviewedAt(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
        return review;
    }
}
