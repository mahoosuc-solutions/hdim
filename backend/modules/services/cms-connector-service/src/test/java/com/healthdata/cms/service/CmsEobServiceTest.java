package com.healthdata.cms.service;

import ca.uhn.fhir.context.FhirContext;
import com.healthdata.cms.client.DpcClient;
import com.healthdata.cms.model.CmsClaim;
import com.healthdata.cms.repository.CmsClaimRepository;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CmsEobServiceTest {

    @Mock
    private DpcClient dpcClient;

    @Mock
    private CmsClaimRepository claimRepository;

    private CmsEobService service;
    private FhirContext fhirContext;

    @BeforeEach
    void setUp() {
        fhirContext = FhirContext.forR4();
        service = new CmsEobService(dpcClient, claimRepository, fhirContext);
    }

    @Test
    void fetchEobForPatientParsesFhirBundle() {
        UUID tenantId = UUID.randomUUID();
        String patientId = "patient-123";

        Bundle bundle = new Bundle();
        ExplanationOfBenefit eob = new ExplanationOfBenefit();
        eob.setId("eob-1");
        eob.addTotal()
            .setCategory(new org.hl7.fhir.r4.model.CodeableConcept().addCoding(new Coding().setCode("submitted")))
            .setAmount(new Money().setValue(BigDecimal.valueOf(1200.50)));
        eob.addTotal()
            .setCategory(new org.hl7.fhir.r4.model.CodeableConcept().addCoding(new Coding().setCode("benefit")))
            .setAmount(new Money().setValue(BigDecimal.valueOf(800.25)));
        bundle.addEntry().setResource(eob);

        String bundleJson = fhirContext.newJsonParser().encodeResourceToString(bundle);
        when(dpcClient.getExplanationOfBenefits(eq(patientId))).thenReturn(bundleJson);

        List<CmsClaim> claims = service.fetchEobForPatient(patientId, tenantId);

        assertThat(claims).hasSize(1);
        CmsClaim claim = claims.get(0);
        assertThat(claim.getTenantId()).isEqualTo(tenantId);
        assertThat(claim.getBeneficiaryId()).isEqualTo(patientId);
        assertThat(claim.getClaimId()).isEqualTo("eob-1");
        assertThat(claim.getDataSource()).isEqualTo("DPC");
        assertThat(claim.getTotalChargeAmount()).isEqualTo(1200.50);
        assertThat(claim.getTotalAllowedAmount()).isEqualTo(800.25);
        assertThat(claim.getContentHash()).isNotBlank();
    }

    @Test
    void syncEobForPatientPersistsClaims() {
        UUID tenantId = UUID.randomUUID();
        String patientId = "patient-456";

        Bundle bundle = new Bundle();
        ExplanationOfBenefit eob = new ExplanationOfBenefit();
        eob.setId("eob-2");
        bundle.addEntry().setResource(eob);

        String bundleJson = fhirContext.newJsonParser().encodeResourceToString(bundle);
        when(dpcClient.getExplanationOfBenefits(eq(patientId))).thenReturn(bundleJson);
        when(claimRepository.saveAll(anyList())).thenAnswer(invocation -> new ArrayList<>((List<CmsClaim>) invocation.getArgument(0)));

        int savedCount = service.syncEobForPatient(patientId, tenantId);

        assertThat(savedCount).isEqualTo(1);
        verify(claimRepository).saveAll(anyList());
    }
}
