package com.healthdata.quality.events;

import com.healthdata.quality.domain.MeasureResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event published when a quality measure is calculated
 * Other modules can listen to this for care gap detection, notifications, etc.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeasureCalculatedEvent {
    private String patientId;
    private String measureId;
    private MeasureResult result;
    private String tenantId;
    private LocalDateTime timestamp = LocalDateTime.now();

    public MeasureCalculatedEvent(String patientId, String measureId, MeasureResult result, String tenantId) {
        this.patientId = patientId;
        this.measureId = measureId;
        this.result = result;
        this.tenantId = tenantId;
        this.timestamp = LocalDateTime.now();
    }
}