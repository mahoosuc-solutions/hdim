package com.healthdata;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Base class for Spring service layer tests.
 *
 * This class provides utilities for testing service classes with:
 * - Spring Boot test environment
 * - Transaction management
 * - Mock data generation
 * - Assertion helper methods
 *
 * Usage:
 * <code>
 * @SpringBootTest
 * public class PatientServiceTest extends BaseServiceTest {
 *
 *     @Autowired
 *     private PatientService patientService;
 *
 *     @MockBean
 *     private PatientRepository patientRepository;
 *
 *     @Test
 *     public void testGetPatient() {
 *         // Test service logic with mocks
 *     }
 * }
 * </code>
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(HealthDataTestConfiguration.class)
@Transactional
public abstract class BaseServiceTest {

    /**
     * Initialize test setup before each test.
     * Override in subclasses to add specific setup logic.
     */
    @BeforeEach
    public void setUp() {
        // Override in subclasses
    }

    /**
     * Generate a random UUID string.
     * Useful for creating unique identifiers in tests.
     *
     * @return a random UUID as a string
     */
    protected String generateRandomId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generate a random UUID string with a prefix.
     * Useful for creating human-readable test identifiers.
     *
     * @param prefix the prefix for the identifier
     * @return a prefixed random identifier
     */
    protected String generateRandomId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString();
    }

    /**
     * Wait for a specified duration.
     * Useful for testing async behavior.
     *
     * @param millis the number of milliseconds to wait
     */
    protected void waitMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrupted", e);
        }
    }

    /**
     * Wait for a specified duration in seconds.
     * Useful for testing async behavior.
     *
     * @param seconds the number of seconds to wait
     */
    protected void waitSeconds(long seconds) {
        waitMillis(seconds * 1000);
    }

    /**
     * Assert that an object is not null with a custom message.
     *
     * @param obj the object to check
     * @param message the message if assertion fails
     */
    protected void assertNotNull(Object obj, String message) {
        if (obj == null) {
            throw new AssertionError(message);
        }
    }

    /**
     * Assert that a string is not empty.
     *
     * @param str the string to check
     * @param message the message if assertion fails
     */
    protected void assertNotEmpty(String str, String message) {
        if (str == null || str.trim().isEmpty()) {
            throw new AssertionError(message);
        }
    }

    /**
     * Assert that two objects are equal with a custom message.
     *
     * @param expected the expected value
     * @param actual the actual value
     * @param message the message if assertion fails
     */
    protected void assertEqual(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(
                    String.format("%s. Expected: %s, Actual: %s", message, expected, actual)
            );
        }
    }

    /**
     * Assert that two objects are not equal.
     *
     * @param expected the unexpected value
     * @param actual the actual value
     * @param message the message if assertion fails
     */
    protected void assertNotEqual(Object expected, Object actual, String message) {
        if (expected.equals(actual)) {
            throw new AssertionError(
                    String.format("%s. Both values are equal: %s", message, actual)
            );
        }
    }

    /**
     * Assert that a condition is true with a custom message.
     *
     * @param condition the condition to test
     * @param message the message if assertion fails
     */
    protected void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    /**
     * Assert that a condition is false with a custom message.
     *
     * @param condition the condition to test
     * @param message the message if assertion fails
     */
    protected void assertFalse(boolean condition, String message) {
        if (condition) {
            throw new AssertionError(message);
        }
    }

    /**
     * Get the test data configuration from HealthDataTestConfiguration.
     * Provides access to predefined test constants.
     *
     * @return the HealthDataTestConfiguration.TestDataConfig class
     */
    protected HealthDataTestConfiguration.TestDataConfig getTestDataConfig() {
        return new HealthDataTestConfiguration.TestDataConfig();
    }

    /**
     * Get the test feature flags from HealthDataTestConfiguration.
     * Provides access to feature toggle settings.
     *
     * @return the HealthDataTestConfiguration.TestFeatureFlags class
     */
    protected HealthDataTestConfiguration.TestFeatureFlags getFeatureFlags() {
        return new HealthDataTestConfiguration.TestFeatureFlags();
    }

    /**
     * Get the test timeout configuration from HealthDataTestConfiguration.
     * Provides access to timeout settings.
     *
     * @return the HealthDataTestConfiguration.TestTimeouts class
     */
    protected HealthDataTestConfiguration.TestTimeouts getTestTimeouts() {
        return new HealthDataTestConfiguration.TestTimeouts();
    }
}
