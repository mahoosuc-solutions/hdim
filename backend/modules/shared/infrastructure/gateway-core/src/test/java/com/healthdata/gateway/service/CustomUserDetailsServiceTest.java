package com.healthdata.gateway.service;

import com.healthdata.authentication.domain.User;
import com.healthdata.authentication.domain.UserRole;
import com.healthdata.authentication.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService Tests")
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("loadUserByUsername should load active user with mapped ROLE_ authorities")
    void loadUserByUsernameShouldLoadActiveUser() {
        User user = User.builder()
                .username("gateway-admin")
                .email("gateway-admin@test.com")
                .passwordHash("{noop}Password123!")
                .firstName("Gateway")
                .lastName("Admin")
                .roles(Set.of(UserRole.ADMIN, UserRole.QUALITY_OFFICER))
                .tenantIds(Set.of("tenant-a"))
                .active(true)
                .build();

        when(userRepository.findByUsername("gateway-admin")).thenReturn(Optional.of(user));

        UserDetails loaded = customUserDetailsService.loadUserByUsername("gateway-admin");

        assertThat(loaded.getUsername()).isEqualTo("gateway-admin");
        assertThat(loaded.getPassword()).isEqualTo("{noop}Password123!");
        assertThat(loaded.isEnabled()).isTrue();
        assertThat(loaded.isAccountNonLocked()).isTrue();

        Set<String> authorities = loaded.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toSet());
        assertThat(authorities).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_QUALITY_OFFICER");
    }

    @Test
    @DisplayName("loadUserByUsername should throw when user does not exist")
    void loadUserByUsernameShouldThrowWhenNotFound() {
        when(userRepository.findByUsername("missing-user")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("missing-user"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found: missing-user");
    }

    @Test
    @DisplayName("loadUserByUsername should reject inactive user")
    void loadUserByUsernameShouldRejectInactiveUser() {
        User inactive = User.builder()
                .username("inactive-user")
                .email("inactive@test.com")
                .passwordHash("{noop}Password123!")
                .firstName("Inactive")
                .lastName("User")
                .roles(Set.of(UserRole.EVALUATOR))
                .tenantIds(Set.of("tenant-a"))
                .active(false)
                .build();

        when(userRepository.findByUsername("inactive-user")).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("inactive-user"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("disabled");
    }
}
