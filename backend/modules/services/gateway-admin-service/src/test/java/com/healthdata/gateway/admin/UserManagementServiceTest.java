package com.healthdata.gateway.admin;

import com.healthdata.authentication.domain.User;
import com.healthdata.authentication.domain.UserRole;
import com.healthdata.authentication.dto.UpdateUserRequest;
import com.healthdata.authentication.repository.UserRepository;
import com.healthdata.authentication.service.UserManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("UserManagementService")
class UserManagementServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private UserManagementService service;

    @BeforeEach
    void setUp() {
        service = new UserManagementService(userRepository, passwordEncoder);
    }

    private User createTestUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");
        user.setEmail("test@hdim.local");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setActive(true);
        user.setRoles(Set.of(UserRole.VIEWER));
        user.setTenantIds(Set.of("demo"));
        return user;
    }

    @Test
    @DisplayName("should update user profile fields")
    void shouldUpdateUserProfileFields() {
        User user = createTestUser();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UpdateUserRequest request = new UpdateUserRequest();
        request.setFirstName("Updated");
        request.setLastName("Name");

        User result = service.updateUser(user.getId(), request);

        assertThat(result.getFirstName()).isEqualTo("Updated");
        assertThat(result.getLastName()).isEqualTo("Name");
    }

    @Test
    @DisplayName("should deactivate user and record actor")
    void shouldDeactivateUser() {
        User user = createTestUser();
        UUID actorId = UUID.randomUUID();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        User result = service.deactivateUser(user.getId(), actorId);

        assertThat(result.getActive()).isFalse();
        assertThat(result.getDeactivatedAt()).isNotNull();
        assertThat(result.getDeactivatedBy()).isEqualTo(actorId);
    }

    @Test
    @DisplayName("should reactivate user and clear deactivation fields")
    void shouldReactivateUser() {
        User user = createTestUser();
        user.setActive(false);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        User result = service.reactivateUser(user.getId());

        assertThat(result.getActive()).isTrue();
        assertThat(result.getDeactivatedAt()).isNull();
        assertThat(result.getDeactivatedBy()).isNull();
    }

    @Test
    @DisplayName("should update user roles")
    void shouldUpdateUserRoles() {
        User user = createTestUser();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Set<UserRole> newRoles = Set.of(UserRole.ADMIN, UserRole.EVALUATOR);
        User result = service.updateRoles(user.getId(), newRoles);

        assertThat(result.getRoles()).containsExactlyInAnyOrder(UserRole.ADMIN, UserRole.EVALUATOR);
    }

    @Test
    @DisplayName("should reset password and set force change flag")
    void shouldResetPasswordAndForceChange() {
        User user = createTestUser();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(any())).thenReturn("$2a$10$encoded");
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String tempPassword = service.resetPassword(user.getId());

        assertThat(tempPassword).isNotBlank();
        assertThat(tempPassword.length()).isGreaterThanOrEqualTo(12);
        verify(userRepository).save(argThat(u -> u.getForcePasswordChange()));
    }

    @Test
    @DisplayName("should throw when user not found")
    void shouldThrowWhenUserNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateUser(id, new UpdateUserRequest()))
            .isInstanceOf(RuntimeException.class);
    }
}
