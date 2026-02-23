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

import com.healthdata.fhir.persistence.PractitionerRoleEntity;
import com.healthdata.fhir.persistence.PractitionerRoleRepository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

/**
 * Unit tests for PractitionerRoleService.
 * Tests CRUD, search, FHIR conversions, field extraction, and Kafka events
 * for practitioner-organization role linkage.
 */
@Tag("unit")
@DisplayName("PractitionerRoleService Tests")
class PractitionerRoleServiceTest {

    private static final String TENANT = "tenant-1";
    private static final UUID ROLE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PRACTITIONER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ORG_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final String USER = "admin-user";
    private static final String ROLE_IDENTIFIER = "ROLE-001";

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final IParser JSON_PARSER = FHIR_CONTEXT.newJsonParser().setPrettyPrint(false);

    @Mock
    private PractitionerRoleRepository practitionerRoleRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private PractitionerRoleService practitionerRoleService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        practitionerRoleService = new PractitionerRoleService(practitionerRoleRepository, kafkaTemplate);
    }

    // ==================== Create Operations ====================

    @Nested
    @DisplayName("Create PractitionerRole")
    class CreatePractitionerRole {

        @Test
        @DisplayName("Should persist role and publish Kafka event")
        void createRoleShouldPersistAndPublishEvent() {
            // Given
            PractitionerRole role = createFhirPractitionerRole();
            PractitionerRoleEntity savedEntity = createRoleEntity();

            when(practitionerRoleRepository.save(any(PractitionerRoleEntity.class))).thenReturn(savedEntity);

            // When
            PractitionerRole result = practitionerRoleService.createPractitionerRole(TENANT, role, USER);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(ROLE_ID.toString());

            verify(practitionerRoleRepository).save(any(PractitionerRoleEntity.class));
            verify(kafkaTemplate).send(eq("fhir.practitioner-roles.created"), eq(ROLE_ID.toString()), any());
        }

        @Test
        @DisplayName("Should assign UUID when ID is not present")
        void createRoleShouldAssignIdIfNotPresent() {
            // Given
            PractitionerRole role = new PractitionerRole();
            role.setPractitioner(new Reference("Practitioner/" + PRACTITIONER_ID));
            role.setActive(true);

            PractitionerRoleEntity savedEntity = PractitionerRoleEntity.builder()
                    .id(UUID.randomUUID())
                    .tenantId(TENANT)
                    .resourceType("PractitionerRole")
                    .resourceJson("{}")
                    .practitionerId(PRACTITIONER_ID.toString())
                    .active(true)
                    .createdBy(USER)
                    .lastModifiedBy(USER)
                    .createdAt(Instant.now())
                    .lastModifiedAt(Instant.now())
                    .version(0)
                    .build();

            when(practitionerRoleRepository.save(any(PractitionerRoleEntity.class))).thenReturn(savedEntity);

            // When
            PractitionerRole result = practitionerRoleService.createPractitionerRole(TENANT, role, USER);

            // Then
            assertThat(result.hasId()).isTrue();
            verify(practitionerRoleRepository).save(any(PractitionerRoleEntity.class));
        }

        @Test
        @DisplayName("Should capture all entity fields from FHIR resource")
        void createRoleShouldCaptureEntityFields() {
            // Given
            PractitionerRole role = createFhirPractitionerRole();
            ArgumentCaptor<PractitionerRoleEntity> captor = ArgumentCaptor.forClass(PractitionerRoleEntity.class);

            PractitionerRoleEntity savedEntity = createRoleEntity();
            when(practitionerRoleRepository.save(captor.capture())).thenReturn(savedEntity);

            // When
            practitionerRoleService.createPractitionerRole(TENANT, role, USER);

            // Then
            PractitionerRoleEntity captured = captor.getValue();
            assertThat(captured.getTenantId()).isEqualTo(TENANT);
            assertThat(captured.getResourceType()).isEqualTo("PractitionerRole");
            assertThat(captured.getPractitionerId()).isEqualTo(PRACTITIONER_ID.toString());
            assertThat(captured.getOrganizationId()).isEqualTo(ORG_ID.toString());
            assertThat(captured.getRoleCode()).isEqualTo("doctor");
            assertThat(captured.getRoleDisplay()).isEqualTo("Doctor");
            assertThat(captured.getSpecialtyCode()).isEqualTo("394814009");
            assertThat(captured.getSpecialtyDisplay()).isEqualTo("General practice");
            assertThat(captured.getIdentifierValue()).isEqualTo(ROLE_IDENTIFIER);
            assertThat(captured.getActive()).isTrue();
            assertThat(captured.getResourceJson()).isNotBlank();
            assertThat(captured.getCreatedBy()).isEqualTo(USER);
        }

        @Test
        @DisplayName("Should extract practitioner ID stripping prefix")
        void createRoleShouldStripPractitionerPrefix() {
            // Given — practitioner ref has "Practitioner/" prefix
            PractitionerRole role = createFhirPractitionerRole();
            ArgumentCaptor<PractitionerRoleEntity> captor = ArgumentCaptor.forClass(PractitionerRoleEntity.class);

            when(practitionerRoleRepository.save(captor.capture())).thenReturn(createRoleEntity());

            // When
            practitionerRoleService.createPractitionerRole(TENANT, role, USER);

            // Then — entity stores just the UUID
            assertThat(captor.getValue().getPractitionerId()).isEqualTo(PRACTITIONER_ID.toString());
        }

        @Test
        @DisplayName("Should handle missing optional references gracefully")
        void createRoleShouldHandleMissingOptionalFields() {
            // Given
            PractitionerRole role = new PractitionerRole();
            role.setId(ROLE_ID.toString());
            role.setActive(true);
            // No practitioner, organization, role code, specialty, identifier

            ArgumentCaptor<PractitionerRoleEntity> captor = ArgumentCaptor.forClass(PractitionerRoleEntity.class);

            PractitionerRoleEntity savedEntity = PractitionerRoleEntity.builder()
                    .id(ROLE_ID)
                    .tenantId(TENANT)
                    .resourceType("PractitionerRole")
                    .resourceJson("{}")
                    .active(true)
                    .createdBy(USER)
                    .lastModifiedBy(USER)
                    .createdAt(Instant.now())
                    .lastModifiedAt(Instant.now())
                    .version(0)
                    .build();

            when(practitionerRoleRepository.save(captor.capture())).thenReturn(savedEntity);

            // When
            practitionerRoleService.createPractitionerRole(TENANT, role, USER);

            // Then
            PractitionerRoleEntity captured = captor.getValue();
            assertThat(captured.getPractitionerId()).isNull();
            assertThat(captured.getOrganizationId()).isNull();
            assertThat(captured.getRoleCode()).isNull();
            assertThat(captured.getRoleDisplay()).isNull();
            assertThat(captured.getSpecialtyCode()).isNull();
            assertThat(captured.getSpecialtyDisplay()).isNull();
            assertThat(captured.getIdentifierValue()).isNull();
        }

        @Test
        @DisplayName("Should continue successfully when Kafka publish fails")
        void createRoleShouldIgnorePublishFailure() {
            // Given
            PractitionerRole role = createFhirPractitionerRole();
            PractitionerRoleEntity savedEntity = createRoleEntity();

            when(practitionerRoleRepository.save(any(PractitionerRoleEntity.class))).thenReturn(savedEntity);
            when(kafkaTemplate.send(anyString(), anyString(), any()))
                    .thenThrow(new RuntimeException("Kafka unavailable"));

            // When — should not throw
            PractitionerRole result = practitionerRoleService.createPractitionerRole(TENANT, role, USER);

            // Then
            assertThat(result).isNotNull();
            verify(practitionerRoleRepository).save(any(PractitionerRoleEntity.class));
        }
    }

    // ==================== Read Operations ====================

    @Nested
    @DisplayName("Get PractitionerRole")
    class GetPractitionerRole {

        @Test
        @DisplayName("Should return role when found")
        void getRoleShouldReturnFhirResource() {
            // Given
            PractitionerRoleEntity entity = createRoleEntity();
            when(practitionerRoleRepository.findByTenantIdAndId(TENANT, ROLE_ID))
                    .thenReturn(Optional.of(entity));

            // When
            Optional<PractitionerRole> result = practitionerRoleService.getPractitionerRole(TENANT, ROLE_ID);

            // Then
            assertThat(result).isPresent();
            PractitionerRole role = result.get();
            assertThat(role.getPractitioner().getReference())
                    .isEqualTo("Practitioner/" + PRACTITIONER_ID);
        }

        @Test
        @DisplayName("Should return empty when role not found")
        void getRoleShouldReturnEmptyWhenNotFound() {
            // Given
            when(practitionerRoleRepository.findByTenantIdAndId(TENANT, ROLE_ID))
                    .thenReturn(Optional.empty());

            // When
            Optional<PractitionerRole> result = practitionerRoleService.getPractitionerRole(TENANT, ROLE_ID);

            // Then
            assertThat(result).isEmpty();
        }
    }

    // ==================== Update Operations ====================

    @Nested
    @DisplayName("Update PractitionerRole")
    class UpdatePractitionerRole {

        @Test
        @DisplayName("Should update role and publish event")
        void updateRoleShouldUpdateAndPublishEvent() {
            // Given
            PractitionerRoleEntity existing = createRoleEntity();
            when(practitionerRoleRepository.findByTenantIdAndId(TENANT, ROLE_ID))
                    .thenReturn(Optional.of(existing));
            when(practitionerRoleRepository.save(any(PractitionerRoleEntity.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            PractitionerRole updated = createFhirPractitionerRole();

            // When
            PractitionerRole result = practitionerRoleService.updatePractitionerRole(TENANT, ROLE_ID, updated, USER);

            // Then
            assertThat(result).isNotNull();
            verify(practitionerRoleRepository).save(any(PractitionerRoleEntity.class));
            verify(kafkaTemplate).send(eq("fhir.practitioner-roles.updated"), eq(ROLE_ID.toString()), any());
        }

        @Test
        @DisplayName("Should throw when role not found on update")
        void updateRoleShouldThrowWhenNotFound() {
            // Given
            when(practitionerRoleRepository.findByTenantIdAndId(TENANT, ROLE_ID))
                    .thenReturn(Optional.empty());

            PractitionerRole role = createFhirPractitionerRole();

            // When/Then
            assertThatThrownBy(() ->
                    practitionerRoleService.updatePractitionerRole(TENANT, ROLE_ID, role, USER))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("PractitionerRole not found");
        }
    }

    // ==================== Delete Operations ====================

    @Nested
    @DisplayName("Delete PractitionerRole")
    class DeletePractitionerRole {

        @Test
        @DisplayName("Should delete role and publish event")
        void deleteRoleShouldDeleteAndPublishEvent() {
            // Given
            PractitionerRoleEntity entity = createRoleEntity();
            when(practitionerRoleRepository.findByTenantIdAndId(TENANT, ROLE_ID))
                    .thenReturn(Optional.of(entity));

            // When
            practitionerRoleService.deletePractitionerRole(TENANT, ROLE_ID, USER);

            // Then
            verify(practitionerRoleRepository).delete(entity);
            verify(kafkaTemplate).send(eq("fhir.practitioner-roles.deleted"), eq(ROLE_ID.toString()), any());
        }

        @Test
        @DisplayName("Should throw when role not found on delete")
        void deleteRoleShouldThrowWhenNotFound() {
            // Given
            when(practitionerRoleRepository.findByTenantIdAndId(TENANT, ROLE_ID))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() ->
                    practitionerRoleService.deletePractitionerRole(TENANT, ROLE_ID, USER))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("PractitionerRole not found");
        }
    }

    // ==================== Search Operations ====================

    @Nested
    @DisplayName("Search PractitionerRoles")
    class SearchPractitionerRoles {

        @Test
        @DisplayName("Should return paged results")
        void searchRolesShouldReturnPagedResults() {
            // Given
            PractitionerRoleEntity entity = createRoleEntity();
            PageRequest pageable = PageRequest.of(0, 20);
            Page<PractitionerRoleEntity> page = new PageImpl<>(List.of(entity), pageable, 1);

            when(practitionerRoleRepository.findByTenantId(TENANT, pageable)).thenReturn(page);

            // When
            Page<PractitionerRole> result = practitionerRoleService.searchPractitionerRoles(TENANT, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should find roles by practitioner ID")
        void findByPractitionerShouldReturnMatchingRoles() {
            // Given
            PractitionerRoleEntity entity = createRoleEntity();
            when(practitionerRoleRepository.findByTenantIdAndPractitionerId(TENANT, PRACTITIONER_ID.toString()))
                    .thenReturn(List.of(entity));

            // When
            List<PractitionerRole> results = practitionerRoleService.findByPractitioner(TENANT, PRACTITIONER_ID.toString());

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getPractitioner().getReference())
                    .isEqualTo("Practitioner/" + PRACTITIONER_ID);
        }

        @Test
        @DisplayName("Should find roles by role code")
        void findByRoleCodeShouldReturnMatchingRoles() {
            // Given
            PractitionerRoleEntity entity = createRoleEntity();
            when(practitionerRoleRepository.findByTenantIdAndRoleCode(TENANT, "doctor"))
                    .thenReturn(List.of(entity));

            // When
            List<PractitionerRole> results = practitionerRoleService.findByRoleCode(TENANT, "doctor");

            // Then
            assertThat(results).hasSize(1);
        }

        @Test
        @DisplayName("Should find role by identifier")
        void findByIdentifierShouldReturnMatchingRole() {
            // Given
            PractitionerRoleEntity entity = createRoleEntity();
            when(practitionerRoleRepository.findByTenantIdAndIdentifierValue(TENANT, ROLE_IDENTIFIER))
                    .thenReturn(Optional.of(entity));

            // When
            Optional<PractitionerRole> result = practitionerRoleService.findByIdentifier(TENANT, ROLE_IDENTIFIER);

            // Then
            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("Should find all roles for a tenant")
        void findAllShouldReturnAllTenantRoles() {
            // Given
            PractitionerRoleEntity entity = createRoleEntity();
            when(practitionerRoleRepository.findByTenantId(TENANT)).thenReturn(List.of(entity));

            // When
            List<PractitionerRole> results = practitionerRoleService.findAll(TENANT);

            // Then
            assertThat(results).hasSize(1);
        }
    }

    // ==================== Helper Methods ====================

    private PractitionerRole createFhirPractitionerRole() {
        PractitionerRole role = new PractitionerRole();
        role.setId(ROLE_ID.toString());
        role.setActive(true);

        role.setPractitioner(new Reference("Practitioner/" + PRACTITIONER_ID)
                .setDisplay("Dr. Sarah Chen"));
        role.setOrganization(new Reference("Organization/" + ORG_ID)
                .setDisplay("Main Street Family Practice"));

        CodeableConcept roleCode = new CodeableConcept();
        roleCode.addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/practitioner-role")
                .setCode("doctor")
                .setDisplay("Doctor");
        role.addCode(roleCode);

        CodeableConcept specialty = new CodeableConcept();
        specialty.addCoding()
                .setSystem("http://snomed.info/sct")
                .setCode("394814009")
                .setDisplay("General practice");
        role.addSpecialty(specialty);

        Identifier identifier = role.addIdentifier();
        identifier.setSystem("http://healthdata.com/fhir/identifier/practitioner-role");
        identifier.setValue(ROLE_IDENTIFIER);

        return role;
    }

    private PractitionerRoleEntity createRoleEntity() {
        PractitionerRole role = createFhirPractitionerRole();
        String json = JSON_PARSER.encodeResourceToString(role);

        return PractitionerRoleEntity.builder()
                .id(ROLE_ID)
                .tenantId(TENANT)
                .resourceType("PractitionerRole")
                .resourceJson(json)
                .practitionerId(PRACTITIONER_ID.toString())
                .organizationId(ORG_ID.toString())
                .roleCode("doctor")
                .roleDisplay("Doctor")
                .specialtyCode("394814009")
                .specialtyDisplay("General practice")
                .identifierValue(ROLE_IDENTIFIER)
                .active(true)
                .createdBy(USER)
                .lastModifiedBy(USER)
                .createdAt(Instant.now())
                .lastModifiedAt(Instant.now())
                .version(0)
                .build();
    }
}
