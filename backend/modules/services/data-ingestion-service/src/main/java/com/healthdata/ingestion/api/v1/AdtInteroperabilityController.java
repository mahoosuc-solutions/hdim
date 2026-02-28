package com.healthdata.ingestion.api.v1;

import com.healthdata.ingestion.application.AdtExchangeService;
import com.healthdata.ingestion.interoperability.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/interoperability/adt")
@RequiredArgsConstructor
@Tag(name = "ADT Interoperability", description = "Wave-1 ADT exchange backbone contract APIs")
public class AdtInteroperabilityController {

    private final AdtExchangeService adtExchangeService;

    @PostMapping("/messages")
    @Operation(summary = "Ingest ADT message")
    public ResponseEntity<AdtMessageIngestResponse> ingestMessage(
            @Valid @RequestBody AdtMessageIngestRequest request
    ) {
        return ResponseEntity.ok(adtExchangeService.ingestMessage(request));
    }

    @PostMapping("/acks")
    @Operation(summary = "Record ADT acknowledgement")
    public ResponseEntity<AdtAcknowledgementResponse> acknowledge(
            @Valid @RequestBody AdtAcknowledgementRequest request
    ) {
        return ResponseEntity.ok(adtExchangeService.acknowledge(request));
    }

    @GetMapping("/events/{eventId}")
    @Operation(summary = "Get canonical encounter event record")
    public ResponseEntity<EncounterEventRecord> getEvent(@PathVariable String eventId) {
        EncounterEventRecord record = adtExchangeService.getEvent(eventId);
        return record == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(record);
    }
}
