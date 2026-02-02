package com.healthdata.patient.contracts;

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

import static com.healthdata.patient.contracts.PatientContractStateSetup.PATIENT_JOHN_DOE_ID;
import static com.healthdata.patient.contracts.PatientContractStateSetup.TEST_MRN_JOHN_DOE;

/**
 * Pact provider verification test for Patient Service.
 *
 * <p>Verifies that Patient Service fulfills the contracts defined by its consumers:
 * <ul>
 *   <li>ClinicalPortal - Angular UI for patient data display</li>
 *   <li>CareGapService - Care gap detection requiring patient lookup</li>
 *   <li>QualityMeasureService - Quality measure evaluation requiring patient data</li>
 * </ul>
 *
 * <p>This test retrieves contracts from the Pact Broker and verifies that the
 * Patient Service API responses match the expected format and content defined
 * in consumer contracts.
 *
 * <h2>Provider States</h2>
 * <p>Each {@code @State} method sets up the required test data for a specific
 * consumer expectation. States are referenced by name in consumer contracts.
 *
 * <h2>Running Tests</h2>
 * <pre>
 * # Verify all contracts
 * ./gradlew :modules:services:patient-service:test --tests "*PatientServiceProviderTest"
 *
 * # Verify specific consumer
 * ./gradlew :modules:services:patient-service:test --tests "*PatientServiceProviderTest" \
 *   -Dpact.filter.consumers=ClinicalPortal
 * </pre>
 *
 * @see ContractTestBase
 * @see PatientContractStateSetup
 */
@Provider("PatientService")
@ExtendWith(SpringExtension.class)
public class PatientServiceProviderTest extends ContractTestBase {

    @Autowired
    private PatientContractStateSetup stateSetup;

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
     * State: Patient exists with specific UUID.
     *
     * <p>Used by consumers that need to retrieve a patient by their unique identifier.
     * Creates John Doe with the deterministic UUID for consistent verification.
     */
    @State("patient exists with id " + PATIENT_JOHN_DOE_ID)
    void patientJohnDoeExists() {
        stateSetup.setupPatientJohnDoe();
    }

    /**
     * State: Patient John Doe exists (by name reference).
     *
     * <p>Alternative state description for consumers that reference patients by name.
     * Creates the same John Doe patient as {@link #patientJohnDoeExists()}.
     */
    @State("patient John Doe exists")
    void patientJohnDoeExistsByName() {
        stateSetup.setupPatientJohnDoe();
    }

    /**
     * State: Patient exists with specific MRN.
     *
     * <p>Used by consumers that look up patients by Medical Record Number.
     * Creates John Doe with MRN-12345.
     */
    @State("patient exists with MRN " + TEST_MRN_JOHN_DOE)
    void patientExistsWithMrn() {
        stateSetup.setupPatientJohnDoe();
    }

    /**
     * State: No patient exists with given ID.
     *
     * <p>Used to verify 404 Not Found responses when a patient doesn't exist.
     * No data setup is needed - the patient simply doesn't exist.
     */
    @State("no patient exists with id 00000000-0000-0000-0000-000000000000")
    void noPatientExists() {
        // No setup needed - patient doesn't exist
    }

    /**
     * State: Multiple patients exist.
     *
     * <p>Used by consumers that need to test list/search functionality.
     * Creates both John Doe and Jane Smith patients.
     */
    @State("multiple patients exist")
    void multiplePatientsExist() {
        stateSetup.setupPatientJohnDoe();
        stateSetup.setupPatientJaneSmith();
    }

    /**
     * State: Patient exists for care gap lookup.
     *
     * <p>Used by CareGapService to verify patient data retrieval during
     * care gap identification. Creates John Doe as the target patient.
     */
    @State("patient exists for care gap lookup")
    void patientExistsForCareGap() {
        stateSetup.setupPatientJohnDoe();
    }

    /**
     * State: Patient exists for quality measure evaluation.
     *
     * <p>Used by QualityMeasureService to verify patient data retrieval
     * during HEDIS measure evaluation. Creates John Doe as the target patient.
     */
    @State("patient exists for quality measure evaluation")
    void patientExistsForQualityMeasure() {
        stateSetup.setupPatientJohnDoe();
    }

    /**
     * State: Active patients exist for listing.
     *
     * <p>Used by consumers that need to list all active patients.
     * Creates multiple patients, all in active status.
     */
    @State("active patients exist")
    void activePatientsExist() {
        stateSetup.setupPatientJohnDoe();
        stateSetup.setupPatientJaneSmith();
    }

    /**
     * State: Patient health record data exists.
     *
     * <p>Used by consumers that request comprehensive health records
     * via the /patient/health-record endpoint.
     */
    @State("patient health record exists")
    void patientHealthRecordExists() {
        stateSetup.setupPatientJohnDoe();
    }
}
