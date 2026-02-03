package com.healthdata.agentvalidation.repository;

import com.healthdata.agentvalidation.domain.entity.TestSuite;
import com.healthdata.agentvalidation.domain.enums.TestStatus;
import com.healthdata.agentvalidation.domain.enums.UserStoryType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for TestSuiteRepository.
 * Tests JPA repository operations with H2 in-memory database.
 */
@Tag("integration")
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Test Suite Repository Integration Tests")
class TestSuiteRepositoryIntegrationTest {

    @Autowired
    private TestSuiteRepository testSuiteRepository;

    private static final String TENANT_ID = "test-tenant";
    private static final String OTHER_TENANT_ID = "other-tenant";

    @BeforeEach
    void setUp() {
        testSuiteRepository.deleteAll();
    }

    @Nested
    @DisplayName("Save and Find Operations")
    class SaveAndFindTests {

        @Test
        @DisplayName("Should save and retrieve test suite by ID and tenant")
        void shouldSaveAndRetrieveTestSuite() {
            // Given
            TestSuite suite = createTestSuite("Patient Summary Suite", UserStoryType.PATIENT_SUMMARY_REVIEW);

            // When
            TestSuite saved = testSuiteRepository.save(suite);
            Optional<TestSuite> found = testSuiteRepository.findByIdAndTenantId(saved.getId(), TENANT_ID);

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("Patient Summary Suite");
            assertThat(found.get().getTenantId()).isEqualTo(TENANT_ID);
            assertThat(found.get().getUserStoryType()).isEqualTo(UserStoryType.PATIENT_SUMMARY_REVIEW);
        }

        @Test
        @DisplayName("Should not find suite with wrong tenant ID")
        void shouldNotFindSuiteWithWrongTenant() {
            // Given
            TestSuite suite = createTestSuite("Tenant Isolated Suite", UserStoryType.CARE_GAP_EVALUATION);
            TestSuite saved = testSuiteRepository.save(suite);

            // When
            Optional<TestSuite> found = testSuiteRepository.findByIdAndTenantId(saved.getId(), OTHER_TENANT_ID);

            // Then
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Should return empty when suite ID does not exist")
        void shouldReturnEmptyWhenSuiteNotFound() {
            // When
            Optional<TestSuite> found = testSuiteRepository.findByIdAndTenantId(UUID.randomUUID(), TENANT_ID);

            // Then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("Query By Tenant Operations")
    class QueryByTenantTests {

        @Test
        @DisplayName("Should find all suites by tenant with pagination")
        void shouldFindAllSuitesByTenantPaginated() {
            // Given
            for (int i = 0; i < 5; i++) {
                testSuiteRepository.save(createTestSuite("Suite " + i, UserStoryType.PATIENT_SUMMARY_REVIEW));
            }

            // When
            Page<TestSuite> page = testSuiteRepository.findByTenantId(TENANT_ID, PageRequest.of(0, 3));

            // Then
            assertThat(page.getContent()).hasSize(3);
            assertThat(page.getTotalElements()).isEqualTo(5);
            assertThat(page.getTotalPages()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should find suites by tenant and user story type")
        void shouldFindSuitesByTenantAndUserStoryType() {
            // Given
            testSuiteRepository.save(createTestSuite("Patient Summary", UserStoryType.PATIENT_SUMMARY_REVIEW));
            testSuiteRepository.save(createTestSuite("Care Gap", UserStoryType.CARE_GAP_EVALUATION));
            testSuiteRepository.save(createTestSuite("Another Patient Summary", UserStoryType.PATIENT_SUMMARY_REVIEW));

            // When
            List<TestSuite> found = testSuiteRepository.findByTenantIdAndUserStoryType(
                TENANT_ID, UserStoryType.PATIENT_SUMMARY_REVIEW);

            // Then
            assertThat(found).hasSize(2);
            assertThat(found).allMatch(s -> s.getUserStoryType() == UserStoryType.PATIENT_SUMMARY_REVIEW);
        }

        @Test
        @DisplayName("Should find suites by tenant and target role")
        void shouldFindSuitesByTenantAndTargetRole() {
            // Given
            TestSuite clinicianSuite = createTestSuite("Clinician Suite", UserStoryType.PATIENT_SUMMARY_REVIEW);
            clinicianSuite.setTargetRole("CLINICIAN");
            testSuiteRepository.save(clinicianSuite);

            TestSuite qualityOfficerSuite = createTestSuite("QO Suite", UserStoryType.HEDIS_MEASURE_EVALUATION);
            qualityOfficerSuite.setTargetRole("QUALITY_OFFICER");
            testSuiteRepository.save(qualityOfficerSuite);

            // When
            List<TestSuite> found = testSuiteRepository.findByTenantIdAndTargetRole(TENANT_ID, "CLINICIAN");

            // Then
            assertThat(found).hasSize(1);
            assertThat(found.get(0).getTargetRole()).isEqualTo("CLINICIAN");
        }
    }

    @Nested
    @DisplayName("Active and Status Queries")
    class ActiveAndStatusTests {

        @Test
        @DisplayName("Should find only active suites")
        void shouldFindOnlyActiveSuites() {
            // Given
            TestSuite activeSuite = createTestSuite("Active Suite", UserStoryType.PATIENT_SUMMARY_REVIEW);
            activeSuite.setActive(true);
            testSuiteRepository.save(activeSuite);

            TestSuite inactiveSuite = createTestSuite("Inactive Suite", UserStoryType.CARE_GAP_EVALUATION);
            inactiveSuite.setActive(false);
            testSuiteRepository.save(inactiveSuite);

            // When
            List<TestSuite> found = testSuiteRepository.findByTenantIdAndActiveTrue(TENANT_ID);

            // Then
            assertThat(found).hasSize(1);
            assertThat(found.get(0).getName()).isEqualTo("Active Suite");
        }

        @Test
        @DisplayName("Should find failing test suites")
        void shouldFindFailingTestSuites() {
            // Given - The findFailingTestSuites query checks lastPassRate < passThreshold
            TestSuite passingSuite = createTestSuite("Passing Suite", UserStoryType.PATIENT_SUMMARY_REVIEW);
            passingSuite.setPassThreshold(new BigDecimal("0.80"));
            passingSuite.setLastPassRate(new BigDecimal("0.90")); // Above threshold = passing
            testSuiteRepository.save(passingSuite);

            TestSuite failingSuite = createTestSuite("Failing Suite", UserStoryType.CARE_GAP_EVALUATION);
            failingSuite.setPassThreshold(new BigDecimal("0.80"));
            failingSuite.setLastPassRate(new BigDecimal("0.50")); // Below threshold = failing
            testSuiteRepository.save(failingSuite);

            // When
            List<TestSuite> found = testSuiteRepository.findFailingTestSuites(TENANT_ID);

            // Then
            assertThat(found).hasSize(1);
            assertThat(found.get(0).getName()).isEqualTo("Failing Suite");
            assertThat(found.get(0).getLastPassRate()).isLessThan(found.get(0).getPassThreshold());
        }
    }

    @Nested
    @DisplayName("Aggregation Queries")
    class AggregationTests {

        @Test
        @DisplayName("Should count suites by status for tenant")
        void shouldCountSuitesByStatusForTenant() {
            // Given
            TestSuite pending = createTestSuite("Pending", UserStoryType.PATIENT_SUMMARY_REVIEW);
            pending.setStatus(TestStatus.PENDING);
            testSuiteRepository.save(pending);

            TestSuite passed1 = createTestSuite("Passed 1", UserStoryType.CARE_GAP_EVALUATION);
            passed1.setStatus(TestStatus.PASSED);
            testSuiteRepository.save(passed1);

            TestSuite passed2 = createTestSuite("Passed 2", UserStoryType.LAB_INTERPRETATION);
            passed2.setStatus(TestStatus.PASSED);
            testSuiteRepository.save(passed2);

            TestSuite failed = createTestSuite("Failed", UserStoryType.MEDICATION_RECONCILIATION);
            failed.setStatus(TestStatus.FAILED);
            testSuiteRepository.save(failed);

            // When
            List<Object[]> counts = testSuiteRepository.countByStatusForTenant(TENANT_ID);

            // Then
            assertThat(counts).isNotEmpty();
            // Results should contain status counts
            long totalCount = counts.stream()
                .mapToLong(arr -> (Long) arr[1])
                .sum();
            assertThat(totalCount).isEqualTo(4);
        }
    }

    @Nested
    @DisplayName("Update Operations")
    class UpdateTests {

        @Test
        @DisplayName("Should update suite fields correctly")
        void shouldUpdateSuiteFields() {
            // Given
            TestSuite suite = createTestSuite("Original Name", UserStoryType.PATIENT_SUMMARY_REVIEW);
            TestSuite saved = testSuiteRepository.save(suite);

            // When
            saved.setName("Updated Name");
            saved.setPassThreshold(new BigDecimal("0.90"));
            saved.setStatus(TestStatus.PASSED);
            saved.setLastExecutionAt(Instant.now());
            testSuiteRepository.save(saved);

            // Then
            Optional<TestSuite> updated = testSuiteRepository.findByIdAndTenantId(saved.getId(), TENANT_ID);
            assertThat(updated).isPresent();
            assertThat(updated.get().getName()).isEqualTo("Updated Name");
            assertThat(updated.get().getPassThreshold()).isEqualByComparingTo(new BigDecimal("0.90"));
            assertThat(updated.get().getStatus()).isEqualTo(TestStatus.PASSED);
            assertThat(updated.get().getLastExecutionAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Delete Operations")
    class DeleteTests {

        @Test
        @DisplayName("Should delete suite correctly")
        void shouldDeleteSuite() {
            // Given
            TestSuite suite = createTestSuite("To Delete", UserStoryType.PATIENT_SUMMARY_REVIEW);
            TestSuite saved = testSuiteRepository.save(suite);
            UUID suiteId = saved.getId();

            // When
            testSuiteRepository.delete(saved);

            // Then
            Optional<TestSuite> found = testSuiteRepository.findByIdAndTenantId(suiteId, TENANT_ID);
            assertThat(found).isEmpty();
        }
    }

    // Helper method
    private TestSuite createTestSuite(String name, UserStoryType userStoryType) {
        return TestSuite.builder()
            .tenantId(TENANT_ID)
            .name(name)
            .description("Test suite for " + name)
            .userStoryType(userStoryType)
            .targetRole(userStoryType.getTargetRole())
            .agentType("clinical-decision")
            .passThreshold(new BigDecimal("0.80"))
            .status(TestStatus.PENDING)
            .active(true)
            .createdBy("test-user")
            .build();
    }
}
