package com.healthdata.fhir.rest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal CapabilityStatement for demo metadata requests.
 */
@RestController
@RequestMapping(produces = {"application/fhir+json", "application/json"})
public class MetadataController {

    @Value("${smart.base-url:http://localhost:8085/fhir}")
    private String baseUrl;

    @GetMapping("/metadata")
    public Map<String, Object> metadata() {
        Map<String, Object> capability = new LinkedHashMap<>();
        capability.put("resourceType", "CapabilityStatement");
        capability.put("status", "active");
        capability.put("date", OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        capability.put("kind", "instance");
        capability.put("fhirVersion", "4.0.1");
        capability.put("format", List.of("json"));
        capability.put("implementation", Map.of(
            "description", "HDIM FHIR Service",
            "url", baseUrl
        ));
        capability.put("rest", List.of(Map.of("mode", "server")));
        return capability;
    }
}
