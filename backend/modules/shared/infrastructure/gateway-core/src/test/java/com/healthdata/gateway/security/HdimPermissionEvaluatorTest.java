package com.healthdata.gateway.security;

import com.healthdata.authentication.domain.Permission;
import com.healthdata.authentication.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("HdimPermissionEvaluator Unit Tests")
class HdimPermissionEvaluatorTest {

    private HdimPermissionEvaluator permissionEvaluator;

    @BeforeEach
    void setUp() {
        permissionEvaluator = new HdimPermissionEvaluator();
    }

    // --- Authentication State Tests ---

    @Test
    @DisplayName("Should deny permission when user is not authenticated")
    void shouldDenyPermission_WhenNotAuthenticated() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);

        boolean hasPermission = permissionEvaluator.hasPermission(auth, null, "PATIENT_READ");

        assertThat(hasPermission).isFalse();
    }

    @Test
    @DisplayName("Should deny permission when authentication is null")
    void shouldDenyPermission_WhenAuthenticationIsNull() {
        boolean hasPermission = permissionEvaluator.hasPermission(null, null, "PATIENT_READ");

        assertThat(hasPermission).isFalse();
    }

    @Test
    @DisplayName("Should deny permission when permission is null")
    void shouldDenyPermission_WhenPermissionIsNull() {
        Authentication auth = createAuthentication("user", UserRole.ADMIN);

        boolean hasPermission = permissionEvaluator.hasPermission(auth, null, null);

        assertThat(hasPermission).isFalse();
    }

    // --- SUPER_ADMIN Role Tests ---

    @Test
    @DisplayName("SUPER_ADMIN should have all permissions")
    void superAdmin_ShouldHaveAllPermissions() {
        Authentication auth = createAuthentication("superadmin", UserRole.SUPER_ADMIN);

        // Test sample of permissions across all categories
        assertThat(permissionEvaluator.hasPermission(auth, null, "USER_READ")).isTrue();
        assertThat(permissionEvaluator.hasPermission(auth, null, "USER_WRITE")).isTrue();
        assertThat(permissionEvaluator.hasPermission(auth, null, "USER_DELETE")).isTrue();
        assertThat(permissionEvaluator.hasPermission(auth, null, "PATIENT_READ")).isTrue();
        assertThat(permissionEvaluator.hasPermission(auth, null, "PATIENT_WRITE")).isTrue();
        assertThat(permissionEvaluator.hasPermission(auth, null, "MEASURE_PUBLISH")).isTrue();
        assertThat(permissionEvaluator.hasPermission(auth, null, "TENANT_MANAGE")).isTrue();
    }

    // --- ADMIN Role Tests ---

    @Test
    @DisplayName("ADMIN should have tenant-level permissions")
    void admin_ShouldHaveTenantPermissions() {
        Authentication auth = createAuthentication("admin", UserRole.ADMIN);

        // Should have most permissions
        assertThat(permissionEvaluator.hasPermission(auth, null, "USER_READ")).isTrue();
        assertThat(permissionEvaluator.hasPermission(auth, null, "USER_WRITE")).isTrue();
        assertThat(permissionEvaluator.hasPermission(auth, null, "PATIENT_READ")).isTrue();
        assertThat(permissionEvaluator.hasPermission(auth, null, "MEASURE_PUBLISH")).isTrue();

        // Should NOT have SUPER_ADMIN-only permissions
        assertThat(permissionEvaluator.hasPermission(auth, null, "USER_DELETE")).isFalse();
        assertThat(permissionEvaluator.hasPermission(auth, null, "PATIENT_DELETE")).isFalse();
        assertThat(permissionEvaluator.hasPermission(auth, null, "TENANT_MANAGE")).isFalse();
    }

    // --- CLINICIAN Role Tests ---

    @Test
    @DisplayName("CLINICIAN should have patient care permissions")
    void clinician_ShouldHavePatientCarePermissions() {
        Authentication auth = createAuthentication("clinician", UserRole.CLINICIAN);

        // Should have patient and care gap access
        assertThat(permissionEvaluator.hasPermission(auth, null, "PATIENT_READ")).isTrue();
        assertThat(permissionEvaluator.hasPermission(auth, null, "PATIENT_WRITE")).isTrue();
        assertThat(permissionEvaluator.hasPermission(auth, null, "CARE_GAP_READ")).isTrue();
        assertThat(permissionEvaluator.hasPermission(auth, null, "CARE_GAP_WRITE")).isTrue();
        assertThat(permissionEvaluator.hasPermission(auth, null, "CARE_GAP_CLOSE")).isTrue();

        // Should NOT have admin permissions
        assertThat(permissionEvaluator.hasPermission(auth, null, "USER_WRITE")).isFalse();
        assertThat(permissionEvaluator.hasPermission(auth, null, "MEASURE_WRITE")).isFalse();
        assertThat(permissionEvaluator.hasPermission(auth, null, "CONFIG_WRITE")).isFalse();
    }

    // --- VIEWER Role Tests ---

    @Test
    @DisplayName("VIEWER should have read-only permissions")
    void viewer_ShouldHaveReadOnlyPermissions() {
        Authentication auth = createAuthentication("viewer", UserRole.VIEWER);

        // Should have read permissions only
        assertThat(permissionEvaluator.hasPermission(auth, null, "MEASURE_READ")).isTrue();
        assertThat(permissionEvaluator.hasPermission(auth, null, "REPORT_READ")).isTrue();

        // Should NOT have write permissions
        assertThat(permissionEvaluator.hasPermission(auth, null, "PATIENT_READ")).isFalse();
        assertThat(permissionEvaluator.hasPermission(auth, null, "MEASURE_WRITE")).isFalse();
        assertThat(permissionEvaluator.hasPermission(auth, null, "REPORT_CREATE")).isFalse();
    }

    // --- RESTRICTED Role Tests ---

    @Test
    @DisplayName("RESTRICTED should have no permissions")
    void restricted_ShouldHaveNoPermissions() {
        Authentication auth = createAuthentication("restricted", UserRole.RESTRICTED);

        // Should have no permissions
        assertThat(permissionEvaluator.hasPermission(auth, null, "PATIENT_READ")).isFalse();
        assertThat(permissionEvaluator.hasPermission(auth, null, "MEASURE_READ")).isFalse();
        assertThat(permissionEvaluator.hasPermission(auth, null, "REPORT_READ")).isFalse();
    }

    // --- Multi-Role Tests ---

    @Test
    @DisplayName("User with multiple roles should have combined permissions")
    void multipleRoles_ShouldHaveCombinedPermissions() {
        Authentication auth = createAuthentication("user",
            UserRole.CLINICIAN,
            UserRole.EVALUATOR
        );

        // Clinician permissions
        assertThat(permissionEvaluator.hasPermission(auth, null, "PATIENT_READ")).isTrue();
        assertThat(permissionEvaluator.hasPermission(auth, null, "CARE_GAP_WRITE")).isTrue();

        // Evaluator permissions
        assertThat(permissionEvaluator.hasPermission(auth, null, "MEASURE_EXECUTE")).isTrue();
        assertThat(permissionEvaluator.hasPermission(auth, null, "REPORT_CREATE")).isTrue();

        // Neither role has these permissions
        assertThat(permissionEvaluator.hasPermission(auth, null, "USER_WRITE")).isFalse();
        assertThat(permissionEvaluator.hasPermission(auth, null, "MEASURE_PUBLISH")).isFalse();
    }

    // --- Permission Category Tests ---

    @Test
    @DisplayName("QUALITY_OFFICER should have quality measurement permissions")
    void qualityOfficer_ShouldHaveQualityPermissions() {
        Authentication auth = createAuthentication("qo", UserRole.QUALITY_OFFICER);

        // Quality measurement permissions
        assertThat(permissionEvaluator.hasPermission(auth, null, "MEASURE_READ")).isTrue();
        assertThat(permissionEvaluator.hasPermission(auth, null, "MEASURE_EXECUTE")).isTrue();
        assertThat(permissionEvaluator.hasPermission(auth, null, "MEASURE_PUBLISH")).isTrue();

        // Audit permissions
        assertThat(permissionEvaluator.hasPermission(auth, null, "AUDIT_READ")).isTrue();
        assertThat(permissionEvaluator.hasPermission(auth, null, "AUDIT_REVIEW")).isTrue();

        // Limited patient access (read-only for quality review)
        assertThat(permissionEvaluator.hasPermission(auth, null, "PATIENT_READ")).isTrue();
        assertThat(permissionEvaluator.hasPermission(auth, null, "PATIENT_WRITE")).isFalse();
    }

    @Test
    @DisplayName("AUDITOR should have compliance permissions")
    void auditor_ShouldHaveCompliancePermissions() {
        Authentication auth = createAuthentication("auditor", UserRole.AUDITOR);

        // Audit permissions
        assertThat(permissionEvaluator.hasPermission(auth, null, "AUDIT_READ")).isTrue();
        assertThat(permissionEvaluator.hasPermission(auth, null, "AUDIT_EXPORT")).isTrue();
        assertThat(permissionEvaluator.hasPermission(auth, null, "AUDIT_REVIEW")).isTrue();

        // Read-only patient access for auditing
        assertThat(permissionEvaluator.hasPermission(auth, null, "PATIENT_READ")).isTrue();
        assertThat(permissionEvaluator.hasPermission(auth, null, "PATIENT_WRITE")).isFalse();

        // Reporting permissions
        assertThat(permissionEvaluator.hasPermission(auth, null, "REPORT_READ")).isTrue();
        assertThat(permissionEvaluator.hasPermission(auth, null, "REPORT_EXPORT")).isTrue();
    }

    @Test
    @DisplayName("DEVELOPER should have API permissions")
    void developer_ShouldHaveApiPermissions() {
        Authentication auth = createAuthentication("dev", UserRole.DEVELOPER);

        // API permissions
        assertThat(permissionEvaluator.hasPermission(auth, null, "API_READ")).isTrue();
        assertThat(permissionEvaluator.hasPermission(auth, null, "API_WRITE")).isTrue();
        assertThat(permissionEvaluator.hasPermission(auth, null, "API_MANAGE_KEYS")).isTrue();

        // Configuration access
        assertThat(permissionEvaluator.hasPermission(auth, null, "CONFIG_READ")).isTrue();
        assertThat(permissionEvaluator.hasPermission(auth, null, "INTEGRATION_MANAGE")).isTrue();

        // No PHI access (synthetic data only)
        assertThat(permissionEvaluator.hasPermission(auth, null, "PATIENT_READ")).isFalse();
        assertThat(permissionEvaluator.hasPermission(auth, null, "PATIENT_WRITE")).isFalse();
    }

    // --- Invalid Permission Tests ---

    @Test
    @DisplayName("Should deny permission for invalid permission name")
    void shouldDenyPermission_ForInvalidPermissionName() {
        Authentication auth = createAuthentication("admin", UserRole.ADMIN);

        boolean hasPermission = permissionEvaluator.hasPermission(auth, null, "INVALID_PERMISSION");

        assertThat(hasPermission).isFalse();
    }

    // --- Target Object Tests ---

    @Test
    @DisplayName("Should evaluate permission with target object")
    void shouldEvaluatePermission_WithTargetObject() {
        Authentication auth = createAuthentication("clinician", UserRole.CLINICIAN);
        Object targetObject = new Object(); // Mock domain object

        boolean hasPermission = permissionEvaluator.hasPermission(
            auth, targetObject, "PATIENT_READ");

        assertThat(hasPermission).isTrue();
    }

    @Test
    @DisplayName("Should evaluate permission with target ID and type")
    void shouldEvaluatePermission_WithTargetIdAndType() {
        Authentication auth = createAuthentication("clinician", UserRole.CLINICIAN);

        boolean hasPermission = permissionEvaluator.hasPermission(
            auth, "123", "Patient", "PATIENT_READ");

        assertThat(hasPermission).isTrue();
    }

    // --- Utility Method Tests ---

    @Test
    @DisplayName("hasPermission utility should work with Permission enum")
    void utilityMethod_ShouldWorkWithPermissionEnum() {
        Authentication auth = createAuthentication("admin", UserRole.ADMIN);

        boolean hasPermission = permissionEvaluator.hasPermission(auth, Permission.PATIENT_READ);

        assertThat(hasPermission).isTrue();
    }

    @Test
    @DisplayName("hasAnyPermission should return true if user has one permission")
    void hasAnyPermission_ShouldReturnTrue_IfUserHasOnePermission() {
        Authentication auth = createAuthentication("viewer", UserRole.VIEWER);

        boolean hasAnyPermission = permissionEvaluator.hasAnyPermission(
            auth,
            Permission.PATIENT_READ,  // VIEWER doesn't have this
            Permission.MEASURE_READ   // VIEWER has this
        );

        assertThat(hasAnyPermission).isTrue();
    }

    @Test
    @DisplayName("hasAllPermissions should return true only if user has all permissions")
    void hasAllPermissions_ShouldReturnTrue_OnlyIfUserHasAllPermissions() {
        Authentication auth = createAuthentication("admin", UserRole.ADMIN);

        // ADMIN has both
        boolean hasAll1 = permissionEvaluator.hasAllPermissions(
            auth,
            Permission.PATIENT_READ,
            Permission.MEASURE_READ
        );
        assertThat(hasAll1).isTrue();

        // ADMIN doesn't have TENANT_MANAGE (SUPER_ADMIN only)
        boolean hasAll2 = permissionEvaluator.hasAllPermissions(
            auth,
            Permission.PATIENT_READ,
            Permission.TENANT_MANAGE
        );
        assertThat(hasAll2).isFalse();
    }

    // --- Helper Methods ---

    private Authentication createAuthentication(String username, UserRole... roles) {
        List<GrantedAuthority> authorities = List.of(roles).stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
            .map(authority -> (GrantedAuthority) authority)
            .toList();

        return new Authentication() {
            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return authorities;
            }

            @Override
            public Object getCredentials() {
                return null;
            }

            @Override
            public Object getDetails() {
                return null;
            }

            @Override
            public Object getPrincipal() {
                return username;
            }

            @Override
            public boolean isAuthenticated() {
                return true;
            }

            @Override
            public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
            }

            @Override
            public String getName() {
                return username;
            }
        };
    }
}
