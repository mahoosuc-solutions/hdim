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
        double overallRating = 4.5;
        Map<String, Object> measureScores = new HashMap<>();
        measureScores.put("C01_breastCancerScreening", 85.5);
        measureScores.put("C02_colorectalCancerScreening", 78.3);
        measureScores.put("D01_diabetesA1c", 82.1);

        // Act
        auditIntegration.publishStarRatingCalculationEvent(
                TENANT_ID,
                PLAN_ID,
                "2025",
                overallRating,
                measureScores,
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

        JsonNode context = event.get("decisionContext");
        assertThat(context.get("planId").asText()).isEqualTo(PLAN_ID);
        assertThat(context.get("measurementYear").asText()).isEqualTo("2025");
        assertThat(context.get("overallRating").asDouble()).isEqualTo(overallRating);

        JsonNode scores = context.get("measureScores");
        assertThat(scores.get("C01_breastCancerScreening").asDouble()).isEqualTo(85.5);
    }

    @Test
    @DisplayName("Should publish Medicaid compliance report event to Kafka")
    void shouldPublishMedicaidComplianceEvent() throws Exception {
        // Arrange
        String stateCode = "TX";
        String reportingPeriod = "2025-Q1";
        boolean compliant = true;
        Map<String, Object> complianceMetrics = new HashMap<>();
        complianceMetrics.put("measuresMetThreshold", 45);
        complianceMetrics.put("measuresBelowThreshold", 5);
        complianceMetrics.put("complianceRate", 0.90);

        // Act
        auditIntegration.publishMedicaidComplianceEvent(
                MCO_ID,
                stateCode,
                reportingPeriod,
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

        JsonNode context = event.get("decisionContext");
        assertThat(context.get("stateCode").asText()).isEqualTo(stateCode);
        assertThat(context.get("reportingPeriod").asText()).isEqualTo(reportingPeriod);
        assertThat(context.get("compliant").asBoolean()).isTrue();

        JsonNode metrics = context.get("complianceMetrics");
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
                "2025-Q1",
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

        JsonNode context = event.get("decisionContext");
        assertThat(context.get("compliant").asBoolean()).isFalse();

        JsonNode complianceMetrics = context.get("complianceMetrics");
        assertThat(complianceMetrics.get("correctiveActionsRequired").asInt()).isEqualTo(15);
    }

    @Test
    @DisplayName("Should publish payer dashboard update event to Kafka")
    void shouldPublishDashboardUpdateEvent() throws Exception {
        // Arrange
        String dashboardType = "MEDICARE_ADVANTAGE";
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("avgStarRating", 4.2);
        metrics.put("totalEnrollment", 125000);
        metrics.put("bonusPaymentEligible", true);

        // Act
        auditIntegration.publishPayerDashboardUpdateEvent(
                TENANT_ID,
                PLAN_ID,
                dashboardType,
                metrics,
                180L,
                "dashboard-service"
        );

        // Assert
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.isEmpty()).isFalse();

        ConsumerRecord<String, String> record = records.iterator().next();
        JsonNode event = objectMapper.readTree(record.value());

        assertThat(event.get("decisionType").asText()).isEqualTo("PAYER_DASHBOARD_UPDATE");
        assertThat(event.get("decisionOutcome").asText()).isEqualTo("UPDATED");

        JsonNode context = event.get("decisionContext");
        assertThat(context.get("dashboardType").asText()).isEqualTo(dashboardType);

        JsonNode dashboardMetrics = context.get("metrics");
        assertThat(dashboardMetrics.get("avgStarRating").asDouble()).isEqualTo(4.2);
        assertThat(dashboardMetrics.get("bonusPaymentEligible").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("Should handle multiple Star Rating calculations for different plans")
    void shouldHandleMultiplePlanCalculations() throws Exception {
        // Arrange - Calculate ratings for 3 different plans
        String[] planIds = {PLAN_ID, "H5678-002", "H9012-003"};
        double[] ratings = {4.5, 3.5, 5.0};

        // Act
        for (int i = 0; i < planIds.length; i++) {
            Map<String, Object> scores = new HashMap<>();
            scores.put("overallScore", ratings[i] * 20); // Convert to percentage
            
            auditIntegration.publishStarRatingCalculationEvent(
                    TENANT_ID,
                    planIds[i],
                    "2025",
                    ratings[i],
                    scores,
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
                    quarters[i],
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
            Map<String, Object> scores = new HashMap<>();
            scores.put("year", years[i]);
            scores.put("trend", i > 0 ? "IMPROVING" : "BASELINE");

            auditIntegration.publishStarRatingCalculationEvent(
                    TENANT_ID,
                    PLAN_ID,
                    years[i],
                    yearlyRatings[i],
                    scores,
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

        for (ConsumerRecord<String, String> record : records) {
            JsonNode event = objectMapper.readTree(record.value());
            JsonNode context = event.get("decisionContext");

            if (context.get("measurementYear").asText().equals("2023")) {
                firstRating = context.get("overallRating").asDouble();
                foundFirst = true;
            }
            if (context.get("measurementYear").asText().equals("2025")) {
                lastRating = context.get("overallRating").asDouble();
                foundLast = true;
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
            Map<String, Object> scores = new HashMap<>();
            scores.put("index", i);

            auditIntegration.publishStarRatingCalculationEvent(
                    TENANT_ID + "-" + i,
                    PLAN_ID + "-" + i,
                    "2025",
                    4.0 + (i % 5) * 0.1, // Vary ratings
                    scores,
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
        Map<String, Object> scores = new HashMap<>();
        scores.put("phase", "calculation");
        auditIntegration.publishStarRatingCalculationEvent(
                TENANT_ID, cycleId, "2025", 4.5, scores, 200L, "calculate"
        );

        // Step 2: Generate Compliance Report
        Thread.sleep(50);
        Map<String, Object> complianceMetrics = new HashMap<>();
        complianceMetrics.put("phase", "compliance");
        auditIntegration.publishMedicaidComplianceEvent(
                cycleId, "TX", "2025-Q4", true, complianceMetrics, 300L, "report"
        );

        // Step 3: Update Dashboard
        Thread.sleep(50);
        Map<String, Object> dashMetrics = new HashMap<>();
        dashMetrics.put("phase", "dashboard");
        auditIntegration.publishPayerDashboardUpdateEvent(
                TENANT_ID, cycleId, "COMBINED", dashMetrics, 150L, "dashboard"
        );

        // Assert - Complete cycle audited
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.count()).isGreaterThanOrEqualTo(3);

        String[] expectedTypes = {
            "STAR_RATING_CALCULATION",
            "MEDICAID_COMPLIANCE_REPORT",
            "PAYER_DASHBOARD_UPDATE"
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
