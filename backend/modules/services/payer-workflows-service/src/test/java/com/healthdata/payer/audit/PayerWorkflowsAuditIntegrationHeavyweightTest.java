package com.healthdata.payer.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Heavyweight Integration Test for Payer Workflows Audit Integration with real Kafka.
 * 
 * Tests Medicare Star Ratings and Medicaid compliance audit events for regulatory reporting.
 * 
 * This test requires Docker and uses Testcontainers to spin up a real Kafka instance.
 */
@SpringBootTest
@Testcontainers
@DisplayName("Payer Workflows Audit Integration - Heavyweight Kafka Tests")
class PayerWorkflowsAuditIntegrationHeavyweightTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("apache/kafka:3.8.0"))
            .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true");

    @DynamicPropertySource
    static void configureKafka(DynamicPropertyRegistry registry) {
        String bootstrapServers = kafka.getBootstrapServers();
        registry.add("spring.kafka.bootstrap-servers", () -> bootstrapServers);
        registry.add("spring.kafka.producer.bootstrap-servers", () -> bootstrapServers);
        registry.add("spring.kafka.consumer.bootstrap-servers", () -> bootstrapServers);
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add("audit.kafka.enabled", () -> "true");
        registry.add("audit.kafka.topic.ai-decisions", () -> "ai.agent.decisions");
    }

    @Autowired
    private PayerWorkflowsAuditIntegration auditIntegration;

    @Autowired
    private ObjectMapper objectMapper;

    private Consumer<String, String> consumer;

    private static final String TENANT_ID = "payer-tenant-123";
    private static final String PLAN_ID = "H1234-001";
    private static final String MCO_ID = "MCO-TX-001";
    private static final String TOPIC = "ai.agent.decisions";

    @BeforeEach
    void setUp() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-" + UUID.randomUUID());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");

        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(TOPIC));
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    @DisplayName("Should publish Star Rating calculation event to Kafka")
    void shouldPublishStarRatingCalculationEvent() throws Exception {
        // Arrange
        int overallRating = 5; // int, not double
        Map<String, Double> domainScores = new HashMap<>(); // Map<String, Double>
        domainScores.put("C01_breastCancerScreening", 85.5);
        domainScores.put("C02_colorectalCancerScreening", 78.3);
        domainScores.put("D01_diabetesA1c", 82.1);

        // Act
        auditIntegration.publishStarRatingCalculationEvent(
                TENANT_ID,
                PLAN_ID,
                overallRating, // no year parameter
                domainScores,
                250L,
                "star-rating-engine"
        );

        // Assert
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.isEmpty()).isFalse();

        ConsumerRecord<String, String> record = records.iterator().next();
        JsonNode event = objectMapper.readTree(record.value());

        assertThat(event.get("agentType").asText()).isEqualTo("PAYER_WORKFLOWS_SERVICE");
        assertThat(event.get("decisionType").asText()).isEqualTo("STAR_RATING_CALCULATION");
        assertThat(event.get("decisionOutcome").asText()).isEqualTo("CALCULATED");

        JsonNode metrics = event.get("inputMetrics");
        assertThat(metrics.get("planId").asText()).isEqualTo(PLAN_ID);
        assertThat(metrics.get("starRating").asInt()).isEqualTo(overallRating);

        JsonNode scores = metrics.get("domainScores");
        assertThat(scores.get("C01_breastCancerScreening").asDouble()).isEqualTo(85.5);
    }

    @Test
    @DisplayName("Should publish Medicaid compliance report event to Kafka")
    void shouldPublishMedicaidComplianceEvent() throws Exception {
        // Arrange
        String stateCode = "TX";
        String reportType = "QUARTERLY"; // reportType, not reportingPeriod
        boolean compliant = true;
        Map<String, Object> complianceMetrics = new HashMap<>();
        complianceMetrics.put("reportingPeriod", "2025-Q1");
        complianceMetrics.put("measuresMetThreshold", 45);
        complianceMetrics.put("measuresBelowThreshold", 5);
        complianceMetrics.put("complianceRate", 0.90);

        // Act
        auditIntegration.publishMedicaidComplianceEvent(
                MCO_ID,
                stateCode,
                reportType, // reportType parameter
                compliant,
                complianceMetrics,
                400L,
                "compliance-engine"
        );

        // Assert
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.isEmpty()).isFalse();

        ConsumerRecord<String, String> record = records.iterator().next();
        JsonNode event = objectMapper.readTree(record.value());

        assertThat(event.get("decisionType").asText()).isEqualTo("MEDICAID_COMPLIANCE_REPORT");
        assertThat(event.get("decisionOutcome").asText()).isEqualTo("REPORTED");

        JsonNode metrics = event.get("inputMetrics");
        assertThat(metrics.get("state").asText()).isEqualTo(stateCode);
        assertThat(metrics.get("reportType").asText()).isEqualTo(reportType);
        assertThat(metrics.get("compliant").asBoolean()).isTrue();
        assertThat(metrics.get("reportingPeriod").asText()).isEqualTo("2025-Q1");
        assertThat(metrics.get("measuresMetThreshold").asInt()).isEqualTo(45);
        assertThat(metrics.get("complianceRate").asDouble()).isEqualTo(0.90);
    }

    @Test
    @DisplayName("Should publish non-compliant Medicaid report event")
    void shouldPublishNonCompliantMedicaidEvent() throws Exception {
        // Arrange
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("measuresMetThreshold", 30);
        metrics.put("measuresBelowThreshold", 20);
        metrics.put("complianceRate", 0.60);
        metrics.put("correctiveActionsRequired", 15);

        // Act
        auditIntegration.publishMedicaidComplianceEvent(
                MCO_ID,
                "CA",
                "QUARTERLY", // reportType
                false,
                metrics,
                380L,
                "compliance-engine"
        );

        // Assert
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.isEmpty()).isFalse();

        ConsumerRecord<String, String> record = records.iterator().next();
        JsonNode event = objectMapper.readTree(record.value());

        JsonNode eventMetrics = event.get("inputMetrics");
        assertThat(eventMetrics.get("compliant").asBoolean()).isFalse();
        assertThat(eventMetrics.get("correctiveActionsRequired").asInt()).isEqualTo(15);
    }

    @Test
    @DisplayName("Should publish payer workflow step event to Kafka")
    void shouldPublishWorkflowStepEvent() throws Exception {
        // Arrange
        String stepName = "dashboard_update";
        String stepStatus = "COMPLETED";
        Map<String, Object> stepData = new HashMap<>();
        stepData.put("dashboardType", "MEDICARE_ADVANTAGE");
        stepData.put("avgStarRating", 4.2);
        stepData.put("totalEnrollment", 125000);
        stepData.put("bonusPaymentEligible", true);

        // Act
        auditIntegration.publishPayerWorkflowStepEvent(
                TENANT_ID,
                PLAN_ID,
                stepName,
                stepStatus,
                stepData,
                "dashboard-service"
        );

        // Assert
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.isEmpty()).isFalse();

        ConsumerRecord<String, String> record = records.iterator().next();
        JsonNode event = objectMapper.readTree(record.value());

        assertThat(event.get("decisionType").asText()).isEqualTo("PAYER_WORKFLOW_STEP");
        assertThat(event.get("decisionOutcome").asText()).isEqualTo("APPLIED");

        JsonNode metrics = event.get("inputMetrics");
        assertThat(metrics.get("stepName").asText()).isEqualTo(stepName);
        assertThat(metrics.get("stepStatus").asText()).isEqualTo(stepStatus);
        assertThat(metrics.get("dashboardType").asText()).isEqualTo("MEDICARE_ADVANTAGE");
        assertThat(metrics.get("avgStarRating").asDouble()).isEqualTo(4.2);
        assertThat(metrics.get("bonusPaymentEligible").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("Should handle multiple Star Rating calculations for different plans")
    void shouldHandleMultiplePlanCalculations() throws Exception {
        // Arrange - Calculate ratings for 3 different plans
        String[] planIds = {PLAN_ID, "H5678-002", "H9012-003"};
        double[] ratings = {4.5, 3.5, 5.0};

        // Act
        for (int i = 0; i < planIds.length; i++) {
            Map<String, Double> domainScores = new HashMap<>();
            domainScores.put("overallScore", ratings[i] * 20); // Convert to percentage
            
            auditIntegration.publishStarRatingCalculationEvent(
                    TENANT_ID,
                    planIds[i],
                    (int) Math.round(ratings[i]), // Convert to int
                    domainScores,
                    200L,
                    "engine"
            );
            Thread.sleep(50); // Ensure ordering
        }

        // Assert - All 3 calculations audited
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.count()).isGreaterThanOrEqualTo(3);

        int foundCount = 0;
        for (ConsumerRecord<String, String> record : records) {
            JsonNode event = objectMapper.readTree(record.value());
            if (event.get("decisionType").asText().equals("STAR_RATING_CALCULATION")) {
                foundCount++;
            }
        }

        assertThat(foundCount).isEqualTo(3);
    }

    @Test
    @DisplayName("Should handle quarterly Medicaid compliance reporting")
    void shouldHandleQuarterlyCompliance() throws Exception {
        // Arrange - 4 quarterly reports for a year
        String[] quarters = {"2025-Q1", "2025-Q2", "2025-Q3", "2025-Q4"};
        boolean[] compliantStatus = {true, true, false, true};

        // Act
        for (int i = 0; i < quarters.length; i++) {
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("quarter", quarters[i]);
            metrics.put("complianceRate", compliantStatus[i] ? 0.92 : 0.75);

            auditIntegration.publishMedicaidComplianceEvent(
                    MCO_ID,
                    "TX",
                    "QUARTERLY", // reportType
                    compliantStatus[i],
                    metrics,
                    350L,
                    "quarterly-report"
            );
            Thread.sleep(50);
        }

        // Assert
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.count()).isGreaterThanOrEqualTo(4);

        // Verify quarterly progression
        int compliantCount = 0;
        for (ConsumerRecord<String, String> record : records) {
            JsonNode event = objectMapper.readTree(record.value());
            if (event.get("decisionType").asText().equals("MEDICAID_COMPLIANCE_REPORT")) {
                JsonNode context = event.get("decisionContext");
                if (context.get("compliant").asBoolean()) {
                    compliantCount++;
                }
            }
        }

        assertThat(compliantCount).isEqualTo(3); // 3 out of 4 quarters compliant
    }

    @Test
    @DisplayName("Should track Star Rating performance trends over time")
    void shouldTrackStarRatingTrends() throws Exception {
        // Arrange - Rating progression: 3.5 -> 4.0 -> 4.5 (improving)
        double[] yearlyRatings = {3.5, 4.0, 4.5};
        String[] years = {"2023", "2024", "2025"};

        // Act
        for (int i = 0; i < years.length; i++) {
            Map<String, Double> domainScores = new HashMap<>();
            domainScores.put("yearScore", yearlyRatings[i] * 20); // Convert to domain score
            
            auditIntegration.publishStarRatingCalculationEvent(
                    TENANT_ID,
                    PLAN_ID,
                    (int) Math.round(yearlyRatings[i]), // Convert to int
                    domainScores,
                    220L,
                    "trend-analysis"
            );
            Thread.sleep(50);
        }

        // Assert - Verify improvement trend captured
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.count()).isGreaterThanOrEqualTo(3);

        double firstRating = 0, lastRating = 0;
        boolean foundFirst = false, foundLast = false;

        int index = 0;
        for (ConsumerRecord<String, String> record : records) {
            JsonNode event = objectMapper.readTree(record.value());
            if (event.get("decisionType").asText().equals("PAYER_WORKFLOW_STEP")) {
                JsonNode metrics = event.get("inputMetrics");
                double rating = metrics.get("starRating").asDouble();
                
                if (index == 0) {
                    firstRating = rating;
                    foundFirst = true;
                } else if (index == 2) {
                    lastRating = rating;
                    foundLast = true;
                }
                index++;
            }
        }

        assertThat(foundFirst && foundLast).isTrue();
        assertThat(lastRating).isGreaterThan(firstRating); // Confirm improvement
    }

    @Test
    @DisplayName("Should handle high-volume payer calculations")
    void shouldHandleHighVolumeCalculations() throws Exception {
        // Arrange - 50 concurrent calculations (simulating batch processing)
        int calculationCount = 50;

        // Act
        for (int i = 0; i < calculationCount; i++) {
            Map<String, Double> domainScores = new HashMap<>();
            domainScores.put("indexScore", (double) i);

            auditIntegration.publishStarRatingCalculationEvent(
                    TENANT_ID + "-" + i,
                    PLAN_ID + "-" + i,
                    4 + (i % 2), // Vary ratings between 4 and 5
                    domainScores,
                    200L,
                    "batch"
            );
        }

        // Assert
        int receivedCount = 0;
        long startTime = System.currentTimeMillis();
        long timeout = 20000; // 20 seconds

        while (receivedCount < calculationCount && (System.currentTimeMillis() - startTime) < timeout) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
            receivedCount += records.count();
        }

        assertThat(receivedCount).isEqualTo(calculationCount);
    }

    @Test
    @DisplayName("Should audit complete regulatory reporting cycle")
    void shouldAuditCompleteReportingCycle() throws Exception {
        // Simulate complete cycle: Calculate -> Report -> Dashboard Update
        String cycleId = PLAN_ID + "-cycle";

        // Step 1: Calculate Star Ratings
        Map<String, Double> domainScores = new HashMap<>();
        domainScores.put("calculation", 90.0);
        auditIntegration.publishStarRatingCalculationEvent(
                TENANT_ID, cycleId, 5, domainScores, 200L, "calculate"
        );

        // Step 2: Generate Compliance Report
        Thread.sleep(50);
        Map<String, Object> complianceMetrics = new HashMap<>();
        complianceMetrics.put("phase", "compliance");
        auditIntegration.publishMedicaidComplianceEvent(
                cycleId, "TX", "ANNUAL", true, complianceMetrics, 300L, "report"
        );

        // Step 3: Update Workflow
        Thread.sleep(50);
        Map<String, Object> stepData = new HashMap<>();
        stepData.put("phase", "workflow");
        auditIntegration.publishPayerWorkflowStepEvent(
                TENANT_ID, cycleId, "reporting_complete", "COMPLETED", stepData, "workflow"
        );

        // Assert - Complete cycle audited
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.count()).isGreaterThanOrEqualTo(3);

        String[] expectedTypes = {
            "PAYER_WORKFLOW_STEP",
            "PAYER_WORKFLOW_STEP",
            "PAYER_WORKFLOW_STEP"
        };

        int foundCount = 0;
        for (ConsumerRecord<String, String> record : records) {
            JsonNode event = objectMapper.readTree(record.value());
            String type = event.get("decisionType").asText();
            for (String expected : expectedTypes) {
                if (type.equals(expected)) {
                    foundCount++;
                    break;
                }
            }
        }

        assertThat(foundCount).isGreaterThanOrEqualTo(3);
    }
}
