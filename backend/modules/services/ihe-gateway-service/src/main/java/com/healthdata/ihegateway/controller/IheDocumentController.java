package com.healthdata.ihegateway.controller;

import com.healthdata.ihegateway.actors.DocumentConsumer;
import com.healthdata.ihegateway.actors.XcaInitiatingGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ihe/xds")
public class IheDocumentController {

    private final DocumentConsumer documentConsumer;
    private final XcaInitiatingGateway xcaInitiatingGateway;

    public IheDocumentController(
            DocumentConsumer documentConsumer,
            @Autowired(required = false) XcaInitiatingGateway xcaInitiatingGateway) {
        this.documentConsumer = documentConsumer;
        this.xcaInitiatingGateway = xcaInitiatingGateway;
    }

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

    @GetMapping("/xca/query")
    public ResponseEntity<?> crossGatewayQuery(
            @RequestParam String patientId,
            @RequestParam(defaultValue = "CCD") String documentType) {
        if (xcaInitiatingGateway == null) {
            return ResponseEntity.status(503).body(Map.of("error", "XCA gateway not enabled"));
        }
        var result = xcaInitiatingGateway.crossGatewayQuery(patientId, documentType);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/xca/retrieve")
    public ResponseEntity<byte[]> crossGatewayRetrieve(@RequestParam String documentUrl) {
        if (xcaInitiatingGateway == null) {
            return ResponseEntity.status(503).build();
        }
        byte[] document = xcaInitiatingGateway.crossGatewayRetrieve(documentUrl);
        return ResponseEntity.ok(document);
    }
}
