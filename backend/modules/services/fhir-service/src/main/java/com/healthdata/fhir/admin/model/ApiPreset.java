package com.healthdata.fhir.admin.model;

import java.util.Map;

public record ApiPreset(
        String id,
        String name,
        String method,
        String path,
        Map<String, String> headers,
        Map<String, String> queryParameters,
        String samplePayload
) {
}
