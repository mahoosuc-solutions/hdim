package com.healthdata.contracts;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.PactBrokerAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for Pact provider verification tests.
 *
 * <p>Provides standardized configuration for verifying consumer contracts against
 * provider implementations. Integrates with the Pact Broker for contract retrieval
 * and verification result publishing.
 *
 * <h2>Usage</h2>
 * <ol>
 *   <li>Extend this class</li>
 *   <li>Add {@code @Provider("YourServiceName")} annotation</li>
 *   <li>Implement {@code @State} methods for each provider state</li>
 *   <li>Run tests to verify contracts</li>
 * </ol>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * @Provider("PatientService")
 * class PatientServiceProviderTest extends ContractTestBase {
 *
 *     @Autowired
 *     private PatientRepository patientRepository;
 *
 *     @State("patient exists with id f47ac10b-58cc-4372-a567-0e02b2c3d479")
 *     void setupPatientExists() {
 *         Patient patient = new Patient();
 *         patient.setId(UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479"));
 *         patient.setTenantId(getTestTenantId());
 *         patientRepository.save(patient);
 *     }
 *
 *     @TestTemplate
 *     @ExtendWith(PactVerificationInvocationContextProvider.class)
 *     void pactVerificationTestTemplate(PactVerificationContext context) {
 *         context.verifyInteraction();
 *     }
 * }
 * }</pre>
 *
 * <h2>Multi-Tenant Support</h2>
 * <p>Override {@link #getTestTenantId()} to customize the tenant used in tests.
 * The default tenant ID is "test-tenant-contracts".
 *
 * <h2>Authentication</h2>
 * <p>Override {@link #getTestUserId()} to customize the user context for authenticated
 * endpoint tests. The default user ID is "contract-test-user".
 *
 * @see au.com.dius.pact.provider.junitsupport.Provider
 * @see au.com.dius.pact.provider.junitsupport.State
 */
@Tag("contract")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@PactBroker(
    url = "${pact.broker.url:http://localhost:9292}",
    authentication = @PactBrokerAuth(
        username = "${pact.broker.username:hdim}",
        password = "${pact.broker.password:hdimcontract}"
    )
)
public abstract class ContractTestBase {

    private static final Logger log = LoggerFactory.getLogger(ContractTestBase.class);

    /**
     * Default credentials for local development only.
     * SECURITY: These should NEVER be used in production environments.
     */
    private static final String DEFAULT_BROKER_USERNAME = "hdim";
    private static final String DEFAULT_BROKER_PASSWORD = "hdimcontract";

    /**
     * The random port assigned to the test server.
     */
    @LocalServerPort
    protected int port;

    /**
     * Optional Pact configuration bean. May be null if not configured in the test context.
     */
    @Autowired(required = false)
    protected PactConfig pactConfig;

    /**
     * Pact Broker username from configuration.
     */
    @Value("${pact.broker.username:" + DEFAULT_BROKER_USERNAME + "}")
    private String brokerUsername;

    /**
     * Pact Broker password from configuration.
     */
    @Value("${pact.broker.password:" + DEFAULT_BROKER_PASSWORD + "}")
    private String brokerPassword;

    /**
     * Sets up the HTTP test target before each test.
     *
     * <p>Configures the Pact verification context to point to the local test server.
     * This method is called before each verification test to ensure the context
     * is properly initialized.
     *
     * @param context the Pact verification context, may be null during test discovery
     */
    @BeforeEach
    protected void setupTestTarget(PactVerificationContext context) {
        if (context != null) {
            context.setTarget(new HttpTestTarget("localhost", port));
        }
        warnIfUsingDefaultCredentials();
    }

    /**
     * Logs a warning if default Pact Broker credentials are being used.
     *
     * <p>SECURITY: Default credentials are acceptable for local development
     * but should never be used in CI/CD or production environments.
     * Configure proper credentials via environment variables or secrets.
     */
    private void warnIfUsingDefaultCredentials() {
        if (DEFAULT_BROKER_USERNAME.equals(brokerUsername)
                && DEFAULT_BROKER_PASSWORD.equals(brokerPassword)) {
            log.warn("SECURITY WARNING: Using default Pact Broker credentials. "
                    + "This is acceptable for local development only. "
                    + "For CI/CD and production, configure PACT_BROKER_USERNAME "
                    + "and PACT_BROKER_PASSWORD environment variables or secrets.");
        }
    }

    /**
     * Gets the tenant ID to use for multi-tenant tests.
     *
     * <p>Override this method to customize the tenant context for your tests.
     * All test data should be associated with this tenant ID to ensure proper
     * isolation and cleanup.
     *
     * @return the test tenant ID, defaults to "test-tenant-contracts"
     */
    protected String getTestTenantId() {
        return "test-tenant-contracts";
    }

    /**
     * Gets the user ID to use for authenticated tests.
     *
     * <p>Override this method to customize the user context for tests that
     * require authentication. This is useful when testing endpoints that
     * have user-specific behavior or audit logging.
     *
     * @return the test user ID, defaults to "contract-test-user"
     */
    protected String getTestUserId() {
        return "contract-test-user";
    }
}
