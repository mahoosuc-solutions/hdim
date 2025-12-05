package com.healthdata.eventrouter.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventMessage {
    private String eventId;
    private String eventType;
    private String tenantId;
    private String sourceTopic;
    private Instant timestamp;
    private Map<String, Object> payload = new HashMap<>();
    private Map<String, String> metadata = new HashMap<>();
}
