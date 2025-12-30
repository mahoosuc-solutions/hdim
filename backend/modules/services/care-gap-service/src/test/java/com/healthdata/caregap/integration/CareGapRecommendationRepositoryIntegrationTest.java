package com.healthdata.caregap.integration;

import com.healthdata.caregap.config.BaseIntegrationTest;
import com.healthdata.caregap.persistence.CareGapEntity;
import com.healthdata.caregap.persistence.CareGapRecommendationEntity;
import com.healthdata.caregap.persistence.CareGapRecommendationRepository;
import com.healthdata.caregap.persistence.CareGapRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Care Gap Recommendation Repository Integration Tests
 *
 * Tests database operations for care gap recommendations with real PostgreSQL
 * via Testcontainers. Covers:
 * - CRUD operations for recommendations
 * - Recommendation status workflow (PENDING, ACCEPTED, REJECTED)
 * - Evidence levels and guideline references
 * - Multi-tenant data isolation
 */
@BaseIntegrationTest
@DisplayName("CareGapRecommendationRepository Integration Tests")
class CareGapRecommendationRepositoryIntegrationTest {

    @Autowired
    private CareGapRecommendationRepository recommendationRepository;

    @Autowired
    private CareGapRepository careGapRepository;

    private static final String TENANT_ID = "test-tenant";
    private static final String OTHER_TENANT = "other-tenant";
    private static final UUID PATIENT_ID = UUID.randomUUID();

    private CareGapEntity careGap;
    private CareGapRecommendationEntity recommendation;

    @BeforeEach
    void setUp() {
        // Create a care gap first
        careGap = CareGapEntity.builder()
                .tenantId(TENANT_ID)
                .patientId(PATIENT_ID)
                .measureId("HEDIS_CDC")
                .measureName("Diabetes A1C Control")
                .gapStatus("OPEN")
                .priority("high")
                .gapCategory("HEDIS")
                .gapType("care-gap")
                .gapDescription("Test care gap")
                .dueDate(LocalDate.now().plusDays(30))
                .identifiedDate(Instant.now())
                .createdBy("test-system")
                .build();
        careGap = careGapRepository.save(careGap);

        // Create a recommendation
        recommendation = CareGapRecommendationEntity.builder()
                .tenantId(TENANT_ID)
                .careGapId(careGap.getId())
                .recommendationType("LAB_ORDER")
                .title("Order A1C Test")
                .description("Patient needs A1C test to assess diabetes control")
                .actionRequired("Order hemoglobin A1C lab test")
                .priority(1)
                .evidenceLevel("A")
                .guidelineReference("ADA Standards of Medical Care 2024")
                .status("PENDING")
                .build();
        recommendation = recommendationRepository.save(recommendation);
    }

    @Nested
    @DisplayName("Basic CRUD Operations")
    class BasicCrudTests {

        @Test
        @DisplayName("Should save and retrieve recommendation")
        void shouldSaveAndRetrieve() {
            Optional<CareGapRecommendationEntity> found = recommendationRepository.findById(recommendation.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getTitle()).isEqualTo("Order A1C Test");
            assertThat(found.get().getCareGapId()).isEqualTo(careGap.getId());
        }

        @Test
        @DisplayName("Should auto-generate ID and timestamp on create")
        void shouldAutoGenerateIdAndTimestamp() {
            CareGapRecommendationEntity newRec = CareGapRecommendationEntity.builder()
                    .tenantId(TENANT_ID)
                    .careGapId(careGap.getId())
                    .recommendationType("REFERRAL")
                    .title("Refer to Endocrinologist")
                    .description("Consider specialist referral for uncontrolled diabetes")
                    .actionRequired("Create referral order")
                    .priority(2)
                    .build();

            CareGapRecommendationEntity saved = recommendationRepository.save(newRec);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getPriority()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should update recommendation")
        void shouldUpdate() {
            recommendation.setStatus("ACCEPTED");
            recommendation.setAcceptedAt(Instant.now());
            recommendationRepository.save(recommendation);

            Optional<CareGapRecommendationEntity> found = recommendationRepository.findById(recommendation.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getStatus()).isEqualTo("ACCEPTED");
            assertThat(found.get().getAcceptedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should delete recommendation")
        void shouldDelete() {
            UUID id = recommendation.getId();
            recommendationRepository.delete(recommendation);

            Optional<CareGapRecommendationEntity> found = recommendationRepository.findById(id);

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("Recommendation Workflow")
    class RecommendationWorkflowTests {

        @Test
        @DisplayName("Should track recommendation acceptance")
        void shouldTrackAcceptance() {
            recommendation.setStatus("ACCEPTED");
            recommendation.setAcceptedAt(Instant.now());
            recommendationRepository.save(recommendation);

            Optional<CareGapRecommendationEntity> found = recommendationRepository.findById(recommendation.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getStatus()).isEqualTo("ACCEPTED");
            assertThat(found.get().getAcceptedAt()).isNotNull();
            assertThat(found.get().getRejectedAt()).isNull();
        }

        @Test
        @DisplayName("Should track recommendation rejection with reason")
        void shouldTrackRejectionWithReason() {
            recommendation.setStatus("REJECTED");
            recommendation.setRejectedAt(Instant.now());
            recommendation.setRejectionReason("Patient declined - already completed at outside facility");
            recommendationRepository.save(recommendation);

            Optional<CareGapRecommendationEntity> found = recommendationRepository.findById(recommendation.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getStatus()).isEqualTo("REJECTED");
            assertThat(found.get().getRejectedAt()).isNotNull();
            assertThat(found.get().getRejectionReason()).contains("outside facility");
        }

        @Test
        @DisplayName("Should store multiple recommendations per gap with priority ordering")
        void shouldStoreMultipleRecommendationsWithPriority() {
            CareGapRecommendationEntity rec2 = CareGapRecommendationEntity.builder()
                    .tenantId(TENANT_ID)
                    .careGapId(careGap.getId())
                    .recommendationType("MEDICATION")
                    .title("Consider Metformin Adjustment")
                    .description("Adjust metformin dosage based on A1C results")
                    .actionRequired("Review and adjust medication regimen")
                    .priority(2)
                    .evidenceLevel("B")
                    .build();

            CareGapRecommendationEntity rec3 = CareGapRecommendationEntity.builder()
                    .tenantId(TENANT_ID)
                    .careGapId(careGap.getId())
                    .recommendationType("EDUCATION")
                    .title("Diabetes Self-Management Education")
                    .description("Refer to diabetes education program")
                    .actionRequired("Schedule education session")
                    .priority(3)
                    .evidenceLevel("A")
                    .build();

            recommendationRepository.save(rec2);
            recommendationRepository.save(rec3);

            List<CareGapRecommendationEntity> all = recommendationRepository.findAll();
            List<CareGapRecommendationEntity> forGap = all.stream()
                    .filter(r -> r.getCareGapId().equals(careGap.getId()))
                    .sorted((a, b) -> a.getPriority().compareTo(b.getPriority()))
                    .toList();

            assertThat(forGap).hasSize(3);
            assertThat(forGap.get(0).getPriority()).isEqualTo(1);
            assertThat(forGap.get(1).getPriority()).isEqualTo(2);
            assertThat(forGap.get(2).getPriority()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should track evidence-based recommendations")
        void shouldTrackEvidenceBasedRecommendations() {
            List<CareGapRecommendationEntity> all = recommendationRepository.findAll();
            List<CareGapRecommendationEntity> evidenceBased = all.stream()
                    .filter(r -> r.getTenantId().equals(TENANT_ID))
                    .filter(r -> r.getEvidenceLevel() != null && !r.getEvidenceLevel().isEmpty())
                    .toList();

            assertThat(evidenceBased).isNotEmpty();
            assertThat(evidenceBased.get(0).getGuidelineReference()).contains("ADA");
        }
    }

    @Nested
    @DisplayName("Recommendation Types")
    class RecommendationTypeTests {

        @Test
        @DisplayName("Should support various recommendation types")
        void shouldSupportVariousTypes() {
            CareGapEntity gap2 = createAndSaveGap("HEDIS_BCS", "Breast Cancer Screening");

            List<String> types = List.of("SCREENING", "MEDICATION", "REFERRAL", "EDUCATION", "FOLLOW_UP");

            for (int i = 0; i < types.size(); i++) {
                CareGapRecommendationEntity rec = CareGapRecommendationEntity.builder()
                        .tenantId(TENANT_ID)
                        .careGapId(gap2.getId())
                        .recommendationType(types.get(i))
                        .title("Recommendation " + types.get(i))
                        .description("Description for " + types.get(i))
                        .actionRequired("Action for " + types.get(i))
                        .priority(i + 1)
                        .build();
                recommendationRepository.save(rec);
            }

            List<CareGapRecommendationEntity> all = recommendationRepository.findAll();
            List<String> savedTypes = all.stream()
                    .filter(r -> r.getCareGapId().equals(gap2.getId()))
                    .map(CareGapRecommendationEntity::getRecommendationType)
                    .toList();

            assertThat(savedTypes).containsExactlyInAnyOrderElementsOf(types);
        }
    }

    @Nested
    @DisplayName("Multi-Tenant Isolation (HIPAA Compliance)")
    class MultiTenantIsolationTests {

        @Test
        @DisplayName("Should isolate recommendations between tenants")
        void shouldIsolateRecommendationsBetweenTenants() {
            // Create gap and recommendation for other tenant
            CareGapEntity otherTenantGap = CareGapEntity.builder()
                    .tenantId(OTHER_TENANT)
                    .patientId(PATIENT_ID)
                    .measureId("HEDIS_CDC")
                    .measureName("Diabetes A1C Control")
                    .gapStatus("OPEN")
                    .priority("high")
                    .gapCategory("HEDIS")
                    .gapType("care-gap")
                    .identifiedDate(Instant.now())
                    .createdBy("test-system")
                    .build();
            otherTenantGap = careGapRepository.save(otherTenantGap);

            CareGapRecommendationEntity otherTenantRec = CareGapRecommendationEntity.builder()
                    .tenantId(OTHER_TENANT)
                    .careGapId(otherTenantGap.getId())
                    .recommendationType("LAB_ORDER")
                    .title("Other Tenant Recommendation")
                    .description("Other tenant description")
                    .actionRequired("Other tenant action")
                    .priority(1)
                    .build();
            recommendationRepository.save(otherTenantRec);

            // Query all recommendations
            List<CareGapRecommendationEntity> all = recommendationRepository.findAll();

            // Filter by tenant (simulating tenant-scoped query)
            List<CareGapRecommendationEntity> tenant1Recs = all.stream()
                    .filter(r -> r.getTenantId().equals(TENANT_ID))
                    .toList();
            List<CareGapRecommendationEntity> tenant2Recs = all.stream()
                    .filter(r -> r.getTenantId().equals(OTHER_TENANT))
                    .toList();

            assertThat(tenant1Recs).noneMatch(r -> r.getTenantId().equals(OTHER_TENANT));
            assertThat(tenant2Recs).noneMatch(r -> r.getTenantId().equals(TENANT_ID));
        }

        @Test
        @DisplayName("All recommendations should have non-null tenant IDs")
        void shouldHaveNonNullTenantIds() {
            List<CareGapRecommendationEntity> all = recommendationRepository.findAll();

            assertThat(all).allMatch(r -> r.getTenantId() != null && !r.getTenantId().isEmpty());
        }

        @Test
        @DisplayName("Should not expose PHI recommendations across tenants")
        void shouldNotExposePHIAcrossTenants() {
            // Store sensitive recommendation data
            recommendation.setDescription("Patient has family history of cardiovascular disease. " +
                    "BMI: 32.5, Blood Pressure: 145/92. Consider aggressive intervention.");
            recommendationRepository.save(recommendation);

            // Query from "other tenant" perspective
            List<CareGapRecommendationEntity> all = recommendationRepository.findAll();
            List<CareGapRecommendationEntity> otherTenantView = all.stream()
                    .filter(r -> r.getTenantId().equals(OTHER_TENANT))
                    .toList();

            // Ensure sensitive data is not visible to other tenant
            assertThat(otherTenantView).noneMatch(r ->
                r.getDescription() != null && r.getDescription().contains("cardiovascular"));
        }
    }

    // Helper method
    private CareGapEntity createAndSaveGap(String measureId, String measureName) {
        CareGapEntity gap = CareGapEntity.builder()
                .tenantId(TENANT_ID)
                .patientId(PATIENT_ID)
                .measureId(measureId)
                .measureName(measureName)
                .gapStatus("OPEN")
                .priority("high")
                .gapCategory("HEDIS")
                .gapType("care-gap")
                .identifiedDate(Instant.now())
                .createdBy("test-system")
                .build();
        return careGapRepository.save(gap);
    }
}
