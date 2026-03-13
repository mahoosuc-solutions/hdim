package com.healthdata.caregap.listener;

import com.healthdata.caregap.event.CareGapDetectedEvent;
import com.healthdata.caregap.event.GapClosedEvent;
import com.healthdata.caregap.service.StarsProjectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class StarsGapEventListener {

    private final StarsProjectionService starsProjectionService;

    @KafkaListener(topics = "gap.events", groupId = "care-gap-stars-projection")
    public void onGapEvent(@Payload Object payload) {
        if (payload instanceof CareGapDetectedEvent event) {
            recalculateAfterDetection(event.getTenantId(), event.getGapCode());
            return;
        }

        if (payload instanceof GapClosedEvent event) {
            recalculateAfterClosure(event.getTenantId(), event.getGapCode());
            return;
        }

        if (payload instanceof Map<?, ?> mapPayload) {
            String tenantId = asString(mapPayload.get("tenantId"));
            String gapCode = asString(mapPayload.get("gapCode"));
            if (tenantId == null || gapCode == null) {
                return;
            }

            if (mapPayload.containsKey("closureStatus") || mapPayload.containsKey("closureReason")) {
                recalculateAfterClosure(tenantId, gapCode);
                return;
            }

            if (mapPayload.containsKey("severity") || mapPayload.containsKey("gapDescription")) {
                recalculateAfterDetection(tenantId, gapCode);
            }
        }
    }

    private void recalculateAfterDetection(String tenantId, String gapCode) {
        log.debug("Recalculating Stars projection after gap detection for tenant {}", tenantId);
        starsProjectionService.recalculateCurrentProjection(tenantId, "gap.detected:" + gapCode);
    }

    private void recalculateAfterClosure(String tenantId, String gapCode) {
        log.debug("Recalculating Stars projection after gap closure for tenant {}", tenantId);
        starsProjectionService.recalculateCurrentProjection(tenantId, "gap.closed:" + gapCode);
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
