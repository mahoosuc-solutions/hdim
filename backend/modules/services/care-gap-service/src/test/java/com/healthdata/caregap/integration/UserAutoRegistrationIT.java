package com.healthdata.caregap.integration;

import com.healthdata.authentication.constants.AuthHeaderConstants;
import com.healthdata.authentication.domain.User;
import com.healthdata.authentication.domain.UserRole;
import com.healthdata.authentication.filter.UserAutoRegistrationFilter;
import com.healthdata.caregap.config.BaseIntegrationTest;
import com.healthdata.caregap.persistence.UserRepository;
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
 * Integration test for UserAutoRegistrationFilter in Care Gap Service.
 */
@BaseIntegrationTest
@DisplayName("UserAutoRegistrationFilter — Care Gap Service")
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

        verify(chain).doFilter(request, response);

        Optional<User> saved = userRepository.findById(userId);
        assertThat(saved).isPresent();
        assertThat(saved.get().getUsername()).isEqualTo(username);
        assertThat(saved.get().getRoles()).contains(UserRole.ADMIN, UserRole.EVALUATOR);
        assertThat(saved.get().getTenantIds()).containsExactlyInAnyOrder("tenant-1", "tenant-2");
        assertThat(saved.get().getActive()).isTrue();
    }

    @Test
    @DisplayName("skips registration when no gateway validation header present")
    void shouldSkipWithoutValidationHeader() throws Exception {
        UUID userId = UUID.randomUUID();

        UserAutoRegistrationFilter filter = new UserAutoRegistrationFilter(userRepository);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AuthHeaderConstants.HEADER_USER_ID, userId.toString());
        request.addHeader(AuthHeaderConstants.HEADER_USERNAME, "no-validated-header");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(userRepository.findById(userId)).isEmpty();
    }
}
