package com.healthdata.caregap.api.v1.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class StarRatingSimulationRequest {

    @Valid
    @NotEmpty
    private List<SimulatedGapClosureRequest> closures;
}
