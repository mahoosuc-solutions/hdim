package com.healthdata.cms.service;

import ca.uhn.fhir.context.FhirContext;
import com.healthdata.cms.client.DpcClient;
import com.healthdata.cms.model.CmsCondition;
import com.healthdata.cms.model.CmsMedicationRequest;
import com.healthdata.cms.model.CmsObservation;
import com.healthdata.cms.model.CmsProcedure;
import com.healthdata.cms.repository.CmsConditionRepository;
import com.healthdata.cms.repository.CmsMedicationRequestRepository;
import com.healthdata.cms.repository.CmsObservationRepository;
import com.healthdata.cms.repository.CmsProcedureRepository;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.Quantity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CmsClinicalDataServiceTest {

    @Mock
    private DpcClient dpcClient;

    @Mock
    private CmsConditionRepository conditionRepository;

    @Mock
    private CmsProcedureRepository procedureRepository;

    @Mock
    private CmsMedicationRequestRepository medicationRequestRepository;

    @Mock
    private CmsObservationRepository observationRepository;

    private CmsClinicalDataService service;
    private FhirContext fhirContext;

    @BeforeEach
    void setUp() {
        fhirContext = FhirContext.forR4();
        service = new CmsClinicalDataService(
            dpcClient,
            fhirContext,
            conditionRepository,
            procedureRepository,
            medicationRequestRepository,
            observationRepository
        );

        when(conditionRepository.saveAll(anyList())).thenAnswer(invocation -> new ArrayList<>((List<CmsCondition>) invocation.getArgument(0)));
        when(procedureRepository.saveAll(anyList())).thenAnswer(invocation -> new ArrayList<>((List<CmsProcedure>) invocation.getArgument(0)));
        when(medicationRequestRepository.saveAll(anyList())).thenAnswer(invocation -> new ArrayList<>((List<CmsMedicationRequest>) invocation.getArgument(0)));
        when(observationRepository.saveAll(anyList())).thenAnswer(invocation -> new ArrayList<>((List<CmsObservation>) invocation.getArgument(0)));
    }

    @Test
    void syncAllClinicalDataParsesAndPersistsEachResourceType() {
        UUID tenantId = UUID.randomUUID();
        String patientId = "patient-999";

        when(dpcClient.getConditions(eq(patientId))).thenReturn(bundleJson(conditionResource("cond-1")));
        when(dpcClient.getProcedures(eq(patientId))).thenReturn(bundleJson(procedureResource("proc-1")));
        when(dpcClient.getMedicationRequests(eq(patientId))).thenReturn(bundleJson(medicationResource("med-1")));
        when(dpcClient.getObservations(eq(patientId))).thenReturn(bundleJson(observationResource("obs-1")));

        CmsClinicalDataService.ClinicalDataSyncResult result = service.syncAllClinicalData(patientId, tenantId);

        assertThat(result.getConditionsSynced()).isEqualTo(1);
        assertThat(result.getProceduresSynced()).isEqualTo(1);
        assertThat(result.getMedicationRequestsSynced()).isEqualTo(1);
        assertThat(result.getObservationsSynced()).isEqualTo(1);
        assertThat(result.getTotalSynced()).isEqualTo(4);

        verify(conditionRepository).saveAll(anyList());
        verify(procedureRepository).saveAll(anyList());
        verify(medicationRequestRepository).saveAll(anyList());
        verify(observationRepository).saveAll(anyList());
    }

    private String bundleJson(org.hl7.fhir.r4.model.Resource resource) {
        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(resource);
        return fhirContext.newJsonParser().encodeResourceToString(bundle);
    }

    private Condition conditionResource(String id) {
        Condition condition = new Condition();
        condition.setId(id);
        condition.getCode().addCoding(new Coding("http://hl7.org/fhir/sid/icd-10-cm", "E11.9", "Type 2 diabetes mellitus"));
        condition.getClinicalStatus().addCoding(new Coding().setCode("active"));
        return condition;
    }

    private Procedure procedureResource(String id) {
        Procedure procedure = new Procedure();
        procedure.setId(id);
        procedure.setStatus(Procedure.ProcedureStatus.COMPLETED);
        procedure.getCode().addCoding(new Coding("http://www.ama-assn.org/go/cpt", "99213", "Office visit"));
        return procedure;
    }

    private MedicationRequest medicationResource(String id) {
        MedicationRequest medicationRequest = new MedicationRequest();
        medicationRequest.setId(id);
        medicationRequest.setStatus(MedicationRequest.MedicationRequestStatus.ACTIVE);
        medicationRequest.setIntent(MedicationRequest.MedicationRequestIntent.ORDER);
        medicationRequest.getMedicationCodeableConcept()
            .addCoding(new Coding("http://www.nlm.nih.gov/research/umls/rxnorm", "860975", "Metformin 500 MG Oral Tablet"));
        return medicationRequest;
    }

    private Observation observationResource(String id) {
        Observation observation = new Observation();
        observation.setId(id);
        observation.setStatus(Observation.ObservationStatus.FINAL);
        observation.getCode().addCoding(new Coding("http://loinc.org", "8302-2", "Body height"));
        observation.setValue(new Quantity().setValue(175).setUnit("cm"));
        return observation;
    }
}
