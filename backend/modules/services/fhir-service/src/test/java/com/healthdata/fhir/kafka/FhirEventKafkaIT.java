package com.healthdata.fhir.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.healthdata.fhir.FhirServiceApplication;
import com.healthdata.fhir.persistence.PatientRepository;
import com.healthdata.fhir.service.PatientService;

/**
 * Kafka integration tests for FHIR resource event publishing.
 * Uses EmbeddedKafka to test the complete event flow.
 */
@SpringBootTest(
    classes = FhirServiceApplication.class,
    properties = {
        "spring.cache.type=simple",
        "spring.data.redis.repositories.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.liquibase.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
    }
)
@EmbeddedKafka(
    partitions = 1,
    topics = {
        "fhir.patients.created",
        "fhir.patients.updated",
        "fhir.patients.deleted"
    },
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:9092",
        "port=9092"
    }
)
@DirtiesContext
@ActiveProfiles("test")
class FhirEventKafkaIT {

    private static final String H2_URL = "jdbc:h2:mem:fhir_kafka_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH";
    private static final String TENANT_ID = "kafka-test-tenant";
    private static final String USER_ID = "test-user";

    @DynamicPropertySource
    static void overrideDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> H2_URL);
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.H2Dialect");
    }

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Autowired
    private PatientService patientService;

    @Autowired
    private PatientRepository patientRepository;

    private Consumer<String, Map<String, Object>> consumer;

    @BeforeEach
    void setUp() {
        // Create a test consumer
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
            "fhir-test-group",
            "true",
            embeddedKafka
        );
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        ConsumerFactory<String, Map<String, Object>> cf = new DefaultKafkaConsumerFactory<>(consumerProps);
        consumer = cf.createConsumer();
        embeddedKafka.consumeFromAllEmbeddedTopics(consumer);
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    @Transactional
    void shouldPublishPatientCreatedEvent() {
        // Given
        Patient patient = createTestPatient("Created", "Patient");

        // When
        Patient created = patientService.createPatient(TENANT_ID, patient, USER_ID);

        // Then
        await().atMost(10, TimeUnit.SECONDS)
               .pollInterval(Duration.ofMillis(100))
               .untilAsserted(() -> {
                   ConsumerRecords<String, Map<String, Object>> records =
                       KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(5));

                   assertThat(records.count()).isGreaterThan(0);

                   boolean foundCreatedEvent = false;
                   for (var record : records) {
                       if (record.topic().equals("fhir.patients.created") &&
                           record.key().equals(created.getIdPart())) {
                           foundCreatedEvent = true;
                           Map<String, Object> event = record.value();
                           assertThat(event.get("resourceType")).isEqualTo("Patient");
                           assertThat(event.get("tenantId")).isEqualTo(TENANT_ID);
                       }
                   }
                   assertThat(foundCreatedEvent).isTrue();
               });
    }

    @Test
    @Transactional
    void shouldPublishPatientUpdatedEvent() {
        // Given
        Patient patient = createTestPatient("Original", "Name");
        Patient created = patientService.createPatient(TENANT_ID, patient, USER_ID);

        // Consume the create event first
        KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(2));

        // When - Update the patient
        created.getNameFirstRep().setFamily("Updated");
        Patient updated = patientService.updatePatient(
            TENANT_ID,
            created.getIdPart(),
            created,
            USER_ID
        );

        // Then
        await().atMost(10, TimeUnit.SECONDS)
               .pollInterval(Duration.ofMillis(100))
               .untilAsserted(() -> {
                   ConsumerRecords<String, Map<String, Object>> records =
                       KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(5));

                   boolean foundUpdatedEvent = false;
                   for (var record : records) {
                       if (record.topic().equals("fhir.patients.updated") &&
                           record.key().equals(updated.getIdPart())) {
                           foundUpdatedEvent = true;
                           Map<String, Object> event = record.value();
                           assertThat(event.get("resourceType")).isEqualTo("Patient");
                       }
                   }
                   assertThat(foundUpdatedEvent).isTrue();
               });
    }

    @Test
    @Transactional
    void shouldPublishPatientDeletedEvent() {
        // Given
        Patient patient = createTestPatient("ToDelete", "Patient");
        Patient created = patientService.createPatient(TENANT_ID, patient, USER_ID);
        String patientId = created.getIdPart();

        // Consume the create event first
        KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(2));

        // When - Delete the patient
        patientService.deletePatient(TENANT_ID, patientId, USER_ID);

        // Then
        await().atMost(10, TimeUnit.SECONDS)
               .pollInterval(Duration.ofMillis(100))
               .untilAsserted(() -> {
                   ConsumerRecords<String, Map<String, Object>> records =
                       KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(5));

                   boolean foundDeletedEvent = false;
                   for (var record : records) {
                       if (record.topic().equals("fhir.patients.deleted") &&
                           record.key().equals(patientId)) {
                           foundDeletedEvent = true;
                       }
                   }
                   assertThat(foundDeletedEvent).isTrue();
               });
    }

    @Test
    @Transactional
    void eventsShouldContainCorrectMetadata() {
        // Given
        Patient patient = createTestPatient("Metadata", "Test");

        // When
        Patient created = patientService.createPatient(TENANT_ID, patient, USER_ID);

        // Then
        await().atMost(10, TimeUnit.SECONDS)
               .pollInterval(Duration.ofMillis(100))
               .untilAsserted(() -> {
                   ConsumerRecords<String, Map<String, Object>> records =
                       KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(5));

                   for (var record : records) {
                       if (record.topic().equals("fhir.patients.created")) {
                           Map<String, Object> event = record.value();

                           // Verify required metadata fields
                           assertThat(event).containsKey("id");
                           assertThat(event).containsKey("resourceType");
                           assertThat(event).containsKey("tenantId");
                           assertThat(event.get("resourceType")).isEqualTo("Patient");
                           assertThat(event.get("tenantId")).isEqualTo(TENANT_ID);
                       }
                   }
               });
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
