package com.healthdata.fhir.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class EntityLifecycleTest {

    @Test
    void allergyIntoleranceEntityShouldInitializeAuditFields() {
        AllergyIntoleranceEntity entity = new AllergyIntoleranceEntity();

        entity.onCreate();

        assertThat(entity.getId()).isNotNull();
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getLastModifiedAt()).isNotNull();
    }

    @Test
    void allergyIntoleranceEntityShouldUpdateTimestamp() {
        AllergyIntoleranceEntity entity = new AllergyIntoleranceEntity();
        entity.setLastModifiedAt(LocalDateTime.of(2024, 1, 1, 0, 0));

        entity.onUpdate();

        assertThat(entity.getLastModifiedAt()).isNotNull();
    }

    @Test
    void documentReferenceEntityShouldSetDefaultsAndFlags() {
        DocumentReferenceEntity entity = new DocumentReferenceEntity();
        entity.setStatus("current");
        entity.setDocStatus("final");

        entity.onCreate();

        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getIndexedDate()).isNotNull();
        assertThat(entity.getLastModifiedAt()).isNotNull();
        assertThat(entity.isCurrent()).isTrue();
        assertThat(entity.isFinal()).isTrue();
    }

    @Test
    void goalEntityShouldReflectLifecycleStates() {
        GoalEntity entity = new GoalEntity();
        entity.setLifecycleStatus("active");
        entity.setAchievementStatus("achieved");
        entity.setTargetDate(LocalDate.now().minusDays(1));

        entity.onCreate();

        assertThat(entity.isActive()).isTrue();
        assertThat(entity.isAchieved()).isTrue();
        assertThat(entity.isOverdue()).isFalse();
    }

    @Test
    void goalEntityShouldDetectOverdueGoals() {
        GoalEntity entity = new GoalEntity();
        entity.setLifecycleStatus("active");
        entity.setAchievementStatus("in-progress");
        entity.setTargetDate(LocalDate.now().minusDays(2));

        assertThat(entity.isOverdue()).isTrue();
    }

    @Test
    void carePlanEntityShouldEvaluateStatusAndPeriod() {
        CarePlanEntity entity = new CarePlanEntity();
        entity.setStatus("active");
        entity.setPeriodStart(Instant.now().minusSeconds(3600));
        entity.setPeriodEnd(Instant.now().plusSeconds(3600));

        assertThat(entity.isActive()).isTrue();
        assertThat(entity.isCompleted()).isFalse();
        assertThat(entity.isCurrentlyInEffect()).isTrue();
        assertThat(entity.isPrimary()).isTrue();
    }

    @Test
    void carePlanEntityShouldHandleNonPrimaryPlans() {
        CarePlanEntity entity = new CarePlanEntity();
        entity.setStatus("completed");
        entity.setPartOfReference("CarePlan/parent");

        assertThat(entity.isPrimary()).isFalse();
        assertThat(entity.isCurrentlyInEffect()).isFalse();
    }

    @Test
    void immunizationEntityShouldSetDefaults() {
        ImmunizationEntity entity = new ImmunizationEntity();

        entity.onCreate();

        assertThat(entity.getId()).isNotNull();
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getLastModifiedAt()).isNotNull();
    }

    @Test
    void immunizationEntityShouldUpdateTimestamp() {
        ImmunizationEntity entity = new ImmunizationEntity();
        entity.setId(UUID.randomUUID());
        entity.setCreatedAt(LocalDateTime.of(2024, 1, 1, 0, 0));
        entity.setLastModifiedAt(LocalDateTime.of(2024, 1, 1, 0, 0));

        entity.onUpdate();

        assertThat(entity.getLastModifiedAt()).isNotNull();
    }

    @Test
    void conditionEntityShouldInitializeAuditFields() {
        ConditionEntity entity = new ConditionEntity();

        entity.onCreate();

        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getLastModifiedAt()).isNotNull();
        assertThat(entity.getVersion()).isZero();
    }

    @Test
    void medicationRequestEntityShouldInitializeAuditFields() {
        MedicationRequestEntity entity = new MedicationRequestEntity();

        entity.onCreate();

        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getLastModifiedAt()).isNotNull();
        assertThat(entity.getVersion()).isZero();
    }

    @Test
    void medicationAdministrationEntityShouldInitializeAuditFields() {
        MedicationAdministrationEntity entity = new MedicationAdministrationEntity();

        entity.onCreate();

        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getLastModifiedAt()).isNotNull();
        assertThat(entity.getVersion()).isZero();
    }

    @Test
    void observationEntityShouldInitializeAuditFields() {
        ObservationEntity entity = new ObservationEntity();

        entity.onCreate();

        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getLastModifiedAt()).isNotNull();
        assertThat(entity.getVersion()).isZero();
    }

    @Test
    void encounterEntityShouldSetResourceTypeAndAuditFields() {
        EncounterEntity entity = new EncounterEntity();

        entity.onCreate();

        assertThat(entity.getResourceType()).isEqualTo("Encounter");
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getLastModifiedAt()).isNotNull();
        assertThat(entity.getVersion()).isZero();
    }

    @Test
    void procedureEntityShouldSetResourceTypeAndAuditFields() {
        ProcedureEntity entity = new ProcedureEntity();

        entity.onCreate();

        assertThat(entity.getResourceType()).isEqualTo("Procedure");
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getLastModifiedAt()).isNotNull();
        assertThat(entity.getVersion()).isZero();
    }

    @Test
    void entityOnUpdateShouldRefreshTimestamp() {
        ConditionEntity condition = new ConditionEntity();
        MedicationRequestEntity medicationRequest = new MedicationRequestEntity();
        MedicationAdministrationEntity medicationAdmin = new MedicationAdministrationEntity();
        ObservationEntity observation = new ObservationEntity();
        EncounterEntity encounter = new EncounterEntity();
        ProcedureEntity procedure = new ProcedureEntity();

        condition.onUpdate();
        medicationRequest.onUpdate();
        medicationAdmin.onUpdate();
        observation.onUpdate();
        encounter.onUpdate();
        procedure.onUpdate();

        assertThat(condition.getLastModifiedAt()).isNotNull();
        assertThat(medicationRequest.getLastModifiedAt()).isNotNull();
        assertThat(medicationAdmin.getLastModifiedAt()).isNotNull();
        assertThat(observation.getLastModifiedAt()).isNotNull();
        assertThat(encounter.getLastModifiedAt()).isNotNull();
        assertThat(procedure.getLastModifiedAt()).isNotNull();
    }

    @Test
    void coverageEntityShouldEvaluateActivePeriod() {
        CoverageEntity entity = new CoverageEntity();
        entity.setStatus("active");
        entity.setPeriodStart(Instant.now().minusSeconds(3600));
        entity.setPeriodEnd(Instant.now().plusSeconds(3600));

        entity.onCreate();

        assertThat(entity.isCurrentlyActive()).isTrue();
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getLastModifiedAt()).isNotNull();
    }

    @Test
    void coverageEntityShouldHandleInactiveStatus() {
        CoverageEntity entity = new CoverageEntity();
        entity.setStatus("cancelled");

        assertThat(entity.isCurrentlyActive()).isFalse();
    }

    @Test
    void diagnosticReportEntityShouldExposeFlags() {
        DiagnosticReportEntity entity = new DiagnosticReportEntity();
        entity.setStatus("final");
        entity.setCategoryCode("RAD");

        entity.onCreate();

        assertThat(entity.isFinal()).isTrue();
        assertThat(entity.isPreliminary()).isFalse();
        assertThat(entity.isImagingReport()).isTrue();
        assertThat(entity.isLabReport()).isFalse();
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getLastModifiedAt()).isNotNull();
    }

    @Test
    void diagnosticReportEntityShouldHandlePreliminaryLabReports() {
        DiagnosticReportEntity entity = new DiagnosticReportEntity();
        entity.setStatus("preliminary");
        entity.setCategoryCode("lab");

        assertThat(entity.isFinal()).isFalse();
        assertThat(entity.isPreliminary()).isTrue();
        assertThat(entity.isLabReport()).isTrue();
        assertThat(entity.isImagingReport()).isFalse();
    }
}
