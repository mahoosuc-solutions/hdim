package com.healthdata.corehiveadapter.controller;

import com.healthdata.corehiveadapter.config.CorehiveProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/external/corehive")
@RequiredArgsConstructor
public class CorehiveHealthController {

    private final CorehiveProperties properties;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
                "service", "corehive-adapter-service",
                "enabled", properties.isEnabled(),
                "targetUrl", properties.getBaseUrl(),
                "phiLevel", "NONE"
        ));
    }
}
