package com.healthdata.caregap.api.v1.controller;

import com.healthdata.caregap.api.v1.dto.StarRatingResponse;
import com.healthdata.caregap.api.v1.dto.StarRatingSimulationRequest;
import com.healthdata.caregap.api.v1.dto.StarRatingTrendResponse;
import com.healthdata.caregap.service.StarsProjectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/star-ratings")
@RequiredArgsConstructor
@Tag(name = "Star Ratings", description = "Current Stars projection, trending, and simulation")
public class StarRatingController {

    private final StarsProjectionService starsProjectionService;

    @GetMapping("/current")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'ANALYST')")
    @Operation(summary = "Get current Stars projection")
    public ResponseEntity<StarRatingResponse> getCurrent(@RequestHeader("X-Tenant-ID") String tenantId) {
        return ResponseEntity.ok(starsProjectionService.getCurrentRating(tenantId));
    }

    @GetMapping("/trend")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'ANALYST')")
    @Operation(summary = "Get weekly/monthly Stars trend")
    public ResponseEntity<StarRatingTrendResponse> getTrend(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam(defaultValue = "12") int weeks,
        @RequestParam(defaultValue = "WEEKLY") String granularity
    ) {
        return ResponseEntity.ok(starsProjectionService.getTrend(tenantId, weeks, granularity));
    }

    @PostMapping("/simulate")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    @Operation(summary = "Run on-demand Stars simulation")
    public ResponseEntity<StarRatingResponse> simulate(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @Valid @RequestBody StarRatingSimulationRequest request
    ) {
        return ResponseEntity.ok(starsProjectionService.simulate(tenantId, request));
    }
}
