package com.healthdata.gateway.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.healthdata.authentication.domain.User;
import com.healthdata.authentication.domain.UserRole;
import com.healthdata.authentication.repository.UserRepository;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService")
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("Should load user details with roles")
    void shouldLoadUserDetails() {
        User user = User.builder()
            .username("alice")
            .passwordHash("hashed")
            .active(true)
            .roles(Set.of(UserRole.ADMIN))
            .build();

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        CustomUserDetailsService service = new CustomUserDetailsService(userRepository);
        UserDetails details = service.loadUserByUsername("alice");

        assertThat(details.getUsername()).isEqualTo("alice");
        assertThat(details.getAuthorities()).extracting("authority").contains("ROLE_ADMIN");
    }

    @Test
    @DisplayName("Should throw when user is missing")
    void shouldThrowWhenUserMissing() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        CustomUserDetailsService service = new CustomUserDetailsService(userRepository);

        assertThatThrownBy(() -> service.loadUserByUsername("missing"))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("Should throw when user is inactive")
    void shouldThrowWhenUserInactive() {
        User user = User.builder()
            .username("bob")
            .passwordHash("hashed")
            .active(false)
            .roles(Set.of(UserRole.VIEWER))
            .build();

        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(user));

        CustomUserDetailsService service = new CustomUserDetailsService(userRepository);

        assertThatThrownBy(() -> service.loadUserByUsername("bob"))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessageContaining("disabled");
    }
}
