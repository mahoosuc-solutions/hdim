package com.healthdata.test.base;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.annotation.*;

/**
 * Base annotation for lightweight unit tests.
 * 
 * Characteristics:
 * - Fast execution (< 1 second per test)
 * - No external dependencies (Docker, databases, Kafka)
 * - Uses mocks for external services
 * - Isolated and deterministic
 * - Runs on every build
 * 
 * Usage:
 * <pre>
 * {@code
 * @BaseUnitTest
 * class MyServiceTest {
 *     @Mock
 *     private ExternalService externalService;
 *     
 *     @InjectMocks
 *     private MyService myService;
 *     
 *     @Test
 *     void shouldDoSomething() {
 *         // Test with mocked dependencies
 *     }
 * }
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ExtendWith(MockitoExtension.class)
public @interface BaseUnitTest {
}

