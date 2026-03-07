package com.healthdata.corehiveadapter.audit;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class AtnaAuditArrForwardingTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void logAudit_withKafkaTemplate_forwardsToArrTopic() {
        AtnaAuditService service = new AtnaAuditService("corehive-adapter-service", "NONE", kafkaTemplate);
        AtnaAuditEvent event = service.buildAuditEvent("tenant-1", "CARE_GAP_SCORED",
                "CareGap", "gap-123", null, "corr-1", "SUCCESS", null);
        service.logAudit(event);
        verify(kafkaTemplate).send(eq("ihe.audit.events"), eq("tenant-1"), any(AtnaAuditEvent.class));
    }

    @Test
    void logAudit_withoutKafkaTemplate_onlyLogs() {
        AtnaAuditService service = new AtnaAuditService("corehive-adapter-service", "NONE");
        AtnaAuditEvent event = service.buildAuditEvent("tenant-1", "CARE_GAP_SCORED",
                "CareGap", "gap-123", null, "corr-1", "SUCCESS", null);
        service.logAudit(event);
        verifyNoInteractions(kafkaTemplate);
    }
}
