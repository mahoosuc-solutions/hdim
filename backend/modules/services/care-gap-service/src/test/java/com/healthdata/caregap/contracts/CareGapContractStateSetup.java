package com.healthdata.caregap.contracts;

import com.healthdata.caregap.persistence.CareGapEntity;
import com.healthdata.caregap.persistence.CareGapRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * State setup helper for Care Gap Service contract tests.
 *
 * <p>Provides methods to set up care gap data for various provider states
 * required by consumer contract tests. Uses deterministic UUIDs to ensure
 * consistent test data across contract verification runs.
 *
 * <p>All care gap data is created with the test tenant ID to ensure proper
 * multi-tenant isolation during contract verification.
 *
 * <h2>HEDIS Quality Measures</h2>
 * <p>Test data includes common HEDIS quality measures:
 * <ul>
 *   <li>CDC-HBA1C - Hemoglobin A1c Testing for Diabetics</li>
 *   <li>BCS - Breast Cancer Screening</li>
 *   <li>COL - Colorectal Cancer Screening</li>
 * </ul>
 *
 * @see CareGapServiceProviderTest
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CareGapContractStateSetup {

    /**
     * Deterministic UUID for HBA1C care gap (FHIR-compliant string format).
     * This ID is referenced in consumer contracts and must remain stable.
     */
    public static final String CARE_GAP_HBA1C_ID = "550e8400-e29b-41d4-a716-446655440001";

    /**
     * Deterministic UUID for Breast Cancer Screening care gap.
     */
    public static final String CARE_GAP_BCS_ID = "550e8400-e29b-41d4-a716-446655440002";

    /**
     * Deterministic UUID for Colorectal Cancer Screening care gap.
     */
    public static final String CARE_GAP_COL_ID = "550e8400-e29b-41d4-a716-446655440003";

    /**
     * Patient ID for patient with open care gaps.
     * Matches the patient ID from Patient Service contract tests.
     */
    public static final String PATIENT_WITH_GAPS_ID = "550e8400-e29b-41d4-a716-446655440000";

    /**
     * Patient ID for patient with no care gaps.
     * Used to verify empty response scenarios.
     */
    public static final String PATIENT_NO_GAPS_ID = "00000000-0000-0000-0000-000000000000";

    /**
     * Test tenant ID for contract tests.
     * All test data is associated with this tenant.
     */
    public static final String TEST_TENANT_ID = "test-tenant-contracts";

    /**
     * HEDIS measure ID for Hemoglobin A1c Testing.
     */
    public static final String MEASURE_HBA1C = "CDC-HBA1C";

    /**
     * HEDIS measure ID for Breast Cancer Screening.
     */
    public static final String MEASURE_BCS = "BCS";

    /**
     * HEDIS measure ID for Colorectal Cancer Screening.
     */
    public static final String MEASURE_COL = "COL";

    private final CareGapRepository careGapRepository;

    /**
     * Sets up all open care gaps for the test patient.
     *
     * <p>Creates multiple care gaps covering different HEDIS measures:
     * <ul>
     *   <li>HBA1C - High priority diabetes screening gap</li>
     *   <li>BCS - Medium priority breast cancer screening gap</li>
     * </ul>
     */
    @Transactional
    public void setupOpenCareGaps() {
        log.info("Setting up open care gaps for patient: {}", PATIENT_WITH_GAPS_ID);
        setupHba1cGap();
        setupBcsGap();
    }

    /**
     * Sets up the HBA1C (Diabetes) care gap.
     *
     * <p>Creates a high-priority care gap for Hemoglobin A1c testing:
     * <ul>
     *   <li>ID: 550e8400-e29b-41d4-a716-446655440001</li>
     *   <li>Measure: CDC-HBA1C</li>
     *   <li>Status: OPEN</li>
     *   <li>Priority: high</li>
     *   <li>Due Date: 30 days from now</li>
     * </ul>
     *
     * @return the created care gap entity
     */
    @Transactional
    public CareGapEntity setupHba1cGap() {
        log.info("Setting up HBA1C care gap (ID: {})", CARE_GAP_HBA1C_ID);

        UUID gapId = UUID.fromString(CARE_GAP_HBA1C_ID);
        UUID patientId = UUID.fromString(PATIENT_WITH_GAPS_ID);

        // Check if already exists
        return careGapRepository.findByIdAndTenantId(gapId, TEST_TENANT_ID)
            .orElseGet(() -> {
                CareGapEntity gap = CareGapEntity.builder()
                    .id(gapId)
                    .tenantId(TEST_TENANT_ID)
                    .patientId(patientId)
                    .measureId(MEASURE_HBA1C)
                    .measureName("Hemoglobin A1c Testing for Diabetics")
                    .gapCategory("HEDIS")
                    .measureYear(LocalDate.now().getYear())
                    .gapType("preventive-care")
                    .gapStatus("OPEN")
                    .gapDescription("Patient has not had HbA1c test in the measurement period")
                    .gapReason("No HbA1c test result found in the last 12 months")
                    .priority("high")
                    .severity("high")
                    .starImpact(new BigDecimal("0.50"))
                    .riskScore(0.75)
                    .identifiedDate(Instant.now())
                    .dueDate(LocalDate.now().plusDays(30))
                    .recommendation("Schedule HbA1c blood test")
                    .recommendationType("screening")
                    .recommendedAction("Order HbA1c test and schedule follow-up appointment")
                    .cqlLibrary("DiabetesHbA1cScreening")
                    .cqlExpression("NeedsHbA1cTest")
                    .createdAt(Instant.now())
                    .createdBy("contract-test-setup")
                    .updatedAt(Instant.now())
                    .build();

                return careGapRepository.save(gap);
            });
    }

    /**
     * Sets up the BCS (Breast Cancer Screening) care gap.
     *
     * <p>Creates a medium-priority care gap for breast cancer screening:
     * <ul>
     *   <li>ID: 550e8400-e29b-41d4-a716-446655440002</li>
     *   <li>Measure: BCS</li>
     *   <li>Status: OPEN</li>
     *   <li>Priority: medium</li>
     *   <li>Due Date: 60 days from now</li>
     * </ul>
     *
     * @return the created care gap entity
     */
    @Transactional
    public CareGapEntity setupBcsGap() {
        log.info("Setting up BCS care gap (ID: {})", CARE_GAP_BCS_ID);

        UUID gapId = UUID.fromString(CARE_GAP_BCS_ID);
        UUID patientId = UUID.fromString(PATIENT_WITH_GAPS_ID);

        // Check if already exists
        return careGapRepository.findByIdAndTenantId(gapId, TEST_TENANT_ID)
            .orElseGet(() -> {
                CareGapEntity gap = CareGapEntity.builder()
                    .id(gapId)
                    .tenantId(TEST_TENANT_ID)
                    .patientId(patientId)
                    .measureId(MEASURE_BCS)
                    .measureName("Breast Cancer Screening")
                    .gapCategory("HEDIS")
                    .measureYear(LocalDate.now().getYear())
                    .gapType("preventive-care")
                    .gapStatus("OPEN")
                    .gapDescription("Patient is overdue for mammogram screening")
                    .gapReason("No mammogram found in the last 2 years")
                    .priority("medium")
                    .severity("medium")
                    .starImpact(new BigDecimal("0.35"))
                    .riskScore(0.50)
                    .identifiedDate(Instant.now())
                    .dueDate(LocalDate.now().plusDays(60))
                    .recommendation("Schedule mammogram screening")
                    .recommendationType("screening")
                    .recommendedAction("Order screening mammogram and coordinate with imaging center")
                    .cqlLibrary("BreastCancerScreening")
                    .cqlExpression("NeedsMammogram")
                    .createdAt(Instant.now())
                    .createdBy("contract-test-setup")
                    .updatedAt(Instant.now())
                    .build();

                return careGapRepository.save(gap);
            });
    }

    /**
     * Sets up a care gap that can be closed (for state change testing).
     *
     * <p>Creates a colorectal cancer screening care gap that tests
     * can use to verify gap closure functionality:
     * <ul>
     *   <li>ID: 550e8400-e29b-41d4-a716-446655440003</li>
     *   <li>Measure: COL</li>
     *   <li>Status: OPEN</li>
     *   <li>Priority: medium</li>
     * </ul>
     *
     * @return the created care gap entity
     */
    @Transactional
    public CareGapEntity setupClosableCareGap() {
        log.info("Setting up closable care gap (ID: {})", CARE_GAP_COL_ID);

        UUID gapId = UUID.fromString(CARE_GAP_COL_ID);
        UUID patientId = UUID.fromString(PATIENT_WITH_GAPS_ID);

        // Check if already exists
        return careGapRepository.findByIdAndTenantId(gapId, TEST_TENANT_ID)
            .orElseGet(() -> {
                CareGapEntity gap = CareGapEntity.builder()
                    .id(gapId)
                    .tenantId(TEST_TENANT_ID)
                    .patientId(patientId)
                    .measureId(MEASURE_COL)
                    .measureName("Colorectal Cancer Screening")
                    .gapCategory("HEDIS")
                    .measureYear(LocalDate.now().getYear())
                    .gapType("preventive-care")
                    .gapStatus("OPEN")
                    .gapDescription("Patient is overdue for colorectal cancer screening")
                    .gapReason("No colonoscopy or FIT test found in the required timeframe")
                    .priority("medium")
                    .severity("medium")
                    .starImpact(new BigDecimal("0.40"))
                    .riskScore(0.55)
                    .identifiedDate(Instant.now())
                    .dueDate(LocalDate.now().plusDays(90))
                    .recommendation("Schedule colorectal cancer screening")
                    .recommendationType("screening")
                    .recommendedAction("Order colonoscopy or FIT test per patient preference")
                    .cqlLibrary("ColorectalCancerScreening")
                    .cqlExpression("NeedsColorectalScreening")
                    .createdAt(Instant.now())
                    .createdBy("contract-test-setup")
                    .updatedAt(Instant.now())
                    .build();

                return careGapRepository.save(gap);
            });
    }

    /**
     * Cleans up all test data created during contract verification.
     *
     * <p>Removes all care gaps associated with the test tenant ID.
     * This should be called after each test to ensure clean state.
     */
    @Transactional
    public void cleanupTestData() {
        log.info("Cleaning up contract test data for tenant: {}", TEST_TENANT_ID);

        // Delete specific test care gaps by ID
        UUID hba1cId = UUID.fromString(CARE_GAP_HBA1C_ID);
        UUID bcsId = UUID.fromString(CARE_GAP_BCS_ID);
        UUID colId = UUID.fromString(CARE_GAP_COL_ID);

        careGapRepository.findByIdAndTenantId(hba1cId, TEST_TENANT_ID)
            .ifPresent(careGapRepository::delete);

        careGapRepository.findByIdAndTenantId(bcsId, TEST_TENANT_ID)
            .ifPresent(careGapRepository::delete);

        careGapRepository.findByIdAndTenantId(colId, TEST_TENANT_ID)
            .ifPresent(careGapRepository::delete);

        log.info("Contract test data cleanup completed");
    }
}
