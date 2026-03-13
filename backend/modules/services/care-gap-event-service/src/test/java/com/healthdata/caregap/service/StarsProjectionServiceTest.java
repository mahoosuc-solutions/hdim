package com.healthdata.caregap.service;

import com.healthdata.caregap.api.v1.dto.SimulatedGapClosureRequest;
import com.healthdata.caregap.api.v1.dto.StarRatingResponse;
import com.healthdata.caregap.api.v1.dto.StarRatingSimulationRequest;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
                .overallRating(java.math.BigDecimal.valueOf(4.25))
                .roundedRating(java.math.BigDecimal.valueOf(4.5))
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
            .overallRating(java.math.BigDecimal.valueOf(4.0))
            .roundedRating(java.math.BigDecimal.valueOf(4.0))
            .measureCount(1)
            .openGapCount(1)
            .closedGapCount(1)
            .qualityBonusEligible(true)
            .capturedAt(Instant.now())
            .build()));

        starsProjectionService.captureWeeklySnapshots();

        verify(starRatingSnapshotRepository, never()).save(any());
    }

    private CareGapProjection gap(String patientId, String gapCode, String status) {
        CareGapProjection gap = new CareGapProjection(patientId, TENANT_ID, gapCode, "desc", "HIGH");
        gap.setStatus(status);
        return gap;
    }
}
