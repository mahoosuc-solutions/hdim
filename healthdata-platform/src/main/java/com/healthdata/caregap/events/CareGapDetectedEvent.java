package com.healthdata.caregap.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Event published when a care gap is detected
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CareGapDetectedEvent {
    private String gapId;
    private String patientId;
    private String gapType;
    private String priority;
    private LocalDateTime detectedAt;
    private String measureId;

    public CareGapDetectedEvent(String gapId, String patientId, String gapType, String priority) {
        this.gapId = gapId;
        this.patientId = patientId;
        this.gapType = gapType;
        this.priority = priority;
        this.detectedAt = LocalDateTime.now();
    }
}