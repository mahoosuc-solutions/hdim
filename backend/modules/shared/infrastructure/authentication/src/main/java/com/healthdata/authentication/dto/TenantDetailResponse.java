package com.healthdata.authentication.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantDetailResponse {
    private String id;
    private String name;
    private String status;
    private long userCount;
    private Instant createdAt;
    private Instant updatedAt;
}
