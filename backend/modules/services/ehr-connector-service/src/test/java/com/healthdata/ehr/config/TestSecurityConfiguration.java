package com.healthdata.ehr.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.healthdata.audit.repository.AuditEventRepository;
import com.healthdata.audit.service.AuditService;
import com.healthdata.authentication.repository.ApiKeyRepository;
import com.healthdata.authentication.repository.RefreshTokenRepository;
import com.healthdata.authentication.repository.UserRepository;
import com.healthdata.authentication.service.ApiKeyService;
import com.healthdata.authentication.service.JwtTokenService;
import com.healthdata.authentication.service.LogoutService;
import com.healthdata.ehr.connector.cerner.CernerAuthProvider;
import com.healthdata.ehr.connector.cerner.CernerDataMapper;
import com.healthdata.ehr.connector.cerner.CernerFhirConnector;
import com.healthdata.ehr.connector.cerner.config.CernerConnectionConfig;
import com.healthdata.ehr.connector.epic.EpicAuthProvider;
import com.healthdata.ehr.connector.epic.EpicConnectionConfig;
import com.healthdata.ehr.connector.epic.EpicDataMapper;
import com.healthdata.ehr.connector.epic.EpicFhirConnector;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test Security Configuration for EHR Connector Service Tests.
 *
 * This configuration:
 * - Provides mock JwtTokenService to avoid JWT infrastructure
 * - Provides mock FHIR infrastructure (Epic/Cerner connectors)
 * - Disables CSRF protection for tests
 * - Permits all requests without authentication
 * - Only active in 'test' profile
 */
@TestConfiguration
@Profile("test")
@EnableWebSecurity
public class TestSecurityConfiguration {

    /**
     * Mock JwtTokenService to avoid needing real JWT infrastructure.
     */
    @MockBean
    private JwtTokenService jwtTokenService;

    /**
     * Mock ApiKeyRepository to avoid database dependency.
     */
    @MockBean
    private ApiKeyRepository apiKeyRepository;

    /**
     * Mock UserRepository to avoid database dependency.
     */
    @MockBean
    private UserRepository userRepository;

    /**
     * Mock RefreshTokenRepository to avoid database dependency.
     */
    @MockBean
    private RefreshTokenRepository refreshTokenRepository;

    /**
     * Mock ApiKeyService to avoid authentication infrastructure.
     */
    @MockBean
    private ApiKeyService apiKeyService;

    /**
     * Mock LogoutService to avoid authentication infrastructure.
     */
    @MockBean
    private LogoutService logoutService;

    /**
     * Mock AuditEventRepository to avoid database dependency.
     */
    @MockBean
    private AuditEventRepository auditEventRepository;

    /**
     * Mock AuditService to avoid audit infrastructure.
     */
    @MockBean
    private AuditService auditService;

    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );
        return http.build();
    }

    /**
     * Provide FhirContext for tests.
     */
    @Bean
    @Primary
    public FhirContext fhirContext() {
        return FhirContext.forR4();
    }

    /**
     * Mock Epic connection config for tests.
     */
    @Bean
    @Primary
    public EpicConnectionConfig epicConnectionConfig() {
        EpicConnectionConfig config = new EpicConnectionConfig();
        config.setBaseUrl("https://test-fhir.epic.com/api/FHIR/R4");
        config.setTokenUrl("https://test-fhir.epic.com/oauth2/token");
        config.setClientId("test-client-id");
        config.setRequestTimeoutSeconds(30);
        config.setSandboxMode(true);
        return config;
    }

    /**
     * Mock Epic auth provider for tests.
     */
    @Bean
    @Primary
    public EpicAuthProvider epicAuthProvider() {
        EpicAuthProvider mock = mock(EpicAuthProvider.class);
        when(mock.getAccessToken()).thenReturn("test-access-token");
        when(mock.isTokenValid()).thenReturn(true);
        return mock;
    }

    /**
     * Mock FHIR client for tests.
     */
    @Bean
    @Primary
    public IGenericClient fhirClient(FhirContext fhirContext) {
        return mock(IGenericClient.class);
    }

    /**
     * Mock Epic FHIR connector for tests.
     */
    @Bean
    @Primary
    public EpicFhirConnector epicFhirConnector() {
        return mock(EpicFhirConnector.class);
    }

    /**
     * Mock Cerner connection config for tests.
     */
    @Bean
    @Primary
    public CernerConnectionConfig cernerConnectionConfig() {
        CernerConnectionConfig config = new CernerConnectionConfig();
        config.setBaseUrl("https://test-fhir.cerner.com/r4");
        config.setTokenUrl("https://test-authorization.cerner.com/oauth2/token");
        config.setClientId("test-client-id");
        config.setClientSecret("test-client-secret");
        config.setTenantId("test-tenant-id");
        return config;
    }

    /**
     * Mock Cerner auth provider for tests.
     */
    @Bean
    @Primary
    public CernerAuthProvider cernerAuthProvider() {
        CernerAuthProvider mock = mock(CernerAuthProvider.class);
        when(mock.getAccessToken()).thenReturn("test-access-token");
        when(mock.getAuthorizationHeader()).thenReturn("Bearer test-access-token");
        return mock;
    }

    /**
     * Mock Cerner FHIR connector for tests.
     */
    @Bean
    @Primary
    public CernerFhirConnector cernerFhirConnector() {
        return mock(CernerFhirConnector.class);
    }

    /**
     * Mock Cerner data mapper for tests.
     */
    @Bean
    @Primary
    public CernerDataMapper cernerDataMapper() {
        return mock(CernerDataMapper.class);
    }

    /**
     * Mock Epic data mapper for tests.
     */
    @Bean
    @Primary
    public EpicDataMapper epicDataMapper() {
        return mock(EpicDataMapper.class);
    }

    /**
     * CacheManager for tests.
     */
    @Bean
    @Primary
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("cernerTokens", "epicTokens");
    }
}
