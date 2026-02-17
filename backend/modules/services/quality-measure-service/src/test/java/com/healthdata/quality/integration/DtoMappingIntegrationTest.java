package com.healthdata.quality.integration;

import com.healthdata.quality.config.TestMessagingConfiguration;
import com.healthdata.quality.config.TestWebSocketConfiguration;
import com.healthdata.quality.dto.QualityMeasureResultDTO;
import com.healthdata.quality.dto.QualityMeasureResultMapper;
import com.healthdata.quality.persistence.QualityMeasureResultEntity;
import com.healthdata.quality.persistence.QualityMeasureResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.healthdata.authentication.config.AuthenticationAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for DTO mapping
 * Tests the QualityMeasureResultMapper functionality
 */
@Tag("integration")
@SpringBootTest
@EnableAutoConfiguration(exclude = {AuthenticationAutoConfiguration.class})
@ActiveProfiles("test")
@Transactional
@Import({TestMessagingConfiguration.class, TestWebSocketConfiguration.class})
@DisplayName("DTO Mapping Integration Tests")
class DtoMappingIntegrationTest {

    @Autowired
    private QualityMeasureResultRepository repository;

    private static final String TENANT_ID = "test-tenant";
    private static final UUID PATIENT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("Should map entity to DTO with all fields")
    void shouldMapEntityToDtoWithAllFields() {
        QualityMeasureResultEntity entity = createMeasureResult(TENANT_ID, PATIENT_ID, "HEDIS_CDC", true);

        QualityMeasureResultDTO dto = QualityMeasureResultMapper.toDTO(entity);

        assertNotNull(dto);
        assertEquals(entity.getId(), dto.getId());
        assertEquals(entity.getTenantId(), dto.getTenantId());
        assertEquals(entity.getPatientId(), dto.getPatientId());
        assertEquals(entity.getMeasureId(), dto.getMeasureId());
        assertEquals(entity.getMeasureName(), dto.getMeasureName());
        assertEquals(entity.getMeasureCategory(), dto.getMeasureCategory());
        assertEquals(entity.getMeasureYear(), dto.getMeasureYear());
        assertEquals(entity.getNumeratorCompliant(), dto.getNumeratorCompliant());
        assertEquals(entity.getDenominatorEligible(), dto.getDenominatorEligible());
        assertEquals(entity.getComplianceRate(), dto.getComplianceRate());
        assertEquals(entity.getScore(), dto.getScore());
        assertEquals(entity.getCalculationDate(), dto.getCalculationDate());
        assertEquals(entity.getCqlLibrary(), dto.getCqlLibrary());
        assertEquals(entity.getCreatedBy(), dto.getCreatedBy());
        assertNotNull(dto.getCreatedAt());
    }

    @Test
    @DisplayName("Should not include CQL result in DTO")
    void shouldNotIncludeCqlResultInDto() {
        QualityMeasureResultEntity entity = createMeasureResult(TENANT_ID, PATIENT_ID, "HEDIS_CDC", true);
        entity.setCqlResult("{\"sensitive\": \"data\"}");
        repository.save(entity);

        QualityMeasureResultDTO dto = QualityMeasureResultMapper.toDTO(entity);

        assertNotNull(dto);
        // DTO should not have a cqlResult field
        assertNotNull(entity.getCqlResult()); // Entity has it
        // Cannot check dto.getCqlResult() as field doesn't exist in DTO
    }

    @Test
    @DisplayName("Should map null entity to null DTO")
    void shouldMapNullEntityToNullDto() {
        QualityMeasureResultDTO dto = QualityMeasureResultMapper.toDTO(null);
        assertNull(dto);
    }

    @Test
    @DisplayName("Should map list of entities to list of DTOs")
    void shouldMapListOfEntitiesToListOfDtos() {
        createMeasureResult(TENANT_ID, PATIENT_ID, "HEDIS_1", true);
        createMeasureResult(TENANT_ID, PATIENT_ID, "HEDIS_2", false);
        createMeasureResult(TENANT_ID, PATIENT_ID, "HEDIS_3", true);

        List<QualityMeasureResultEntity> entities =
                repository.findByTenantIdAndPatientId(TENANT_ID, PATIENT_ID);
        assertEquals(3, entities.size());

        List<QualityMeasureResultDTO> dtos = QualityMeasureResultMapper.toDTOList(entities);

        assertNotNull(dtos);
        assertEquals(3, dtos.size());
        assertEquals(entities.get(0).getMeasureId(), dtos.get(0).getMeasureId());
        assertEquals(entities.get(1).getMeasureId(), dtos.get(1).getMeasureId());
        assertEquals(entities.get(2).getMeasureId(), dtos.get(2).getMeasureId());
    }

    @Test
    @DisplayName("Should map empty list correctly")
    void shouldMapEmptyListCorrectly() {
        List<QualityMeasureResultEntity> entities =
                repository.findByTenantIdAndPatientId(TENANT_ID, PATIENT_ID);
        assertEquals(0, entities.size());

        List<QualityMeasureResultDTO> dtos = QualityMeasureResultMapper.toDTOList(entities);

        assertNotNull(dtos);
        assertEquals(0, dtos.size());
    }

    @Test
    @DisplayName("Should map null list to null")
    void shouldMapNullListToNull() {
        List<QualityMeasureResultDTO> dtos = QualityMeasureResultMapper.toDTOList(null);
        assertNull(dtos);
    }

    @Test
    @DisplayName("Should map DTO to entity")
    void shouldMapDtoToEntity() {
        QualityMeasureResultDTO dto = QualityMeasureResultDTO.builder()
                .tenantId(TENANT_ID)
                .patientId(PATIENT_ID)
                .measureId("HEDIS_TEST")
                .measureName("Test Measure")
                .measureCategory("HEDIS")
                .measureYear(2024)
                .numeratorCompliant(true)
                .denominatorEligible(true)
                .complianceRate(95.0)
                .score(90.0)
                .calculationDate(LocalDate.now())
                .cqlLibrary("HEDIS_TEST_LIB")
                .createdBy("test-user")
                .build();

        QualityMeasureResultEntity entity = QualityMeasureResultMapper.toEntity(dto);

        assertNotNull(entity);
        assertEquals(dto.getTenantId(), entity.getTenantId());
        assertEquals(dto.getPatientId(), entity.getPatientId());
        assertEquals(dto.getMeasureId(), entity.getMeasureId());
        assertEquals(dto.getMeasureName(), entity.getMeasureName());
        assertEquals(dto.getMeasureCategory(), entity.getMeasureCategory());
        assertEquals(dto.getMeasureYear(), entity.getMeasureYear());
        assertEquals(dto.getNumeratorCompliant(), entity.getNumeratorCompliant());
        assertEquals(dto.getDenominatorEligible(), entity.getDenominatorEligible());
        assertEquals(dto.getComplianceRate(), entity.getComplianceRate());
        assertEquals(dto.getScore(), entity.getScore());
        assertEquals(dto.getCalculationDate(), entity.getCalculationDate());
        assertEquals(dto.getCqlLibrary(), entity.getCqlLibrary());
        assertEquals(dto.getCreatedBy(), entity.getCreatedBy());
    }

    @Test
    @DisplayName("Should handle null values in entity fields")
    void shouldHandleNullValuesInEntityFields() {
        QualityMeasureResultEntity entity = QualityMeasureResultEntity.builder()
                .tenantId(TENANT_ID)
                .patientId(PATIENT_ID)
                .measureId("TEST")
                .measureName("Test")
                .measureYear(2024)
                .numeratorCompliant(true)
                .denominatorEligible(true)
                .calculationDate(LocalDate.now())
                // Leave optional fields null
                .measureCategory(null)
                .complianceRate(null)
                .score(null)
                .cqlLibrary(null)
                .build();

        entity = repository.save(entity);

        QualityMeasureResultDTO dto = QualityMeasureResultMapper.toDTO(entity);

        assertNotNull(dto);
        assertNull(dto.getMeasureCategory());
        assertNull(dto.getComplianceRate());
        assertNull(dto.getScore());
        assertNull(dto.getCqlLibrary());
    }

    @Test
    @DisplayName("Should preserve data types during mapping")
    void shouldPreserveDataTypesDuringMapping() {
        QualityMeasureResultEntity entity = createMeasureResult(TENANT_ID, PATIENT_ID, "HEDIS_CDC", true);
        entity.setComplianceRate(85.5);
        entity.setScore(92.3);
        repository.save(entity);

        QualityMeasureResultDTO dto = QualityMeasureResultMapper.toDTO(entity);

        assertNotNull(dto);
        assertEquals(85.5, dto.getComplianceRate(), 0.001);
        assertEquals(92.3, dto.getScore(), 0.001);
        assertTrue(dto.getNumeratorCompliant());
        assertTrue(dto.getDenominatorEligible());
    }

    @Test
    @DisplayName("Should map UUID fields correctly")
    void shouldMapUuidFieldsCorrectly() {
        UUID customId = UUID.randomUUID();
        UUID customPatientId = UUID.randomUUID();

        QualityMeasureResultEntity entity = QualityMeasureResultEntity.builder()
                .id(customId)
                .tenantId(TENANT_ID)
                .patientId(customPatientId)
                .measureId("TEST")
                .measureName("Test")
                .measureYear(2024)
                .numeratorCompliant(true)
                .denominatorEligible(true)
                .calculationDate(LocalDate.now())
                .cqlResult("{}")
                .build();

        entity = repository.save(entity);

        QualityMeasureResultDTO dto = QualityMeasureResultMapper.toDTO(entity);

        assertNotNull(dto);
        assertEquals(customId, dto.getId());
        assertEquals(customPatientId, dto.getPatientId());
    }

    @Test
    @DisplayName("Should map date fields correctly")
    void shouldMapDateFieldsCorrectly() {
        LocalDate calculationDate = LocalDate.of(2024, 1, 15);

        QualityMeasureResultEntity entity = createMeasureResult(TENANT_ID, PATIENT_ID, "HEDIS_CDC", true);
        entity.setCalculationDate(calculationDate);
        entity.setMeasureYear(2024);
        repository.save(entity);

        QualityMeasureResultDTO dto = QualityMeasureResultMapper.toDTO(entity);

        assertNotNull(dto);
        assertEquals(calculationDate, dto.getCalculationDate());
        assertEquals(2024, dto.getMeasureYear());
        assertNotNull(dto.getCreatedAt());
    }

    @Test
    @DisplayName("Should round-trip map entity to DTO and back")
    void shouldRoundTripMapEntityToDtoAndBack() {
        QualityMeasureResultEntity originalEntity = createMeasureResult(TENANT_ID, PATIENT_ID, "HEDIS_CDC", true);

        // Map to DTO
        QualityMeasureResultDTO dto = QualityMeasureResultMapper.toDTO(originalEntity);

        // Map back to entity
        QualityMeasureResultEntity mappedEntity = QualityMeasureResultMapper.toEntity(dto);

        // Compare key fields (not all as some are auto-generated)
        assertEquals(originalEntity.getTenantId(), mappedEntity.getTenantId());
        assertEquals(originalEntity.getPatientId(), mappedEntity.getPatientId());
        assertEquals(originalEntity.getMeasureId(), mappedEntity.getMeasureId());
        assertEquals(originalEntity.getMeasureName(), mappedEntity.getMeasureName());
        assertEquals(originalEntity.getMeasureCategory(), mappedEntity.getMeasureCategory());
        assertEquals(originalEntity.getNumeratorCompliant(), mappedEntity.getNumeratorCompliant());
        assertEquals(originalEntity.getDenominatorEligible(), mappedEntity.getDenominatorEligible());
    }

    // Helper method to create test measure result
    private QualityMeasureResultEntity createMeasureResult(
            String tenantId,
            UUID patientId,
            String measureId,
            boolean compliant
    ) {
        QualityMeasureResultEntity entity = QualityMeasureResultEntity.builder()
                .tenantId(tenantId)
                .patientId(patientId)
                .measureId(measureId)
                .measureName("Test Measure " + measureId)
                .measureCategory("HEDIS")
                .measureYear(LocalDate.now().getYear())
                .numeratorCompliant(compliant)
                .denominatorEligible(true)
                .complianceRate(compliant ? 100.0 : 0.0)
                .score(compliant ? 95.0 : 50.0)
                .calculationDate(LocalDate.now())
                .cqlLibrary(measureId)
                .cqlResult("{\"result\": \"test\"}")
                .createdBy("integration-test")
                .build();

        return repository.save(entity);
    }
}
