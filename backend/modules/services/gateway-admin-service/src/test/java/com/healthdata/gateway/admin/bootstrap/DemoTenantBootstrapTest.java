package com.healthdata.gateway.admin.bootstrap;

import com.healthdata.authentication.bootstrap.DemoTenantBootstrap;
import com.healthdata.authentication.bootstrap.DemoTenantProperties;
import com.healthdata.authentication.domain.Tenant;
import com.healthdata.authentication.domain.User;
import com.healthdata.authentication.domain.UserRole;
import com.healthdata.authentication.repository.TenantRepository;
import com.healthdata.authentication.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("DemoTenantBootstrap")
class DemoTenantBootstrapTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private DemoTenantProperties properties;
    private DemoTenantBootstrap bootstrap;

    @BeforeEach
    void setUp() {
        properties = new DemoTenantProperties();
        properties.setEnabled(true);
        properties.setId("demo");
        properties.setName("HDIM Demo");
        properties.setAdminUsername("demo_admin");
        properties.setAdminEmail("demo_admin@hdim.local");
        properties.setAdminPassword("changeme123");
        properties.setAnalystUsername("demo_analyst");
        properties.setAnalystEmail("demo_analyst@hdim.local");
        properties.setViewerUsername("demo_viewer");
        properties.setViewerEmail("demo_viewer@hdim.local");

        bootstrap = new DemoTenantBootstrap(tenantRepository, userRepository, passwordEncoder, properties);
    }

    @Test
    @DisplayName("should create demo tenant on first run")
    void shouldCreateDemoTenantOnFirstRun() throws Exception {
        // Given
        when(tenantRepository.existsByIdIgnoreCase("demo")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$encoded");
        when(userRepository.existsByUsername(anyString())).thenReturn(false);

        ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);

        // When
        bootstrap.run(null);

        // Then
        verify(tenantRepository).save(tenantCaptor.capture());
        Tenant saved = tenantCaptor.getValue();
        assertThat(saved.getId()).isEqualTo("demo");
        assertThat(saved.getName()).isEqualTo("HDIM Demo");
    }

    @Test
    @DisplayName("should create three demo users with correct roles")
    void shouldCreateThreeDemoUsersWithCorrectRoles() throws Exception {
        // Given
        when(tenantRepository.existsByIdIgnoreCase("demo")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$encoded");
        when(userRepository.existsByUsername(anyString())).thenReturn(false);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        // When
        bootstrap.run(null);

        // Then
        verify(userRepository, times(3)).save(userCaptor.capture());
        List<User> savedUsers = userCaptor.getAllValues();

        // Admin user has ADMIN + EVALUATOR roles
        User adminUser = savedUsers.stream()
            .filter(u -> u.getUsername().equals("demo_admin"))
            .findFirst()
            .orElseThrow();
        assertThat(adminUser.getRoles()).contains(UserRole.ADMIN, UserRole.EVALUATOR);
        assertThat(adminUser.getForcePasswordChange()).isTrue();
        assertThat(adminUser.getTenantIds()).contains("demo");

        // Analyst user has ANALYST + EVALUATOR roles
        User analystUser = savedUsers.stream()
            .filter(u -> u.getUsername().equals("demo_analyst"))
            .findFirst()
            .orElseThrow();
        assertThat(analystUser.getRoles()).contains(UserRole.ANALYST, UserRole.EVALUATOR);
        assertThat(analystUser.getForcePasswordChange()).isTrue();
        assertThat(analystUser.getTenantIds()).contains("demo");

        // Viewer user has VIEWER role only
        User viewerUser = savedUsers.stream()
            .filter(u -> u.getUsername().equals("demo_viewer"))
            .findFirst()
            .orElseThrow();
        assertThat(viewerUser.getRoles()).contains(UserRole.VIEWER);
        assertThat(viewerUser.getForcePasswordChange()).isTrue();
        assertThat(viewerUser.getTenantIds()).contains("demo");
    }

    @Test
    @DisplayName("should skip seeding when tenant already exists")
    void shouldSkipSeedingWhenTenantAlreadyExists() throws Exception {
        // Given
        when(tenantRepository.existsByIdIgnoreCase("demo")).thenReturn(true);

        // When
        bootstrap.run(null);

        // Then
        verify(tenantRepository, never()).save(any(Tenant.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("should skip seeding when disabled")
    void shouldSkipSeedingWhenDisabled() throws Exception {
        // Given
        properties.setEnabled(false);

        // When
        bootstrap.run(null);

        // Then
        verify(tenantRepository, never()).existsByIdIgnoreCase(anyString());
        verify(tenantRepository, never()).save(any(Tenant.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("should skip existing users but create missing ones")
    void shouldSkipExistingUsersButCreateMissingOnes() throws Exception {
        // Given: tenant does not exist, admin already exists, analyst and viewer do not
        when(tenantRepository.existsByIdIgnoreCase("demo")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$encoded");
        when(userRepository.existsByUsername("demo_admin")).thenReturn(true);
        when(userRepository.existsByUsername("demo_analyst")).thenReturn(false);
        when(userRepository.existsByUsername("demo_viewer")).thenReturn(false);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        // When
        bootstrap.run(null);

        // Then: only 2 users saved (analyst + viewer), admin skipped
        verify(userRepository, times(2)).save(userCaptor.capture());
        List<User> savedUsers = userCaptor.getAllValues();
        assertThat(savedUsers).extracting(User::getUsername)
            .containsExactlyInAnyOrder("demo_analyst", "demo_viewer");
    }
}
