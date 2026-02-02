package com.healthdata.caregap.contracts;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBrokerConsumerVersionSelectors;
import au.com.dius.pact.provider.junitsupport.loader.SelectorBuilder;
import com.healthdata.contracts.ContractTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static com.healthdata.caregap.contracts.CareGapContractStateSetup.CARE_GAP_BCS_ID;
import static com.healthdata.caregap.contracts.CareGapContractStateSetup.CARE_GAP_HBA1C_ID;
import static com.healthdata.caregap.contracts.CareGapContractStateSetup.PATIENT_NO_GAPS_ID;
import static com.healthdata.caregap.contracts.CareGapContractStateSetup.PATIENT_WITH_GAPS_ID;

/**
 * Pact provider verification test for Care Gap Service.
 *
 * <p>Verifies that Care Gap Service fulfills the contracts defined by its consumers:
 * <ul>
 *   <li>ClinicalPortal - Angular UI for care gap display and management</li>
 *   <li>QualityMeasureService - Quality measure evaluation requiring care gap data</li>
 *   <li>PatientService - Patient dashboard requiring care gap summary</li>
 * </ul>
 *
 * <p>This test retrieves contracts from the Pact Broker and verifies that the
 * Care Gap Service API responses match the expected format and content defined
 * in consumer contracts.
 *
 * <h2>Provider States</h2>
 * <p>Each {@code @State} method sets up the required test data for a specific
 * consumer expectation. States are referenced by name in consumer contracts.
 *
 * <h2>HEDIS Quality Measures</h2>
 * <p>Test data covers common HEDIS quality measures:
 * <ul>
 *   <li>CDC-HBA1C - Hemoglobin A1c Testing for Diabetics</li>
 *   <li>BCS - Breast Cancer Screening</li>
 *   <li>COL - Colorectal Cancer Screening</li>
 * </ul>
 *
 * <h2>Running Tests</h2>
 * <pre>
 * # Verify all contracts
 * ./gradlew :modules:services:care-gap-service:test --tests "*CareGapServiceProviderTest"
 *
 * # Verify specific consumer
 * ./gradlew :modules:services:care-gap-service:test --tests "*CareGapServiceProviderTest" \
 *   -Dpact.filter.consumers=ClinicalPortal
 * </pre>
 *
 * @see ContractTestBase
 * @see CareGapContractStateSetup
 */
@Provider("CareGapService")
@ExtendWith(SpringExtension.class)
public class CareGapServiceProviderTest extends ContractTestBase {

    @Autowired
    private CareGapContractStateSetup stateSetup;

    /**
     * Configures consumer version selectors for contract retrieval.
     *
     * <p>Retrieves contracts from:
     * <ul>
     *   <li>Main branch - production contracts</li>
     *   <li>Matching branch - PR branch contracts (when running in PR)</li>
     *   <li>Deployed or released versions - verified production contracts</li>
     * </ul>
     *
     * @return selector builder for consumer versions
     */
    @PactBrokerConsumerVersionSelectors
    public static SelectorBuilder consumerVersionSelectors() {
        return new SelectorBuilder()
            .mainBranch()
            .matchingBranch()
            .deployedOrReleased();
    }

    /**
     * Sets up the test target before each verification.
     *
     * @param context the Pact verification context
     */
    @BeforeEach
    void setupMocks(PactVerificationContext context) {
        super.setupTestTarget(context);
    }

    /**
     * Cleans up test data after each verification.
     */
    @AfterEach
    void cleanup() {
        stateSetup.cleanupTestData();
    }

    /**
     * Template method for Pact verification.
     *
     * <p>This method is called for each interaction defined in the consumer contracts.
     * The actual verification is performed by the Pact framework.
     *
     * @param context the Pact verification context
     */
    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyPact(PactVerificationContext context) {
        if (context != null) {
            context.verifyInteraction();
        }
    }

    // ==================== Provider States ====================

    /**
     * State: Open care gaps exist for a specific patient.
     *
     * <p>Used by consumers that need to retrieve care gaps for a patient.
     * Creates multiple open HEDIS care gaps (HBA1C, BCS) for the test patient.
     */
    @State("open care gaps exist for patient " + PATIENT_WITH_GAPS_ID)
    void openCareGapsExistForPatient() {
        stateSetup.setupOpenCareGaps();
    }

    /**
     * State: Care gap HBA1C exists.
     *
     * <p>Used by consumers that need to retrieve a specific HBA1C care gap.
     * Creates the Hemoglobin A1c testing care gap.
     */
    @State("care gap HBA1C exists")
    void careGapHba1cExists() {
        stateSetup.setupHba1cGap();
    }

    /**
     * State: Care gap with specific ID exists.
     *
     * <p>Used by consumers that look up care gaps by their unique identifier.
     * Creates the HBA1C care gap with the deterministic UUID.
     */
    @State("care gap exists with id " + CARE_GAP_HBA1C_ID)
    void careGapExistsWithId() {
        stateSetup.setupHba1cGap();
    }

    /**
     * State: Care gap BCS exists.
     *
     * <p>Used by consumers that need to retrieve a specific breast cancer screening gap.
     * Creates the Breast Cancer Screening care gap.
     */
    @State("care gap BCS exists")
    void careGapBcsExists() {
        stateSetup.setupBcsGap();
    }

    /**
     * State: Care gap BCS exists with specific ID.
     *
     * <p>Used by consumers that look up the BCS care gap by its unique identifier.
     */
    @State("care gap exists with id " + CARE_GAP_BCS_ID)
    void careGapBcsExistsWithId() {
        stateSetup.setupBcsGap();
    }

    /**
     * State: No care gaps exist for patient.
     *
     * <p>Used to verify empty list responses when a patient has no care gaps.
     * No data setup is needed - the patient simply has no gaps.
     */
    @State("no care gaps exist for patient " + PATIENT_NO_GAPS_ID)
    void noCareGapsExist() {
        // No setup needed - patient has no care gaps
    }

    /**
     * State: Care gap can be closed.
     *
     * <p>Used by consumers that need to test care gap closure functionality.
     * Creates a care gap that can be transitioned from OPEN to CLOSED status.
     */
    @State("care gap can be closed")
    void careGapCanBeClosed() {
        stateSetup.setupClosableCareGap();
    }

    /**
     * State: High priority care gaps exist.
     *
     * <p>Used by consumers that filter care gaps by priority.
     * Creates the HBA1C gap which has high priority.
     */
    @State("high priority care gaps exist")
    void highPriorityCareGapsExist() {
        stateSetup.setupHba1cGap();
    }

    /**
     * State: Care gaps exist for quality measure evaluation.
     *
     * <p>Used by QualityMeasureService to verify care gap data retrieval
     * during HEDIS measure evaluation. Creates all open care gaps.
     */
    @State("care gaps exist for quality measure evaluation")
    void careGapsExistForQualityMeasure() {
        stateSetup.setupOpenCareGaps();
    }

    /**
     * State: Multiple care gaps exist for patient.
     *
     * <p>Used by consumers that need to test pagination or list functionality.
     * Creates multiple care gaps covering different HEDIS measures.
     */
    @State("multiple care gaps exist for patient")
    void multipleCareGapsExist() {
        stateSetup.setupOpenCareGaps();
        stateSetup.setupClosableCareGap();
    }

    /**
     * State: HEDIS care gaps exist.
     *
     * <p>Used by consumers that filter care gaps by category (HEDIS).
     * Creates all HEDIS measure care gaps.
     */
    @State("HEDIS care gaps exist")
    void hedisCareGapsExist() {
        stateSetup.setupOpenCareGaps();
    }

    /**
     * State: Care gap summary data exists.
     *
     * <p>Used by consumers that request care gap summary/dashboard data.
     * Creates multiple care gaps of different priorities and measures.
     */
    @State("care gap summary data exists")
    void careGapSummaryDataExists() {
        stateSetup.setupOpenCareGaps();
        stateSetup.setupClosableCareGap();
    }
}
