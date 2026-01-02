package com.healthdata.notification.api.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkNotificationResponse {

    private int totalRequested;
    private int successCount;
    private int failedCount;
    private List<NotificationResponse> notifications;
}
