package com.healthdata.common.external;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Metadata envelope for events crossing external integration boundaries.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalEventMetadata {
    private SourceSystem sourceSystem;
    private PhiLevel phiLevel;
    private String auditTraceId;
}
