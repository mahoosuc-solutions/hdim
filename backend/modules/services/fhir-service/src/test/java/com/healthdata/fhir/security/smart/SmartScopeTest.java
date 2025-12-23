package com.healthdata.fhir.security.smart;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SMART Scope Tests")
class SmartScopeTest {

    @Test
    @DisplayName("Should list all scope strings")
    void shouldListAllScopeStrings() {
        assertThat(SmartScope.getAllScopeStrings()).contains("launch", "patient/*.read");
    }

    @Test
    @DisplayName("Should validate scopes")
    void shouldValidateScopes() {
        Set<String> scopes = Set.of("launch", "patient/Observation.read");
        assertThat(SmartScope.validateScopes(scopes)).isTrue();
    }

    @Test
    @DisplayName("Should reject unknown scopes")
    void shouldRejectUnknownScopes() {
        Set<String> scopes = Set.of("launch", "patient/NotAResource.read");
        assertThat(SmartScope.validateScopes(scopes)).isFalse();
    }

    @Test
    @DisplayName("Should resolve scope from string")
    void shouldResolveScopeFromString() {
        assertThat(SmartScope.fromString("launch")).isEqualTo(SmartScope.LAUNCH);
    }

    @Test
    @DisplayName("Should return null when scope is unknown")
    void shouldReturnNullWhenScopeUnknown() {
        assertThat(SmartScope.fromString("unknown/scope")).isNull();
    }

    @Test
    @DisplayName("Should return scopes by category")
    void shouldReturnScopesByCategory() {
        assertThat(SmartScope.getScopesByCategory(SmartScope.ScopeCategory.IDENTITY))
                .contains(SmartScope.OPENID, SmartScope.FHIR_USER, SmartScope.PROFILE, SmartScope.OFFLINE_ACCESS);
    }

    @Test
    @DisplayName("Should grant read access with wildcard")
    void shouldGrantReadAccessWithWildcard() {
        Set<String> scopes = Set.of("patient/*.read");
        assertThat(SmartScope.grantsReadAccess(scopes, "Observation", SmartScope.ScopeCategory.PATIENT)).isTrue();
    }

    @Test
    @DisplayName("Should grant read access with specific scope")
    void shouldGrantReadAccessWithSpecificScope() {
        Set<String> scopes = Set.of("user/Patient.read");
        assertThat(SmartScope.grantsReadAccess(scopes, "Patient", SmartScope.ScopeCategory.USER)).isTrue();
    }

    @Test
    @DisplayName("Should deny read access when scope missing")
    void shouldDenyReadAccessWhenScopeMissing() {
        Set<String> scopes = Set.of("patient/Observation.read");
        assertThat(SmartScope.grantsReadAccess(scopes, "Patient", SmartScope.ScopeCategory.PATIENT)).isFalse();
    }

    @Test
    @DisplayName("Should grant write access with wildcard and deny when missing")
    void shouldHandleWriteAccessScopes() {
        Set<String> scopes = Set.of("system/*.write");
        assertThat(SmartScope.grantsWriteAccess(scopes, "Patient", SmartScope.ScopeCategory.SYSTEM)).isTrue();
        assertThat(SmartScope.grantsWriteAccess(scopes, "Patient", SmartScope.ScopeCategory.USER)).isFalse();
    }
}
