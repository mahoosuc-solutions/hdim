package com.healthdata.caregap.listener;

import com.healthdata.caregap.event.CareGapDetectedEvent;
import com.healthdata.caregap.event.GapClosedEvent;
import com.healthdata.caregap.event.InterventionRecommendedEvent;
import com.healthdata.caregap.event.PatientQualifiedEvent;
import com.healthdata.caregap.service.StarsProjectionService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@Tag("unit")
class StarsGapEventListenerTest {

    private final StarsProjectionService starsProjectionService = mock(StarsProjectionService.class);
    private final StarsGapEventListener listener = new StarsGapEventListener(starsProjectionService);

    @Test
    void onGapEvent_recalculatesProjectionForDetectedGap() {
        listener.onGapEvent(new CareGapDetectedEvent("tenant-a", "patient-1", "COL", "desc", "HIGH"));

        verify(starsProjectionService).recalculateCurrentProjection("tenant-a", "gap.detected:COL");
    }

    @Test
    void onGapEvent_recalculatesProjectionForClosedGap() {
        listener.onGapEvent(new GapClosedEvent("tenant-a", "patient-1", "COL", "reason", "CLOSED"));

        verify(starsProjectionService).recalculateCurrentProjection("tenant-a", "gap.closed:COL");
    }

    @Test
    void onGapEvent_recalculatesProjectionForPatientQualified() {
        listener.onGapEvent(new PatientQualifiedEvent("tenant-a", "patient-1", "BCS", true, "eligible"));

        verify(starsProjectionService).recalculateCurrentProjection("tenant-a", "patient.qualified:BCS");
    }

    @Test
    void onGapEvent_recalculatesProjectionForInterventionRecommended() {
        listener.onGapEvent(new InterventionRecommendedEvent("tenant-a", "patient-1", "COL", "Colonoscopy", "HIGH"));

        verify(starsProjectionService).recalculateCurrentProjection("tenant-a", "intervention.recommended:COL");
    }

    @Test
    void onGapEvent_ignoresUnsupportedPayload() {
        listener.onGapEvent("not-an-event");

        verifyNoInteractions(starsProjectionService);
    }

    @Test
    void onGapEvent_recalculatesProjectionForDetectedGapMapPayload() {
        listener.onGapEvent(Map.of(
            "tenantId", "tenant-a",
            "gapCode", "COL",
            "severity", "HIGH"
        ));

        verify(starsProjectionService).recalculateCurrentProjection("tenant-a", "gap.detected:COL");
    }

    @Test
    void onGapEvent_recalculatesProjectionForClosedGapMapPayload() {
        listener.onGapEvent(Map.of(
            "tenantId", "tenant-a",
            "gapCode", "COL",
            "closureStatus", "CLOSED"
        ));

        verify(starsProjectionService).recalculateCurrentProjection("tenant-a", "gap.closed:COL");
    }

    @Test
    void onGapEvent_ignoresMapPayloadWithMissingTenantId() {
        listener.onGapEvent(Map.of(
            "gapCode", "COL",
            "severity", "HIGH"
        ));

        verifyNoInteractions(starsProjectionService);
    }

    @Test
    void onGapEvent_recalculatesProjectionForLifecycleMapPayload() {
        listener.onGapEvent(Map.of(
            "tenantId", "tenant-a",
            "gapCode", "BCS",
            "qualified", true
        ));

        verify(starsProjectionService).recalculateCurrentProjection("tenant-a", "lifecycle:BCS");
    }
}
