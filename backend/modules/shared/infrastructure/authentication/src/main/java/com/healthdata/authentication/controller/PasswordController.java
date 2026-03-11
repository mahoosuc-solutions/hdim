package com.healthdata.authentication.controller;

import com.healthdata.authentication.dto.ChangePasswordRequest;
import com.healthdata.authentication.dto.ForceChangePasswordRequest;
import com.healthdata.authentication.service.UserManagementService;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth/password")
@ConditionalOnProperty(
    prefix = "authentication.controller",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class PasswordController {

    private final UserManagementService userManagementService;

    public PasswordController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @PostMapping("/change")
    public ResponseEntity<Map<String, String>> changePassword(
            @RequestHeader("X-Auth-User-ID") String userIdStr,
            @Valid @RequestBody ChangePasswordRequest request) {
        UUID userId = UUID.fromString(userIdStr);
        userManagementService.changePassword(userId, request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    @PostMapping("/force-change")
    public ResponseEntity<Map<String, String>> forceChangePassword(
            @RequestHeader("X-Auth-User-ID") String userIdStr,
            @Valid @RequestBody ForceChangePasswordRequest request) {
        UUID userId = UUID.fromString(userIdStr);
        userManagementService.forceChangePassword(userId, request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }
}
