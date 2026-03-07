package com.healthdata.corehiveadapter.event;

import com.healthdata.corehiveadapter.config.CorehiveProperties;
import com.healthdata.corehiveadapter.model.CareGapScoringRequest;
import com.healthdata.corehiveadapter.observability.AdapterSpanHelper;
import com.healthdata.corehiveadapter.service.CorehiveAdapterService;
import com.healthdata.corehiveadapter.service.PhiDeIdentificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Listens to HDIM care gap events and triggers CoreHive AI scoring
 * after de-identifying all patient data.
 */
@Component
@ConditionalOnProperty(name = "external.corehive.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class CareGapEventListener {

    private final CorehiveAdapterService adapterService;
    private final PhiDeIdentificationService deIdentificationService;
    private final AdapterSpanHelper spanHelper;

    @KafkaListener(
            topics = "external.hdim.caregaps",
            groupId = "corehive-adapter-caregap-consumer"
    )
    public void onCareGapEvent(Map<String, Object> event) {
        spanHelper.tracedRun("corehive.kafka.care_gap_received", () -> {
            log.info("Received HDIM care gap event for AI scoring");

            String tenantId = (String) event.getOrDefault("tenantId", "unknown");
            String realPatientId = (String) event.get("patientId");

            if (realPatientId == null) {
                log.warn("Care gap event missing patientId, skipping");
                return;
            }

            String syntheticPatientId = deIdentificationService.toSyntheticId(realPatientId);

            CareGapScoringRequest request = CareGapScoringRequest.builder()
                    .syntheticPatientId(syntheticPatientId)
                    .tenantId(tenantId)
                    .careGaps(List.of())
                    .build();

            try {
                adapterService.scoreCareGaps(request, tenantId);
            } catch (Exception e) {
                log.error("Failed to score care gaps via CoreHive: {}", e.getMessage());
            }
        }, "adapter", "corehive", "phi.level", "NONE");
    }
}
