package com.healthdata.caregap.listener;

import com.healthdata.caregap.event.CareGapDetectedEvent;
import com.healthdata.caregap.event.GapClosedEvent;
import com.healthdata.caregap.event.InterventionRecommendedEvent;
import com.healthdata.caregap.event.PatientQualifiedEvent;
import com.healthdata.caregap.service.StarsProjectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Kafka listener that recomputes the CMS Star Ratings projection
 * whenever a care gap lifecycle event occurs.
 *
 * Handles: detect, close, reopen, qualify, intervention
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StarsGapEventListener {

    private final StarsProjectionService starsProjectionService;

    @KafkaListener(topics = "gap.events", groupId = "care-gap-stars-projection")
    public void onGapEvent(@Payload Object payload) {
        if (payload instanceof CareGapDetectedEvent event) {
            recalculate(event.getTenantId(), "gap.detected:" + event.getGapCode());
            return;
        }

        if (payload instanceof GapClosedEvent event) {
            recalculate(event.getTenantId(), "gap.closed:" + event.getGapCode());
            return;
        }

        if (payload instanceof PatientQualifiedEvent event) {
            recalculate(event.getTenantId(), "patient.qualified:" + event.getGapCode());
            return;
        }

        if (payload instanceof InterventionRecommendedEvent event) {
            recalculate(event.getTenantId(), "intervention.recommended:" + event.getGapCode());
            return;
        }

        if (payload instanceof Map<?, ?> mapPayload) {
            String tenantId = asString(mapPayload.get("tenantId"));
            String gapCode = asString(mapPayload.get("gapCode"));
            if (tenantId == null || gapCode == null) {
                return;
            }

            if (mapPayload.containsKey("closureStatus") || mapPayload.containsKey("closureReason")) {
                recalculate(tenantId, "gap.closed:" + gapCode);
                return;
            }

            if (mapPayload.containsKey("severity") || mapPayload.containsKey("gapDescription")) {
                recalculate(tenantId, "gap.detected:" + gapCode);
                return;
            }

            // Catch-all for any other lifecycle event from Map payload
            if (mapPayload.containsKey("qualified") || mapPayload.containsKey("recommendation")
                || mapPayload.containsKey("priority")) {
                recalculate(tenantId, "lifecycle:" + gapCode);
            }
        }
    }

    private void recalculate(String tenantId, String triggerEvent) {
        log.debug("Recalculating Stars projection for tenant {} (trigger: {})", tenantId, triggerEvent);
        starsProjectionService.recalculateCurrentProjection(tenantId, triggerEvent);
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
