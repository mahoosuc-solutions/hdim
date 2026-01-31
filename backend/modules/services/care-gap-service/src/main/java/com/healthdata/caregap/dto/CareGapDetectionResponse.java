package com.healthdata.caregap.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class CareGapDetectionResponse {
    private UUID id;
    private String category;
    private String title;
    private String priority;
    private String status;
    private LocalDate dueDate;
}
