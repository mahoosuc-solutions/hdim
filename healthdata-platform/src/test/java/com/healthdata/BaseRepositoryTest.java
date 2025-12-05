package com.healthdata;

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for JPA repository tests.
 *
 * This class provides a lightweight test environment specifically for testing
 * Spring Data JPA repositories without loading the entire application context.
 *
 * Features:
 * - @DataJpaTest annotation for automatic JPA configuration
 * - H2 in-memory database
 * - No web or security configuration
 * - Automatic rollback after each test
 *
 * Usage:
 * <code>
 * @DataJpaTest
 * public class PatientRepositoryTest extends BaseRepositoryTest {
 *
 *     @Autowired
 *     private PatientRepository patientRepository;
 *
 *     @Test
 *     public void testFindByEmail() {
 *         // Test repository queries
 *     }
 * }
 * </code>
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(HealthDataTestConfiguration.class)
public abstract class BaseRepositoryTest {

    // Subclasses can add repository-specific test utilities here

    /**
     * Wait for a specified duration.
     * Useful for testing time-dependent behavior.
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
     * Useful for testing time-dependent behavior.
     *
     * @param seconds the number of seconds to wait
     */
    protected void waitSeconds(long seconds) {
        waitMillis(seconds * 1000);
    }
}
