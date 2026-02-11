package com.healthdata.agentvalidation.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.healthdata.agentvalidation.domain.enums.TestStatus;
import com.healthdata.agentvalidation.domain.enums.UserStoryType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a collection of test cases for a specific user story.
 * Test suites group related test scenarios that validate agent behavior
 * for a particular role and use case.
 */
@Entity
@Table(name = "test_suites", indexes = {
    @Index(name = "idx_test_suites_tenant", columnList = "tenant_id"),
    @Index(name = "idx_test_suites_user_story_type", columnList = "user_story_type"),
    @Index(name = "idx_test_suites_target_role", columnList = "target_role"),
    @Index(name = "idx_test_suites_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestSuite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_story_type", nullable = false)
    private UserStoryType userStoryType;

    @Column(name = "target_role", nullable = false)
    private String targetRole;

    @Column(name = "agent_type", nullable = false)
    private String agentType;

    @Column(name = "pass_threshold", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal passThreshold = new BigDecimal("0.80");

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TestStatus status = TestStatus.PENDING;

    @Column(name = "last_execution_at")
    private Instant lastExecutionAt;

    @Column(name = "last_pass_rate", precision = 5, scale = 2)
    private BigDecimal lastPassRate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @OneToMany(mappedBy = "testSuite", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore  // Prevent lazy loading issues during serialization; use dedicated endpoints for test cases
    @Builder.Default
    private List<TestCase> testCases = new ArrayList<>();

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    @Column(name = "version")
    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Adds a test case to this suite.
     */
    public void addTestCase(TestCase testCase) {
        testCases.add(testCase);
        testCase.setTestSuite(this);
    }

    /**
     * Removes a test case from this suite.
     */
    public void removeTestCase(TestCase testCase) {
        testCases.remove(testCase);
        testCase.setTestSuite(null);
    }

    /**
     * Calculate the pass rate from test case results.
     */
    public BigDecimal calculatePassRate() {
        if (testCases.isEmpty()) {
            return BigDecimal.ZERO;
        }
        long passedCount = testCases.stream()
            .filter(tc -> tc.getStatus() == TestStatus.PASSED)
            .count();
        return BigDecimal.valueOf(passedCount)
            .divide(BigDecimal.valueOf(testCases.size()), 2, java.math.RoundingMode.HALF_UP);
    }
}
