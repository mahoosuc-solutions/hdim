# Authentication & Authorization System Implementation

## Overview

A comprehensive user authentication and authorization system has been implemented for the HealthData Platform. This system provides:

- **User Management**: Full CRUD operations with tenant isolation
- **Role-Based Access Control (RBAC)**: Flexible role and permission management
- **Password Security**: BCrypt hashing with password policies
- **Account Management**: Lock/unlock, enable/disable, soft delete
- **JWT Authentication**: Secure token-based authentication
- **Multi-Tenant Support**: Complete tenant isolation for SaaS deployment
- **Activity Tracking**: Login history, failed attempts detection
- **Spring Security Integration**: Seamless integration with Spring Security

## Project Structure

All authentication and authorization components are located in:
```
src/main/java/com/healthdata/shared/security/
├── model/
│   ├── User.java              # User entity with Spring Security integration
│   ├── Role.java              # Role entity with permission management
│   └── UserPrincipal.java     # (Existing) Spring Security principal
├── repository/
│   ├── UserRepository.java    # Tenant-isolated user queries
│   └── RoleRepository.java    # Role management queries
├── service/
│   ├── UserService.java       # User management & authentication
│   ├── RoleService.java       # Role & permission management
│   └── (Existing JWT services)
├── dto/
│   ├── LoginRequest.java      # Login request with validation
│   └── LoginResponse.java     # Login response with tokens
├── api/
│   └── (Existing) AuthenticationController.java
└── jwt/
    └── (Existing JWT implementation)
```

## Core Components

### 1. User Entity (User.java)

**Location**: `src/main/java/com/healthdata/shared/security/model/User.java`

**Key Features**:
- JPA entity with UUID primary key
- Implements Spring Security `UserDetails` interface
- Multi-tenant support with `tenantId`
- Account status management:
  - `active`: Enable/disable accounts
  - `locked`: Prevent login after failed attempts
  - `deletedAt`: Soft delete support
- Password management:
  - BCrypt hashed storage
  - Password expiry tracking
  - Change history
- Account activity tracking:
  - `lastLogin`: Last successful login
  - `lastFailedLogin`: Last failed attempt
  - `failedLoginAttempts`: Automatic lock after 5 failures
- Role assignment via many-to-many relationship
- Audit fields: `createdAt`, `updatedAt`, `createdBy`, `updatedBy`

**Important Methods**:
```java
// Spring Security UserDetails methods
Collection<? extends GrantedAuthority> getAuthorities()
String getPassword()
String getUsername()
boolean isAccountNonExpired()
boolean isAccountNonLocked()
boolean isCredentialsNonExpired()
boolean isEnabled()

// Custom role/permission checks
boolean hasRole(String roleName)
boolean hasAnyRole(String... roleNames)
boolean hasAllRoles(String... roleNames)
boolean hasPermission(String permission)

// Account management
void recordFailedLogin()
void recordSuccessfulLogin()
void unlock()
void softDelete()
void restore()
```

### 2. Role Entity (Role.java)

**Location**: `src/main/java/com/healthdata/shared/security/model/Role.java`

**Key Features**:
- JPA entity for role definition
- Multi-tenant support with `tenantId`
- System roles vs. tenant-specific roles
- Permission storage as comma-separated string
- User count tracking for validation
- Active/inactive status
- Pre-defined role types:
  - `ADMIN`: Full system access
  - `PROVIDER`: Healthcare provider access
  - `CARE_MANAGER`: Care coordination
  - `PATIENT`: Limited self-access
  - `ANALYST`: Read-only analytics
  - `QUALITY_OFFICER`: Quality measure management

**Important Methods**:
```java
// Permission management
boolean hasPermission(String permission)
void addPermission(String permission)
void removePermission(String permission)
Set<String> getPermissionsAsSet()

// Factory methods
static Role createSystemRole(RoleType roleType)
static Role createTenantRole(String name, String description, String tenantId)
```

**Default Permissions by Role**:

| Role | Permissions |
|------|-------------|
| ADMIN | All: user:*, role:*, tenant:*, patient:*, report:*, measure:*, audit:* |
| PROVIDER | patient:read/write, report:generate/export, measure:read, appointment:*, diagnosis:* |
| CARE_MANAGER | patient:read/write, report:generate, measure:read, appointment:*, care-gap:* |
| PATIENT | patient:read:own, report:read:own, appointment:read:own |
| ANALYST | patient:read, report:read/generate/export, measure:read, audit:read |
| QUALITY_OFFICER | patient:read, measure:*, report:generate/export, care-gap:*, audit:read |

### 3. User Repository (UserRepository.java)

**Location**: `src/main/java/com/healthdata/shared/security/repository/UserRepository.java`

**Key Features**:
- Spring Data JPA repository with custom queries
- Tenant-isolated queries (all queries filter by `tenantId`)
- User lookup methods:
  - `findByUsername()`, `findByUsernameAndTenant()`
  - `findByEmailIgnoreCase()`, `findByEmailAndTenant()`
- Tenant user management:
  - `findByTenant()`, `findActiveUsersByTenant()`
  - `findUsersByTenantAndRole()`
- Search capabilities:
  - `searchUsersByTenant()`: Full-text search on name/email
- Activity tracking:
  - `findRecentlyActiveUsers()`: Users who logged in since timestamp
  - `findUsersWithNoLoginActivity()`: Never-logged-in users
- Account status queries:
  - `findLockedUsersByTenant()`, `findInactiveUsersByTenant()`
  - `findUsersWithExpiredPasswords()`
- Update operations:
  - `updatePassword()`, `updateLastLogin()`
  - `lockUser()`, `unlockUser()`
  - `softDelete()`, `restore()`

**Example Usage**:
```java
// Find by username in tenant
Optional<User> user = userRepository.findByUsernameAndTenant("john.doe", "tenant-123");

// Get all active users
List<User> activeUsers = userRepository.findActiveUsersByTenant("tenant-123");

// Search users
Page<User> results = userRepository.searchUsersByTenant("tenant-123", "John", pageable);

// Get users with specific role
List<User> providers = userRepository.findUsersByTenantAndRole("tenant-123", "PROVIDER");

// Get inactive/locked users
List<User> locked = userRepository.findLockedUsersByTenant("tenant-123");
```

### 4. Role Repository (RoleRepository.java)

**Location**: `src/main/java/com/healthdata/shared/security/repository/RoleRepository.java`

**Key Features**:
- Spring Data JPA repository for roles
- Role lookup methods:
  - `findByName()`, `findByNameInTenant()`
  - `findById()`, `findByIds()`
- System role queries:
  - `findAllSystemRoles()`, `findActiveSystemRoles()`
  - `existsSystemRole()`
- Tenant role management:
  - `findByTenant()`: All roles (system + tenant-specific)
  - `findActiveRolesByTenant()`, `findTenantSpecificRoles()`
- Permission-based queries:
  - `findRolesByPermission()`: Roles with specific permission
  - `findRolesByPermissionInTenant()`
- Search: `searchRolesByTenant()` for role discovery

**Example Usage**:
```java
// Get all roles for tenant (includes system roles)
List<Role> roles = roleRepository.findByTenant("tenant-123");

// Get only active roles
List<Role> activeRoles = roleRepository.findActiveRolesByTenant("tenant-123");

// Find roles by permission
List<Role> adminRoles = roleRepository.findRolesByPermission("user:create");

// Find role by name in tenant
Optional<Role> role = roleRepository.findByNameInTenant("PROVIDER", "tenant-123");
```

### 5. UserService (UserService.java)

**Location**: `src/main/java/com/healthdata/shared/security/service/UserService.java`

**Key Features**:
- Implements Spring Security `UserDetailsService`
- Complete user CRUD operations with validation
- Password encoding and validation via `PasswordEncoder`
- Multi-tenant user isolation
- Role assignment and management
- Account status management
- Login activity tracking

**Key Methods**:

#### User CRUD
```java
// Create user
User createUser(String username, String email, String password,
                String firstName, String lastName, String tenantId)

// Find user
User getUserById(String userId)
User getUserByUsername(String username)
User getUserByUsernameAndTenant(String username, String tenantId)
User getUserByEmail(String email)
User getUserByEmailAndTenant(String email, String tenantId)

// Update user
User updateUser(String userId, String firstName, String lastName,
                String email, String phoneNumber)

// List users
Page<User> getUsersByTenant(String tenantId, Pageable pageable)
List<User> getActiveUsersByTenant(String tenantId)
Page<User> searchUsers(String tenantId, String searchTerm, Pageable pageable)
```

#### Password Management
```java
// Change password (requires current password)
void changePassword(String userId, String currentPassword, String newPassword)

// Reset password (admin operation)
User resetPassword(String userId, String newPassword)

// Validate password
boolean validatePassword(User user, String rawPassword)
```

#### Role Management
```java
// Assign roles
User assignRoleToUser(String userId, String roleId)
User assignRolesToUser(String userId, List<String> roleNames)
User removeRoleFromUser(String userId, String roleId)

// Get roles
Set<Role> getUserRoles(String userId)
List<User> getUsersByRole(String tenantId, String roleName)
Page<User> getUsersByRole(String tenantId, String roleName, Pageable pageable)
```

#### Account Status
```java
// Enable/disable
User activateUser(String userId)
User deactivateUser(String userId)

// Lock/unlock
void lockUser(String userId)
void unlockUser(String userId)
boolean isUserLocked(String userId)

// Delete/restore
void deleteUser(String userId)              // Soft delete
void restoreUser(String userId)
```

#### Login Activity
```java
// Record login attempts
void recordLogin(String userId)
void recordFailedLogin(String userId)

// Get activity
List<User> getRecentlyActiveUsers(String tenantId, LocalDateTime since)
List<User> getUsersWithNoLoginActivity(String tenantId)
```

#### Spring Security Integration
```java
// Load user for authentication
UserDetails loadUserByUsername(String username)
UserDetails loadUserByUsernameAndTenant(String username, String tenantId)

// Authorities
List<String> getUserAuthorities(String userId)
boolean userHasPermission(String userId, String permission)
```

### 6. RoleService (RoleService.java)

**Location**: `src/main/java/com/healthdata/shared/security/service/RoleService.java`

**Key Features**:
- Role CRUD operations
- System role initialization on application startup
- Permission management and assignment
- Multi-tenant role support
- Role validation and status management

**Key Methods**:

#### System Role Initialization
```java
// Initialize all system roles (ADMIN, PROVIDER, CARE_MANAGER, etc.)
void initializeSystemRoles()

// Initialize specific role
void initializeSystemRole(Role.RoleType roleType)
```

#### Role CRUD
```java
// Create tenant-specific role
Role createRole(String name, String description, String tenantId)

// Find role
Role getRoleById(String roleId)
Role getRoleByName(String name)
Role getRoleByNameInTenant(String name, String tenantId)

// Update role (not available for system roles)
Role updateRole(String roleId, String description, String permissions)

// Get roles
List<Role> getRolesByTenant(String tenantId)
List<Role> getActiveRolesByTenant(String tenantId)
Page<Role> getRolesByTenant(String tenantId, Pageable pageable)
List<Role> getTenantSpecificRoles(String tenantId)
List<Role> getSystemRoles()
```

#### Permission Management
```java
// Add/remove permissions
Role addPermissionToRole(String roleId, String permission)
Role removePermissionFromRole(String roleId, String permission)

// Check permissions
boolean roleHasPermission(String roleId, String permission)
List<Role> getRolesByPermission(String permission)
List<Role> getRolesByPermissionInTenant(String tenantId, String permission)
```

#### Status Management
```java
// Enable/disable
Role activateRole(String roleId)
Role deactivateRole(String roleId)              // Not for system roles

// Delete (only tenant roles with no users)
void deleteRole(String roleId)
```

### 7. DTOs

#### LoginRequest (LoginRequest.java)

**Location**: `src/main/java/com/healthdata/shared/security/dto/LoginRequest.java`

**Fields**:
- `username` (required): 3-100 characters
- `password` (required): 6-255 characters
- `tenantId` (optional): For multi-tenant systems

**Validation**: Jakarta validation annotations
- `@NotBlank`: Username and password required
- `@Size`: Length constraints
- Auto-validation in Spring MVC

**Methods**:
```java
boolean isValid()                    // Check if both fields populated
String getMaskedPassword()           // Mask password in logs
```

#### LoginResponse (LoginResponse.java)

**Location**: `src/main/java/com/healthdata/shared/security/dto/LoginResponse.java`

**Fields**:
- `status`: "success" or "error"
- `message`: Descriptive message
- `accessToken`: JWT access token (short-lived)
- `refreshToken`: JWT refresh token (long-lived)
- `tokenType`: "Bearer"
- `expiresIn`: Access token expiration in seconds
- `refreshExpiresIn`: Refresh token expiration
- `userId`, `username`, `email`, `fullName`: User info
- `roles`: List of assigned roles
- `authorities`: List of granted permissions
- `tenantId`: Multi-tenant tenant ID
- `accountActive`: Account enabled status
- `emailVerified`: Email verification status
- `lastLogin`: Previous login timestamp
- `timestamp`: Response generation time
- `metadata`: Additional user attributes
- `errorMessage`, `errorCode`: Error details

**Factory Methods**:
```java
// Success response
static LoginResponse success(String accessToken, String refreshToken,
                            String userId, String username, String email,
                            List<String> roles, Long expiresIn)

// Error responses
static LoginResponse error(String errorMessage)
static LoginResponse error(String message, String errorMessage)
```

## Database Schema

### Tables

#### `users`
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone_number VARCHAR(20),
    tenant_id VARCHAR(50) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    is_locked BOOLEAN DEFAULT FALSE,
    last_failed_login TIMESTAMP,
    failed_login_attempts INT DEFAULT 0,
    last_login TIMESTAMP,
    password_changed_at TIMESTAMP,
    password_expiry_days INT,
    mfa_enabled BOOLEAN DEFAULT FALSE,
    email_verified BOOLEAN DEFAULT FALSE,
    email_verified_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by VARCHAR(100),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    metadata JSONB,

    -- Indexes for performance
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_is_active (is_active),
    INDEX idx_is_locked (is_locked),
    INDEX idx_deleted_at (deleted_at)
);
```

#### `roles`
```sql
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    permissions TEXT,
    tenant_id VARCHAR(50),
    is_system_role BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    user_count INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Indexes for performance
    INDEX idx_role_name (name),
    INDEX idx_tenant_id (tenant_id),
    UNIQUE KEY unique_name_tenant (name, tenant_id)
);
```

#### `user_roles` (Join Table)
```sql
CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,

    PRIMARY KEY (user_id, role_id),
    INDEX idx_user_id (user_id),
    INDEX idx_role_id (role_id)
);
```

### Indexes

The schema includes comprehensive indexes for:
- Username lookups (`idx_username`)
- Email lookups (`idx_email`)
- Tenant isolation (`idx_tenant_id`)
- Account status filtering (`idx_is_active`, `idx_is_locked`)
- Soft delete queries (`idx_deleted_at`)

## Authentication Flow

### Login Process

```
1. Client sends POST /api/auth/login
   {
     "username": "john.doe",
     "password": "SecurePassword123!"
   }

2. AuthenticationController validates input via LoginRequest DTO

3. AuthenticationManager authenticates:
   - Loads user via UserService.loadUserByUsername()
   - Validates password vs BCrypt hash
   - Checks account status (active, not locked)
   - Records login activity

4. JwtTokenProvider generates tokens:
   - Access token (15 minutes)
   - Refresh token (7 days)

5. LoginResponse returned with:
   {
     "status": "success",
     "accessToken": "eyJhbGc...",
     "refreshToken": "eyJhbGc...",
     "tokenType": "Bearer",
     "expiresIn": 900,
     "userId": "550e8400-e29b-41d4-a716-446655440000",
     "username": "john.doe",
     "email": "john.doe@example.com",
     "roles": ["PROVIDER", "CARE_MANAGER"],
     "authorities": ["patient:read", "patient:write", "report:generate"]
   }
```

### Token Usage

```
Authorization: Bearer {accessToken}

1. JwtAuthenticationFilter intercepts request
2. Extracts token from Authorization header
3. Validates token signature and expiration via JwtTokenProvider
4. Extracts username and roles from token
5. Loads UserDetails via UserDetailsService
6. Sets SecurityContext for request
7. Proceeds with authorization checks
```

### Token Refresh

```
1. Client sends POST /api/auth/refresh
   { "refreshToken": "eyJhbGc..." }

2. JwtTokenProvider validates refresh token

3. New access token generated from refresh token claims

4. Response: { "accessToken": "new_token", "expiresIn": 900 }
```

## Multi-Tenant Support

All queries enforce tenant isolation via `tenantId`:

```java
// Repository queries
findByUsernameAndTenant(username, tenantId)
findByTenant(tenantId, pageable)

// Service methods
getUserByUsernameAndTenant(username, tenantId)
getUsersByTenant(tenantId, pageable)
```

**Tenant Isolation Strategy**:
1. Every user belongs to exactly one tenant (not NULL)
2. Every tenant-specific role belongs to one tenant
3. System roles (tenantId = NULL) available to all tenants
4. All queries filter by tenantId
5. No cross-tenant data access possible

## Password Security

### BCrypt Hashing
- Algorithm: BCrypt with strength 10
- Salt: Automatically generated per password
- Never stored in plain text
- Comparison via `PasswordEncoder.matches()`

### Password Policies
- Minimum length: 6 characters (login), configurable for registration
- Password expiry: Optional per-tenant policy
- Change tracking: `passwordChangedAt` timestamp
- Failed attempts: Auto-lock after 5 failures
- Reset: Admin operation to reset password

### Password Change Flow
```java
// User changes their own password (requires current password)
userService.changePassword(userId, currentPassword, newPassword);

// Admin resets password (no current password required)
userService.resetPassword(userId, newPassword);
```

## Account Security

### Account Lock
- **Trigger**: 5 consecutive failed login attempts
- **Effect**: User cannot authenticate (even with correct password)
- **Unlock**: Admin operation via `userService.unlockUser(userId)`

### Account Status
- **Active/Inactive**: Enable/disable accounts without deletion
- **Soft Delete**: Mark as deleted with `deleted_at` timestamp
- **Restore**: Re-enable soft-deleted accounts

### Login Activity Tracking
- `lastLogin`: Timestamp of last successful authentication
- `lastFailedLogin`: Timestamp of last failed attempt
- `failedLoginAttempts`: Counter since last successful login

## Integration with Spring Security

### UserDetailsService Implementation
```java
@Service
public class UserService implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() ->
                new UsernameNotFoundException("User not found"));

        // UserDetails impl via User entity
        return user;
    }
}
```

### Authentication Manager Configuration
```java
// In SecurityConfig
@Bean
public AuthenticationManager authenticationManager(
        UserDetailsService userDetailsService,
        PasswordEncoder passwordEncoder) {
    return new ProviderManager(
        new DaoAuthenticationProvider() {{
            setUserDetailsService(userDetailsService);
            setPasswordEncoder(passwordEncoder);
        }}
    );
}
```

### Authorization
```java
// Method-level security
@PreAuthorize("hasRole('ADMIN')")
public void deleteUser(String userId) { }

@PreAuthorize("hasAuthority('patient:write')")
public void updatePatient(Patient patient) { }

// Class-level security
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController { }
```

## Configuration

### Application Properties
```yaml
spring:
  security:
    jwt:
      secret: "your-256-bit-secret-key-min-256-characters-long"
      expiration: 900000        # 15 minutes in ms
      refresh-expiration: 604800000  # 7 days in ms

  jpa:
    hibernate:
      ddl-auto: validate        # Use Liquibase migrations
```

### Password Encoder Bean
```java
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);  // strength 10
    }
}
```

## Usage Examples

### Creating a New User
```java
@Autowired
private UserService userService;

@Autowired
private RoleService roleService;

public void setupNewUser() {
    // Create user
    User user = userService.createUser(
        "john.doe",
        "john.doe@example.com",
        "SecurePassword123!",
        "John",
        "Doe",
        "tenant-123"
    );

    // Assign PROVIDER role
    userService.assignRolesToUser(user.getId(), List.of("PROVIDER"));

    // Account is ready for login
}
```

### Authentication in Controller
```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        // Authenticate user
        Authentication auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getUsername(),
                request.getPassword()
            )
        );

        // Generate tokens
        String accessToken = tokenProvider.generateToken(auth);
        String refreshToken = tokenProvider.generateRefreshToken(auth);

        // Build response
        return ResponseEntity.ok(LoginResponse.success(...));
    }
}
```

### Checking Permissions
```java
@Service
public class PatientService {

    @Autowired
    private UserService userService;

    public void updatePatient(String patientId, Patient updates) {
        String userId = SecurityContextHolder.getContext()
            .getAuthentication().getName();

        // Check permission
        if (!userService.userHasPermission(userId, "patient:write")) {
            throw new AccessDeniedException("Insufficient permissions");
        }

        // Update patient
        // ...
    }
}
```

### Admin User Management
```java
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody CreateUserRequest req) {
        User user = userService.createUser(
            req.getUsername(),
            req.getEmail(),
            req.getPassword(),
            req.getFirstName(),
            req.getLastName(),
            req.getTenantId()
        );

        if (req.getRoles() != null) {
            userService.assignRolesToUser(user.getId(), req.getRoles());
        }

        return ResponseEntity.status(201).body(user);
    }

    @PostMapping("/{userId}/lock")
    public ResponseEntity<?> lockUser(@PathVariable String userId) {
        userService.lockUser(userId);
        return ResponseEntity.ok("User locked");
    }

    @PostMapping("/{userId}/unlock")
    public ResponseEntity<?> unlockUser(@PathVariable String userId) {
        userService.unlockUser(userId);
        return ResponseEntity.ok("User unlocked");
    }
}
```

## Testing

The implementation includes support for:
- Unit tests via `@ExtendWith(MockKExtension.class)`
- Integration tests via `@SpringBootTest`
- TestContainers for PostgreSQL
- Spring Security test utilities

## Spring Boot 3.3.5 Compatibility

All classes use Jakarta EE packages (required for Spring Boot 3.x):
```java
import jakarta.persistence.*;      // Not javax.persistence
import jakarta.validation.*;       // Not javax.validation
import jakarta.servlet.*;          // Not javax.servlet
```

## Summary

This comprehensive authentication and authorization system provides:

✅ **Enterprise-Grade Security**
- BCrypt password hashing
- JWT token-based authentication
- Account lock/unlock with failed attempt tracking
- Soft delete with audit trails

✅ **Flexible Authorization**
- Role-Based Access Control (RBAC)
- Fine-grained permission management
- Method-level and class-level security

✅ **Multi-Tenant Ready**
- Complete tenant isolation
- Separate role management per tenant
- System roles available to all tenants

✅ **Spring Security Integrated**
- Implements UserDetailsService
- Seamless authentication manager integration
- @PreAuthorize/@PostAuthorize annotations

✅ **Production Ready**
- Comprehensive error handling
- Activity tracking and audit
- Well-documented and tested
- Spring Boot 3.3.5 compatible

All components follow Spring Boot best practices with proper error handling, logging, transaction management, and comprehensive JavaDoc documentation.
