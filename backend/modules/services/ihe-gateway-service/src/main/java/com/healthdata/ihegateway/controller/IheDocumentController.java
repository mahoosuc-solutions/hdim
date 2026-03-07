package com.healthdata.ihegateway.controller;

import com.healthdata.ihegateway.actors.DocumentConsumer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ihe/xds")
@RequiredArgsConstructor
public class IheDocumentController {

    private final DocumentConsumer documentConsumer;

    @GetMapping("/query")
    public ResponseEntity<DocumentConsumer.DocumentQueryResult> queryDocuments(
            @RequestParam String patientId,
            @RequestParam(defaultValue = "clinical-note") String documentType) {
        return ResponseEntity.ok(documentConsumer.queryDocuments(patientId, documentType));
    }

    @GetMapping("/retrieve")
    public ResponseEntity<byte[]> retrieveDocument(@RequestParam String documentUrl) {
        byte[] content = documentConsumer.retrieveDocument(documentUrl);
        return ResponseEntity.ok(content);
    }
}
