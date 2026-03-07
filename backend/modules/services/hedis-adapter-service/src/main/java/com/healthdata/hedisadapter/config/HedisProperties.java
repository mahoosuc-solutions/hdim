package com.healthdata.hedisadapter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "external.hedis")
public class HedisProperties {
    private boolean enabled = false;
    private String baseUrl = "http://localhost:3333";
    private String cqlUrl = "http://localhost:8090";
    private String apiKey = "";
    private int timeoutMs = 10000;
    private int connectTimeoutMs = 5000;
    private int measureSyncIntervalMs = 3600000; // 1 hour
    private int maxWebsocketConnections = 1000;
}
