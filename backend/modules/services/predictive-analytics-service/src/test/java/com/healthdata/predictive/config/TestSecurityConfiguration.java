package com.healthdata.predictive.config;

import com.healthdata.authentication.service.JwtTokenService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Profile;

/**
 * Test Security Configuration for Integration Tests.
 *
 * This configuration provides:
 * - Mock JwtTokenService to avoid JWT validation in tests
 * - JwtAuthenticationFilter is automatically excluded via @Profile("!test") on the real filter
 * - SecurityFilterChain is already configured in PredictiveAnalyticsSecurityConfig for test profile
 *
 * Usage:
 * Import this configuration in your integration tests with:
 * @Import(TestSecurityConfiguration.class)
 * OR include it in the @SpringBootTest classes attribute
 *
 * Best Practices:
 * - Only active in 'test' profile
 * - Mocks JWT dependencies to avoid infrastructure setup
 * - Works with @AutoConfigureMockMvc(addFilters = false) to disable security filters
 * - PredictiveAnalyticsSecurityConfig already provides testSecurityFilterChain that permits all requests
 *
 * Example Test Setup:
 * <pre>
 * @SpringBootTest(classes = {
 *     PredictiveAnalyticsServiceApplication.class,
 *     TestSecurityConfiguration.class
 * })
 * @AutoConfigureMockMvc(addFilters = false)
 * @ActiveProfiles("test")
 * class YourIntegrationTest {
 *     // Your test code here
 * }
 * </pre>
 *
 * Note: All mocks are provided via @MockBean which means:
 * - They're automatically registered as beans in the test context
 * - They replace any real beans that would normally be created
 * - Tests can inject and configure them with custom behavior if needed
 * - By default, all methods return null/false unless configured
 */
@TestConfiguration
@Profile("test")
public class TestSecurityConfiguration {

    /**
     * Mock JwtTokenService to avoid needing real JWT infrastructure.
     *
     * This mock bean replaces any real JwtTokenService in the test context.
     * The JwtAuthenticationFilter won't be loaded due to @Profile("!test"),
     * so this mock typically won't be called, but it's available if needed.
     *
     * Tests can inject and configure this mock with custom behavior:
     * <pre>
     * @Autowired
     * private JwtTokenService jwtTokenService;
     *
     * @BeforeEach
     * void setup() {
     *     when(jwtTokenService.validateToken(anyString())).thenReturn(true);
     *     when(jwtTokenService.extractUsername(anyString())).thenReturn("test-user");
     * }
     * </pre>
     */
    @MockBean
    private JwtTokenService jwtTokenService;
}
