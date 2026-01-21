package com.healthdata.audit.integration;

import com.healthdata.audit.dto.qa.AgentStats;
import com.healthdata.audit.dto.qa.QAMetrics;
import com.healthdata.audit.dto.qa.QATrendData;
import com.healthdata.audit.entity.ai.AIAgentDecisionEventEntity;
import com.healthdata.audit.entity.QAReviewEntity;
import com.healthdata.audit.models.ai.AIAgentDecisionEvent;
import com.healthdata.audit.repository.ai.AIAgentDecisionEventRepository;
import com.healthdata.audit.repository.QAReviewRepository;
import com.healthdata.audit.service.qa.QAReviewService;
import com.healthdata.audit.integration.AuditIntegrationTestConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for QA Review Service per-agent statistics.
 * 
 * Tests with real PostgreSQL database using Testcontainers.
 * Validates statistical calculations with actual database data.
 */
@SpringBootTest(classes = {AuditIntegrationTestConfiguration.class})
@Import(QAReviewService.class)
@Testcontainers
@Transactional
@DisplayName("QA Review Service - Per-Agent Statistics Integration Tests")
class QAReviewServicePerAgentIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("audit_test_db")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired
    private QAReviewService qaReviewService;

    @Autowired
    private AIAgentDecisionEventRepository aiDecisionRepository;

    @Autowired
    private QAReviewRepository qaReviewRepository;

    private static final String TENANT_ID = "test-tenant-qa";

    @BeforeEach
    void setUp() {
        aiDecisionRepository.deleteAll();
        qaReviewRepository.deleteAll();
    }

    @Test
    @DisplayName("Should calculate per-agent statistics from database")
    void testGetMetrics_PerAgentStatistics_FromDatabase() {
        // Given - Create events and reviews for multiple agent types
        UUID event1 = createEventAndReview(
            AIAgentDecisionEvent.AgentType.CLINICAL_WORKFLOW, 0.9, "APPROVED", false, false);
        UUID event2 = createEventAndReview(
            AIAgentDecisionEvent.AgentType.CLINICAL_WORKFLOW, 0.85, "APPROVED", false, false);
        UUID event3 = createEventAndReview(
            AIAgentDecisionEvent.AgentType.CARE_GAP_IDENTIFIER, 0.8, "REJECTED", true, false);
        UUID event4 = createEventAndReview(
            AIAgentDecisionEvent.AgentType.CARE_GAP_IDENTIFIER, 0.75, "APPROVED", false, false);
        UUID event5 = createEventAndReview(
            AIAgentDecisionEvent.AgentType.AI_AGENT, 0.95, "APPROVED", false, false);

        LocalDate startDate = LocalDate.now().minusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(1);

        // When
        QAMetrics metrics = qaReviewService.getMetrics(TENANT_ID, null, startDate, endDate);

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
        assertEquals(1.0, clinicalStats.getAccuracy(), 0.01);

        // Verify CARE_GAP_IDENTIFIER stats
        AgentStats careGapStats = agentStats.get("CARE_GAP_IDENTIFIER");
        assertEquals(2, careGapStats.getTotalDecisions());
        assertEquals(1, careGapStats.getApproved());
        assertEquals(1, careGapStats.getRejected());
        assertEquals(0.5, careGapStats.getApprovalRate(), 0.01);
        assertEquals(0.775, careGapStats.getAverageConfidence(), 0.01);
        assertEquals(0.5, careGapStats.getAccuracy(), 0.01); // 1 false positive out of 2
    }

    @Test
    @DisplayName("Should calculate per-agent trends from database")
    void testGetAccuracyTrends_PerAgentTrends_FromDatabase() {
        // Given - Create events and reviews for different dates
        LocalDate date1 = LocalDate.now().minusDays(2);
        LocalDate date2 = LocalDate.now().minusDays(1);

        UUID event1 = createEventAndReviewWithDate(
            AIAgentDecisionEvent.AgentType.CLINICAL_WORKFLOW, 0.9, "APPROVED", date1);
        UUID event2 = createEventAndReviewWithDate(
            AIAgentDecisionEvent.AgentType.CLINICAL_WORKFLOW, 0.85, "APPROVED", date1);
        UUID event3 = createEventAndReviewWithDate(
            AIAgentDecisionEvent.AgentType.CARE_GAP_IDENTIFIER, 0.8, "APPROVED", date2);

        // When
        QATrendData trends = qaReviewService.getAccuracyTrends(TENANT_ID, null, 7);

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
    @DisplayName("Should filter per-agent statistics by agent type from database")
    void testGetMetrics_PerAgentStatistics_Filtered_FromDatabase() {
        // Given - Create events for different agent types
        createEventAndReview(AIAgentDecisionEvent.AgentType.CLINICAL_WORKFLOW, 0.9, "APPROVED", false, false);
        createEventAndReview(AIAgentDecisionEvent.AgentType.CLINICAL_WORKFLOW, 0.85, "APPROVED", false, false);
        createEventAndReview(AIAgentDecisionEvent.AgentType.CARE_GAP_IDENTIFIER, 0.8, "APPROVED", false, false);

        LocalDate startDate = LocalDate.now().minusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(1);
        String filterAgentType = "CLINICAL_WORKFLOW";

        // When
        QAMetrics metrics = qaReviewService.getMetrics(TENANT_ID, filterAgentType, startDate, endDate);

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
    @DisplayName("Should handle large dataset performance")
    void testPerformance_WithLargeDataset() {
        // Given - Create 100 events and reviews
        List<UUID> eventIds = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            AIAgentDecisionEvent.AgentType agentType = (i % 3 == 0) 
                ? AIAgentDecisionEvent.AgentType.CLINICAL_WORKFLOW
                : (i % 3 == 1)
                    ? AIAgentDecisionEvent.AgentType.CARE_GAP_IDENTIFIER
                    : AIAgentDecisionEvent.AgentType.AI_AGENT;
            
            String status = (i % 10 == 0) ? "REJECTED" : "APPROVED";
            boolean falsePositive = (i % 20 == 0);
            
            eventIds.add(createEventAndReview(agentType, 0.8 + (i % 20) * 0.01, status, falsePositive, false));
        }

        LocalDate startDate = LocalDate.now().minusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(1);

        // When
        long startTime = System.currentTimeMillis();
        QAMetrics metrics = qaReviewService.getMetrics(TENANT_ID, null, startDate, endDate);
        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertNotNull(metrics);
        assertNotNull(metrics.getAgentPerformance());
        
        // Should complete in reasonable time (< 2 seconds for 100 decisions)
        assertTrue(duration < 2000, "Metrics calculation took " + duration + "ms, exceeds 2s target");
        
        // Should have stats for all 3 agent types
        Map<String, AgentStats> agentStats = metrics.getAgentPerformance().getByAgentType();
        assertTrue(agentStats.size() >= 3);
    }

    // ==================== Helper Methods ====================

    private UUID createEventAndReview(
            AIAgentDecisionEvent.AgentType agentType,
            double confidence,
            String reviewStatus,
            boolean falsePositive,
            boolean falseNegative) {
        
        UUID eventId = UUID.randomUUID();
        AIAgentDecisionEventEntity event = new AIAgentDecisionEventEntity();
        event.setEventId(eventId);
        event.setTimestamp(Instant.now());
        event.setTenantId(TENANT_ID);
        event.setAgentType(agentType);
        event.setDecisionType(AIAgentDecisionEvent.DecisionType.CDS_RECOMMENDATION);
        event.setConfidenceScore(confidence);
        event.setRecommendedValue("Test recommendation");
        aiDecisionRepository.save(event);

        QAReviewEntity review = new QAReviewEntity();
        review.setDecisionId(eventId.toString());
        review.setTenantId(TENANT_ID);
        review.setReviewStatus(reviewStatus);
        review.setReviewedAt(Instant.now());
        review.setIsFalsePositive(falsePositive);
        review.setIsFalseNegative(falseNegative);
        qaReviewRepository.save(review);

        return eventId;
    }

    private UUID createEventAndReviewWithDate(
            AIAgentDecisionEvent.AgentType agentType,
            double confidence,
            String reviewStatus,
            LocalDate reviewDate) {
        
        UUID eventId = UUID.randomUUID();
        AIAgentDecisionEventEntity event = new AIAgentDecisionEventEntity();
        event.setEventId(eventId);
        event.setTimestamp(reviewDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        event.setTenantId(TENANT_ID);
        event.setAgentType(agentType);
        event.setDecisionType(AIAgentDecisionEvent.DecisionType.CDS_RECOMMENDATION);
        event.setConfidenceScore(confidence);
        event.setRecommendedValue("Test recommendation");
        aiDecisionRepository.save(event);

        QAReviewEntity review = new QAReviewEntity();
        review.setDecisionId(eventId.toString());
        review.setTenantId(TENANT_ID);
        review.setReviewStatus(reviewStatus);
        review.setReviewedAt(reviewDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        review.setIsFalsePositive(false);
        review.setIsFalseNegative(false);
        qaReviewRepository.save(review);

        return eventId;
    }
}
