package com.healthdata.test.base;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.*;

/**
 * Base annotation for integration tests.
 * 
 * Characteristics:
 * - Loads full Spring context
 * - May use mocked external services
 * - Uses in-memory or test databases
 * - Transactional rollback for isolation
 * - Moderate execution time
 * 
 * Usage:
 * <pre>
 * {@code
 * @BaseIntegrationTest
 * class MyServiceIntegrationTest {
 *     @Autowired
 *     private MyService myService;
 *     
 *     @MockBean
 *     private ExternalService externalService;
 *     
 *     @Test
 *     void shouldIntegrateWithDatabase() {
 *         // Test with real Spring context
 *     }
 * }
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public @interface BaseIntegrationTest {
}

