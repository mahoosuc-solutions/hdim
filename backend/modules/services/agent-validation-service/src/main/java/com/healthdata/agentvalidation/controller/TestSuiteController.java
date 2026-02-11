package com.healthdata.agentvalidation.controller;

import com.healthdata.agentvalidation.domain.entity.TestSuite;
import com.healthdata.agentvalidation.domain.enums.TestStatus;
import com.healthdata.agentvalidation.domain.enums.UserStoryType;
import com.healthdata.agentvalidation.repository.TestSuiteRepository;
import com.healthdata.agentvalidation.service.TestOrchestratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing test suites.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/validation/suites")
@RequiredArgsConstructor
@Tag(name = "Test Suites", description = "Manage AI agent validation test suites")
public class TestSuiteController {

    private final TestSuiteRepository testSuiteRepository;
    private final TestOrchestratorService testOrchestratorService;

    @Operation(summary = "Create a new test suite")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_OFFICER')")
    public ResponseEntity<TestSuite> createTestSuite(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-User-ID") String userId,
            @Valid @RequestBody CreateTestSuiteRequest request) {

        log.info("Creating test suite: {} for tenant: {}", request.name(), tenantId);

        TestSuite suite = TestSuite.builder()
            .tenantId(tenantId)
            .name(request.name())
            .description(request.description())
            .userStoryType(request.userStoryType())
            .targetRole(request.targetRole())
            .agentType(request.agentType())
            .passThreshold(request.passThreshold())
            .createdBy(userId)
            .build();

        TestSuite saved = testSuiteRepository.save(suite);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @Operation(summary = "Get a test suite by ID")
    @GetMapping("/{suiteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_OFFICER', 'EVALUATOR')")
    public ResponseEntity<TestSuite> getTestSuite(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID suiteId) {

        return testSuiteRepository.findByIdAndTenantId(suiteId, tenantId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "List all test suites for tenant")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_OFFICER', 'EVALUATOR')")
    public ResponseEntity<Page<TestSuite>> listTestSuites(
            @RequestHeader("X-Tenant-ID") String tenantId,
            Pageable pageable) {

        return ResponseEntity.ok(testSuiteRepository.findByTenantId(tenantId, pageable));
    }

    @Operation(summary = "List test suites by user story type")
    @GetMapping("/by-story-type/{userStoryType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_OFFICER', 'EVALUATOR')")
    public ResponseEntity<List<TestSuite>> listByUserStoryType(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UserStoryType userStoryType) {

        return ResponseEntity.ok(
            testSuiteRepository.findByTenantIdAndUserStoryType(tenantId, userStoryType));
    }

    @Operation(summary = "List test suites by target role")
    @GetMapping("/by-role/{role}")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_OFFICER', 'EVALUATOR')")
    public ResponseEntity<List<TestSuite>> listByTargetRole(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String role) {

        return ResponseEntity.ok(
            testSuiteRepository.findByTenantIdAndTargetRole(tenantId, role));
    }

    @Operation(summary = "Update a test suite")
    @PutMapping("/{suiteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_OFFICER')")
    public ResponseEntity<TestSuite> updateTestSuite(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID suiteId,
            @Valid @RequestBody UpdateTestSuiteRequest request) {

        return testSuiteRepository.findByIdAndTenantId(suiteId, tenantId)
            .map(suite -> {
                if (request.name() != null) suite.setName(request.name());
                if (request.description() != null) suite.setDescription(request.description());
                if (request.passThreshold() != null) suite.setPassThreshold(request.passThreshold());
                if (request.active() != null) suite.setActive(request.active());
                return ResponseEntity.ok(testSuiteRepository.save(suite));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Delete a test suite")
    @DeleteMapping("/{suiteId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTestSuite(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID suiteId) {

        return testSuiteRepository.findByIdAndTenantId(suiteId, tenantId)
            .map(suite -> {
                testSuiteRepository.delete(suite);
                return ResponseEntity.noContent().<Void>build();
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Execute a test suite")
    @PostMapping("/{suiteId}/execute")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_OFFICER', 'EVALUATOR')")
    public ResponseEntity<TestOrchestratorService.TestSuiteExecutionResult> executeSuite(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-User-ID") String userId,
            @PathVariable UUID suiteId) {

        log.info("Executing test suite {} for tenant {}", suiteId, tenantId);

        TestOrchestratorService.TestSuiteExecutionResult result =
            testOrchestratorService.executeSuite(suiteId, tenantId, userId);

        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Get failing test suites")
    @GetMapping("/failing")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_OFFICER')")
    public ResponseEntity<List<TestSuite>> getFailingTestSuites(
            @RequestHeader("X-Tenant-ID") String tenantId) {

        return ResponseEntity.ok(testSuiteRepository.findFailingTestSuites(tenantId));
    }

    @Operation(summary = "Get test suite statistics")
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_OFFICER', 'EVALUATOR')")
    public ResponseEntity<TestSuiteStats> getTestSuiteStats(
            @RequestHeader("X-Tenant-ID") String tenantId) {

        List<Object[]> statusCounts = testSuiteRepository.countByStatusForTenant(tenantId);
        long total = testSuiteRepository.findByTenantIdAndActiveTrue(tenantId).size();
        long failing = testSuiteRepository.findFailingTestSuites(tenantId).size();

        return ResponseEntity.ok(new TestSuiteStats(total, failing, statusCounts));
    }

    // DTOs
    public record CreateTestSuiteRequest(
        String name,
        String description,
        UserStoryType userStoryType,
        String targetRole,
        String agentType,
        java.math.BigDecimal passThreshold
    ) {}

    public record UpdateTestSuiteRequest(
        String name,
        String description,
        java.math.BigDecimal passThreshold,
        Boolean active
    ) {}

    public record TestSuiteStats(
        long totalActive,
        long failing,
        List<Object[]> statusBreakdown
    ) {}
}
