# Admin Operations & Bootstrap System Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enable zero-touch bootstrap so `docker compose up` / K3s deployment → login → working dashboard with no manual SQL or scripts.

**Architecture:** A `DemoTenantBootstrap` ApplicationRunner in gateway-admin-service seeds demo tenant + users on first start. New `UserManagementController`, `PasswordController`, and `TenantManagementController` provide complete admin CRUD. Angular admin components are rewritten from mocks to call real APIs.

**Tech Stack:** Java 21, Spring Boot 3.x, Liquibase, BCrypt, Angular 17, Angular Material

**Spec:** `docs/superpowers/specs/2026-03-10-admin-operations-bootstrap-design.md`

---

## File Structure

### Layer A: Bootstrap

| Action | File | Responsibility |
|--------|------|----------------|
| Create | `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/bootstrap/DemoTenantBootstrap.java` | ApplicationRunner that seeds demo tenant + users on startup |
| Create | `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/bootstrap/DemoTenantProperties.java` | Config properties for `hdim.demo-tenant.enabled`, `id` |
| Modify | `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/domain/User.java` | Add `forcePasswordChange` field |
| Create | `backend/modules/services/gateway-admin-service/src/main/resources/db/changelog/db.changelog-master.xml` | Liquibase master changelog for gateway-admin |
| Create | `backend/modules/services/gateway-admin-service/src/main/resources/db/changelog/0001-add-force-password-change-column.xml` | Migration for new column |
| Modify | `backend/modules/services/gateway-admin-service/src/main/resources/application.yml` | Enable Liquibase, add demo-tenant config |
| Create | `backend/modules/services/gateway-admin-service/src/test/java/com/healthdata/gateway/admin/bootstrap/DemoTenantBootstrapTest.java` | Unit tests |

### Layer B: Backend APIs

| Action | File | Responsibility |
|--------|------|----------------|
| Create | `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/controller/UserManagementController.java` | User CRUD endpoints |
| Create | `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/controller/PasswordController.java` | Password change/force-change endpoints |
| Create | `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/controller/TenantManagementController.java` | Tenant list/detail/update endpoints |
| Create | `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/service/UserManagementService.java` | User management business logic |
| Create | `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/dto/UpdateUserRequest.java` | DTO for user updates |
| Create | `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/dto/UserListResponse.java` | DTO for paginated user list |
| Create | `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/dto/TenantDetailResponse.java` | DTO for tenant detail |
| Create | `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/dto/ChangePasswordRequest.java` | DTO for password change |
| Create | `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/dto/TempPasswordResponse.java` | DTO for admin password reset |
| Modify | `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/dto/JwtAuthenticationResponse.java` | Add `mustChangePassword` field |
| Modify | `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/controller/AuthController.java` | Set `mustChangePassword` in login response |
| Modify | `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/repository/UserRepository.java` | Add paginated query methods |
| Create | `backend/modules/services/gateway-admin-service/src/main/resources/db/changelog/0002-add-deactivation-columns.xml` | Migration for deactivated_at, deactivated_by |
| Create | `backend/modules/services/gateway-admin-service/src/test/java/com/healthdata/gateway/admin/UserManagementControllerTest.java` | Controller tests |
| Create | `backend/modules/services/gateway-admin-service/src/test/java/com/healthdata/gateway/admin/PasswordControllerTest.java` | Controller tests |
| Create | `backend/modules/services/gateway-admin-service/src/test/java/com/healthdata/gateway/admin/TenantManagementControllerTest.java` | Controller tests |
| Create | `backend/modules/services/gateway-admin-service/src/test/java/com/healthdata/gateway/admin/UserManagementServiceTest.java` | Service tests |

### Layer C: Frontend

| Action | File | Responsibility |
|--------|------|----------------|
| Create | `apps/clinical-portal/src/app/services/user-management.service.ts` | HTTP client for user admin APIs |
| Create | `apps/clinical-portal/src/app/services/tenant-management.service.ts` | HTTP client for tenant admin APIs |
| Create | `apps/clinical-portal/src/app/services/password.service.ts` | HTTP client for password APIs |
| Rewrite | `apps/clinical-portal/src/app/pages/admin/admin-users.component.ts` | Full user management UI |
| Rewrite | `apps/clinical-portal/src/app/pages/admin/admin-tenant-settings.component.ts` | Tenant management UI |
| Rewrite | `apps/clinical-portal/src/app/pages/admin/admin-audit-logs.component.ts` | Real audit log viewer |
| Create | `apps/clinical-portal/src/app/pages/change-password/change-password.component.ts` | Force password change screen |
| Modify | `apps/clinical-portal/src/app/components/navigation/navigation.component.ts` | Add admin nav section |
| Modify | `apps/clinical-portal/src/app/app.routes.ts` | Add change-password route |
| Modify | `apps/clinical-portal/src/app/services/auth.service.ts` | Handle mustChangePassword redirect |

---

## Chunk 1: Layer A — Bootstrap & Demo Tenant

### Task 1: Add `forcePasswordChange` field to User entity

**Files:**
- Modify: `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/domain/User.java`

- [ ] **Step 1: Add field to User entity**

In `User.java`, add after the `emailVerified` field:

```java
@Column(name = "force_password_change", nullable = false)
private Boolean forcePasswordChange = false;
```

Add getter and setter:

```java
public Boolean getForcePasswordChange() {
    return forcePasswordChange;
}

public void setForcePasswordChange(Boolean forcePasswordChange) {
    this.forcePasswordChange = forcePasswordChange;
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd backend && ./gradlew :modules:shared:infrastructure:authentication:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/domain/User.java
git commit -m "feat(auth): add forcePasswordChange field to User entity"
```

---

### Task 2: Create Liquibase migration for force_password_change column

**Files:**
- Create: `backend/modules/services/gateway-admin-service/src/main/resources/db/changelog/db.changelog-master.xml`
- Create: `backend/modules/services/gateway-admin-service/src/main/resources/db/changelog/0001-add-force-password-change-column.xml`
- Modify: `backend/modules/services/gateway-admin-service/src/main/resources/application.yml`

- [ ] **Step 1: Create changelog directory**

Run: `mkdir -p backend/modules/services/gateway-admin-service/src/main/resources/db/changelog`

- [ ] **Step 2: Create master changelog**

Create `db.changelog-master.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.23.xsd">

    <include file="db/changelog/0001-add-force-password-change-column.xml"/>

</databaseChangeLog>
```

- [ ] **Step 3: Create migration file**

Create `0001-add-force-password-change-column.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.23.xsd">

    <changeSet id="0001-add-force-password-change" author="hdim-admin-bootstrap">
        <preConditions onFail="MARK_RAN">
            <not>
                <columnExists tableName="users" columnName="force_password_change"/>
            </not>
        </preConditions>
        <comment>Add force_password_change column to users table for first-login password reset</comment>
        <addColumn tableName="users">
            <column name="force_password_change" type="boolean" defaultValueBoolean="false">
                <constraints nullable="false"/>
            </column>
        </addColumn>
        <rollback>
            <dropColumn tableName="users" columnName="force_password_change"/>
        </rollback>
    </changeSet>

</databaseChangeLog>
```

- [ ] **Step 4: Enable Liquibase in gateway-admin-service application.yml**

Change lines 11-12 from:

```yaml
  liquibase:
    enabled: false  # gateway-service manages schema
```

To:

```yaml
  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.xml
```

- [ ] **Step 5: Verify compilation**

Run: `cd backend && ./gradlew :modules:services:gateway-admin-service:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add backend/modules/services/gateway-admin-service/src/main/resources/db/changelog/
git add backend/modules/services/gateway-admin-service/src/main/resources/application.yml
git commit -m "feat(auth): add Liquibase migration for force_password_change column"
```

---

### Task 3: Create DemoTenantProperties configuration

**Files:**
- Create: `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/bootstrap/DemoTenantProperties.java`
- Modify: `backend/modules/services/gateway-admin-service/src/main/resources/application.yml`

- [ ] **Step 1: Create properties class**

```java
package com.healthdata.authentication.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hdim.demo-tenant")
public class DemoTenantProperties {

    private boolean enabled = true;
    private String id = "demo";
    private String name = "HDIM Demo";
    private String adminUsername = "demo_admin";
    private String adminEmail = "demo_admin@hdim.local";
    private String adminPassword = "changeme123";
    private String analystUsername = "demo_analyst";
    private String analystEmail = "demo_analyst@hdim.local";
    private String viewerUsername = "demo_viewer";
    private String viewerEmail = "demo_viewer@hdim.local";

    // Getters and setters for all fields
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAdminUsername() { return adminUsername; }
    public void setAdminUsername(String adminUsername) { this.adminUsername = adminUsername; }
    public String getAdminEmail() { return adminEmail; }
    public void setAdminEmail(String adminEmail) { this.adminEmail = adminEmail; }
    public String getAdminPassword() { return adminPassword; }
    public void setAdminPassword(String adminPassword) { this.adminPassword = adminPassword; }
    public String getAnalystUsername() { return analystUsername; }
    public void setAnalystUsername(String analystUsername) { this.analystUsername = analystUsername; }
    public String getAnalystEmail() { return analystEmail; }
    public void setAnalystEmail(String analystEmail) { this.analystEmail = analystEmail; }
    public String getViewerUsername() { return viewerUsername; }
    public void setViewerUsername(String viewerUsername) { this.viewerUsername = viewerUsername; }
    public String getViewerEmail() { return viewerEmail; }
    public void setViewerEmail(String viewerEmail) { this.viewerEmail = viewerEmail; }
}
```

- [ ] **Step 2: Add config to application.yml**

Append to `gateway-admin-service/src/main/resources/application.yml`:

```yaml
hdim:
  demo-tenant:
    enabled: ${HDIM_DEMO_TENANT_ENABLED:true}
    id: ${HDIM_DEMO_TENANT_ID:demo}
    name: ${HDIM_DEMO_TENANT_NAME:HDIM Demo}
    admin-username: ${HDIM_DEMO_ADMIN_USERNAME:demo_admin}
    admin-email: ${HDIM_DEMO_ADMIN_EMAIL:demo_admin@hdim.local}
    admin-password: ${HDIM_DEMO_ADMIN_PASSWORD:changeme123}
    analyst-username: ${HDIM_DEMO_ANALYST_USERNAME:demo_analyst}
    analyst-email: ${HDIM_DEMO_ANALYST_EMAIL:demo_analyst@hdim.local}
    viewer-username: ${HDIM_DEMO_VIEWER_USERNAME:demo_viewer}
    viewer-email: ${HDIM_DEMO_VIEWER_EMAIL:demo_viewer@hdim.local}
```

- [ ] **Step 3: Verify compilation**

Run: `cd backend && ./gradlew :modules:shared:infrastructure:authentication:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/bootstrap/DemoTenantProperties.java
git add backend/modules/services/gateway-admin-service/src/main/resources/application.yml
git commit -m "feat(auth): add DemoTenantProperties configuration"
```

---

### Task 4: Create DemoTenantBootstrap ApplicationRunner

**Files:**
- Create: `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/bootstrap/DemoTenantBootstrap.java`

**Context:**
- `UserRepository` at: `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/repository/UserRepository.java`
- `TenantRepository` at: `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/repository/TenantRepository.java`
- `User` entity at: `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/domain/User.java`
- `Tenant` entity at: `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/domain/Tenant.java`
- `UserRole` enum at: `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/domain/UserRole.java`
- `TenantStatus` used in Tenant: values are `ACTIVE`, `SUSPENDED`, `INACTIVE`

- [ ] **Step 1: Write the failing test**

Create `backend/modules/services/gateway-admin-service/src/test/java/com/healthdata/gateway/admin/bootstrap/DemoTenantBootstrapTest.java`:

```java
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("DemoTenantBootstrap")
class DemoTenantBootstrapTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private DemoTenantProperties properties;
    private DemoTenantBootstrap bootstrap;

    @BeforeEach
    void setUp() {
        properties = new DemoTenantProperties();
        bootstrap = new DemoTenantBootstrap(tenantRepository, userRepository, passwordEncoder, properties);
    }

    @Test
    @DisplayName("should create demo tenant on first run")
    void shouldCreateDemoTenantOnFirstRun() throws Exception {
        when(tenantRepository.existsByIdIgnoreCase("demo")).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encoded");

        bootstrap.run();

        ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).save(tenantCaptor.capture());
        Tenant tenant = tenantCaptor.getValue();
        assertThat(tenant.getId()).isEqualTo("demo");
        assertThat(tenant.getName()).isEqualTo("HDIM Demo");
    }

    @Test
    @DisplayName("should create three demo users with correct roles")
    void shouldCreateThreeDemoUsersWithCorrectRoles() throws Exception {
        when(tenantRepository.existsByIdIgnoreCase("demo")).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encoded");

        bootstrap.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(3)).save(userCaptor.capture());

        User admin = userCaptor.getAllValues().stream()
            .filter(u -> u.getUsername().equals("demo_admin")).findFirst().orElseThrow();
        assertThat(admin.getRoles()).contains(UserRole.ADMIN, UserRole.EVALUATOR);
        assertThat(admin.getTenantIds()).contains("demo");
        assertThat(admin.getForcePasswordChange()).isTrue();
    }

    @Test
    @DisplayName("should skip seeding when tenant already exists")
    void shouldSkipSeedingWhenTenantAlreadyExists() throws Exception {
        when(tenantRepository.existsByIdIgnoreCase("demo")).thenReturn(true);

        bootstrap.run();

        verify(tenantRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("should skip seeding when disabled")
    void shouldSkipSeedingWhenDisabled() throws Exception {
        properties.setEnabled(false);

        bootstrap.run();

        verify(tenantRepository, never()).save(any());
    }

    @Test
    @DisplayName("should skip existing users but create missing ones")
    void shouldSkipExistingUsersButCreateMissingOnes() throws Exception {
        when(tenantRepository.existsByIdIgnoreCase("demo")).thenReturn(false);
        when(userRepository.existsByUsername("demo_admin")).thenReturn(true);
        when(userRepository.existsByUsername("demo_analyst")).thenReturn(false);
        when(userRepository.existsByUsername("demo_viewer")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encoded");

        bootstrap.run();

        verify(userRepository, times(2)).save(any(User.class));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew :modules:services:gateway-admin-service:test --tests "*DemoTenantBootstrapTest" -x testIntegration`
Expected: FAIL — `DemoTenantBootstrap` class does not exist

- [ ] **Step 3: Implement DemoTenantBootstrap**

Create `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/bootstrap/DemoTenantBootstrap.java`:

```java
package com.healthdata.authentication.bootstrap;

import com.healthdata.authentication.domain.Tenant;
import com.healthdata.authentication.domain.User;
import com.healthdata.authentication.domain.UserRole;
import com.healthdata.authentication.repository.TenantRepository;
import com.healthdata.authentication.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Component
@EnableConfigurationProperties(DemoTenantProperties.class)
public class DemoTenantBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoTenantBootstrap.class);

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DemoTenantProperties properties;

    public DemoTenantBootstrap(
            TenantRepository tenantRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            DemoTenantProperties properties) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            log.info("Demo tenant bootstrap disabled (hdim.demo-tenant.enabled=false)");
            return;
        }

        if (tenantRepository.existsByIdIgnoreCase(properties.getId())) {
            log.info("Demo tenant '{}' already exists — skipping bootstrap", properties.getId());
            return;
        }

        log.info("Bootstrapping demo tenant '{}'...", properties.getId());

        Tenant tenant = new Tenant();
        tenant.setId(properties.getId());
        tenant.setName(properties.getName());
        tenant.activate();
        tenantRepository.save(tenant);

        String encodedPassword = passwordEncoder.encode(properties.getAdminPassword());

        createUserIfMissing(
            properties.getAdminUsername(),
            properties.getAdminEmail(),
            "Demo", "Admin",
            encodedPassword,
            Set.of(UserRole.ADMIN, UserRole.EVALUATOR),
            properties.getId()
        );

        createUserIfMissing(
            properties.getAnalystUsername(),
            properties.getAnalystEmail(),
            "Demo", "Analyst",
            encodedPassword,
            Set.of(UserRole.ANALYST, UserRole.EVALUATOR),
            properties.getId()
        );

        createUserIfMissing(
            properties.getViewerUsername(),
            properties.getViewerEmail(),
            "Demo", "Viewer",
            encodedPassword,
            Set.of(UserRole.VIEWER),
            properties.getId()
        );

        log.info("Demo tenant bootstrap complete:");
        log.info("  Tenant: {} ({})", properties.getName(), properties.getId());
        log.info("  Users: {}, {}, {}", properties.getAdminUsername(), properties.getAnalystUsername(), properties.getViewerUsername());
        log.info("  Default password: {} (force change on first login)", properties.getAdminPassword());
    }

    private void createUserIfMissing(
            String username, String email, String firstName, String lastName,
            String encodedPassword, Set<UserRole> roles, String tenantId) {

        if (userRepository.existsByUsername(username)) {
            log.debug("User '{}' already exists — skipping", username);
            return;
        }

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPasswordHash(encodedPassword);
        user.setRoles(roles);
        user.setTenantIds(Set.of(tenantId));
        user.setActive(true);
        user.setEmailVerified(true);
        user.setMfaEnabled(false);
        user.setForcePasswordChange(true);
        user.setFailedLoginAttempts(0);

        userRepository.save(user);
        log.info("  Created user: {} ({})", username, roles);
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd backend && ./gradlew :modules:services:gateway-admin-service:test --tests "*DemoTenantBootstrapTest" -x testIntegration`
Expected: All 5 tests PASS

- [ ] **Step 5: Commit**

```bash
git add backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/bootstrap/
git add backend/modules/services/gateway-admin-service/src/test/java/com/healthdata/gateway/admin/bootstrap/
git commit -m "feat(auth): implement DemoTenantBootstrap with idempotent user seeding"
```

---

### Task 5: Update K3s deployment for bootstrap

**Files:**
- Modify: K3s configmap or deployment to include `HDIM_DEMO_TENANT_ENABLED=true`

- [ ] **Step 1: Find and update K3s gateway-admin deployment**

Check the K3s deployment config for gateway-admin-service. The `SPRING_JPA_HIBERNATE_DDL_AUTO=update` env var is already set. Add the demo tenant env vars to the same location:

```yaml
- name: HDIM_DEMO_TENANT_ENABLED
  value: "true"
```

- [ ] **Step 2: Verify gateway-admin-service restarts cleanly with bootstrap**

Run: `kubectl rollout restart deployment/gateway-admin-service -n hdim-demo`
Then: `kubectl logs -n hdim-demo deployment/gateway-admin-service --tail=30 -f`
Expected: Log output showing "Bootstrapping demo tenant 'demo'..." and three user creation lines

- [ ] **Step 3: Verify login works**

Run: `curl -s -X POST http://localhost:80/api/v1/auth/login -H "Host: api.healthdatainmotion.com" -H "Content-Type: application/json" -d '{"username":"demo_admin","password":"changeme123"}'`
Expected: 200 OK with JWT token and `"mustChangePassword": true`

- [ ] **Step 4: Commit**

```bash
git add k8s/
git commit -m "feat(k8s): enable demo tenant bootstrap in K3s deployment"
```

---

### Task 6: Verify end-to-end bootstrap in browser

- [ ] **Step 1: Open browser to http://demo.healthdatainmotion.com**
Expected: Login page loads

- [ ] **Step 2: Log in with demo_admin / changeme123**
Expected: Login succeeds (may redirect to password change screen once Layer C is built; for now, should reach dashboard)

- [ ] **Step 3: Verify demo data seeding status**

Navigate to `/admin/demo-seeding` (already functional).
Expected: Demo seeding page loads, showing scenarios and system status

---

## Chunk 2: Layer B — Backend Admin APIs

### Task 7: Add deactivation columns via Liquibase

**Files:**
- Create: `backend/modules/services/gateway-admin-service/src/main/resources/db/changelog/0002-add-deactivation-columns.xml`
- Modify: `backend/modules/services/gateway-admin-service/src/main/resources/db/changelog/db.changelog-master.xml`
- Modify: `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/domain/User.java`

- [ ] **Step 1: Create migration**

Create `0002-add-deactivation-columns.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.23.xsd">

    <changeSet id="0002-add-deactivation-columns" author="hdim-admin-bootstrap">
        <preConditions onFail="MARK_RAN">
            <not>
                <columnExists tableName="users" columnName="deactivated_at"/>
            </not>
        </preConditions>
        <comment>Add deactivation tracking columns to users table</comment>
        <addColumn tableName="users">
            <column name="deactivated_at" type="timestamp(6) with time zone"/>
            <column name="deactivated_by" type="uuid"/>
        </addColumn>
        <rollback>
            <dropColumn tableName="users" columnName="deactivated_at"/>
            <dropColumn tableName="users" columnName="deactivated_by"/>
        </rollback>
    </changeSet>

</databaseChangeLog>
```

- [ ] **Step 2: Add to master changelog**

Add to `db.changelog-master.xml`:

```xml
    <include file="db/changelog/0002-add-deactivation-columns.xml"/>
```

- [ ] **Step 3: Add fields to User entity**

In `User.java`, add:

```java
@Column(name = "deactivated_at")
private Instant deactivatedAt;

@Column(name = "deactivated_by")
private UUID deactivatedBy;
```

With getters and setters.

- [ ] **Step 4: Commit**

```bash
git add backend/modules/services/gateway-admin-service/src/main/resources/db/changelog/
git add backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/domain/User.java
git commit -m "feat(auth): add deactivation tracking columns to users table"
```

---

### Task 8: Add mustChangePassword to login response

**Files:**
- Modify: `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/dto/JwtAuthenticationResponse.java`
- Modify: `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/controller/AuthController.java`

- [ ] **Step 1: Add field to JwtAuthenticationResponse**

In `JwtAuthenticationResponse.java`, add field:

```java
private Boolean mustChangePassword;
```

With getter and setter (or if using builder pattern, add to builder).

- [ ] **Step 2: Set field in AuthController.login()**

In `AuthController.java`, find the login method where `JwtAuthenticationResponse` is constructed. After setting other fields, add:

```java
response.setMustChangePassword(user.getForcePasswordChange());
```

Where `user` is the authenticated `User` entity. Locate the exact line by finding where `JwtAuthenticationResponse` is built in the login flow.

- [ ] **Step 3: Verify compilation**

Run: `cd backend && ./gradlew :modules:shared:infrastructure:authentication:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/dto/JwtAuthenticationResponse.java
git add backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/controller/AuthController.java
git commit -m "feat(auth): add mustChangePassword to login response"
```

---

### Task 9: Create DTOs for admin APIs

**Files:**
- Create: `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/dto/UpdateUserRequest.java`
- Create: `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/dto/ChangePasswordRequest.java`
- Create: `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/dto/ForceChangePasswordRequest.java`
- Create: `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/dto/TempPasswordResponse.java`
- Create: `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/dto/TenantDetailResponse.java`
- Create: `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/dto/UpdateTenantRequest.java`

- [ ] **Step 1: Create all DTOs**

`UpdateUserRequest.java`:
```java
package com.healthdata.authentication.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public class UpdateUserRequest {
    @Size(max = 100) private String firstName;
    @Size(max = 100) private String lastName;
    @Email @Size(max = 100) private String email;
    @Size(max = 500) private String notes;

    // Getters and setters
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
```

`ChangePasswordRequest.java`:
```java
package com.healthdata.authentication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChangePasswordRequest {
    @NotBlank private String currentPassword;
    @NotBlank @Size(min = 8) private String newPassword;

    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
```

`ForceChangePasswordRequest.java`:
```java
package com.healthdata.authentication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ForceChangePasswordRequest {
    @NotBlank @Size(min = 8) private String newPassword;

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
```

`TempPasswordResponse.java`:
```java
package com.healthdata.authentication.dto;

public class TempPasswordResponse {
    private String temporaryPassword;
    private String message;

    public TempPasswordResponse(String temporaryPassword) {
        this.temporaryPassword = temporaryPassword;
        this.message = "Temporary password set. User must change password on next login.";
    }

    public String getTemporaryPassword() { return temporaryPassword; }
    public String getMessage() { return message; }
}
```

`TenantDetailResponse.java`:
```java
package com.healthdata.authentication.dto;

import java.time.Instant;

public class TenantDetailResponse {
    private String id;
    private String name;
    private String status;
    private long userCount;
    private Instant createdAt;
    private Instant updatedAt;

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public long getUserCount() { return userCount; }
    public void setUserCount(long userCount) { this.userCount = userCount; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
```

`UpdateTenantRequest.java`:
```java
package com.healthdata.authentication.dto;

import jakarta.validation.constraints.Size;

public class UpdateTenantRequest {
    @Size(max = 255) private String name;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd backend && ./gradlew :modules:shared:infrastructure:authentication:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/dto/
git commit -m "feat(auth): add DTOs for admin user management and password APIs"
```

---

### Task 10: Create UserManagementService

**Files:**
- Create: `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/service/UserManagementService.java`
- Create: `backend/modules/services/gateway-admin-service/src/test/java/com/healthdata/gateway/admin/UserManagementServiceTest.java`

- [ ] **Step 1: Write the failing test**

Create `UserManagementServiceTest.java`:

```java
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew :modules:services:gateway-admin-service:test --tests "*UserManagementServiceTest" -x testIntegration`
Expected: FAIL — `UserManagementService` does not exist

- [ ] **Step 3: Implement UserManagementService**

Create `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/service/UserManagementService.java`:

```java
package com.healthdata.authentication.service;

import com.healthdata.authentication.domain.User;
import com.healthdata.authentication.domain.UserRole;
import com.healthdata.authentication.dto.UpdateUserRequest;
import com.healthdata.authentication.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserManagementService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String TEMP_PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789!@#$";

    public UserManagementService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User getUser(UUID id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }

    public List<User> getUsersByTenant(String tenantId) {
        return userRepository.findByTenantId(tenantId);
    }

    public List<User> getAllActiveUsers() {
        return userRepository.findByActiveTrue();
    }

    public List<User> getAllUsers() {
        return userRepository.findAllNonDeleted();
    }

    @Transactional
    public User updateUser(UUID id, UpdateUserRequest request) {
        User user = getUser(id);

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getNotes() != null) user.setNotes(request.getNotes());

        return userRepository.save(user);
    }

    @Transactional
    public User deactivateUser(UUID id, UUID actorId) {
        User user = getUser(id);
        user.setActive(false);
        user.setDeactivatedAt(Instant.now());
        user.setDeactivatedBy(actorId);
        return userRepository.save(user);
    }

    @Transactional
    public User reactivateUser(UUID id) {
        User user = getUser(id);
        user.setActive(true);
        user.setDeactivatedAt(null);
        user.setDeactivatedBy(null);
        return userRepository.save(user);
    }

    @Transactional
    public User updateRoles(UUID id, Set<UserRole> roles) {
        User user = getUser(id);
        user.setRoles(roles);
        return userRepository.save(user);
    }

    @Transactional
    public User updateTenants(UUID id, Set<String> tenantIds) {
        User user = getUser(id);
        user.setTenantIds(tenantIds);
        return userRepository.save(user);
    }

    @Transactional
    public User unlockAccount(UUID id) {
        User user = getUser(id);
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        return userRepository.save(user);
    }

    @Transactional
    public String resetPassword(UUID id) {
        User user = getUser(id);
        String tempPassword = generateTempPassword(12);
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setForcePasswordChange(true);
        userRepository.save(user);
        return tempPassword;
    }

    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        User user = getUser(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new RuntimeException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setForcePasswordChange(false);
        userRepository.save(user);
    }

    @Transactional
    public void forceChangePassword(UUID userId, String newPassword) {
        User user = getUser(userId);
        if (!user.getForcePasswordChange()) {
            throw new RuntimeException("Force password change is not required for this user");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setForcePasswordChange(false);
        userRepository.save(user);
    }

    private String generateTempPassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(TEMP_PASSWORD_CHARS.charAt(RANDOM.nextInt(TEMP_PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd backend && ./gradlew :modules:services:gateway-admin-service:test --tests "*UserManagementServiceTest" -x testIntegration`
Expected: All 6 tests PASS

- [ ] **Step 5: Commit**

```bash
git add backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/service/UserManagementService.java
git add backend/modules/services/gateway-admin-service/src/test/java/com/healthdata/gateway/admin/UserManagementServiceTest.java
git commit -m "feat(auth): implement UserManagementService with user CRUD operations"
```

---

### Task 11: Create UserManagementController

**Files:**
- Create: `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/controller/UserManagementController.java`
- Create: `backend/modules/services/gateway-admin-service/src/test/java/com/healthdata/gateway/admin/UserManagementControllerTest.java`

- [ ] **Step 1: Write the failing test**

Create `UserManagementControllerTest.java`:

```java
package com.healthdata.gateway.admin;

import com.healthdata.authentication.controller.UserManagementController;
import com.healthdata.authentication.domain.User;
import com.healthdata.authentication.domain.UserRole;
import com.healthdata.authentication.service.UserManagementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("UserManagementController")
class UserManagementControllerTest {

    @Mock private UserManagementService userManagementService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    private User testUser;

    @BeforeEach
    void setUp() {
        UserManagementController controller = new UserManagementController(userManagementService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("test_user");
        testUser.setEmail("test@hdim.local");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setActive(true);
        testUser.setRoles(Set.of(UserRole.VIEWER));
        testUser.setTenantIds(Set.of("demo"));
    }

    @Test
    @DisplayName("GET /api/v1/users should return all users")
    void shouldReturnAllUsers() throws Exception {
        when(userManagementService.getAllUsers()).thenReturn(List.of(testUser));

        mockMvc.perform(get("/api/v1/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].username").value("test_user"));
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} should return user by id")
    void shouldReturnUserById() throws Exception {
        when(userManagementService.getUser(testUser.getId())).thenReturn(testUser);

        mockMvc.perform(get("/api/v1/users/{id}", testUser.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("test_user"));
    }

    @Test
    @DisplayName("POST /api/v1/users/{id}/deactivate should deactivate user")
    void shouldDeactivateUser() throws Exception {
        testUser.setActive(false);
        when(userManagementService.deactivateUser(eq(testUser.getId()), any())).thenReturn(testUser);

        mockMvc.perform(post("/api/v1/users/{id}/deactivate", testUser.getId())
                .header("X-Auth-User-ID", UUID.randomUUID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/users/{id}/reset-password should return temp password")
    void shouldResetPassword() throws Exception {
        when(userManagementService.resetPassword(testUser.getId())).thenReturn("TempPass123!");

        mockMvc.perform(post("/api/v1/users/{id}/reset-password", testUser.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.temporaryPassword").value("TempPass123!"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew :modules:services:gateway-admin-service:test --tests "*UserManagementControllerTest" -x testIntegration`
Expected: FAIL — `UserManagementController` does not exist

- [ ] **Step 3: Implement UserManagementController**

Create `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/controller/UserManagementController.java`:

```java
package com.healthdata.authentication.controller;

import com.healthdata.authentication.domain.User;
import com.healthdata.authentication.domain.UserRole;
import com.healthdata.authentication.dto.TempPasswordResponse;
import com.healthdata.authentication.dto.UpdateUserRequest;
import com.healthdata.authentication.service.UserManagementService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserManagementController {

    private final UserManagementService userManagementService;

    public UserManagementController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping
    @PreAuthorize("hasPermission('USER_READ')")
    public ResponseEntity<List<User>> listUsers(
            @RequestParam(required = false) String tenantId) {
        List<User> users = (tenantId != null)
            ? userManagementService.getUsersByTenant(tenantId)
            : userManagementService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission('USER_READ')")
    public ResponseEntity<User> getUser(@PathVariable UUID id) {
        return ResponseEntity.ok(userManagementService.getUser(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission('USER_WRITE')")
    public ResponseEntity<User> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userManagementService.updateUser(id, request));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasPermission('USER_WRITE')")
    public ResponseEntity<User> deactivateUser(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Auth-User-ID", required = false) String actorIdStr) {
        UUID actorId = (actorIdStr != null) ? UUID.fromString(actorIdStr) : null;
        return ResponseEntity.ok(userManagementService.deactivateUser(id, actorId));
    }

    @PostMapping("/{id}/reactivate")
    @PreAuthorize("hasPermission('USER_WRITE')")
    public ResponseEntity<User> reactivateUser(@PathVariable UUID id) {
        return ResponseEntity.ok(userManagementService.reactivateUser(id));
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasPermission('USER_MANAGE_ROLES')")
    public ResponseEntity<User> updateRoles(
            @PathVariable UUID id,
            @RequestBody Set<UserRole> roles) {
        return ResponseEntity.ok(userManagementService.updateRoles(id, roles));
    }

    @PutMapping("/{id}/tenants")
    @PreAuthorize("hasPermission('USER_MANAGE_ROLES')")
    public ResponseEntity<User> updateTenants(
            @PathVariable UUID id,
            @RequestBody Set<String> tenantIds) {
        return ResponseEntity.ok(userManagementService.updateTenants(id, tenantIds));
    }

    @PostMapping("/{id}/unlock")
    @PreAuthorize("hasPermission('USER_WRITE')")
    public ResponseEntity<User> unlockAccount(@PathVariable UUID id) {
        return ResponseEntity.ok(userManagementService.unlockAccount(id));
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasPermission('USER_WRITE')")
    public ResponseEntity<TempPasswordResponse> resetPassword(@PathVariable UUID id) {
        String tempPassword = userManagementService.resetPassword(id);
        return ResponseEntity.ok(new TempPasswordResponse(tempPassword));
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd backend && ./gradlew :modules:services:gateway-admin-service:test --tests "*UserManagementControllerTest" -x testIntegration`
Expected: All 4 tests PASS

- [ ] **Step 5: Commit**

```bash
git add backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/controller/UserManagementController.java
git add backend/modules/services/gateway-admin-service/src/test/java/com/healthdata/gateway/admin/UserManagementControllerTest.java
git commit -m "feat(auth): implement UserManagementController with user CRUD endpoints"
```

---

### Task 12: Create PasswordController

**Files:**
- Create: `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/controller/PasswordController.java`
- Create: `backend/modules/services/gateway-admin-service/src/test/java/com/healthdata/gateway/admin/PasswordControllerTest.java`

- [ ] **Step 1: Write the failing test**

Create `PasswordControllerTest.java`:

```java
package com.healthdata.gateway.admin;

import com.healthdata.authentication.controller.PasswordController;
import com.healthdata.authentication.dto.ChangePasswordRequest;
import com.healthdata.authentication.dto.ForceChangePasswordRequest;
import com.healthdata.authentication.service.UserManagementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordController")
class PasswordControllerTest {

    @Mock private UserManagementService userManagementService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        PasswordController controller = new PasswordController(userManagementService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("POST /api/v1/auth/password/change should change password")
    void shouldChangePassword() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldpass123");
        request.setNewPassword("newpass456");

        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/auth/password/change")
                .header("X-Auth-User-ID", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        verify(userManagementService).changePassword(userId, "oldpass123", "newpass456");
    }

    @Test
    @DisplayName("POST /api/v1/auth/password/force-change should force change password")
    void shouldForceChangePassword() throws Exception {
        ForceChangePasswordRequest request = new ForceChangePasswordRequest();
        request.setNewPassword("newpass456");

        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/auth/password/force-change")
                .header("X-Auth-User-ID", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        verify(userManagementService).forceChangePassword(userId, "newpass456");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew :modules:services:gateway-admin-service:test --tests "*PasswordControllerTest" -x testIntegration`
Expected: FAIL

- [ ] **Step 3: Implement PasswordController**

Create `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/controller/PasswordController.java`:

```java
package com.healthdata.authentication.controller;

import com.healthdata.authentication.dto.ChangePasswordRequest;
import com.healthdata.authentication.dto.ForceChangePasswordRequest;
import com.healthdata.authentication.service.UserManagementService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth/password")
public class PasswordController {

    private final UserManagementService userManagementService;

    public PasswordController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @PostMapping("/change")
    public ResponseEntity<Map<String, String>> changePassword(
            @RequestHeader("X-Auth-User-ID") String userIdStr,
            @Valid @RequestBody ChangePasswordRequest request) {
        UUID userId = UUID.fromString(userIdStr);
        userManagementService.changePassword(userId, request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    @PostMapping("/force-change")
    public ResponseEntity<Map<String, String>> forceChangePassword(
            @RequestHeader("X-Auth-User-ID") String userIdStr,
            @Valid @RequestBody ForceChangePasswordRequest request) {
        UUID userId = UUID.fromString(userIdStr);
        userManagementService.forceChangePassword(userId, request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd backend && ./gradlew :modules:services:gateway-admin-service:test --tests "*PasswordControllerTest" -x testIntegration`
Expected: All 2 tests PASS

- [ ] **Step 5: Commit**

```bash
git add backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/controller/PasswordController.java
git add backend/modules/services/gateway-admin-service/src/test/java/com/healthdata/gateway/admin/PasswordControllerTest.java
git commit -m "feat(auth): implement PasswordController for password change and force-change"
```

---

### Task 13: Create TenantManagementController

**Files:**
- Create: `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/controller/TenantManagementController.java`
- Create: `backend/modules/services/gateway-admin-service/src/test/java/com/healthdata/gateway/admin/TenantManagementControllerTest.java`

- [ ] **Step 1: Write the failing test**

Create `TenantManagementControllerTest.java`:

```java
package com.healthdata.gateway.admin;

import com.healthdata.authentication.controller.TenantManagementController;
import com.healthdata.authentication.domain.Tenant;
import com.healthdata.authentication.domain.User;
import com.healthdata.authentication.dto.TenantDetailResponse;
import com.healthdata.authentication.repository.TenantRepository;
import com.healthdata.authentication.service.UserManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("TenantManagementController")
class TenantManagementControllerTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private UserManagementService userManagementService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        TenantManagementController controller = new TenantManagementController(tenantRepository, userManagementService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /api/v1/tenants should return all tenants")
    void shouldReturnAllTenants() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setId("demo");
        tenant.setName("HDIM Demo");
        tenant.activate();
        when(tenantRepository.findAll()).thenReturn(List.of(tenant));
        when(tenantRepository.countByStatus(any())).thenReturn(0L);

        mockMvc.perform(get("/api/v1/tenants"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("demo"));
    }

    @Test
    @DisplayName("GET /api/v1/tenants/{id}/users should return tenant users")
    void shouldReturnTenantUsers() throws Exception {
        when(userManagementService.getUsersByTenant("demo")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/tenants/demo/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew :modules:services:gateway-admin-service:test --tests "*TenantManagementControllerTest" -x testIntegration`
Expected: FAIL

- [ ] **Step 3: Implement TenantManagementController**

Create `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/controller/TenantManagementController.java`:

```java
package com.healthdata.authentication.controller;

import com.healthdata.authentication.domain.Tenant;
import com.healthdata.authentication.domain.User;
import com.healthdata.authentication.dto.TenantDetailResponse;
import com.healthdata.authentication.dto.UpdateTenantRequest;
import com.healthdata.authentication.repository.TenantRepository;
import com.healthdata.authentication.service.UserManagementService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tenants")
public class TenantManagementController {

    private final TenantRepository tenantRepository;
    private final UserManagementService userManagementService;

    public TenantManagementController(TenantRepository tenantRepository, UserManagementService userManagementService) {
        this.tenantRepository = tenantRepository;
        this.userManagementService = userManagementService;
    }

    @GetMapping
    @PreAuthorize("hasPermission('TENANT_MANAGE')")
    public ResponseEntity<List<TenantDetailResponse>> listTenants() {
        List<Tenant> tenants = tenantRepository.findAll();
        List<TenantDetailResponse> responses = tenants.stream().map(this::toDetailResponse).toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission('TENANT_MANAGE')")
    public ResponseEntity<TenantDetailResponse> getTenant(@PathVariable String id) {
        Tenant tenant = tenantRepository.findByIdIgnoreCase(id)
            .orElseThrow(() -> new RuntimeException("Tenant not found: " + id));
        return ResponseEntity.ok(toDetailResponse(tenant));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission('TENANT_MANAGE')")
    public ResponseEntity<TenantDetailResponse> updateTenant(
            @PathVariable String id,
            @Valid @RequestBody UpdateTenantRequest request) {
        Tenant tenant = tenantRepository.findByIdIgnoreCase(id)
            .orElseThrow(() -> new RuntimeException("Tenant not found: " + id));
        if (request.getName() != null) tenant.setName(request.getName());
        tenantRepository.save(tenant);
        return ResponseEntity.ok(toDetailResponse(tenant));
    }

    @GetMapping("/{id}/users")
    @PreAuthorize("hasPermission('TENANT_MANAGE')")
    public ResponseEntity<List<User>> getTenantUsers(@PathVariable String id) {
        return ResponseEntity.ok(userManagementService.getUsersByTenant(id));
    }

    private TenantDetailResponse toDetailResponse(Tenant tenant) {
        TenantDetailResponse response = new TenantDetailResponse();
        response.setId(tenant.getId());
        response.setName(tenant.getName());
        response.setStatus(tenant.getStatus().name());
        response.setUserCount(userManagementService.getUsersByTenant(tenant.getId()).size());
        response.setCreatedAt(tenant.getCreatedAt());
        response.setUpdatedAt(tenant.getUpdatedAt());
        return response;
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd backend && ./gradlew :modules:services:gateway-admin-service:test --tests "*TenantManagementControllerTest" -x testIntegration`
Expected: All 2 tests PASS

- [ ] **Step 5: Run all gateway-admin-service tests**

Run: `cd backend && ./gradlew :modules:services:gateway-admin-service:test -x testIntegration`
Expected: All tests PASS (existing + new)

- [ ] **Step 6: Commit**

```bash
git add backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/controller/TenantManagementController.java
git add backend/modules/services/gateway-admin-service/src/test/java/com/healthdata/gateway/admin/TenantManagementControllerTest.java
git commit -m "feat(auth): implement TenantManagementController with tenant CRUD endpoints"
```

---

## Chunk 3: Layer C — Frontend Admin UI

### Task 14: Create Angular services for admin APIs

**Files:**
- Create: `apps/clinical-portal/src/app/services/user-management.service.ts`
- Create: `apps/clinical-portal/src/app/services/tenant-management.service.ts`
- Create: `apps/clinical-portal/src/app/services/password.service.ts`

- [ ] **Step 1: Create UserManagementService**

Create `apps/clinical-portal/src/app/services/user-management.service.ts`:

```typescript
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface UserResponse {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  roles: string[];
  tenantIds: string[];
  active: boolean;
  emailVerified: boolean;
  mfaEnabled: boolean;
  forcePasswordChange: boolean;
  lastLoginAt: string | null;
  failedLoginAttempts: number;
  accountLockedUntil: string | null;
  notes: string | null;
}

export interface UpdateUserRequest {
  firstName?: string;
  lastName?: string;
  email?: string;
  notes?: string;
}

export interface TempPasswordResponse {
  temporaryPassword: string;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class UserManagementService {
  private readonly baseUrl = '/api/v1/users';

  constructor(private http: HttpClient) {}

  getUsers(tenantId?: string): Observable<UserResponse[]> {
    let params = new HttpParams();
    if (tenantId) params = params.set('tenantId', tenantId);
    return this.http.get<UserResponse[]>(this.baseUrl, { params });
  }

  getUser(id: string): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.baseUrl}/${id}`);
  }

  updateUser(id: string, request: UpdateUserRequest): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${this.baseUrl}/${id}`, request);
  }

  deactivateUser(id: string): Observable<UserResponse> {
    return this.http.post<UserResponse>(`${this.baseUrl}/${id}/deactivate`, {});
  }

  reactivateUser(id: string): Observable<UserResponse> {
    return this.http.post<UserResponse>(`${this.baseUrl}/${id}/reactivate`, {});
  }

  updateRoles(id: string, roles: string[]): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${this.baseUrl}/${id}/roles`, roles);
  }

  updateTenants(id: string, tenantIds: string[]): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${this.baseUrl}/${id}/tenants`, tenantIds);
  }

  unlockAccount(id: string): Observable<UserResponse> {
    return this.http.post<UserResponse>(`${this.baseUrl}/${id}/unlock`, {});
  }

  resetPassword(id: string): Observable<TempPasswordResponse> {
    return this.http.post<TempPasswordResponse>(`${this.baseUrl}/${id}/reset-password`, {});
  }
}
```

- [ ] **Step 2: Create TenantManagementService**

Create `apps/clinical-portal/src/app/services/tenant-management.service.ts`:

```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UserResponse } from './user-management.service';

export interface TenantResponse {
  id: string;
  name: string;
  status: string;
  userCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface UpdateTenantRequest {
  name?: string;
}

@Injectable({ providedIn: 'root' })
export class TenantManagementService {
  private readonly baseUrl = '/api/v1/tenants';

  constructor(private http: HttpClient) {}

  getTenants(): Observable<TenantResponse[]> {
    return this.http.get<TenantResponse[]>(this.baseUrl);
  }

  getTenant(id: string): Observable<TenantResponse> {
    return this.http.get<TenantResponse>(`${this.baseUrl}/${id}`);
  }

  updateTenant(id: string, request: UpdateTenantRequest): Observable<TenantResponse> {
    return this.http.put<TenantResponse>(`${this.baseUrl}/${id}`, request);
  }

  getTenantUsers(id: string): Observable<UserResponse[]> {
    return this.http.get<UserResponse[]>(`${this.baseUrl}/${id}/users`);
  }

  activateTenant(id: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${id}/activate`, {});
  }

  suspendTenant(id: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${id}/suspend`, {});
  }

  deactivateTenant(id: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${id}/deactivate`, {});
  }
}
```

- [ ] **Step 3: Create PasswordService**

Create `apps/clinical-portal/src/app/services/password.service.ts`:

```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface ForceChangePasswordRequest {
  newPassword: string;
}

@Injectable({ providedIn: 'root' })
export class PasswordService {
  private readonly baseUrl = '/api/v1/auth/password';

  constructor(private http: HttpClient) {}

  changePassword(request: ChangePasswordRequest): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.baseUrl}/change`, request);
  }

  forceChangePassword(request: ForceChangePasswordRequest): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.baseUrl}/force-change`, request);
  }
}
```

- [ ] **Step 4: Verify compilation**

Run: `cd apps/clinical-portal && npx ng build --configuration=development 2>&1 | tail -5`
Expected: Build succeeds (services are tree-shaken if unused, so no errors expected)

- [ ] **Step 5: Commit**

```bash
git add apps/clinical-portal/src/app/services/user-management.service.ts
git add apps/clinical-portal/src/app/services/tenant-management.service.ts
git add apps/clinical-portal/src/app/services/password.service.ts
git commit -m "feat(portal): add Angular services for user, tenant, and password management APIs"
```

---

### Task 15: Create ChangePasswordComponent

**Files:**
- Create: `apps/clinical-portal/src/app/pages/change-password/change-password.component.ts`
- Modify: `apps/clinical-portal/src/app/app.routes.ts`
- Modify: `apps/clinical-portal/src/app/services/auth.service.ts`

- [ ] **Step 1: Create ChangePasswordComponent**

Create `apps/clinical-portal/src/app/pages/change-password/change-password.component.ts`:

```typescript
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Router } from '@angular/router';
import { PasswordService } from '../../services/password.service';
import { LoggerService } from '../../services/logger.service';

@Component({
  selector: 'app-change-password',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatCardModule, MatFormFieldModule,
    MatInputModule, MatButtonModule, MatIconModule, MatProgressBarModule,
  ],
  template: `
    <div class="change-password-container">
      <mat-card class="change-password-card">
        <mat-card-header>
          <mat-card-title>Change Your Password</mat-card-title>
          <mat-card-subtitle>Your password must be changed before continuing.</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <form [formGroup]="form" (ngSubmit)="onSubmit()">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>New Password</mat-label>
              <input matInput [type]="hidePassword ? 'password' : 'text'" formControlName="newPassword"
                     aria-label="New password">
              <button mat-icon-button matSuffix type="button" (click)="hidePassword = !hidePassword"
                      [attr.aria-label]="hidePassword ? 'Show password' : 'Hide password'">
                <mat-icon aria-hidden="true">{{hidePassword ? 'visibility_off' : 'visibility'}}</mat-icon>
              </button>
              <mat-error *ngIf="form.get('newPassword')?.hasError('required')">Password is required</mat-error>
              <mat-error *ngIf="form.get('newPassword')?.hasError('minlength')">Minimum 8 characters</mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Confirm Password</mat-label>
              <input matInput [type]="hideConfirm ? 'password' : 'text'" formControlName="confirmPassword"
                     aria-label="Confirm new password">
              <button mat-icon-button matSuffix type="button" (click)="hideConfirm = !hideConfirm"
                      [attr.aria-label]="hideConfirm ? 'Show password' : 'Hide password'">
                <mat-icon aria-hidden="true">{{hideConfirm ? 'visibility_off' : 'visibility'}}</mat-icon>
              </button>
              <mat-error *ngIf="form.get('confirmPassword')?.hasError('passwordMismatch')">Passwords do not match</mat-error>
            </mat-form-field>

            <div class="password-strength" *ngIf="form.get('newPassword')?.value">
              <mat-progress-bar [value]="passwordStrength" [color]="strengthColor"></mat-progress-bar>
              <span class="strength-label">{{strengthLabel}}</span>
            </div>

            <div class="error-message" *ngIf="errorMessage">{{errorMessage}}</div>

            <button mat-raised-button color="primary" type="submit" class="full-width"
                    [disabled]="form.invalid || loading" aria-label="Change password">
              {{loading ? 'Changing...' : 'Change Password'}}
            </button>
          </form>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .change-password-container { display: flex; justify-content: center; align-items: center; min-height: 80vh; }
    .change-password-card { max-width: 400px; width: 100%; }
    .full-width { width: 100%; margin-bottom: 16px; }
    .password-strength { margin-bottom: 16px; }
    .strength-label { font-size: 12px; margin-top: 4px; display: block; }
    .error-message { color: #f44336; margin-bottom: 16px; font-size: 14px; }
  `],
})
export class ChangePasswordComponent {
  form: FormGroup;
  hidePassword = true;
  hideConfirm = true;
  loading = false;
  errorMessage = '';

  constructor(
    private fb: FormBuilder,
    private passwordService: PasswordService,
    private router: Router,
    private loggerService: LoggerService,
  ) {
    const logger = this.loggerService.withContext('ChangePasswordComponent');
    this.form = this.fb.group({
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required]],
    }, { validators: this.passwordMatchValidator });
  }

  passwordMatchValidator(control: AbstractControl) {
    const password = control.get('newPassword')?.value;
    const confirm = control.get('confirmPassword')?.value;
    if (password && confirm && password !== confirm) {
      control.get('confirmPassword')?.setErrors({ passwordMismatch: true });
    }
    return null;
  }

  get passwordStrength(): number {
    const pw = this.form.get('newPassword')?.value || '';
    let score = 0;
    if (pw.length >= 8) score += 25;
    if (pw.length >= 12) score += 25;
    if (/[A-Z]/.test(pw) && /[a-z]/.test(pw)) score += 25;
    if (/[0-9!@#$%^&*]/.test(pw)) score += 25;
    return score;
  }

  get strengthColor(): string {
    return this.passwordStrength >= 75 ? 'primary' : this.passwordStrength >= 50 ? 'accent' : 'warn';
  }

  get strengthLabel(): string {
    const s = this.passwordStrength;
    if (s >= 75) return 'Strong';
    if (s >= 50) return 'Good';
    if (s >= 25) return 'Weak';
    return 'Very weak';
  }

  onSubmit(): void {
    if (this.form.invalid) return;
    this.loading = true;
    this.errorMessage = '';

    this.passwordService.forceChangePassword({ newPassword: this.form.value.newPassword }).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Failed to change password. Please try again.';
      },
    });
  }
}
```

- [ ] **Step 2: Add route to app.routes.ts**

In `apps/clinical-portal/src/app/app.routes.ts`, add near the admin routes section:

```typescript
{
  path: 'change-password',
  loadComponent: () => import('./pages/change-password/change-password.component').then(m => m.ChangePasswordComponent),
  canActivate: [AuthGuard],
},
```

- [ ] **Step 3: Handle mustChangePassword in auth.service.ts**

In `apps/clinical-portal/src/app/services/auth.service.ts`, find the login method's success handler. After setting the user profile, add:

```typescript
// After successful login, check if password change is required
if (response.mustChangePassword) {
  this.router.navigate(['/change-password']);
  return;
}
```

Also add `mustChangePassword` to the `LoginResponse` interface:

```typescript
mustChangePassword?: boolean;
```

- [ ] **Step 4: Verify build**

Run: `cd apps/clinical-portal && npx ng build --configuration=development 2>&1 | tail -5`
Expected: Build succeeds

- [ ] **Step 5: Commit**

```bash
git add apps/clinical-portal/src/app/pages/change-password/
git add apps/clinical-portal/src/app/app.routes.ts
git add apps/clinical-portal/src/app/services/auth.service.ts
git commit -m "feat(portal): add ChangePasswordComponent with force-change redirect on login"
```

---

### Task 16: Rewrite AdminUsersComponent

**Files:**
- Rewrite: `apps/clinical-portal/src/app/pages/admin/admin-users.component.ts`

This is a large component. The implementation should:
- Use `UserManagementService` for all API calls
- Display paginated user table with Material table
- Support create/edit via dialog
- Support deactivate/reactivate with confirmation
- Support role management via multi-select
- Support password reset with temp password display

- [ ] **Step 1: Rewrite the component**

Replace the full contents of `apps/clinical-portal/src/app/pages/admin/admin-users.component.ts` with a production implementation that calls real APIs. The component should follow the pattern of the existing `AdminDemoSeedingComponent` (952 lines, fully functional) for structure and error handling.

Key patterns to follow from the existing codebase:
- Use `LoggerService` instead of `console.log`
- Use `MatDialog` for create/edit forms
- Use `MatSnackBar` for success/error notifications
- Use `MatTable` with `MatPaginator` for the user list
- Use `catchError` from RxJS for HTTP error handling

- [ ] **Step 2: Verify build**

Run: `cd apps/clinical-portal && npx ng build --configuration=development 2>&1 | tail -5`
Expected: Build succeeds

- [ ] **Step 3: Commit**

```bash
git add apps/clinical-portal/src/app/pages/admin/admin-users.component.ts
git commit -m "feat(portal): rewrite AdminUsersComponent with real API integration"
```

---

### Task 17: Rewrite AdminTenantSettingsComponent

**Files:**
- Rewrite: `apps/clinical-portal/src/app/pages/admin/admin-tenant-settings.component.ts`

- [ ] **Step 1: Rewrite the component**

Replace with a real implementation using `TenantManagementService`. Display tenant list with status badges, user counts, and status action buttons (activate/suspend/deactivate). Follow same patterns as Task 16.

- [ ] **Step 2: Verify build and commit**

```bash
cd apps/clinical-portal && npx ng build --configuration=development
git add apps/clinical-portal/src/app/pages/admin/admin-tenant-settings.component.ts
git commit -m "feat(portal): rewrite AdminTenantSettingsComponent with real API integration"
```

---

### Task 18: Rewrite AdminAuditLogsComponent

**Files:**
- Rewrite: `apps/clinical-portal/src/app/pages/admin/admin-audit-logs.component.ts`

- [ ] **Step 1: Rewrite the component**

Replace with a real implementation that calls the audit-query-service via the gateway forwarding path (`/api/v1/audit/logs/search`). Include date range filter, user filter, action type filter, and export buttons.

- [ ] **Step 2: Verify build and commit**

```bash
cd apps/clinical-portal && npx ng build --configuration=development
git add apps/clinical-portal/src/app/pages/admin/admin-audit-logs.component.ts
git commit -m "feat(portal): rewrite AdminAuditLogsComponent with real audit API integration"
```

---

### Task 19: Update navigation with admin section

**Files:**
- Modify: `apps/clinical-portal/src/app/components/navigation/navigation.component.ts`

- [ ] **Step 1: Add admin navigation items**

In the `navItems` array in `navigation.component.ts`, add an Administration section (visible only to ADMIN role). Add items for Users, Tenants, Audit Logs, and Demo Management. Use a divider or section header to separate from clinical navigation.

Check user role before showing admin items — use `AuthService.hasRole('ADMIN')` in the template with `*ngIf`.

- [ ] **Step 2: Verify build**

Run: `cd apps/clinical-portal && npx ng build --configuration=development 2>&1 | tail -5`
Expected: Build succeeds

- [ ] **Step 3: Commit**

```bash
git add apps/clinical-portal/src/app/components/navigation/navigation.component.ts
git commit -m "feat(portal): add Administration section to navigation sidebar"
```

---

### Task 20: End-to-end validation

- [ ] **Step 1: Rebuild gateway-admin-service Docker image**

Run: `cd backend && ./gradlew :modules:services:gateway-admin-service:bootJar -x test && docker compose build gateway-admin-service`

- [ ] **Step 2: Rebuild clinical-portal Docker image**

Run: `cd apps/clinical-portal && npm run build && docker build -t hdim-clinical-portal:latest .`

- [ ] **Step 3: Redeploy to K3s**

Restart the affected pods:
```bash
kubectl rollout restart deployment/gateway-admin-service -n hdim-demo
kubectl rollout restart deployment/clinical-portal -n hdim-demo
```

- [ ] **Step 4: Verify bootstrap**

Check gateway-admin logs for bootstrap output:
```bash
kubectl logs -n hdim-demo deployment/gateway-admin-service --tail=50 | grep -i "bootstrap\|demo tenant"
```
Expected: "Bootstrapping demo tenant 'demo'..." and user creation messages

- [ ] **Step 5: Verify login flow**

1. Open `http://demo.healthdatainmotion.com`
2. Log in with `demo_admin` / `changeme123`
3. Should redirect to `/change-password` (force password change)
4. Set new password
5. Should redirect to dashboard

- [ ] **Step 6: Verify admin UI**

1. Navigate to Administration → Users
2. Should see 3 demo users (demo_admin, demo_analyst, demo_viewer)
3. Navigate to Administration → Tenants
4. Should see "demo" tenant with ACTIVE status
5. Navigate to Administration → Audit Logs
6. Should show login and password change events

- [ ] **Step 7: Run full test suite**

Run: `cd backend && ./gradlew testFast`
Expected: All tests pass including new tests

- [ ] **Step 8: Final commit**

```bash
git add -A
git commit -m "feat(admin): complete admin operations bootstrap — Layer A+B+C

- DemoTenantBootstrap seeds tenant + 3 users on first start
- UserManagementController: list, get, update, deactivate, reactivate, roles, unlock, reset-password
- PasswordController: change, force-change
- TenantManagementController: list, get, update, users
- Angular admin UI wired to real APIs
- Navigation updated with admin section
- ChangePasswordComponent for force-change flow
- Liquibase migration for force_password_change column
- Demo tenant kill switch via HDIM_DEMO_TENANT_ENABLED env var"
```
