package com.healthdata.fhir.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.healthdata.fhir.FhirServiceApplication;
import com.healthdata.fhir.config.TestCacheConfiguration;
import com.healthdata.fhir.config.TestSecurityConfiguration;
import com.healthdata.fhir.persistence.PatientRepository;
import com.healthdata.fhir.service.PatientService;
import com.healthdata.fhir.service.PatientService.PatientEvent;

/**
 * Kafka integration tests for FHIR resource event publishing.
 * Uses Testcontainers Kafka to test the complete event flow.
 */
@SpringBootTest(
    classes = FhirServiceApplication.class,
    properties = {
        "spring.cache.type=simple",
        "spring.data.redis.repositories.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.liquibase.enabled=false",
        // Only exclude Redis, NOT Kafka - we need Kafka for these integration tests
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration",
        // Override test profile exclusions to enable Kafka
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer"
    }
)
@Import({TestCacheConfiguration.class, TestSecurityConfiguration.class})
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext
@ActiveProfiles("kafka-it")
class FhirEventKafkaIT {

    private static final String H2_URL = "jdbc:tc:postgresql:15-alpine:///testdb";
    private static final String TENANT_ID = "kafka-test-tenant";
    private static final String USER_ID = "test-user";

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"))
        .withStartupTimeout(Duration.ofMinutes(3))
        .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true")
        .withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1")
        .withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1")
        .withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        // Database properties
        registry.add("spring.datasource.url", () -> H2_URL);
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.testcontainers.jdbc.ContainerDatabaseDriver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");

        // Kafka properties from Testcontainers
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.producer.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.consumer.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private PatientService patientService;

    @Autowired
    private PatientRepository patientRepository;

    private Consumer<String, Object> consumer;

    @BeforeEach
    void setUp() {
        // Create a test consumer with Object value type to handle PatientEvent deserialization
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "fhir-test-group-" + UUID.randomUUID());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.healthdata.fhir.service.PatientService$PatientEvent");

        ConsumerFactory<String, Object> cf = new DefaultKafkaConsumerFactory<>(consumerProps);
        consumer = cf.createConsumer();
        consumer.subscribe(Collections.singletonList("fhir.patients.created"));
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    void shouldPublishPatientCreatedEvent() {
        // Given
        Patient patient = createTestPatient("Created", "Patient");

        // When
        Patient created = patientService.createPatient(TENANT_ID, patient, USER_ID);
        String expectedId = created.getIdPart();

        // Then - poll for messages
        boolean foundCreatedEvent = false;
        int pollAttempts = 0;
        while (!foundCreatedEvent && pollAttempts < 30) {
            pollAttempts++;
            ConsumerRecords<String, Object> records = consumer.poll(Duration.ofSeconds(1));

            for (var record : records) {
                if (record.topic().equals("fhir.patients.created") &&
                    record.key() != null &&
                    record.key().equals(expectedId)) {

                    PatientEvent event = (PatientEvent) record.value();
                    foundCreatedEvent = true;

                    // Verify PatientEvent fields: id, tenantId, type, occurredAt, actor
                    assertThat(event.type()).isEqualTo("CREATED");
                    assertThat(event.tenantId()).isEqualTo(TENANT_ID);
                    assertThat(event.id()).isEqualTo(expectedId);
                    assertThat(event.actor()).isEqualTo(USER_ID);
                    assertThat(event.occurredAt()).isNotNull();
                }
            }
        }
        assertThat(foundCreatedEvent)
            .as("Expected to find a CREATED event for patient " + expectedId)
            .isTrue();
    }

    @Test
    void shouldPublishPatientUpdatedEvent() {
        // Subscribe to updated topic
        consumer.subscribe(Collections.singletonList("fhir.patients.updated"));

        // Given
        Patient patient = createTestPatient("Original", "Name");
        Patient created = patientService.createPatient(TENANT_ID, patient, USER_ID);
        String patientId = created.getIdPart();

        // Consume any initial events
        consumer.poll(Duration.ofSeconds(2));

        // When - Update the patient
        created.getNameFirstRep().setFamily("Updated");
        Patient updated = patientService.updatePatient(TENANT_ID, patientId, created, USER_ID);

        // Then
        boolean foundUpdatedEvent = false;
        int pollAttempts = 0;
        while (!foundUpdatedEvent && pollAttempts < 30) {
            pollAttempts++;
            ConsumerRecords<String, Object> records = consumer.poll(Duration.ofSeconds(1));

            for (var record : records) {
                if (record.topic().equals("fhir.patients.updated") &&
                    record.key() != null &&
                    record.key().equals(patientId)) {

                    PatientEvent event = (PatientEvent) record.value();
                    foundUpdatedEvent = true;

                    assertThat(event.type()).isEqualTo("UPDATED");
                    assertThat(event.tenantId()).isEqualTo(TENANT_ID);
                    assertThat(event.id()).isEqualTo(patientId);
                }
            }
        }
        assertThat(foundUpdatedEvent)
            .as("Expected to find an UPDATED event for patient " + patientId)
            .isTrue();
    }

    @Test
    void shouldPublishPatientDeletedEvent() {
        // Subscribe to deleted topic
        consumer.subscribe(Collections.singletonList("fhir.patients.deleted"));

        // Given
        Patient patient = createTestPatient("ToDelete", "Patient");
        Patient created = patientService.createPatient(TENANT_ID, patient, USER_ID);
        String patientId = created.getIdPart();

        // Consume any initial events
        consumer.poll(Duration.ofSeconds(2));

        // When - Delete the patient
        patientService.deletePatient(TENANT_ID, patientId, USER_ID);

        // Then
        boolean foundDeletedEvent = false;
        int pollAttempts = 0;
        while (!foundDeletedEvent && pollAttempts < 30) {
            pollAttempts++;
            ConsumerRecords<String, Object> records = consumer.poll(Duration.ofSeconds(1));

            for (var record : records) {
                if (record.topic().equals("fhir.patients.deleted") &&
                    record.key() != null &&
                    record.key().equals(patientId)) {

                    PatientEvent event = (PatientEvent) record.value();
                    foundDeletedEvent = true;

                    assertThat(event.type()).isEqualTo("DELETED");
                    assertThat(event.id()).isEqualTo(patientId);
                }
            }
        }
        assertThat(foundDeletedEvent)
            .as("Expected to find a DELETED event for patient " + patientId)
            .isTrue();
    }

    @Test
    void eventsShouldContainCorrectMetadata() {
        // Given
        Patient patient = createTestPatient("Metadata", "Test");

        // When
        Patient created = patientService.createPatient(TENANT_ID, patient, USER_ID);
        String expectedId = created.getIdPart();

        // Then
        boolean foundEvent = false;
        int pollAttempts = 0;
        while (!foundEvent && pollAttempts < 30) {
            pollAttempts++;
            ConsumerRecords<String, Object> records = consumer.poll(Duration.ofSeconds(1));

            for (var record : records) {
                if (record.topic().equals("fhir.patients.created") &&
                    record.key() != null &&
                    record.key().equals(expectedId)) {

                    foundEvent = true;
                    PatientEvent event = (PatientEvent) record.value();

                    // Verify all PatientEvent record fields
                    assertThat(event.id()).isEqualTo(expectedId);
                    assertThat(event.tenantId()).isEqualTo(TENANT_ID);
                    assertThat(event.type()).isEqualTo("CREATED");
                    assertThat(event.occurredAt()).isNotNull();
                    assertThat(event.actor()).isEqualTo(USER_ID);
                }
            }
        }
        assertThat(foundEvent)
            .as("Expected to find event with metadata for patient " + expectedId)
            .isTrue();
    }

    private Patient createTestPatient(String givenName, String familyName) {
        Patient patient = new Patient();
        patient.setId(UUID.randomUUID().toString());
        patient.addName()
               .setFamily(familyName)
               .addGiven(givenName);
        patient.setGender(Enumerations.AdministrativeGender.OTHER);
        patient.setBirthDateElement(new org.hl7.fhir.r4.model.DateType(
            java.util.Date.from(LocalDate.of(1990, 1, 1)
                .atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant())
        ));
        return patient;
    }
}
