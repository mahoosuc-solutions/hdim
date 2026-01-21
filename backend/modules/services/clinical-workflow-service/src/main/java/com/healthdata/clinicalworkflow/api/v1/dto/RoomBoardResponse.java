package com.healthdata.clinicalworkflow.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response containing room status board overview
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Room status board with all rooms")
public class RoomBoardResponse {

    @Schema(description = "List of all room statuses")
    private List<RoomStatusResponse> rooms;

    @Schema(description = "Count of available rooms", example = "5")
    private Integer availableCount;

    @Schema(description = "Count of occupied rooms", example = "8")
    private Integer occupiedCount;

    @Schema(description = "Count of rooms being cleaned", example = "2")
    private Integer cleaningCount;

    @Schema(description = "Count of out-of-service rooms", example = "1")
    private Integer outOfServiceCount;

    @Schema(description = "Total number of rooms", example = "16")
    private Integer totalRooms;
}
