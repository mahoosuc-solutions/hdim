# User Management Alignment Plan
**Status:** In Progress
**Created:** January 25, 2026
**Priority:** High (Security & Compliance)

## Executive Summary

This plan ensures all HDIM services have aligned user management with secure registration, synchronization, and multi-tenant access control across Quality Measure, Care Gap, FHIR, and Patient services.

---

## Current State Analysis

### ✅ Completed Components

1. **Gateway Authentication (gateway-core)**
   - JWT validation from Authorization header or HttpOnly cookies
   - Trusted header injection (X-Auth-User-Id, X-Auth-Username, X-Auth-Tenant-Ids, X-Auth-Roles)
   - HMAC signature validation (X-Auth-Validated)
   - Public path registry for unauthenticated endpoints
   - Audit logging for authentication events

2. **User Entity (authentication module)**
   - UUID primary key
   - Multi-tenancy support (user_tenants table)
   - RBAC with UserRole enum
   - MFA support (TOTP and SMS)
   - OAuth2 integration fields
   - Account lockout protection
   - Audit timestamps (created_at, updated_at, last_login_at)

3. **Service Repositories**
   - Quality Measure Service: UserRepository ✅, TenantRepository ✅
   - Care Gap Service: UserRepository ✅, TenantRepository ✅
   - FHIR Service: UserRepository ✅, TenantRepository ✅
   - Patient Service: UserRepository ✅ (NEW), TenantRepository ✅ (NEW)

### ❌ Missing Components

1. **User Auto-Registration** - No automatic user sync on first access
2. **Database Migrations** - Users table not in all service databases
3. **Configuration** - Filter not registered in service configurations
4. **Testing** - No integration tests for user auto-registration
5. **Documentation** - Service-specific setup instructions missing

---

## Implementation Plan

### Phase 1: Core Infrastructure (COMPLETED)

**Goal:** Create shared filter and repository interfaces

**Deliverables:**
- [x] UserAutoRegistrationFilter in authentication module
- [x] UserRepository for Patient Service
- [x] TenantRepository for Patient Service

**Files Created:**
```
backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/filter/
└── UserAutoRegistrationFilter.java

backend/modules/services/patient-service/src/main/java/com/healthdata/patient/persistence/
├── UserRepository.java
└── TenantRepository.java
```

---

### Phase 2: Database Migrations (TODO)

**Goal:** Add users and user_tenants tables to all service databases

**Services to Update:**
1. Quality Measure Service
2. Care Gap Service
3. FHIR Service
4. Patient Service

**Migration Files to Create:**

#### Quality Measure Service
```xml
<!-- backend/modules/services/quality-measure-service/src/main/resources/db/changelog/0026-add-users-table.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <changeSet id="0026-add-users-table" author="system">
        <comment>
            Add users table for user auto-registration
            Supports multi-tenancy and MFA
        </comment>

        <!-- Users table -->
        <createTable tableName="users">
            <column name="id" type="UUID">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="username" type="VARCHAR(50)">
                <constraints nullable="false" unique="true"/>
            </column>
            <column name="email" type="VARCHAR(100)">
                <constraints nullable="false" unique="true"/>
            </column>
            <column name="password_hash" type="VARCHAR(255)">
                <constraints nullable="false"/>
            </column>
            <column name="first_name" type="VARCHAR(100)">
                <constraints nullable="false"/>
            </column>
            <column name="last_name" type="VARCHAR(100)">
                <constraints nullable="false"/>
            </column>
            <column name="active" type="BOOLEAN" defaultValueBoolean="true">
                <constraints nullable="false"/>
            </column>
            <column name="email_verified" type="BOOLEAN" defaultValueBoolean="false">
                <constraints nullable="false"/>
            </column>
            <column name="last_login_at" type="TIMESTAMP WITH TIME ZONE"/>
            <column name="failed_login_attempts" type="INTEGER" defaultValueNumeric="0"/>
            <column name="account_locked_until" type="TIMESTAMP WITH TIME ZONE"/>
            <column name="notes" type="VARCHAR(500)"/>
            <column name="oauth_provider" type="VARCHAR(50)"/>
            <column name="oauth_provider_id" type="VARCHAR(255)"/>
            <column name="mfa_enabled" type="BOOLEAN" defaultValueBoolean="false">
                <constraints nullable="false"/>
            </column>
            <column name="mfa_secret" type="VARCHAR(255)"/>
            <column name="mfa_recovery_codes" type="VARCHAR(1000)"/>
            <column name="mfa_enabled_at" type="TIMESTAMP WITH TIME ZONE"/>
            <column name="mfa_method" type="VARCHAR(10)" defaultValue="TOTP"/>
            <column name="mfa_phone_number" type="VARCHAR(20)"/>
            <column name="created_at" type="TIMESTAMP WITH TIME ZONE" defaultValueComputed="CURRENT_TIMESTAMP">
                <constraints nullable="false"/>
            </column>
            <column name="updated_at" type="TIMESTAMP WITH TIME ZONE" defaultValueComputed="CURRENT_TIMESTAMP">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <!-- User-Tenant mapping table -->
        <createTable tableName="user_tenants">
            <column name="user_id" type="UUID">
                <constraints nullable="false" foreignKeyName="fk_user_tenants_user_id" references="users(id)" deleteCascade="true"/>
            </column>
            <column name="tenant_id" type="VARCHAR(255)">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <!-- User-Role mapping table -->
        <createTable tableName="user_roles">
            <column name="user_id" type="UUID">
                <constraints nullable="false" foreignKeyName="fk_user_roles_user_id" references="users(id)" deleteCascade="true"/>
            </column>
            <column name="role" type="VARCHAR(50)">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <!-- Indexes -->
        <createIndex tableName="users" indexName="idx_users_username">
            <column name="username"/>
        </createIndex>
        <createIndex tableName="users" indexName="idx_users_email">
            <column name="email"/>
        </createIndex>
        <createIndex tableName="users" indexName="idx_users_active">
            <column name="active"/>
        </createIndex>
        <createIndex tableName="user_tenants" indexName="idx_user_tenants_tenant_id">
            <column name="tenant_id"/>
        </createIndex>
        <createIndex tableName="user_tenants" indexName="idx_user_tenants_user_id">
            <column name="user_id"/>
        </createIndex>

        <rollback>
            <dropTable tableName="user_roles"/>
            <dropTable tableName="user_tenants"/>
            <dropTable tableName="users"/>
        </rollback>
    </changeSet>
</databaseChangeLog>
```

**Repeat for:**
- Care Gap Service (0027-add-users-table.xml)
- FHIR Service (0028-add-users-table.xml)
- Patient Service (0029-add-users-table.xml)

**Add to db.changelog-master.xml:**
```xml
<include file="db/changelog/0026-add-users-table.xml"/>
```

---

### Phase 3: Service Configuration (TODO)

**Goal:** Register UserAutoRegistrationFilter in all services

**Files to Modify:**

#### 1. Quality Measure Service
```java
// backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/config/SecurityConfig.java

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserAutoRegistrationFilter userAutoRegistrationFilter;
    private final TrustedHeaderAuthFilter trustedHeaderAuthFilter;
    private final TrustedTenantAccessFilter trustedTenantAccessFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(trustedHeaderAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(userAutoRegistrationFilter, TrustedHeaderAuthFilter.class) // NEW
            .addFilterAfter(trustedTenantAccessFilter, UserAutoRegistrationFilter.class);

        return http.build();
    }
}
```

**Repeat for:**
- Care Gap Service SecurityConfig
- FHIR Service SecurityConfig
- Patient Service SecurityConfig

---

### Phase 4: Testing & Validation (TODO)

**Goal:** Verify user auto-registration works across all services

**Integration Tests to Create:**

```java
// backend/modules/services/quality-measure-service/src/test/java/com/healthdata/quality/UserAutoRegistrationIT.java

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class UserAutoRegistrationIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldAutoRegisterUserOnFirstAccess() throws Exception {
        // Given: New user with valid gateway headers
        UUID userId = UUID.randomUUID();
        String username = "test-user";
        String tenantId = "test-tenant";

        // When: User makes first request to service
        mockMvc.perform(get("/api/v1/quality-measures")
                .header("X-Auth-User-Id", userId.toString())
                .header("X-Auth-Username", username)
                .header("X-Auth-Tenant-Ids", tenantId)
                .header("X-Auth-Roles", "EVALUATOR")
                .header("X-Auth-Validated", "gateway-12345-abcdef"))
            .andExpect(status().isOk());

        // Then: User should be auto-registered in database
        Optional<User> registeredUser = userRepository.findById(userId);
        assertThat(registeredUser).isPresent();
        assertThat(registeredUser.get().getUsername()).isEqualTo(username);
        assertThat(registeredUser.get().getTenantIds()).contains(tenantId);
        assertThat(registeredUser.get().getRoles()).contains(UserRole.EVALUATOR);
    }

    @Test
    void shouldUpdateLastAccessOnSubsequentRequests() throws Exception {
        // Given: Existing user
        User existingUser = createTestUser();
        Instant originalLastAccess = existingUser.getLastLoginAt();

        Thread.sleep(1000); // Ensure time difference

        // When: User makes second request
        mockMvc.perform(get("/api/v1/quality-measures")
                .header("X-Auth-User-Id", existingUser.getId().toString())
                .header("X-Auth-Username", existingUser.getUsername())
                .header("X-Auth-Tenant-Ids", "test-tenant")
                .header("X-Auth-Roles", "EVALUATOR")
                .header("X-Auth-Validated", "gateway-12345-abcdef"))
            .andExpect(status().isOk());

        // Then: Last access timestamp should be updated
        User updatedUser = userRepository.findById(existingUser.getId()).orElseThrow();
        assertThat(updatedUser.getLastLoginAt()).isAfter(originalLastAccess);
    }
}
```

**Repeat for all services.**

---

### Phase 5: Documentation & Deployment (TODO)

**Goal:** Document the user management architecture and deployment steps

**Documents to Create:**

1. **Service Integration Guide**
   - How to add UserAutoRegistrationFilter to new services
   - Required dependencies
   - Database migration templates

2. **Security Architecture Doc**
   - Gateway → Service trust flow diagram
   - Header validation process
   - HIPAA compliance notes

3. **Deployment Runbook**
   - Pre-deployment checklist
   - Migration execution order
   - Rollback procedures
   - Smoke test script

---

## Security Considerations

### ✅ Security Features

1. **Header Validation**
   - Gateway strips external X-Auth-* headers (prevents injection)
   - HMAC signature validates headers came from gateway
   - Services only trust validated headers

2. **Multi-Tenancy Isolation**
   - User-tenant mapping enforced at database level
   - TrustedTenantAccessFilter validates tenant access
   - Each service maintains isolated user records

3. **Audit Trail (HIPAA §164.312(b))**
   - User registration logged with timestamp, IP, service name
   - Last access tracked for all users
   - Audit logs searchable for compliance reporting

4. **Password Security**
   - Passwords managed centrally by authentication service
   - Service databases store "N/A" placeholder (no password access)
   - MFA enforcement handled at gateway level

### ⚠️ Security Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| Header injection attack | Gateway strips external auth headers before processing |
| Unauthorized user creation | Headers only trusted if X-Auth-Validated signature is valid |
| Cross-tenant data access | TrustedTenantAccessFilter validates tenant membership |
| Stale user data | Last access timestamp updated on each request |
| Database compromise | Passwords not stored in service databases |

---

## Rollback Plan

If issues arise during deployment:

1. **Phase 2 Rollback (Database)**
   ```bash
   ./gradlew :modules:services:quality-measure-service:liquibaseRollbackCount -PliquibaseCommandValue=1
   ```

2. **Phase 3 Rollback (Configuration)**
   - Remove `addFilterAfter(userAutoRegistrationFilter, ...)` from SecurityConfig
   - Redeploy service
   - User requests will fail with 401 if user not already registered

3. **Manual User Registration**
   - If auto-registration fails, users can be manually inserted:
   ```sql
   INSERT INTO users (id, username, email, password_hash, first_name, last_name, active)
   VALUES (
     'USER_UUID',
     'username',
     'email@example.com',
     'N/A',
     'First',
     'Last',
     true
   );

   INSERT INTO user_tenants (user_id, tenant_id)
   VALUES ('USER_UUID', 'TENANT_ID');
   ```

---

## Success Criteria

- [ ] All 4 services have UserRepository and TenantRepository
- [ ] All 4 services have users table with Liquibase migrations
- [ ] UserAutoRegistrationFilter registered in all service SecurityConfigs
- [ ] Integration tests pass for all services
- [ ] Manual testing confirms:
  - New users auto-register on first access
  - Existing users have last_access updated
  - Multi-tenant users can access multiple tenants
  - Audit logs contain user registration events
- [ ] Documentation complete (architecture, integration guide, deployment runbook)
- [ ] Production deployment successful with zero downtime

---

## Timeline

| Phase | Duration | Status |
|-------|----------|--------|
| Phase 1: Core Infrastructure | 1 hour | ✅ COMPLETE |
| Phase 2: Database Migrations | 2 hours | 🔄 TODO |
| Phase 3: Service Configuration | 1 hour | 🔄 TODO |
| Phase 4: Testing & Validation | 2 hours | 🔄 TODO |
| Phase 5: Documentation & Deployment | 1 hour | 🔄 TODO |
| **Total** | **7 hours** | **14% Complete** |

---

## Next Steps

1. ✅ Create UserAutoRegistrationFilter
2. ✅ Add UserRepository to Patient Service
3. ⏭️ Create Liquibase migrations for all 4 services
4. ⏭️ Register filter in SecurityConfig for all services
5. ⏭️ Write integration tests
6. ⏭️ Deploy to staging environment
7. ⏭️ Production deployment

---

## References

- [Gateway Trust Architecture](../backend/docs/GATEWAY_TRUST_ARCHITECTURE.md)
- [Database Architecture Guide](../backend/docs/DATABASE_ARCHITECTURE_GUIDE.md)
- [Liquibase Development Workflow](../backend/docs/LIQUIBASE_DEVELOPMENT_WORKFLOW.md)
- [HIPAA Compliance Requirements](../backend/HIPAA-CACHE-COMPLIANCE.md)
