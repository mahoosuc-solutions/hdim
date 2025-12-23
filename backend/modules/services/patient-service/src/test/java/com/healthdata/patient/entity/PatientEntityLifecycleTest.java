package com.healthdata.patient.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Patient Entity Lifecycle Tests")
class PatientEntityLifecycleTest {

    @Test
    @DisplayName("Should initialize demographics timestamps and id on create")
    void shouldInitializeDemographicsOnCreate() {
        PatientDemographicsEntity entity = PatientDemographicsEntity.builder()
                .tenantId("tenant-1")
                .fhirPatientId("fhir-1")
                .firstName("Taylor")
                .lastName("Jordan")
                .dateOfBirth(LocalDate.of(1980, 1, 1))
                .gender("female")
                .build();

        ReflectionTestUtils.invokeMethod(entity, "onCreate");

        assertThat(entity.getId()).isNotNull();
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isNotNull();
        assertThat(entity.getActive()).isTrue();
        assertThat(entity.getDeceased()).isFalse();
    }

    @Test
    @DisplayName("Should update demographics timestamp on update")
    void shouldUpdateDemographicsOnUpdate() {
        PatientDemographicsEntity entity = new PatientDemographicsEntity();
        Instant before = Instant.now().minusSeconds(60);
        entity.setUpdatedAt(before);

        ReflectionTestUtils.invokeMethod(entity, "onUpdate");

        assertThat(entity.getUpdatedAt()).isAfter(before);
    }

    @Test
    @DisplayName("Should initialize insurance timestamps and id on create")
    void shouldInitializeInsuranceOnCreate() {
        PatientInsuranceEntity entity = PatientInsuranceEntity.builder()
                .tenantId("tenant-1")
                .patientId(UUID.randomUUID())
                .coverageType("commercial")
                .payerName("Acme Health")
                .memberId("member-1")
                .effectiveDate(LocalDate.now().minusDays(30))
                .build();

        ReflectionTestUtils.invokeMethod(entity, "onCreate");

        assertThat(entity.getId()).isNotNull();
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isNotNull();
        assertThat(entity.getIsPrimary()).isFalse();
        assertThat(entity.getActive()).isTrue();
    }

    @Test
    @DisplayName("Should update insurance timestamp on update")
    void shouldUpdateInsuranceOnUpdate() {
        PatientInsuranceEntity entity = new PatientInsuranceEntity();
        Instant before = Instant.now().minusSeconds(60);
        entity.setUpdatedAt(before);

        ReflectionTestUtils.invokeMethod(entity, "onUpdate");

        assertThat(entity.getUpdatedAt()).isAfter(before);
    }

    @Test
    @DisplayName("Should initialize risk score defaults on create")
    void shouldInitializeRiskScoreOnCreate() {
        PatientRiskScoreEntity entity = PatientRiskScoreEntity.builder()
                .tenantId("tenant-1")
                .patientId(UUID.randomUUID())
                .scoreType("hcc")
                .scoreValue(new BigDecimal("1.25"))
                .riskCategory("high")
                .build();

        ReflectionTestUtils.invokeMethod(entity, "onCreate");

        assertThat(entity.getId()).isNotNull();
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getCalculationDate()).isNotNull();
    }

    @Test
    @DisplayName("Should preserve risk score timestamps when set")
    void shouldPreserveRiskScoreDefaults() {
        Instant createdAt = Instant.now().minusSeconds(120);
        Instant calculationDate = Instant.now().minusSeconds(90);
        PatientRiskScoreEntity entity = PatientRiskScoreEntity.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant-1")
                .patientId(UUID.randomUUID())
                .scoreType("hcc")
                .scoreValue(new BigDecimal("0.75"))
                .riskCategory("low")
                .createdAt(createdAt)
                .calculationDate(calculationDate)
                .build();

        ReflectionTestUtils.invokeMethod(entity, "onCreate");

        assertThat(entity.getId()).isNotNull();
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(entity.getCalculationDate()).isEqualTo(calculationDate);
    }
}
