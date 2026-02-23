package com.healthdata.fhir.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;

import com.healthdata.fhir.persistence.PractitionerEntity;
import com.healthdata.fhir.persistence.PractitionerRepository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

/**
 * Unit tests for PractitionerService.
 * Tests service layer logic including FHIR conversions, caching annotations,
 * entity field extraction, and Kafka event publishing.
 */
@Tag("unit")
@DisplayName("PractitionerService Tests")
class PractitionerServiceTest {

    private static final String TENANT = "tenant-1";
    private static final String OTHER_TENANT = "tenant-2";
    private static final UUID PRACTITIONER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String USER = "admin-user";
    private static final String NPI = "1234567890";

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final IParser JSON_PARSER = FHIR_CONTEXT.newJsonParser().setPrettyPrint(false);

    @Mock
    private PractitionerRepository practitionerRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private PractitionerService practitionerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        practitionerService = new PractitionerService(practitionerRepository, kafkaTemplate);
    }

    // ==================== Create Operations ====================

    @Nested
    @DisplayName("Create Practitioner")
    class CreatePractitioner {

        @Test
        @DisplayName("Should persist practitioner and publish Kafka event")
        void createPractitionerShouldPersistAndPublishEvent() {
            // Given
            Practitioner practitioner = createFhirPractitioner();
            PractitionerEntity savedEntity = createPractitionerEntity();

            when(practitionerRepository.save(any(PractitionerEntity.class))).thenReturn(savedEntity);

            // When
            Practitioner result = practitionerService.createPractitioner(TENANT, practitioner, USER);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(PRACTITIONER_ID.toString());

            verify(practitionerRepository).save(any(PractitionerEntity.class));
            verify(kafkaTemplate).send(eq("fhir.practitioners.created"), eq(PRACTITIONER_ID.toString()), any());
        }

        @Test
        @DisplayName("Should assign UUID when ID is not present")
        void createPractitionerShouldAssignIdIfNotPresent() {
            // Given
            Practitioner practitioner = new Practitioner();
            practitioner.addName().setFamily("Smith").addGiven("John");
            practitioner.setActive(true);

            PractitionerEntity savedEntity = PractitionerEntity.builder()
                    .id(UUID.randomUUID())
                    .tenantId(TENANT)
                    .resourceType("Practitioner")
                    .resourceJson("{}")
                    .familyName("Smith")
                    .givenName("John")
                    .active(true)
                    .createdBy(USER)
                    .lastModifiedBy(USER)
                    .createdAt(Instant.now())
                    .lastModifiedAt(Instant.now())
                    .version(0)
                    .build();

            when(practitionerRepository.save(any(PractitionerEntity.class))).thenReturn(savedEntity);

            // When
            Practitioner result = practitionerService.createPractitioner(TENANT, practitioner, USER);

            // Then
            assertThat(result.hasId()).isTrue();
            verify(practitionerRepository).save(any(PractitionerEntity.class));
        }

        @Test
        @DisplayName("Should capture correct entity fields from FHIR resource")
        void createPractitionerShouldCaptureEntityFields() {
            // Given
            Practitioner practitioner = createFhirPractitioner();
            ArgumentCaptor<PractitionerEntity> captor = ArgumentCaptor.forClass(PractitionerEntity.class);

            PractitionerEntity savedEntity = createPractitionerEntity();
            when(practitionerRepository.save(captor.capture())).thenReturn(savedEntity);

            // When
            practitionerService.createPractitioner(TENANT, practitioner, USER);

            // Then
            PractitionerEntity captured = captor.getValue();
            assertThat(captured.getTenantId()).isEqualTo(TENANT);
            assertThat(captured.getResourceType()).isEqualTo("Practitioner");
            assertThat(captured.getFamilyName()).isEqualTo("Chen");
            assertThat(captured.getGivenName()).isEqualTo("Sarah");
            assertThat(captured.getPrefix()).isEqualTo("Dr.");
            assertThat(captured.getIdentifierValue()).isEqualTo(NPI);
            assertThat(captured.getActive()).isTrue();
            assertThat(captured.getQualificationCode()).isEqualTo("MD");
            assertThat(captured.getResourceJson()).isNotBlank();
            assertThat(captured.getCreatedBy()).isEqualTo(USER);
            assertThat(captured.getLastModifiedBy()).isEqualTo(USER);
        }

        @Test
        @DisplayName("Should handle practitioner with missing optional fields")
        void createPractitionerShouldHandleMissingOptionalFields() {
            // Given
            Practitioner practitioner = new Practitioner();
            practitioner.setId(PRACTITIONER_ID.toString());
            // No name, prefix, identifier, qualification — just active=true
            practitioner.setActive(true);

            ArgumentCaptor<PractitionerEntity> captor = ArgumentCaptor.forClass(PractitionerEntity.class);

            PractitionerEntity savedEntity = PractitionerEntity.builder()
                    .id(PRACTITIONER_ID)
                    .tenantId(TENANT)
                    .resourceType("Practitioner")
                    .resourceJson("{}")
                    .active(true)
                    .createdBy(USER)
                    .lastModifiedBy(USER)
                    .createdAt(Instant.now())
                    .lastModifiedAt(Instant.now())
                    .version(0)
                    .build();

            when(practitionerRepository.save(captor.capture())).thenReturn(savedEntity);

            // When
            Practitioner result = practitionerService.createPractitioner(TENANT, practitioner, USER);

            // Then
            PractitionerEntity captured = captor.getValue();
            assertThat(captured.getFamilyName()).isNull();
            assertThat(captured.getGivenName()).isNull();
            assertThat(captured.getPrefix()).isNull();
            assertThat(captured.getIdentifierValue()).isNull();
            assertThat(captured.getQualificationCode()).isNull();
            assertThat(captured.getActive()).isTrue();
        }

        @Test
        @DisplayName("Should continue successfully when Kafka publish fails")
        void createPractitionerShouldIgnorePublishFailure() {
            // Given
            Practitioner practitioner = createFhirPractitioner();
            PractitionerEntity savedEntity = createPractitionerEntity();

            when(practitionerRepository.save(any(PractitionerEntity.class))).thenReturn(savedEntity);
            when(kafkaTemplate.send(anyString(), anyString(), any()))
                    .thenThrow(new RuntimeException("Kafka unavailable"));

            // When — should not throw
            Practitioner result = practitionerService.createPractitioner(TENANT, practitioner, USER);

            // Then
            assertThat(result).isNotNull();
            verify(practitionerRepository).save(any(PractitionerEntity.class));
        }
    }

    // ==================== Read Operations ====================

    @Nested
    @DisplayName("Get Practitioner")
    class GetPractitioner {

        @Test
        @DisplayName("Should return practitioner when found")
        void getPractitionerShouldReturnFhirResource() {
            // Given
            PractitionerEntity entity = createPractitionerEntity();
            when(practitionerRepository.findByTenantIdAndId(TENANT, PRACTITIONER_ID))
                    .thenReturn(Optional.of(entity));

            // When
            Optional<Practitioner> result = practitionerService.getPractitioner(TENANT, PRACTITIONER_ID);

            // Then
            assertThat(result).isPresent();
            Practitioner practitioner = result.get();
            assertThat(practitioner.getNameFirstRep().getFamily()).isEqualTo("Chen");
            assertThat(practitioner.getNameFirstRep().getGivenAsSingleString()).isEqualTo("Sarah");
        }

        @Test
        @DisplayName("Should return empty when practitioner not found")
        void getPractitionerShouldReturnEmptyWhenNotFound() {
            // Given
            when(practitionerRepository.findByTenantIdAndId(TENANT, PRACTITIONER_ID))
                    .thenReturn(Optional.empty());

            // When
            Optional<Practitioner> result = practitionerService.getPractitioner(TENANT, PRACTITIONER_ID);

            // Then
            assertThat(result).isEmpty();
        }
    }

    // ==================== Update Operations ====================

    @Nested
    @DisplayName("Update Practitioner")
    class UpdatePractitioner {

        @Test
        @DisplayName("Should update practitioner and publish event")
        void updatePractitionerShouldUpdateAndPublishEvent() {
            // Given
            PractitionerEntity existing = createPractitionerEntity();
            when(practitionerRepository.findByTenantIdAndId(TENANT, PRACTITIONER_ID))
                    .thenReturn(Optional.of(existing));
            when(practitionerRepository.save(any(PractitionerEntity.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Practitioner updated = createFhirPractitioner();
            updated.getNameFirstRep().setFamily("Updated");

            // When
            Practitioner result = practitionerService.updatePractitioner(TENANT, PRACTITIONER_ID, updated, USER);

            // Then
            assertThat(result).isNotNull();
            verify(practitionerRepository).save(any(PractitionerEntity.class));
            verify(kafkaTemplate).send(eq("fhir.practitioners.updated"), eq(PRACTITIONER_ID.toString()), any());
        }

        @Test
        @DisplayName("Should preserve audit fields from existing entity on update")
        void updatePractitionerShouldPreserveAuditFields() {
            // Given
            Instant originalCreatedAt = Instant.parse("2025-01-01T00:00:00Z");
            PractitionerEntity existing = createPractitionerEntity();
            existing.setCreatedAt(originalCreatedAt);
            existing.setCreatedBy("original-user");
            existing.setVersion(3);

            when(practitionerRepository.findByTenantIdAndId(TENANT, PRACTITIONER_ID))
                    .thenReturn(Optional.of(existing));

            ArgumentCaptor<PractitionerEntity> captor = ArgumentCaptor.forClass(PractitionerEntity.class);
            when(practitionerRepository.save(captor.capture()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Practitioner updated = createFhirPractitioner();

            // When
            practitionerService.updatePractitioner(TENANT, PRACTITIONER_ID, updated, USER);

            // Then
            PractitionerEntity captured = captor.getValue();
            assertThat(captured.getCreatedAt()).isEqualTo(originalCreatedAt);
            assertThat(captured.getCreatedBy()).isEqualTo("original-user");
            assertThat(captured.getVersion()).isEqualTo(3);
            assertThat(captured.getLastModifiedBy()).isEqualTo(USER);
        }

        @Test
        @DisplayName("Should throw when practitioner not found on update")
        void updatePractitionerShouldThrowWhenNotFound() {
            // Given
            when(practitionerRepository.findByTenantIdAndId(TENANT, PRACTITIONER_ID))
                    .thenReturn(Optional.empty());

            Practitioner practitioner = createFhirPractitioner();

            // When/Then
            assertThatThrownBy(() ->
                    practitionerService.updatePractitioner(TENANT, PRACTITIONER_ID, practitioner, USER))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Practitioner not found");
        }
    }

    // ==================== Delete Operations ====================

    @Nested
    @DisplayName("Delete Practitioner")
    class DeletePractitioner {

        @Test
        @DisplayName("Should delete practitioner and publish event")
        void deletePractitionerShouldDeleteAndPublishEvent() {
            // Given
            PractitionerEntity entity = createPractitionerEntity();
            when(practitionerRepository.findByTenantIdAndId(TENANT, PRACTITIONER_ID))
                    .thenReturn(Optional.of(entity));

            // When
            practitionerService.deletePractitioner(TENANT, PRACTITIONER_ID, USER);

            // Then
            verify(practitionerRepository).delete(entity);
            verify(kafkaTemplate).send(eq("fhir.practitioners.deleted"), eq(PRACTITIONER_ID.toString()), any());
        }

        @Test
        @DisplayName("Should throw when practitioner not found on delete")
        void deletePractitionerShouldThrowWhenNotFound() {
            // Given
            when(practitionerRepository.findByTenantIdAndId(TENANT, PRACTITIONER_ID))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() ->
                    practitionerService.deletePractitioner(TENANT, PRACTITIONER_ID, USER))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Practitioner not found");
        }
    }

    // ==================== Search Operations ====================

    @Nested
    @DisplayName("Search Practitioners")
    class SearchPractitioners {

        @Test
        @DisplayName("Should return paged practitioners")
        void searchPractitionersShouldReturnPagedResults() {
            // Given
            PractitionerEntity entity = createPractitionerEntity();
            PageRequest pageable = PageRequest.of(0, 20);
            Page<PractitionerEntity> page = new PageImpl<>(List.of(entity), pageable, 1);

            when(practitionerRepository.findByTenantId(TENANT, pageable)).thenReturn(page);

            // When
            Page<Practitioner> result = practitionerService.searchPractitioners(TENANT, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getNameFirstRep().getFamily()).isEqualTo("Chen");
        }

        @Test
        @DisplayName("Should find practitioners by name")
        void findByNameShouldReturnMatchingPractitioners() {
            // Given
            PractitionerEntity entity = createPractitionerEntity();
            when(practitionerRepository.findByTenantIdAndFamilyNameContainingIgnoreCase(TENANT, "Chen"))
                    .thenReturn(List.of(entity));

            // When
            List<Practitioner> results = practitionerService.findByName(TENANT, "Chen");

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getNameFirstRep().getFamily()).isEqualTo("Chen");
        }

        @Test
        @DisplayName("Should find practitioner by identifier")
        void findByIdentifierShouldReturnMatchingPractitioner() {
            // Given
            PractitionerEntity entity = createPractitionerEntity();
            when(practitionerRepository.findByTenantIdAndIdentifierValue(TENANT, NPI))
                    .thenReturn(Optional.of(entity));

            // When
            Optional<Practitioner> result = practitionerService.findByIdentifier(TENANT, NPI);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getIdentifierFirstRep().getValue()).isEqualTo(NPI);
        }

        @Test
        @DisplayName("Should return empty when identifier not found")
        void findByIdentifierShouldReturnEmptyWhenNotFound() {
            // Given
            when(practitionerRepository.findByTenantIdAndIdentifierValue(TENANT, "unknown"))
                    .thenReturn(Optional.empty());

            // When
            Optional<Practitioner> result = practitionerService.findByIdentifier(TENANT, "unknown");

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should find all practitioners for a tenant")
        void findAllShouldReturnAllTenantPractitioners() {
            // Given
            PractitionerEntity entity1 = createPractitionerEntity();
            PractitionerEntity entity2 = PractitionerEntity.builder()
                    .id(UUID.randomUUID())
                    .tenantId(TENANT)
                    .resourceType("Practitioner")
                    .resourceJson(createPractitionerJson("Lopez", "Maria", "NP", "9876543210"))
                    .familyName("Lopez")
                    .givenName("Maria")
                    .active(true)
                    .createdBy(USER)
                    .lastModifiedBy(USER)
                    .createdAt(Instant.now())
                    .lastModifiedAt(Instant.now())
                    .version(0)
                    .build();

            when(practitionerRepository.findByTenantId(TENANT)).thenReturn(List.of(entity1, entity2));

            // When
            List<Practitioner> results = practitionerService.findAll(TENANT);

            // Then
            assertThat(results).hasSize(2);
        }
    }

    // ==================== Helper Methods ====================

    private Practitioner createFhirPractitioner() {
        Practitioner practitioner = new Practitioner();
        practitioner.setId(PRACTITIONER_ID.toString());
        practitioner.setActive(true);

        HumanName name = practitioner.addName();
        name.setFamily("Chen");
        name.addGiven("Sarah");
        name.addPrefix("Dr.");

        Identifier identifier = practitioner.addIdentifier();
        identifier.setSystem("http://hl7.org/fhir/sid/us-npi");
        identifier.setValue(NPI);

        Practitioner.PractitionerQualificationComponent qualification = practitioner.addQualification();
        CodeableConcept code = new CodeableConcept();
        code.addCoding().setSystem("http://terminology.hl7.org/CodeSystem/v2-0360").setCode("MD").setDisplay("Doctor of Medicine");
        qualification.setCode(code);

        return practitioner;
    }

    private PractitionerEntity createPractitionerEntity() {
        Practitioner practitioner = createFhirPractitioner();
        String json = JSON_PARSER.encodeResourceToString(practitioner);

        return PractitionerEntity.builder()
                .id(PRACTITIONER_ID)
                .tenantId(TENANT)
                .resourceType("Practitioner")
                .resourceJson(json)
                .familyName("Chen")
                .givenName("Sarah")
                .prefix("Dr.")
                .identifierValue(NPI)
                .qualificationCode("MD")
                .active(true)
                .createdBy(USER)
                .lastModifiedBy(USER)
                .createdAt(Instant.now())
                .lastModifiedAt(Instant.now())
                .version(0)
                .build();
    }

    private String createPractitionerJson(String family, String given, String prefix, String npi) {
        Practitioner practitioner = new Practitioner();
        practitioner.setId(UUID.randomUUID().toString());
        practitioner.setActive(true);
        HumanName name = practitioner.addName();
        name.setFamily(family);
        name.addGiven(given);
        if (prefix != null) {
            name.addPrefix(prefix);
        }
        if (npi != null) {
            practitioner.addIdentifier()
                    .setSystem("http://hl7.org/fhir/sid/us-npi")
                    .setValue(npi);
        }
        return JSON_PARSER.encodeResourceToString(practitioner);
    }
}
