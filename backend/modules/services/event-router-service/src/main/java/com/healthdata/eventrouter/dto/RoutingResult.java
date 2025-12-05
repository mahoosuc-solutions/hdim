package com.healthdata.eventrouter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoutingResult {
    private boolean success;
    private String targetTopic;
    private String reason;
    private String routingRuleName;

    public static RoutingResult success(String targetTopic, String ruleName) {
        return new RoutingResult(true, targetTopic, "Successfully routed", ruleName);
    }

    public static RoutingResult failure(String reason) {
        return new RoutingResult(false, null, reason, null);
    }
}
