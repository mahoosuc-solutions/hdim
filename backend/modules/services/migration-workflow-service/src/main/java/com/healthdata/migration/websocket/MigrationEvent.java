package com.healthdata.migration.websocket;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket event envelope for migration updates
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MigrationEvent {
    private MigrationEventType type;
    private UUID jobId;
    private Instant timestamp;
    private Object payload;
}
