package com.healthdata.ehr.service;

import com.healthdata.ehr.audit.EhrConnectorAuditIntegration;
import com.healthdata.ehr.connector.EhrConnector;
import com.healthdata.ehr.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test suite for EhrSyncService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EHR Sync Service Tests")
class EhrSyncServiceTest {

    @Mock
    private EhrConnectionManager connectionManager;

    @Mock
    private EhrConnector mockConnector;
    
    @Mock
    private EhrConnectorAuditIntegration auditIntegration;

    private EhrSyncService syncService;

    @BeforeEach
    void setUp() {
        // Use lenient stubbing for audit calls since they may receive null for optional error messages
        // and not all tests trigger all audit methods
        lenient().doNothing().when(auditIntegration).publishEhrDataSyncEvent(
                anyString(), anyString(), anyString(), anyString(), any(), any(),
                anyInt(), anyInt(), anyBoolean(), any(), anyLong(), anyString());
        lenient().doNothing().when(auditIntegration).publishEhrPatientFetchEvent(
                anyString(), anyString(), anyString(), anyString(), anyBoolean(),
                any(), anyLong(), anyString());

        syncService = new EhrSyncService(connectionManager, auditIntegration);
    }

    @Test
    @DisplayName("Should sync patient data successfully")
    void shouldSyncPatientData() {
        // Given
        String connectionId = "conn-1";
        String tenantId = "tenant-1";
        String patientId = "patient-123";
        LocalDateTime startDate = LocalDateTime.now().minusDays(30);
        LocalDateTime endDate = LocalDateTime.now();

        EhrConnector.SyncResult expectedResult = new EhrConnector.SyncResult(
                patientId, 5, 20, startDate, endDate, true, null);

        when(connectionManager.getConnection(connectionId, tenantId)).thenReturn(mockConnector);
        when(mockConnector.syncPatientData(patientId, startDate, endDate, tenantId))
                .thenReturn(Mono.just(expectedResult));

        // When
        Mono<EhrConnector.SyncResult> result = syncService.syncPatientData(
                connectionId, tenantId, patientId, startDate, endDate);

        // Then
        StepVerifier.create(result)
                .assertNext(syncResult -> {
                    assertThat(syncResult.success()).isTrue();
                    assertThat(syncResult.encountersRetrieved()).isEqualTo(5);
                    assertThat(syncResult.observationsRetrieved()).isEqualTo(20);
                })
                .verifyComplete();

        verify(mockConnector).syncPatientData(patientId, startDate, endDate, tenantId);
    }

    @Test
    @DisplayName("Should get patient from EHR")
    void shouldGetPatient() {
        // Given
        String connectionId = "conn-1";
        String tenantId = "tenant-1";
        String patientId = "patient-123";

        EhrPatient expectedPatient = EhrPatient.builder()
                .ehrPatientId(patientId)
                .tenantId(tenantId)
                .familyName("Smith")
                .givenName("John")
                .build();

        when(connectionManager.getConnection(connectionId, tenantId)).thenReturn(mockConnector);
        when(mockConnector.getPatient(patientId, tenantId)).thenReturn(Mono.just(expectedPatient));

        // When
        Mono<EhrPatient> result = syncService.getPatient(connectionId, tenantId, patientId);

        // Then
        StepVerifier.create(result)
                .assertNext(patient -> {
                    assertThat(patient.getEhrPatientId()).isEqualTo(patientId);
                    assertThat(patient.getFamilyName()).isEqualTo("Smith");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should get encounters for patient")
    void shouldGetEncounters() {
        // Given
        String connectionId = "conn-1";
        String tenantId = "tenant-1";
        String patientId = "patient-123";
        LocalDateTime startDate = LocalDateTime.now().minusDays(30);
        LocalDateTime endDate = LocalDateTime.now();

        EhrEncounter encounter1 = EhrEncounter.builder()
                .ehrEncounterId("enc-1")
                .build();
        EhrEncounter encounter2 = EhrEncounter.builder()
                .ehrEncounterId("enc-2")
                .build();

        when(connectionManager.getConnection(connectionId, tenantId)).thenReturn(mockConnector);
        when(mockConnector.getEncounters(patientId, startDate, endDate, tenantId))
                .thenReturn(Flux.just(encounter1, encounter2));

        // When
        Flux<EhrEncounter> result = syncService.getEncounters(
                connectionId, tenantId, patientId, startDate, endDate);

        // Then
        StepVerifier.create(result)
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should get observations for patient")
    void shouldGetObservations() {
        // Given
        String connectionId = "conn-1";
        String tenantId = "tenant-1";
        String patientId = "patient-123";
        String category = "laboratory";
        LocalDateTime startDate = LocalDateTime.now().minusDays(30);
        LocalDateTime endDate = LocalDateTime.now();

        EhrObservation obs1 = EhrObservation.builder()
                .ehrObservationId("obs-1")
                .build();

        when(connectionManager.getConnection(connectionId, tenantId)).thenReturn(mockConnector);
        when(mockConnector.getObservations(patientId, category, startDate, endDate, tenantId))
                .thenReturn(Flux.just(obs1));

        // When
        Flux<EhrObservation> result = syncService.getObservations(
                connectionId, tenantId, patientId, category, startDate, endDate);

        // Then
        StepVerifier.create(result)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle connection not found error")
    void shouldHandleConnectionNotFound() {
        // Given
        when(connectionManager.getConnection(anyString(), anyString())).thenReturn(null);

        // When
        Mono<EhrPatient> result = syncService.getPatient("conn-1", "tenant-1", "patient-123");

        // Then
        StepVerifier.create(result)
                .expectError(IllegalArgumentException.class)
                .verify();
    }
}
