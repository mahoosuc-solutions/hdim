package com.healthdata.investor.controller;

import com.healthdata.investor.dto.DashboardStatsDTO;
import com.healthdata.investor.security.UserPrincipal;
import com.healthdata.investor.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for dashboard statistics.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Dashboard statistics endpoints")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @Operation(summary = "Get dashboard statistics")
    public ResponseEntity<DashboardStatsDTO> getStats(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(dashboardService.getStats(principal.getUserId()));
    }
}
