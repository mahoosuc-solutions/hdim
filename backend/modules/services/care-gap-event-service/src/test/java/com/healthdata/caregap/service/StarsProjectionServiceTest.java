package com.healthdata.caregap.service;

import com.healthdata.caregap.api.v1.dto.SimulatedGapClosureRequest;
import com.healthdata.caregap.api.v1.dto.StarRatingResponse;
import com.healthdata.caregap.api.v1.dto.StarRatingSimulationRequest;
import com.healthdata.caregap.api.v1.dto.StarRatingTrendResponse;
import com.healthdata.caregap.persistence.CareGapProjectionRepository;
import com.healthdata.caregap.persistence.StarRatingProjectionRepository;
import com.healthdata.caregap.persistence.StarRatingSnapshotRepository;
import com.healthdata.caregap.projection.CareGapProjection;
import com.healthdata.caregap.projection.StarRatingSnapshot;
import com.healthdata.caregap.projection.StarRatingProjection;
import com.healthdata.starrating.service.StarRatingCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StarsProjectionServiceTest {

    private static final String TENANT_ID = "tenant-stars";

    @Mock
    private CareGapProjectionRepository careGapProjectionRepository;

    @Mock
    private StarRatingProjectionRepository starRatingProjectionRepository;

    @Mock
    private StarRatingSnapshotRepository starRatingSnapshotRepository;

    @InjectMocks
    private StarsProjectionService starsProjectionService;

    @BeforeEach
    void setUp() {
        starsProjectionService = new StarsProjectionService(
            careGapProjectionRepository,
            starRatingProjectionRepository,
            starRatingSnapshotRepository,
            new StarRatingCalculator()
        );
    }

    @Test
    void recalculateCurrentProjection_persistsComputedSummary() {
        when(careGapProjectionRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(
            gap("patient-1", "COL", "CLOSED"),
            gap("patient-2", "COL", "OPEN"),
            gap("patient-3", "CBP", "CLOSED")
        ));
        when(starRatingProjectionRepository.findById(TENANT_ID)).thenReturn(Optional.empty());

        StarRatingResponse response = starsProjectionService.recalculateCurrentProjection(TENANT_ID, "gap.closed:COL");

        assertThat(response.getMeasureCount()).isEqualTo(2);
        assertThat(response.getClosedGapCount()).isEqualTo(2);
        assertThat(response.getOpenGapCount()).isEqualTo(1);
        assertThat(response.getMeasures()).extracting("measureCode").containsExactly("CBP", "COL");

        ArgumentCaptor<StarRatingProjection> captor = ArgumentCaptor.forClass(StarRatingProjection.class);
        verify(starRatingProjectionRepository).save(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(TENANT_ID);
        assertThat(captor.getValue().getLastTriggerEvent()).isEqualTo("gap.closed:COL");
        assertThat(captor.getValue().getMeasureCount()).isEqualTo(2);
    }

    @Test
    void simulate_appliesRequestedClosuresWithoutPersisting() {
        when(careGapProjectionRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(
            gap("patient-1", "COL", "OPEN"),
            gap("patient-2", "COL", "OPEN"),
            gap("patient-3", "CBP", "CLOSED")
        ));

        SimulatedGapClosureRequest closure = new SimulatedGapClosureRequest();
        closure.setGapCode("COL");
        closure.setClosures(1);

        StarRatingSimulationRequest request = new StarRatingSimulationRequest();
        request.setClosures(List.of(closure));

        StarRatingResponse response = starsProjectionService.simulate(TENANT_ID, request);

        assertThat(response.getClosedGapCount()).isEqualTo(2);
        assertThat(response.getOpenGapCount()).isEqualTo(1);
        assertThat(response.getMeasures())
            .filteredOn(measure -> measure.getMeasureCode().equals("COL"))
            .singleElement()
            .extracting("numerator")
            .isEqualTo(1);

        assertThat(response.getLastTriggerEvent()).isEqualTo("simulation");
        assertThat(response.getCalculatedAt()).isNotNull();
        verifyNoInteractions(starRatingProjectionRepository);
    }

    @Test
    void getCurrentRating_returnsFreshOnDemandMetadata() {
        when(careGapProjectionRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(
            gap("patient-1", "COL", "OPEN"),
            gap("patient-2", "CBP", "CLOSED")
        ));

        StarRatingResponse response = starsProjectionService.getCurrentRating(TENANT_ID);

        assertThat(response.getLastTriggerEvent()).isEqualTo("on-demand-read");
        assertThat(response.getCalculatedAt()).isNotNull();
        verifyNoInteractions(starRatingProjectionRepository);
    }

    @Test
    void getTrend_filtersByRequestedGranularity() {
        LocalDate snapshotDate = LocalDate.now().minusWeeks(1);
        when(starRatingSnapshotRepository
            .findByTenantIdAndSnapshotGranularityAndSnapshotDateGreaterThanEqualOrderBySnapshotDateAsc(
                eq(TENANT_ID),
                eq("MONTHLY"),
                any(LocalDate.class)
            ))
            .thenReturn(List.of(StarRatingSnapshot.builder()
                .id(1L)
                .tenantId(TENANT_ID)
                .snapshotDate(snapshotDate)
                .snapshotGranularity("MONTHLY")
                .overallRating(BigDecimal.valueOf(4.25))
                .roundedRating(BigDecimal.valueOf(4.5))
                .measureCount(3)
                .openGapCount(2)
                .closedGapCount(5)
                .qualityBonusEligible(true)
                .capturedAt(Instant.now())
                .build()));

        var response = starsProjectionService.getTrend(TENANT_ID, 4, "monthly");

        assertThat(response.getPoints()).singleElement().satisfies(point -> {
            assertThat(point.getGranularity()).isEqualTo("MONTHLY");
            assertThat(point.getSnapshotDate()).isEqualTo(snapshotDate);
        });
    }

    @Test
    void captureWeeklySnapshots_skipsExistingSnapshotForSameTenantDateAndGranularity() {
        when(careGapProjectionRepository.findDistinctTenantIds()).thenReturn(List.of(TENANT_ID));
        when(starRatingSnapshotRepository.findByTenantIdAndSnapshotDateAndSnapshotGranularity(
            eq(TENANT_ID),
            any(LocalDate.class),
            eq("WEEKLY")
        )).thenReturn(Optional.of(StarRatingSnapshot.builder()
            .id(10L)
            .tenantId(TENANT_ID)
            .snapshotDate(LocalDate.now())
            .snapshotGranularity("WEEKLY")
            .overallRating(BigDecimal.valueOf(4.0))
            .roundedRating(BigDecimal.valueOf(4.0))
            .measureCount(1)
            .openGapCount(1)
            .closedGapCount(1)
            .qualityBonusEligible(true)
            .capturedAt(Instant.now())
            .build()));

        starsProjectionService.captureWeeklySnapshots();

        verify(starRatingSnapshotRepository, never()).save(any());
    }

    // --- 010: mixed granularity regression ---

    @Test
    void getTrend_weeklyQueryExcludesMonthlySnapshots() {
        LocalDate date = LocalDate.now().minusWeeks(1);
        StarRatingSnapshot weeklySnapshot = snapshot(date, "WEEKLY", 3.50, 3.5);
        StarRatingSnapshot monthlySnapshot = snapshot(date, "MONTHLY", 3.80, 4.0);

        when(starRatingSnapshotRepository
            .findByTenantIdAndSnapshotGranularityAndSnapshotDateGreaterThanEqualOrderBySnapshotDateAsc(
                eq(TENANT_ID), eq("WEEKLY"), any(LocalDate.class)))
            .thenReturn(List.of(weeklySnapshot));

        StarRatingTrendResponse response = starsProjectionService.getTrend(TENANT_ID, 4, "WEEKLY");

        assertThat(response.getPoints()).singleElement().satisfies(point -> {
            assertThat(point.getGranularity()).isEqualTo("WEEKLY");
            assertThat(point.getOverallRating()).isEqualTo(3.50);
        });

        // Verify monthly was never queried
        verify(starRatingSnapshotRepository, never())
            .findByTenantIdAndSnapshotGranularityAndSnapshotDateGreaterThanEqualOrderBySnapshotDateAsc(
                eq(TENANT_ID), eq("MONTHLY"), any(LocalDate.class));
    }

    @Test
    void getTrend_nullGranularityDefaultsToWeekly() {
        when(starRatingSnapshotRepository
            .findByTenantIdAndSnapshotGranularityAndSnapshotDateGreaterThanEqualOrderBySnapshotDateAsc(
                eq(TENANT_ID), eq("WEEKLY"), any(LocalDate.class)))
            .thenReturn(List.of());

        StarRatingTrendResponse response = starsProjectionService.getTrend(TENANT_ID, 4, null);

        assertThat(response.getPoints()).isEmpty();
        verify(starRatingSnapshotRepository)
            .findByTenantIdAndSnapshotGranularityAndSnapshotDateGreaterThanEqualOrderBySnapshotDateAsc(
                eq(TENANT_ID), eq("WEEKLY"), any(LocalDate.class));
    }

    @Test
    void getTrend_blankGranularityDefaultsToWeekly() {
        when(starRatingSnapshotRepository
            .findByTenantIdAndSnapshotGranularityAndSnapshotDateGreaterThanEqualOrderBySnapshotDateAsc(
                eq(TENANT_ID), eq("WEEKLY"), any(LocalDate.class)))
            .thenReturn(List.of());

        StarRatingTrendResponse response = starsProjectionService.getTrend(TENANT_ID, 4, "   ");

        assertThat(response.getPoints()).isEmpty();
        verify(starRatingSnapshotRepository)
            .findByTenantIdAndSnapshotGranularityAndSnapshotDateGreaterThanEqualOrderBySnapshotDateAsc(
                eq(TENANT_ID), eq("WEEKLY"), any(LocalDate.class));
    }

    @Test
    void getTrend_invalidGranularityThrowsException() {
        assertThatThrownBy(() -> starsProjectionService.getTrend(TENANT_ID, 4, "DAILY"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("DAILY");
    }

    // --- 013: metadata freshness regression ---

    @Test
    void getCurrentRating_firstTimeTenantReceivesSensibleMetadata() {
        when(careGapProjectionRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of());

        Instant before = Instant.now();
        StarRatingResponse response = starsProjectionService.getCurrentRating(TENANT_ID);
        Instant after = Instant.now();

        assertThat(response.getLastTriggerEvent()).isEqualTo("on-demand-read");
        assertThat(response.getCalculatedAt()).isBetween(before, after);
        assertThat(response.getOverallRating()).isEqualTo(0.0);
        assertThat(response.getMeasureCount()).isEqualTo(0);
        verifyNoInteractions(starRatingProjectionRepository);
    }

    @Test
    void simulate_returnsFreshMetadataNotPersistedMetadata() {
        when(careGapProjectionRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(
            gap("patient-1", "COL", "OPEN")
        ));

        StarRatingSimulationRequest request = new StarRatingSimulationRequest();
        request.setClosures(List.of());

        Instant before = Instant.now();
        StarRatingResponse response = starsProjectionService.simulate(TENANT_ID, request);
        Instant after = Instant.now();

        assertThat(response.getLastTriggerEvent()).isEqualTo("simulation");
        assertThat(response.getCalculatedAt()).isBetween(before, after);
        verifyNoInteractions(starRatingProjectionRepository);
    }

    // --- 014: snapshot capture coverage ---

    @Test
    void captureWeeklySnapshots_savesNewSnapshotForTenantWithNoExisting() {
        when(careGapProjectionRepository.findDistinctTenantIds()).thenReturn(List.of(TENANT_ID));
        when(starRatingSnapshotRepository.findByTenantIdAndSnapshotDateAndSnapshotGranularity(
            eq(TENANT_ID), any(LocalDate.class), eq("WEEKLY")))
            .thenReturn(Optional.empty());
        when(careGapProjectionRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(
            gap("patient-1", "COL", "CLOSED"),
            gap("patient-2", "CBP", "OPEN")
        ));

        starsProjectionService.captureWeeklySnapshots();

        ArgumentCaptor<StarRatingSnapshot> captor = ArgumentCaptor.forClass(StarRatingSnapshot.class);
        verify(starRatingSnapshotRepository).save(captor.capture());
        StarRatingSnapshot saved = captor.getValue();
        assertThat(saved.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(saved.getSnapshotGranularity()).isEqualTo("WEEKLY");
        assertThat(saved.getSnapshotDate()).isEqualTo(LocalDate.now());
        assertThat(saved.getMeasureCount()).isEqualTo(2);
        assertThat(saved.getOpenGapCount()).isEqualTo(1);
        assertThat(saved.getClosedGapCount()).isEqualTo(1);
        assertThat(saved.getCapturedAt()).isNotNull();
    }

    @Test
    void captureMonthlySnapshots_savesNewSnapshot() {
        when(careGapProjectionRepository.findDistinctTenantIds()).thenReturn(List.of(TENANT_ID));
        when(starRatingSnapshotRepository.findByTenantIdAndSnapshotDateAndSnapshotGranularity(
            eq(TENANT_ID), any(LocalDate.class), eq("MONTHLY")))
            .thenReturn(Optional.empty());
        when(careGapProjectionRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(
            gap("patient-1", "COL", "CLOSED")
        ));

        starsProjectionService.captureMonthlySnapshots();

        ArgumentCaptor<StarRatingSnapshot> captor = ArgumentCaptor.forClass(StarRatingSnapshot.class);
        verify(starRatingSnapshotRepository).save(captor.capture());
        assertThat(captor.getValue().getSnapshotGranularity()).isEqualTo("MONTHLY");
    }

    @Test
    void captureMonthlySnapshots_skipsExistingSnapshot() {
        when(careGapProjectionRepository.findDistinctTenantIds()).thenReturn(List.of(TENANT_ID));
        when(starRatingSnapshotRepository.findByTenantIdAndSnapshotDateAndSnapshotGranularity(
            eq(TENANT_ID), any(LocalDate.class), eq("MONTHLY")))
            .thenReturn(Optional.of(snapshot(LocalDate.now(), "MONTHLY", 3.5, 3.5)));

        starsProjectionService.captureMonthlySnapshots();

        verify(starRatingSnapshotRepository, never()).save(any());
    }

    @Test
    void recalculateCurrentProjection_updatesExistingProjection() {
        when(careGapProjectionRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(
            gap("patient-1", "COL", "CLOSED")
        ));
        StarRatingProjection existing = StarRatingProjection.builder()
            .tenantId(TENANT_ID)
            .overallRating(BigDecimal.valueOf(2.0))
            .roundedRating(BigDecimal.valueOf(2.0))
            .measureCount(1)
            .openGapCount(1)
            .closedGapCount(0)
            .qualityBonusEligible(false)
            .lastTriggerEvent("old-event")
            .lastCalculatedAt(Instant.now().minusSeconds(3600))
            .build();
        when(starRatingProjectionRepository.findById(TENANT_ID)).thenReturn(Optional.of(existing));

        Instant before = Instant.now();
        StarRatingResponse response = starsProjectionService.recalculateCurrentProjection(TENANT_ID, "gap.closed:COL");

        assertThat(response.getLastTriggerEvent()).isEqualTo("gap.closed:COL");
        assertThat(response.getCalculatedAt()).isAfterOrEqualTo(before);
        assertThat(response.getClosedGapCount()).isEqualTo(1);

        ArgumentCaptor<StarRatingProjection> captor = ArgumentCaptor.forClass(StarRatingProjection.class);
        verify(starRatingProjectionRepository).save(captor.capture());
        assertThat(captor.getValue().getLastTriggerEvent()).isEqualTo("gap.closed:COL");
        assertThat(captor.getValue().getLastCalculatedAt()).isAfterOrEqualTo(before);
    }

    // --- helpers ---

    private CareGapProjection gap(String patientId, String gapCode, String status) {
        CareGapProjection gap = new CareGapProjection(patientId, TENANT_ID, gapCode, "desc", "HIGH");
        gap.setStatus(status);
        return gap;
    }

    private StarRatingSnapshot snapshot(LocalDate date, String granularity, double overall, double rounded) {
        return StarRatingSnapshot.builder()
            .id(1L)
            .tenantId(TENANT_ID)
            .snapshotDate(date)
            .snapshotGranularity(granularity)
            .overallRating(BigDecimal.valueOf(overall))
            .roundedRating(BigDecimal.valueOf(rounded))
            .measureCount(2)
            .openGapCount(1)
            .closedGapCount(1)
            .qualityBonusEligible(rounded >= 4.0)
            .capturedAt(Instant.now())
            .build();
    }
}
