package com.healthdata.events.intelligence.startup;

import com.healthdata.eventsourcing.intelligence.IntelligenceTopics;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class IntelligenceStartupPreflight implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(IntelligenceStartupPreflight.class);

    private final JdbcTemplate jdbcTemplate;

    @Value("${intelligence.preflight.enabled:false}")
    private boolean enabled;

    @Value("${intelligence.preflight.kafka-check.enabled:false}")
    private boolean kafkaCheckEnabled;

    @Value("${intelligence.preflight.kafka.bootstrap-servers:${healthdata.kafka.bootstrap-servers:localhost:9094}}")
    private String kafkaBootstrapServers;

    public IntelligenceStartupPreflight(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!enabled) {
            log.info("Intelligence startup preflight is disabled");
            return;
        }

        verifyTableExists("intelligence_recommendations");
        verifyTableExists("intelligence_validation_findings");
        verifyTableExists("intelligence_tenant_trust_projection");

        if (kafkaCheckEnabled) {
            verifyKafkaTopics();
        }

        log.info("Intelligence startup preflight passed");
    }

    private void verifyTableExists(String tableName) {
        String regClass = jdbcTemplate.queryForObject(
                "SELECT to_regclass(?)",
                String.class,
                "public." + tableName
        );

        if (regClass == null || regClass.isBlank()) {
            throw new IllegalStateException("Required intelligence table not found: " + tableName);
        }
    }

    private void verifyKafkaTopics() throws Exception {
        List<String> topics = List.of(
                IntelligenceTopics.INGEST_RAW,
                IntelligenceTopics.INGEST_NORMALIZED,
                IntelligenceTopics.VALIDATION_FINDINGS,
                IntelligenceTopics.INTELLIGENCE_SIGNALS,
                IntelligenceTopics.RECOMMENDATIONS_GENERATED,
                IntelligenceTopics.RECOMMENDATIONS_REVIEWED
        );

        try (AdminClient adminClient = AdminClient.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers
        ))) {
            adminClient.describeTopics(topics)
                    .allTopicNames()
                    .get(Duration.ofSeconds(10).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("Kafka preflight check failed for intelligence topics", e);
        }
    }
}
