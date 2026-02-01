package com.healthdata.authentication.service;

import com.healthdata.authentication.domain.Tenant;
import com.healthdata.authentication.domain.TenantStatus;
import com.healthdata.authentication.domain.User;
import com.healthdata.authentication.domain.UserRole;
import com.healthdata.authentication.dto.TenantRegistrationRequest;
import com.healthdata.authentication.dto.TenantRegistrationResponse;
import com.healthdata.authentication.exception.TenantAlreadyExistsException;
import com.healthdata.authentication.exception.UserAlreadyExistsException;
import com.healthdata.authentication.repository.TenantRepository;
import com.healthdata.authentication.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TenantService.
 * Tests tenant registration, lifecycle management, and error handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TenantService Tests")
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private TenantService tenantService;

    @Captor
    private ArgumentCaptor<Tenant> tenantCaptor;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private TenantRegistrationRequest validRequest;
    private Tenant mockTenant;
    private User mockUser;

    @BeforeEach
    void setUp() {
        // Manually create TenantService without AuditService (it's optional)
        tenantService = new TenantService(
            tenantRepository,
            userRepository,
            passwordEncoder,
            null  // AuditService is optional
        );
        // Valid registration request
        validRequest = TenantRegistrationRequest.builder()
            .tenantId("test-clinic")
            .tenantName("Test Clinic")
            .adminUser(TenantRegistrationRequest.AdminUserRequest.builder()
                .username("admin@test-clinic")
                .email("admin@test-clinic.com")
                .password("Test2026!")
                .firstName("John")
                .lastName("Doe")
                .build())
            .build();

        // Mock tenant
        mockTenant = Tenant.builder()
            .id("test-clinic")
            .name("Test Clinic")
            .status(TenantStatus.ACTIVE)
            .createdAt(Instant.now())
            .build();

        // Mock user
        mockUser = User.builder()
            .id(UUID.randomUUID())
            .username("admin@test-clinic")
            .email("admin@test-clinic.com")
            .passwordHash("$2a$10$encodedHash")
            .firstName("John")
            .lastName("Doe")
            .tenantIds(Set.of("test-clinic"))
            .roles(Set.of(UserRole.ADMIN))
            .active(true)
            .emailVerified(false)
            .failedLoginAttempts(0)
            .build();
    }

    @Nested
    @DisplayName("Tenant Registration Tests")
    class TenantRegistrationTests {

        @Test
        @DisplayName("Should successfully register new tenant with admin user")
        void shouldRegisterNewTenant() {
            // Given
            when(tenantRepository.existsById(anyString())).thenReturn(false);
            when(tenantRepository.save(any(Tenant.class))).thenReturn(mockTenant);
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encodedHash");
            when(userRepository.save(any(User.class))).thenReturn(mockUser);

            // When
            TenantRegistrationResponse response = tenantService.registerTenant(validRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getTenantId()).isEqualTo("test-clinic");
            assertThat(response.getTenantName()).isEqualTo("Test Clinic");
            assertThat(response.getStatus()).isEqualTo(TenantStatus.ACTIVE);
            assertThat(response.getAdminUser()).isNotNull();
            assertThat(response.getAdminUser().getUsername()).isEqualTo("admin@test-clinic");
            assertThat(response.getAdminUser().getEmail()).isEqualTo("admin@test-clinic.com");
            assertThat(response.getAdminUser().getRoles()).contains(UserRole.ADMIN);
            assertThat(response.getAdminUser().getTenantIds()).contains("test-clinic");

            // Verify tenant was saved
            verify(tenantRepository).save(tenantCaptor.capture());
            Tenant savedTenant = tenantCaptor.getValue();
            assertThat(savedTenant.getId()).isEqualTo("test-clinic");
            assertThat(savedTenant.getName()).isEqualTo("Test Clinic");
            assertThat(savedTenant.getStatus()).isEqualTo(TenantStatus.ACTIVE);

            // Verify user was saved
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getUsername()).isEqualTo("admin@test-clinic");
            assertThat(savedUser.getEmail()).isEqualTo("admin@test-clinic.com");
            assertThat(savedUser.getTenantIds()).contains("test-clinic");
            assertThat(savedUser.getRoles()).contains(UserRole.ADMIN);
            assertThat(savedUser.getActive()).isTrue();
            assertThat(savedUser.getEmailVerified()).isFalse();

            // Verify password was encoded
            verify(passwordEncoder).encode("Test2026!");
        }

        @Test
        @DisplayName("Should throw TenantAlreadyExistsException when tenant ID already exists")
        void shouldThrowExceptionWhenTenantExists() {
            // Given
            when(tenantRepository.existsById("test-clinic")).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> tenantService.registerTenant(validRequest))
                .isInstanceOf(TenantAlreadyExistsException.class)
                .hasMessageContaining("test-clinic");

            // Verify no user was created
            verify(userRepository, never()).save(any(User.class));
            verify(tenantRepository, never()).save(any(Tenant.class));
        }

        @Test
        @DisplayName("Should throw UserAlreadyExistsException when username already exists")
        void shouldThrowExceptionWhenUsernameExists() {
            // Given
            when(tenantRepository.existsById(anyString())).thenReturn(false);
            when(tenantRepository.save(any(Tenant.class))).thenReturn(mockTenant);
            when(userRepository.existsByUsername("admin@test-clinic")).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> tenantService.registerTenant(validRequest))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("admin@test-clinic");

            // Verify tenant was created but user was not
            verify(tenantRepository).save(any(Tenant.class));
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw UserAlreadyExistsException when email already exists")
        void shouldThrowExceptionWhenEmailExists() {
            // Given
            when(tenantRepository.existsById(anyString())).thenReturn(false);
            when(tenantRepository.save(any(Tenant.class))).thenReturn(mockTenant);
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail("admin@test-clinic.com")).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> tenantService.registerTenant(validRequest))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("admin@test-clinic.com");

            // Verify tenant was created but user was not
            verify(tenantRepository).save(any(Tenant.class));
            verify(userRepository, never()).save(any(User.class));
        }

    }

    @Nested
    @DisplayName("Tenant Activation Tests")
    class TenantActivationTests {

        @Test
        @DisplayName("Should activate suspended tenant")
        void shouldActivateSuspendedTenant() {
            // Given
            Tenant suspendedTenant = Tenant.builder()
                .id("test-clinic")
                .name("Test Clinic")
                .status(TenantStatus.SUSPENDED)
                .build();

            when(tenantRepository.findById("test-clinic")).thenReturn(Optional.of(suspendedTenant));
            when(tenantRepository.save(any(Tenant.class))).thenReturn(suspendedTenant);

            // When
            tenantService.activateTenant("test-clinic");

            // Then
            verify(tenantRepository).save(tenantCaptor.capture());
            Tenant activatedTenant = tenantCaptor.getValue();
            assertThat(activatedTenant.getStatus()).isEqualTo(TenantStatus.ACTIVE);
        }

        @Test
        @DisplayName("Should throw exception when tenant not found")
        void shouldThrowExceptionWhenTenantNotFound() {
            // Given
            when(tenantRepository.findById("nonexistent")).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> tenantService.activateTenant("nonexistent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tenant not found");

            verify(tenantRepository, never()).save(any(Tenant.class));
        }

        @Test
        @DisplayName("Should throw exception when tenant cannot be reactivated")
        void shouldThrowExceptionWhenCannotReactivate() {
            // Given
            Tenant deactivatedTenant = Tenant.builder()
                .id("test-clinic")
                .name("Test Clinic")
                .status(TenantStatus.INACTIVE)
                .build();

            when(tenantRepository.findById("test-clinic")).thenReturn(Optional.of(deactivatedTenant));

            // When/Then
            assertThatThrownBy(() -> tenantService.activateTenant("test-clinic"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot be reactivated");

            verify(tenantRepository, never()).save(any(Tenant.class));
        }
    }

    @Nested
    @DisplayName("Tenant Suspension Tests")
    class TenantSuspensionTests {

        @Test
        @DisplayName("Should suspend active tenant")
        void shouldSuspendActiveTenant() {
            // Given
            Tenant activeTenant = Tenant.builder()
                .id("test-clinic")
                .name("Test Clinic")
                .status(TenantStatus.ACTIVE)
                .build();

            when(tenantRepository.findById("test-clinic")).thenReturn(Optional.of(activeTenant));
            when(tenantRepository.save(any(Tenant.class))).thenReturn(activeTenant);

            // When
            tenantService.suspendTenant("test-clinic");

            // Then
            verify(tenantRepository).save(tenantCaptor.capture());
            Tenant suspendedTenant = tenantCaptor.getValue();
            assertThat(suspendedTenant.getStatus()).isEqualTo(TenantStatus.SUSPENDED);
        }

        @Test
        @DisplayName("Should throw exception when tenant not found")
        void shouldThrowExceptionWhenTenantNotFound() {
            // Given
            when(tenantRepository.findById("nonexistent")).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> tenantService.suspendTenant("nonexistent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tenant not found");

            verify(tenantRepository, never()).save(any(Tenant.class));
        }
    }

    @Nested
    @DisplayName("Tenant Deactivation Tests")
    class TenantDeactivationTests {

        @Test
        @DisplayName("Should deactivate active tenant")
        void shouldDeactivateActiveTenant() {
            // Given
            Tenant activeTenant = Tenant.builder()
                .id("test-clinic")
                .name("Test Clinic")
                .status(TenantStatus.ACTIVE)
                .build();

            when(tenantRepository.findById("test-clinic")).thenReturn(Optional.of(activeTenant));
            when(tenantRepository.save(any(Tenant.class))).thenReturn(activeTenant);

            // When
            tenantService.deactivateTenant("test-clinic");

            // Then
            verify(tenantRepository).save(tenantCaptor.capture());
            Tenant deactivatedTenant = tenantCaptor.getValue();
            assertThat(deactivatedTenant.getStatus()).isEqualTo(TenantStatus.INACTIVE);
        }

        @Test
        @DisplayName("Should deactivate suspended tenant")
        void shouldDeactivateSuspendedTenant() {
            // Given
            Tenant suspendedTenant = Tenant.builder()
                .id("test-clinic")
                .name("Test Clinic")
                .status(TenantStatus.SUSPENDED)
                .build();

            when(tenantRepository.findById("test-clinic")).thenReturn(Optional.of(suspendedTenant));
            when(tenantRepository.save(any(Tenant.class))).thenReturn(suspendedTenant);

            // When
            tenantService.deactivateTenant("test-clinic");

            // Then
            verify(tenantRepository).save(tenantCaptor.capture());
            Tenant deactivatedTenant = tenantCaptor.getValue();
            assertThat(deactivatedTenant.getStatus()).isEqualTo(TenantStatus.INACTIVE);
        }

        @Test
        @DisplayName("Should throw exception when tenant not found")
        void shouldThrowExceptionWhenTenantNotFound() {
            // Given
            when(tenantRepository.findById("nonexistent")).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> tenantService.deactivateTenant("nonexistent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tenant not found");

            verify(tenantRepository, never()).save(any(Tenant.class));
        }
    }

    @Nested
    @DisplayName("Transactional Behavior Tests")
    class TransactionalBehaviorTests {

        @Test
        @DisplayName("Should rollback transaction if user creation fails")
        void shouldRollbackWhenUserCreationFails() {
            // Given
            when(tenantRepository.existsById(anyString())).thenReturn(false);
            when(tenantRepository.save(any(Tenant.class))).thenReturn(mockTenant);
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encodedHash");
            when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("Database error"));

            // When/Then
            assertThatThrownBy(() -> tenantService.registerTenant(validRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database error");

            // Verify tenant was attempted to be saved
            verify(tenantRepository).save(any(Tenant.class));
            // In real transactional scenario, this would be rolled back
        }
    }
}
