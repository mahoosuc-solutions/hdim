package com.healthdata.cql.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import com.healthdata.audit.annotations.Audited;
import com.healthdata.audit.models.AuditAction;

/**
 * Health check controller for service status
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthCheckController {

    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "cql-engine-service");
        health.put("timestamp", Instant.now().toString());
        health.put("version", "1.0.0");
        return ResponseEntity.ok(health);
    }

    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> readiness() {
        Map<String, Object> readiness = new HashMap<>();
        readiness.put("ready", true);
        readiness.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(readiness);
    }

    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/live")
    public ResponseEntity<Map<String, Object>> liveness() {
        Map<String, Object> liveness = new HashMap<>();
        liveness.put("alive", true);
        liveness.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(liveness);
    }
}
