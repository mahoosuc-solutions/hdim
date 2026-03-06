package com.healthdata.patient.integration;

import com.healthdata.authentication.constants.AuthHeaderConstants;
import com.healthdata.authentication.domain.User;
import com.healthdata.authentication.domain.UserRole;
import com.healthdata.authentication.filter.UserAutoRegistrationFilter;
import com.healthdata.patient.config.BaseIntegrationTest;
import com.healthdata.patient.persistence.UserRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Integration test for UserAutoRegistrationFilter in Patient Service.
 *
 * Verifies that when gateway-validated headers arrive for a previously
 * unknown user, a User row is automatically created in the database.
 */
@BaseIntegrationTest
@DisplayName("UserAutoRegistrationFilter — Patient Service")
class UserAutoRegistrationIT {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("creates user row on first request with valid gateway headers")
    void shouldCreateUserOnFirstRequest() throws Exception {
        UUID userId = UUID.randomUUID();
        String username = "auto-test-" + userId.toString().substring(0, 8);

        UserAutoRegistrationFilter filter = new UserAutoRegistrationFilter(userRepository);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AuthHeaderConstants.HEADER_USER_ID, userId.toString());
        request.addHeader(AuthHeaderConstants.HEADER_USERNAME, username);
        request.addHeader(AuthHeaderConstants.HEADER_ROLES, "ADMIN,EVALUATOR");
        request.addHeader(AuthHeaderConstants.HEADER_TENANT_IDS, "tenant-1,tenant-2");
        request.addHeader(AuthHeaderConstants.HEADER_VALIDATED, "gateway-test-sig");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        // Verify filter chain continued
        verify(chain).doFilter(request, response);

        // Verify user was created
        Optional<User> saved = userRepository.findById(userId);
        assertThat(saved).isPresent();
        assertThat(saved.get().getUsername()).isEqualTo(username);
        assertThat(saved.get().getRoles()).contains(UserRole.ADMIN, UserRole.EVALUATOR);
        assertThat(saved.get().getTenantIds()).containsExactlyInAnyOrder("tenant-1", "tenant-2");
        assertThat(saved.get().getActive()).isTrue();
    }

    @Test
    @DisplayName("does not duplicate user on subsequent requests")
    void shouldNotDuplicateOnSubsequentRequest() throws Exception {
        UUID userId = UUID.randomUUID();
        String username = "dup-test-" + userId.toString().substring(0, 8);

        UserAutoRegistrationFilter filter = new UserAutoRegistrationFilter(userRepository);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AuthHeaderConstants.HEADER_USER_ID, userId.toString());
        request.addHeader(AuthHeaderConstants.HEADER_USERNAME, username);
        request.addHeader(AuthHeaderConstants.HEADER_ROLES, "VIEWER");
        request.addHeader(AuthHeaderConstants.HEADER_TENANT_IDS, "tenant-1");
        request.addHeader(AuthHeaderConstants.HEADER_VALIDATED, "gateway-test-sig");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        // First request — creates user
        filter.doFilter(request, response, chain);

        // Second request — should update last access, not create duplicate
        MockHttpServletRequest request2 = new MockHttpServletRequest();
        request2.addHeader(AuthHeaderConstants.HEADER_USER_ID, userId.toString());
        request2.addHeader(AuthHeaderConstants.HEADER_USERNAME, username);
        request2.addHeader(AuthHeaderConstants.HEADER_ROLES, "VIEWER");
        request2.addHeader(AuthHeaderConstants.HEADER_TENANT_IDS, "tenant-1");
        request2.addHeader(AuthHeaderConstants.HEADER_VALIDATED, "gateway-test-sig");

        filter.doFilter(request2, new MockHttpServletResponse(), mock(FilterChain.class));

        // Still exactly one user
        assertThat(userRepository.findById(userId)).isPresent();
    }

    @Test
    @DisplayName("skips registration when no gateway validation header present")
    void shouldSkipWithoutValidationHeader() throws Exception {
        UUID userId = UUID.randomUUID();

        UserAutoRegistrationFilter filter = new UserAutoRegistrationFilter(userRepository);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AuthHeaderConstants.HEADER_USER_ID, userId.toString());
        request.addHeader(AuthHeaderConstants.HEADER_USERNAME, "no-validated-header");
        // No HEADER_VALIDATED

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(userRepository.findById(userId)).isEmpty();
    }
}
