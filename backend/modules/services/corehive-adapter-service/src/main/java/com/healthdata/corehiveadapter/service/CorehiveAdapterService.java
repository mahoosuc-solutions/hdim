package com.healthdata.corehiveadapter.service;

import com.healthdata.corehiveadapter.client.CorehiveApiClient;
import com.healthdata.corehiveadapter.config.CorehiveProperties;
import com.healthdata.corehiveadapter.model.CareGapScoringRequest;
import com.healthdata.corehiveadapter.model.CareGapScoringResponse;
import com.healthdata.corehiveadapter.model.VbcRoiRequest;
import com.healthdata.corehiveadapter.model.VbcRoiResponse;
import com.healthdata.common.external.ExternalEventEnvelope;
import com.healthdata.common.external.ExternalEventMetadata;
import com.healthdata.common.external.PhiLevel;
import com.healthdata.common.external.SourceSystem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "external.corehive.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class CorehiveAdapterService {

    private final CorehiveApiClient apiClient;
    private final PhiDeIdentificationService deIdentificationService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final CorehiveProperties properties;

    private static final String TOPIC_DECISIONS = "external.corehive.decisions";
    private static final String TOPIC_ROI = "external.corehive.roi";

    public CareGapScoringResponse scoreCareGaps(CareGapScoringRequest request, String tenantId) {
        log.info("Scoring care gaps for tenant={}, gapCount={}",
                tenantId, request.getCareGaps().size());

        validateNoPhiInRequest(request);

        CareGapScoringResponse response = apiClient.scoreCareGaps(request);

        ExternalEventEnvelope<CareGapScoringResponse> envelope = ExternalEventEnvelope.of(
                "external.corehive.decisions.scored",
                "corehive-adapter-service",
                tenantId,
                response,
                ExternalEventMetadata.builder()
                        .sourceSystem(SourceSystem.COREHIVE)
                        .phiLevel(PhiLevel.NONE)
                        .build());

        kafkaTemplate.send(TOPIC_DECISIONS, tenantId, envelope);
        log.info("Published scoring result to {}", TOPIC_DECISIONS);

        return response;
    }

    public VbcRoiResponse calculateRoi(VbcRoiRequest request, String tenantId) {
        log.info("Calculating VBC ROI for tenant={}, contract={}",
                tenantId, request.getContractId());

        VbcRoiResponse response = apiClient.calculateRoi(request);

        ExternalEventEnvelope<VbcRoiResponse> envelope = ExternalEventEnvelope.of(
                "external.corehive.roi.calculated",
                "corehive-adapter-service",
                tenantId,
                response,
                ExternalEventMetadata.builder()
                        .sourceSystem(SourceSystem.COREHIVE)
                        .phiLevel(PhiLevel.NONE)
                        .build());

        kafkaTemplate.send(TOPIC_ROI, tenantId, envelope);

        return response;
    }

    private void validateNoPhiInRequest(CareGapScoringRequest request) {
        if (deIdentificationService.containsPotentialPhi(request.getSyntheticPatientId())) {
            throw new SecurityException("PHI detected in CoreHive request — real patient ID must not be sent");
        }
        for (CareGapScoringRequest.CareGapItem gap : request.getCareGaps()) {
            if (deIdentificationService.containsPotentialPhi(gap.getSyntheticGapId())) {
                throw new SecurityException("PHI detected in care gap ID");
            }
        }
    }
}
