package com.healthdata.corehiveadapter.event;

import com.healthdata.corehiveadapter.service.CorehiveAdapterService;
import com.healthdata.corehiveadapter.service.PhiDeIdentificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("CareGapEventListener")
class CareGapEventListenerTest {

    @Mock
    private CorehiveAdapterService adapterService;

    private PhiDeIdentificationService deIdentificationService;
    private CareGapEventListener listener;

    @BeforeEach
    void setUp() {
        deIdentificationService = new PhiDeIdentificationService();
        listener = new CareGapEventListener(adapterService, deIdentificationService);
    }

    @Test
    @DisplayName("should de-identify patient ID and invoke scoring")
    void onCareGapEvent_shouldDeIdentifyAndScore() {
        Map<String, Object> event = new HashMap<>();
        event.put("patientId", "real-patient-123");
        event.put("tenantId", "tenant-1");

        listener.onCareGapEvent(event);

        var captor = ArgumentCaptor.forClass(com.healthdata.corehiveadapter.model.CareGapScoringRequest.class);
        verify(adapterService).scoreCareGaps(captor.capture(), eq("tenant-1"));

        var request = captor.getValue();
        assertThat(request.getSyntheticPatientId()).isNotEqualTo("real-patient-123");
        assertThat(request.getSyntheticPatientId()).isNotBlank();
        assertThat(request.getTenantId()).isEqualTo("tenant-1");
    }

    @Test
    @DisplayName("should skip events missing patientId")
    void onCareGapEvent_shouldSkipWhenPatientIdMissing() {
        Map<String, Object> event = new HashMap<>();
        event.put("tenantId", "tenant-1");

        listener.onCareGapEvent(event);

        verifyNoInteractions(adapterService);
    }

    @Test
    @DisplayName("should use default tenantId when not provided")
    void onCareGapEvent_shouldUseDefaultTenantId() {
        Map<String, Object> event = new HashMap<>();
        event.put("patientId", "patient-xyz");

        listener.onCareGapEvent(event);

        verify(adapterService).scoreCareGaps(any(), eq("unknown"));
    }

    @Test
    @DisplayName("should not propagate exception from adapter service")
    void onCareGapEvent_shouldCatchAdapterException() {
        Map<String, Object> event = new HashMap<>();
        event.put("patientId", "patient-abc");
        event.put("tenantId", "tenant-1");

        doThrow(new RuntimeException("CoreHive unreachable"))
                .when(adapterService).scoreCareGaps(any(), any());

        // Should not throw
        listener.onCareGapEvent(event);

        verify(adapterService).scoreCareGaps(any(), eq("tenant-1"));
    }
}
