package com.healthdata.quality.event;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.healthdata.quality.persistence.QualityMeasureResultEntity;
import com.healthdata.quality.persistence.QualityMeasureResultRepository;
import com.healthdata.quality.service.CareGapDetectionService;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("MeasureResultEventConsumer Tests")
class MeasureResultEventConsumerTest {

    @Test
    @DisplayName("Should analyze care gaps when tenant matches")
    void shouldAnalyzeCareGapsWhenTenantMatches() {
        CareGapDetectionService detectionService = Mockito.mock(CareGapDetectionService.class);
        QualityMeasureResultRepository repository = Mockito.mock(QualityMeasureResultRepository.class);
        MeasureResultEventConsumer consumer = new MeasureResultEventConsumer(detectionService, repository);

        UUID resultId = UUID.randomUUID();
        QualityMeasureResultEntity result = QualityMeasureResultEntity.builder()
            .id(resultId)
            .tenantId("tenant-1")
            .patientId(UUID.randomUUID())
            .measureId("m-1")
            .measureName("Measure")
            .numeratorCompliant(true)
            .denominatorElligible(true)
            .calculationDate(LocalDate.now())
            .createdBy("system")
            .build();

        when(repository.findById(resultId)).thenReturn(Optional.of(result));

        MeasureResultEventConsumer.MeasureCalculatedEvent event =
            new MeasureResultEventConsumer.MeasureCalculatedEvent(
                resultId,
                "tenant-1",
                result.getPatientId(),
                result.getMeasureId(),
                result.getMeasureName(),
                result.getNumeratorCompliant(),
                result.getDenominatorElligible()
            );

        consumer.handleMeasureCalculatedEvent(event, "topic", 0, 1L);

        verify(detectionService).analyzeAndCreateCareGaps(result);
    }

    @Test
    @DisplayName("Should skip when tenant mismatch")
    void shouldSkipWhenTenantMismatch() {
        CareGapDetectionService detectionService = Mockito.mock(CareGapDetectionService.class);
        QualityMeasureResultRepository repository = Mockito.mock(QualityMeasureResultRepository.class);
        MeasureResultEventConsumer consumer = new MeasureResultEventConsumer(detectionService, repository);

        UUID resultId = UUID.randomUUID();
        QualityMeasureResultEntity result = QualityMeasureResultEntity.builder()
            .id(resultId)
            .tenantId("tenant-2")
            .patientId(UUID.randomUUID())
            .measureId("m-1")
            .measureName("Measure")
            .numeratorCompliant(true)
            .denominatorElligible(true)
            .calculationDate(LocalDate.now())
            .createdBy("system")
            .build();

        when(repository.findById(resultId)).thenReturn(Optional.of(result));

        MeasureResultEventConsumer.MeasureCalculatedEvent event =
            new MeasureResultEventConsumer.MeasureCalculatedEvent(
                resultId,
                "tenant-1",
                result.getPatientId(),
                result.getMeasureId(),
                result.getMeasureName(),
                result.getNumeratorCompliant(),
                result.getDenominatorElligible()
            );

        consumer.handleMeasureCalculatedEvent(event, "topic", 0, 1L);

        verify(detectionService, never()).analyzeAndCreateCareGaps(any());
    }

    @Test
    @DisplayName("Should swallow missing measure result errors")
    void shouldSwallowMissingMeasureResultErrors() {
        CareGapDetectionService detectionService = Mockito.mock(CareGapDetectionService.class);
        QualityMeasureResultRepository repository = Mockito.mock(QualityMeasureResultRepository.class);
        MeasureResultEventConsumer consumer = new MeasureResultEventConsumer(detectionService, repository);

        UUID resultId = UUID.randomUUID();
        when(repository.findById(resultId)).thenReturn(Optional.empty());

        MeasureResultEventConsumer.MeasureCalculatedEvent event =
            new MeasureResultEventConsumer.MeasureCalculatedEvent(
                resultId,
                "tenant-1",
                UUID.randomUUID(),
                "m-1",
                "Measure",
                true,
                true
            );

        assertThatCode(() -> consumer.handleMeasureCalculatedEvent(event, "topic", 0, 1L))
            .doesNotThrowAnyException();
        verify(detectionService, never()).analyzeAndCreateCareGaps(any());
    }

    @Test
    @DisplayName("Should rethrow unexpected errors during processing")
    void shouldRethrowUnexpectedErrorsDuringProcessing() {
        CareGapDetectionService detectionService = Mockito.mock(CareGapDetectionService.class);
        QualityMeasureResultRepository repository = Mockito.mock(QualityMeasureResultRepository.class);
        MeasureResultEventConsumer consumer = new MeasureResultEventConsumer(detectionService, repository);

        UUID resultId = UUID.randomUUID();
        QualityMeasureResultEntity result = QualityMeasureResultEntity.builder()
            .id(resultId)
            .tenantId("tenant-1")
            .patientId(UUID.randomUUID())
            .measureId("m-1")
            .measureName("Measure")
            .numeratorCompliant(true)
            .denominatorElligible(true)
            .calculationDate(LocalDate.now())
            .createdBy("system")
            .build();

        when(repository.findById(resultId)).thenReturn(Optional.of(result));
        Mockito.doThrow(new RuntimeException("boom"))
            .when(detectionService).analyzeAndCreateCareGaps(result);

        MeasureResultEventConsumer.MeasureCalculatedEvent event =
            new MeasureResultEventConsumer.MeasureCalculatedEvent(
                resultId,
                "tenant-1",
                result.getPatientId(),
                result.getMeasureId(),
                result.getMeasureName(),
                result.getNumeratorCompliant(),
                result.getDenominatorElligible()
            );

        assertThatCode(() -> consumer.handleMeasureCalculatedEvent(event, "topic", 0, 1L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("boom");
    }
}
