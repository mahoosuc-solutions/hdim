package com.healthdata.hedisadapter.controller;

import com.healthdata.hedisadapter.config.HedisProperties;
import com.healthdata.hedisadapter.websocket.KafkaToWebSocketBridge;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/external/hedis")
public class HedisHealthController {

    private final HedisProperties properties;
    private final KafkaToWebSocketBridge webSocketBridge;

    public HedisHealthController(
            HedisProperties properties,
            @Autowired(required = false) KafkaToWebSocketBridge webSocketBridge) {
        this.properties = properties;
        this.webSocketBridge = webSocketBridge;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
                "service", "hedis-adapter-service",
                "enabled", properties.isEnabled(),
                "targetUrl", properties.getBaseUrl(),
                "cqlUrl", properties.getCqlUrl(),
                "phiLevel", "LIMITED",
                "activeWebSocketConnections", webSocketBridge != null ? webSocketBridge.getActiveConnectionCount() : 0
        ));
    }
}
