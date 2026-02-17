package com.healthdata.patient.integration;

import com.healthdata.patient.config.BaseIntegrationTest;
import com.healthdata.patient.entity.PatientDemographicsEntity;
import com.healthdata.patient.entity.PatientInsuranceEntity;
import com.healthdata.patient.entity.PatientRiskScoreEntity;
import com.healthdata.patient.repository.PatientDemographicsRepository;
import com.healthdata.patient.repository.PatientInsuranceRepository;
import com.healthdata.patient.repository.PatientRiskScoreRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@BaseIntegrationTest
@DisplayName("Tenant Query Filtering Tests (Patient Service)")
class TenantQueryFilteringTest {

    private static final String TENANT_A = "tenant-alpha";
    private static final String TENANT_B = "tenant-beta";

    @Autowired
    private PatientDemographicsRepository demographicsRepository;

    @Autowired
    private PatientInsuranceRepository insuranceRepository;

    @Autowired
    private PatientRiskScoreRepository riskScoreRepository;

    @Test
    @DisplayName("findByIdAndTenantId should not return cross-tenant demographics records")
    void findByIdAndTenantIdShouldIsolate() {
        PatientDemographicsEntity a = demographicsRepository.save(demoPatient(TENANT_A, "fhir-1", "Alice", "Anderson", true, "pcp-1"));
        demographicsRepository.save(demoPatient(TENANT_B, "fhir-2", "Bob", "Brown", true, "pcp-1"));

        assertThat(demographicsRepository.findByIdAndTenantId(a.getId(), TENANT_A)).isPresent();
        assertThat(demographicsRepository.findByIdAndTenantId(a.getId(), TENANT_B)).isEmpty();
    }

    @Test
    @DisplayName("findByFhirPatientIdAndTenantId should return tenant-scoped patient only")
    void findByFhirPatientIdAndTenantIdShouldIsolate() {
        demographicsRepository.save(demoPatient(TENANT_A, "shared-fhir-id", "Alice", "Anderson", true, "pcp-1"));
        demographicsRepository.save(demoPatient(TENANT_B, "shared-fhir-id", "Bob", "Brown", true, "pcp-1"));

        assertThat(demographicsRepository.findByFhirPatientIdAndTenantId("shared-fhir-id", TENANT_A))
            .isPresent()
            .get()
            .extracting(PatientDemographicsEntity::getTenantId)
            .isEqualTo(TENANT_A);

        assertThat(demographicsRepository.findByFhirPatientIdAndTenantId("shared-fhir-id", TENANT_B))
            .isPresent()
            .get()
            .extracting(PatientDemographicsEntity::getTenantId)
            .isEqualTo(TENANT_B);
    }

    @Test
    @DisplayName("findActiveByTenantId should return only active patients for the requested tenant")
    void findActiveByTenantIdShouldIsolateAndFilterActive() {
        demographicsRepository.save(demoPatient(TENANT_A, "fhir-a1", "Alice", "Anderson", true, "pcp-1"));
        demographicsRepository.save(demoPatient(TENANT_A, "fhir-a2", "Amy", "Allen", false, "pcp-1"));
        demographicsRepository.save(demoPatient(TENANT_B, "fhir-b1", "Bob", "Brown", true, "pcp-1"));

        List<PatientDemographicsEntity> tenantAActive = demographicsRepository.findActiveByTenantId(TENANT_A);
        assertThat(tenantAActive).allMatch(p -> TENANT_A.equals(p.getTenantId()));
        assertThat(tenantAActive).allMatch(p -> Boolean.TRUE.equals(p.getActive()));
        assertThat(tenantAActive).hasSize(1);
    }

    @Test
    @DisplayName("findByPcpIdAndTenantId should not return cross-tenant results for same PCP ID")
    void findByPcpIdAndTenantIdShouldIsolate() {
        demographicsRepository.save(demoPatient(TENANT_A, "fhir-a1", "Alice", "Anderson", true, "pcp-shared"));
        demographicsRepository.save(demoPatient(TENANT_B, "fhir-b1", "Bob", "Brown", true, "pcp-shared"));

        List<PatientDemographicsEntity> tenantA = demographicsRepository.findByPcpIdAndTenantId("pcp-shared", TENANT_A);
        assertThat(tenantA).hasSize(1);
        assertThat(tenantA.get(0).getTenantId()).isEqualTo(TENANT_A);
    }

    @Test
    @DisplayName("findByTenantIdAndActiveTrue (paged) should return only the tenant's active patients")
    void pagedFindByTenantIdAndActiveTrueShouldIsolate() {
        demographicsRepository.save(demoPatient(TENANT_A, "fhir-a1", "Alice", "Anderson", true, "pcp-1"));
        demographicsRepository.save(demoPatient(TENANT_A, "fhir-a2", "Amy", "Allen", false, "pcp-1"));
        demographicsRepository.save(demoPatient(TENANT_B, "fhir-b1", "Bob", "Brown", true, "pcp-1"));

        var page = demographicsRepository.findByTenantIdAndActiveTrue(TENANT_A, PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getTenantId()).isEqualTo(TENANT_A);
        assertThat(page.getContent().get(0).getActive()).isTrue();
    }

    @Test
    @DisplayName("PatientInsuranceRepository tenant-scoped finders should not return cross-tenant records")
    void insuranceQueriesShouldBeTenantScoped() {
        PatientDemographicsEntity a = demographicsRepository.save(demoPatient(TENANT_A, "fhir-a1", "Alice", "Anderson", true, "pcp-1"));
        PatientDemographicsEntity b = demographicsRepository.save(demoPatient(TENANT_B, "fhir-b1", "Bob", "Brown", true, "pcp-1"));

        PatientInsuranceEntity insuranceA = insuranceRepository.save(demoInsurance(TENANT_A, a.getId(), "A-MEMBER"));
        insuranceRepository.save(demoInsurance(TENANT_B, b.getId(), "B-MEMBER"));

        assertThat(insuranceRepository.findByIdAndTenantId(insuranceA.getId(), TENANT_A)).isPresent();
        assertThat(insuranceRepository.findByIdAndTenantId(insuranceA.getId(), TENANT_B)).isEmpty();

        assertThat(insuranceRepository.findByPatientIdAndTenantId(a.getId(), TENANT_A)).hasSize(1);
        assertThat(insuranceRepository.findByPatientIdAndTenantId(a.getId(), TENANT_B)).isEmpty();
    }

    @Test
    @DisplayName("PatientRiskScoreRepository tenant-scoped finders should not return cross-tenant records")
    void riskScoreQueriesShouldBeTenantScoped() {
        PatientDemographicsEntity a = demographicsRepository.save(demoPatient(TENANT_A, "fhir-a1", "Alice", "Anderson", true, "pcp-1"));
        PatientDemographicsEntity b = demographicsRepository.save(demoPatient(TENANT_B, "fhir-b1", "Bob", "Brown", true, "pcp-1"));

        riskScoreRepository.save(demoRiskScore(TENANT_A, a.getId(), new BigDecimal("1.5")));
        PatientRiskScoreEntity scoreB = riskScoreRepository.save(demoRiskScore(TENANT_B, b.getId(), new BigDecimal("0.8")));

        assertThat(riskScoreRepository.findByIdAndTenantId(scoreB.getId(), TENANT_B)).isPresent();
        assertThat(riskScoreRepository.findByIdAndTenantId(scoreB.getId(), TENANT_A)).isEmpty();

        assertThat(riskScoreRepository.findByPatientIdAndTenantId(a.getId(), TENANT_A)).hasSize(1);
        assertThat(riskScoreRepository.findByPatientIdAndTenantId(a.getId(), TENANT_B)).isEmpty();
    }

    private static PatientDemographicsEntity demoPatient(
        String tenantId,
        String fhirPatientId,
        String firstName,
        String lastName,
        boolean active,
        String pcpId
    ) {
        return PatientDemographicsEntity.builder()
            .tenantId(tenantId)
            .fhirPatientId(fhirPatientId)
            .firstName(firstName)
            .lastName(lastName)
            .dateOfBirth(LocalDate.of(1985, 5, 15))
            .gender("female")
            .pcpId(pcpId)
            .active(active)
            .deceased(false)
            .build();
    }

    private static PatientInsuranceEntity demoInsurance(String tenantId, UUID patientId, String memberId) {
        return PatientInsuranceEntity.builder()
            .tenantId(tenantId)
            .patientId(patientId)
            .coverageType("medical")
            .payerName("Demo Payer")
            .memberId(memberId)
            .effectiveDate(LocalDate.of(2024, 1, 1))
            .isPrimary(true)
            .active(true)
            .build();
    }

    private static PatientRiskScoreEntity demoRiskScore(String tenantId, UUID patientId, BigDecimal scoreValue) {
        return PatientRiskScoreEntity.builder()
            .tenantId(tenantId)
            .patientId(patientId)
            .scoreType("hcc")
            .scoreValue(scoreValue)
            .riskCategory("medium")
            .calculationDate(Instant.now())
            .modelVersion("test")
            .build();
    }
}

