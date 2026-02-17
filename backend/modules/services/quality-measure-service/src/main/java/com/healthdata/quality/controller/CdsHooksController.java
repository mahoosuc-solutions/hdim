package com.healthdata.quality.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.healthdata.quality.dto.CdsRecommendationDTO;
import com.healthdata.quality.service.CdsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * CDS Hooks facade over internal CDS recommendation services.
 *
 * Implements:
 * - GET /cds-services
 * - POST /cds-services/patient-view
 */
@RestController
@RequestMapping("/cds-services")
@RequiredArgsConstructor
@Slf4j
@Validated
public class CdsHooksController {

    private final CdsService cdsService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CdsServicesDiscoveryResponse> discoverServices() {
        CdsServiceDescriptor patientView = CdsServiceDescriptor.builder()
            .id("patient-view")
            .hook("patient-view")
            .title("HDIM Care Gap Alerts")
            .description("Returns patient-specific care gap recommendations for point-of-care review.")
            .prefetch(null)
            .build();

        CdsServiceDescriptor orderSelect = CdsServiceDescriptor.builder()
            .id("order-select")
            .hook("order-select")
            .title("HDIM Order Select Gap Closure Guidance")
            .description("Returns gap closure suggestions at order selection time.")
            .prefetch(null)
            .build();

        return ResponseEntity.ok(CdsServicesDiscoveryResponse.builder()
            .services(List.of(patientView, orderSelect))
            .build());
    }

    @PostMapping(path = "/patient-view", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CdsHooksResponse> patientView(
            @RequestHeader("X-Tenant-ID") @NotBlank String tenantId,
            @RequestBody @Valid CdsHooksRequest request) {

        if (request.getContext() == null || request.getContext().getPatientId() == null) {
            return ResponseEntity.badRequest().body(CdsHooksResponse.builder()
                .cards(List.of(CdsCard.builder()
                    .summary("Invalid CDS Hooks request")
                    .detail("context.patientId is required for patient-view hook.")
                    .indicator("warning")
                    .source(CdsCardSource.builder().label("HDIM CDS").build())
                    .build()))
                .build());
        }

        UUID patientId;
        try {
            patientId = UUID.fromString(request.getContext().getPatientId());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(CdsHooksResponse.builder()
                .cards(List.of(CdsCard.builder()
                    .summary("Invalid patient ID")
                    .detail("context.patientId must be a UUID.")
                    .indicator("warning")
                    .source(CdsCardSource.builder().label("HDIM CDS").build())
                    .build()))
                .build());
        }

        List<CdsRecommendationDTO> recommendations = cdsService.getActiveRecommendations(tenantId, patientId);
        List<CdsCard> cards = new ArrayList<>();

        for (CdsRecommendationDTO recommendation : recommendations) {
            cards.add(CdsCard.builder()
                .summary(recommendation.getTitle())
                .detail(buildDetail(recommendation))
                .indicator(toIndicator(recommendation.getUrgency()))
                .source(CdsCardSource.builder()
                    .label("HDIM Care Gap Engine")
                    .url("https://github.com/webemo-aaron/hdim")
                    .build())
                .build());
        }

        log.info("CDS Hooks patient-view generated {} cards for tenant={} patient={}",
            cards.size(), tenantId, patientId);

        return ResponseEntity.ok(CdsHooksResponse.builder().cards(cards).build());
    }

    @PostMapping(path = "/order-select", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CdsHooksResponse> orderSelect(
            @RequestHeader("X-Tenant-ID") @NotBlank String tenantId,
            @RequestBody @Valid CdsHooksRequest request) {
        UUID patientId = extractPatientId(request);
        if (patientId == null) {
            return ResponseEntity.badRequest().body(CdsHooksResponse.builder()
                .cards(List.of(CdsCard.builder()
                    .summary("Invalid CDS Hooks request")
                    .detail("context.patientId is required for order-select hook.")
                    .indicator("warning")
                    .source(CdsCardSource.builder().label("HDIM CDS").build())
                    .build()))
                .build());
        }

        List<CdsRecommendationDTO> recommendations = cdsService.getActiveRecommendations(tenantId, patientId);
        List<CdsCard> cards = new ArrayList<>();

        for (CdsRecommendationDTO recommendation : recommendations) {
            cards.add(CdsCard.builder()
                .summary("Consider order to close care gap")
                .detail(buildDetail(recommendation))
                .indicator(toIndicator(recommendation.getUrgency()))
                .source(CdsCardSource.builder()
                    .label("HDIM Care Gap Engine")
                    .url("https://github.com/webemo-aaron/hdim")
                    .build())
                .suggestions(List.of(
                    CdsSuggestion.builder()
                        .label("Close gap: " + recommendation.getTitle())
                        .actions(List.of(CdsAction.builder()
                            .type("create")
                            .description("Create order to address recommendation: " + recommendation.getTitle())
                            .build()))
                        .build()))
                .build());
        }

        log.info("CDS Hooks order-select generated {} cards for tenant={} patient={}",
            cards.size(), tenantId, patientId);

        return ResponseEntity.ok(CdsHooksResponse.builder().cards(cards).build());
    }

    private UUID extractPatientId(CdsHooksRequest request) {
        if (request == null || request.getContext() == null || request.getContext().getPatientId() == null) {
            return null;
        }
        try {
            return UUID.fromString(request.getContext().getPatientId());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String toIndicator(String urgency) {
        if (urgency == null) {
            return "info";
        }
        String normalized = urgency.toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "EMERGENT", "URGENT" -> "critical";
            case "SOON" -> "warning";
            default -> "info";
        };
    }

    private String buildDetail(CdsRecommendationDTO recommendation) {
        String description = recommendation.getDescription() == null ? "" : recommendation.getDescription();
        String dueDate = recommendation.getDueDate() == null
            ? ""
            : " Due: " + recommendation.getDueDate().toString() + ".";
        return description + dueDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CdsServicesDiscoveryResponse {
        private List<CdsServiceDescriptor> services;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CdsServiceDescriptor {
        private String hook;
        private String id;
        private String title;
        private String description;
        private Object prefetch;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CdsHooksRequest {
        private String hook;
        private String hookInstance;
        private String fhirServer;
        private Instant fhirAuthorization;
        private CdsContext context;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CdsContext {
        private String patientId;
        private String encounterId;
        private String userId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CdsHooksResponse {
        private List<CdsCard> cards;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CdsCard {
        private String summary;
        private String detail;
        private String indicator;
        private CdsCardSource source;
        private List<CdsSuggestion> suggestions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CdsCardSource {
        private String label;
        private String url;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CdsSuggestion {
        private String label;
        private List<CdsAction> actions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CdsAction {
        private String type;
        private String description;
    }
}
