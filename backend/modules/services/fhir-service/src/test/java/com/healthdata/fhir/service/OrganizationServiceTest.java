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

import com.healthdata.fhir.persistence.OrganizationEntity;
import com.healthdata.fhir.persistence.OrganizationRepository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

/**
 * Unit tests for OrganizationService.
 * Tests CRUD, search, FHIR conversions, field extraction, and Kafka events
 * for healthcare organization management.
 */
@Tag("unit")
@DisplayName("OrganizationService Tests")
class OrganizationServiceTest {

    private static final String TENANT = "tenant-1";
    private static final UUID ORG_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final String USER = "admin-user";
    private static final String ORG_NAME = "Main Street Family Practice";
    private static final String ORG_IDENTIFIER = "ORG-001";

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final IParser JSON_PARSER = FHIR_CONTEXT.newJsonParser().setPrettyPrint(false);

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private OrganizationService organizationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        organizationService = new OrganizationService(organizationRepository, kafkaTemplate);
    }

    // ==================== Create Operations ====================

    @Nested
    @DisplayName("Create Organization")
    class CreateOrganization {

        @Test
        @DisplayName("Should persist organization and publish Kafka event")
        void createOrganizationShouldPersistAndPublishEvent() {
            // Given
            Organization organization = createFhirOrganization();
            OrganizationEntity savedEntity = createOrganizationEntity();

            when(organizationRepository.save(any(OrganizationEntity.class))).thenReturn(savedEntity);

            // When
            Organization result = organizationService.createOrganization(TENANT, organization, USER);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(ORG_ID.toString());

            verify(organizationRepository).save(any(OrganizationEntity.class));
            verify(kafkaTemplate).send(eq("fhir.organizations.created"), eq(ORG_ID.toString()), any());
        }

        @Test
        @DisplayName("Should assign UUID when ID is not present")
        void createOrganizationShouldAssignIdIfNotPresent() {
            // Given
            Organization organization = new Organization();
            organization.setName(ORG_NAME);
            organization.setActive(true);

            OrganizationEntity savedEntity = OrganizationEntity.builder()
                    .id(UUID.randomUUID())
                    .tenantId(TENANT)
                    .resourceType("Organization")
                    .resourceJson("{}")
                    .name(ORG_NAME)
                    .active(true)
                    .createdBy(USER)
                    .lastModifiedBy(USER)
                    .createdAt(Instant.now())
                    .lastModifiedAt(Instant.now())
                    .version(0)
                    .build();

            when(organizationRepository.save(any(OrganizationEntity.class))).thenReturn(savedEntity);

            // When
            Organization result = organizationService.createOrganization(TENANT, organization, USER);

            // Then
            assertThat(result.hasId()).isTrue();
            verify(organizationRepository).save(any(OrganizationEntity.class));
        }

        @Test
        @DisplayName("Should capture all entity fields from FHIR resource")
        void createOrganizationShouldCaptureEntityFields() {
            // Given
            Organization organization = createFhirOrganization();
            ArgumentCaptor<OrganizationEntity> captor = ArgumentCaptor.forClass(OrganizationEntity.class);

            OrganizationEntity savedEntity = createOrganizationEntity();
            when(organizationRepository.save(captor.capture())).thenReturn(savedEntity);

            // When
            organizationService.createOrganization(TENANT, organization, USER);

            // Then
            OrganizationEntity captured = captor.getValue();
            assertThat(captured.getTenantId()).isEqualTo(TENANT);
            assertThat(captured.getResourceType()).isEqualTo("Organization");
            assertThat(captured.getName()).isEqualTo(ORG_NAME);
            assertThat(captured.getIdentifierValue()).isEqualTo(ORG_IDENTIFIER);
            assertThat(captured.getActive()).isTrue();
            assertThat(captured.getTypeCode()).isEqualTo("prov");
            assertThat(captured.getTypeDisplay()).isEqualTo("Healthcare Provider");
            assertThat(captured.getResourceJson()).isNotBlank();
            assertThat(captured.getCreatedBy()).isEqualTo(USER);
            assertThat(captured.getLastModifiedBy()).isEqualTo(USER);
        }

        @Test
        @DisplayName("Should handle organization with missing optional fields")
        void createOrganizationShouldHandleMissingOptionalFields() {
            // Given
            Organization organization = new Organization();
            organization.setId(ORG_ID.toString());
            organization.setActive(true);
            // No name, no identifier, no type

            ArgumentCaptor<OrganizationEntity> captor = ArgumentCaptor.forClass(OrganizationEntity.class);

            OrganizationEntity savedEntity = OrganizationEntity.builder()
                    .id(ORG_ID)
                    .tenantId(TENANT)
                    .resourceType("Organization")
                    .resourceJson("{}")
                    .active(true)
                    .createdBy(USER)
                    .lastModifiedBy(USER)
                    .createdAt(Instant.now())
                    .lastModifiedAt(Instant.now())
                    .version(0)
                    .build();

            when(organizationRepository.save(captor.capture())).thenReturn(savedEntity);

            // When
            organizationService.createOrganization(TENANT, organization, USER);

            // Then
            OrganizationEntity captured = captor.getValue();
            assertThat(captured.getName()).isNull();
            assertThat(captured.getIdentifierValue()).isNull();
            assertThat(captured.getTypeCode()).isNull();
            assertThat(captured.getTypeDisplay()).isNull();
        }

        @Test
        @DisplayName("Should continue successfully when Kafka publish fails")
        void createOrganizationShouldIgnorePublishFailure() {
            // Given
            Organization organization = createFhirOrganization();
            OrganizationEntity savedEntity = createOrganizationEntity();

            when(organizationRepository.save(any(OrganizationEntity.class))).thenReturn(savedEntity);
            when(kafkaTemplate.send(anyString(), anyString(), any()))
                    .thenThrow(new RuntimeException("Kafka unavailable"));

            // When — should not throw
            Organization result = organizationService.createOrganization(TENANT, organization, USER);

            // Then
            assertThat(result).isNotNull();
            verify(organizationRepository).save(any(OrganizationEntity.class));
        }
    }

    // ==================== Read Operations ====================

    @Nested
    @DisplayName("Get Organization")
    class GetOrganization {

        @Test
        @DisplayName("Should return organization when found")
        void getOrganizationShouldReturnFhirResource() {
            // Given
            OrganizationEntity entity = createOrganizationEntity();
            when(organizationRepository.findByTenantIdAndId(TENANT, ORG_ID))
                    .thenReturn(Optional.of(entity));

            // When
            Optional<Organization> result = organizationService.getOrganization(TENANT, ORG_ID);

            // Then
            assertThat(result).isPresent();
            Organization org = result.get();
            assertThat(org.getName()).isEqualTo(ORG_NAME);
        }

        @Test
        @DisplayName("Should return empty when organization not found")
        void getOrganizationShouldReturnEmptyWhenNotFound() {
            // Given
            when(organizationRepository.findByTenantIdAndId(TENANT, ORG_ID))
                    .thenReturn(Optional.empty());

            // When
            Optional<Organization> result = organizationService.getOrganization(TENANT, ORG_ID);

            // Then
            assertThat(result).isEmpty();
        }
    }

    // ==================== Update Operations ====================

    @Nested
    @DisplayName("Update Organization")
    class UpdateOrganization {

        @Test
        @DisplayName("Should update organization and publish event")
        void updateOrganizationShouldUpdateAndPublishEvent() {
            // Given
            OrganizationEntity existing = createOrganizationEntity();
            when(organizationRepository.findByTenantIdAndId(TENANT, ORG_ID))
                    .thenReturn(Optional.of(existing));
            when(organizationRepository.save(any(OrganizationEntity.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Organization updated = createFhirOrganization();
            updated.setName("Updated Clinic Name");

            // When
            Organization result = organizationService.updateOrganization(TENANT, ORG_ID, updated, USER);

            // Then
            assertThat(result).isNotNull();
            verify(organizationRepository).save(any(OrganizationEntity.class));
            verify(kafkaTemplate).send(eq("fhir.organizations.updated"), eq(ORG_ID.toString()), any());
        }

        @Test
        @DisplayName("Should preserve audit fields from existing entity on update")
        void updateOrganizationShouldPreserveAuditFields() {
            // Given
            Instant originalCreatedAt = Instant.parse("2025-01-01T00:00:00Z");
            OrganizationEntity existing = createOrganizationEntity();
            existing.setCreatedAt(originalCreatedAt);
            existing.setCreatedBy("original-user");
            existing.setVersion(5);

            when(organizationRepository.findByTenantIdAndId(TENANT, ORG_ID))
                    .thenReturn(Optional.of(existing));

            ArgumentCaptor<OrganizationEntity> captor = ArgumentCaptor.forClass(OrganizationEntity.class);
            when(organizationRepository.save(captor.capture()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Organization updated = createFhirOrganization();

            // When
            organizationService.updateOrganization(TENANT, ORG_ID, updated, USER);

            // Then
            OrganizationEntity captured = captor.getValue();
            assertThat(captured.getCreatedAt()).isEqualTo(originalCreatedAt);
            assertThat(captured.getCreatedBy()).isEqualTo("original-user");
            assertThat(captured.getVersion()).isEqualTo(5);
            assertThat(captured.getLastModifiedBy()).isEqualTo(USER);
        }

        @Test
        @DisplayName("Should throw when organization not found on update")
        void updateOrganizationShouldThrowWhenNotFound() {
            // Given
            when(organizationRepository.findByTenantIdAndId(TENANT, ORG_ID))
                    .thenReturn(Optional.empty());

            Organization org = createFhirOrganization();

            // When/Then
            assertThatThrownBy(() ->
                    organizationService.updateOrganization(TENANT, ORG_ID, org, USER))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Organization not found");
        }
    }

    // ==================== Delete Operations ====================

    @Nested
    @DisplayName("Delete Organization")
    class DeleteOrganization {

        @Test
        @DisplayName("Should delete organization and publish event")
        void deleteOrganizationShouldDeleteAndPublishEvent() {
            // Given
            OrganizationEntity entity = createOrganizationEntity();
            when(organizationRepository.findByTenantIdAndId(TENANT, ORG_ID))
                    .thenReturn(Optional.of(entity));

            // When
            organizationService.deleteOrganization(TENANT, ORG_ID, USER);

            // Then
            verify(organizationRepository).delete(entity);
            verify(kafkaTemplate).send(eq("fhir.organizations.deleted"), eq(ORG_ID.toString()), any());
        }

        @Test
        @DisplayName("Should throw when organization not found on delete")
        void deleteOrganizationShouldThrowWhenNotFound() {
            // Given
            when(organizationRepository.findByTenantIdAndId(TENANT, ORG_ID))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() ->
                    organizationService.deleteOrganization(TENANT, ORG_ID, USER))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Organization not found");
        }
    }

    // ==================== Search Operations ====================

    @Nested
    @DisplayName("Search Organizations")
    class SearchOrganizations {

        @Test
        @DisplayName("Should return paged results")
        void searchOrganizationsShouldReturnPagedResults() {
            // Given
            OrganizationEntity entity = createOrganizationEntity();
            PageRequest pageable = PageRequest.of(0, 20);
            Page<OrganizationEntity> page = new PageImpl<>(List.of(entity), pageable, 1);

            when(organizationRepository.findByTenantId(TENANT, pageable)).thenReturn(page);

            // When
            Page<Organization> result = organizationService.searchOrganizations(TENANT, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo(ORG_NAME);
        }

        @Test
        @DisplayName("Should find organizations by name")
        void findByNameShouldReturnMatchingOrganizations() {
            // Given
            OrganizationEntity entity = createOrganizationEntity();
            when(organizationRepository.findByTenantIdAndNameContainingIgnoreCase(TENANT, "Main"))
                    .thenReturn(List.of(entity));

            // When
            List<Organization> results = organizationService.findByName(TENANT, "Main");

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getName()).isEqualTo(ORG_NAME);
        }

        @Test
        @DisplayName("Should find organization by identifier")
        void findByIdentifierShouldReturnMatchingOrganization() {
            // Given
            OrganizationEntity entity = createOrganizationEntity();
            when(organizationRepository.findByTenantIdAndIdentifierValue(TENANT, ORG_IDENTIFIER))
                    .thenReturn(Optional.of(entity));

            // When
            Optional<Organization> result = organizationService.findByIdentifier(TENANT, ORG_IDENTIFIER);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo(ORG_NAME);
        }

        @Test
        @DisplayName("Should return empty when identifier not found")
        void findByIdentifierShouldReturnEmptyWhenNotFound() {
            // Given
            when(organizationRepository.findByTenantIdAndIdentifierValue(TENANT, "unknown"))
                    .thenReturn(Optional.empty());

            // When
            Optional<Organization> result = organizationService.findByIdentifier(TENANT, "unknown");

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should find all organizations for a tenant")
        void findAllShouldReturnAllTenantOrganizations() {
            // Given
            OrganizationEntity entity = createOrganizationEntity();
            when(organizationRepository.findByTenantId(TENANT)).thenReturn(List.of(entity));

            // When
            List<Organization> results = organizationService.findAll(TENANT);

            // Then
            assertThat(results).hasSize(1);
        }
    }

    // ==================== Helper Methods ====================

    private Organization createFhirOrganization() {
        Organization org = new Organization();
        org.setId(ORG_ID.toString());
        org.setActive(true);
        org.setName(ORG_NAME);

        Identifier identifier = org.addIdentifier();
        identifier.setSystem("http://healthdata.com/fhir/identifier/organization");
        identifier.setValue(ORG_IDENTIFIER);

        CodeableConcept type = new CodeableConcept();
        type.addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/organization-type")
                .setCode("prov")
                .setDisplay("Healthcare Provider");
        org.addType(type);

        return org;
    }

    private OrganizationEntity createOrganizationEntity() {
        Organization org = createFhirOrganization();
        String json = JSON_PARSER.encodeResourceToString(org);

        return OrganizationEntity.builder()
                .id(ORG_ID)
                .tenantId(TENANT)
                .resourceType("Organization")
                .resourceJson(json)
                .name(ORG_NAME)
                .identifierValue(ORG_IDENTIFIER)
                .active(true)
                .typeCode("prov")
                .typeDisplay("Healthcare Provider")
                .createdBy(USER)
                .lastModifiedBy(USER)
                .createdAt(Instant.now())
                .lastModifiedAt(Instant.now())
                .version(0)
                .build();
    }
}
