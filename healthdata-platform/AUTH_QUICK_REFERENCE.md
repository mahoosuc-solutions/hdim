# Authentication & Authorization Quick Reference

## File Locations

```
src/main/java/com/healthdata/shared/security/
├── model/
│   ├── User.java              (JPA entity, UserDetails impl)
│   └── Role.java              (JPA entity, role definitions)
├── repository/
│   ├── UserRepository.java    (50+ tenant-isolated queries)
│   └── RoleRepository.java    (40+ role management queries)
├── service/
│   ├── UserService.java       (UserDetailsService implementation)
│   └── RoleService.java       (Role and permission management)
└── dto/
    ├── LoginRequest.java      (Login credentials with validation)
    └── LoginResponse.java     (Auth response with JWT tokens)
```

Documentation: `AUTHENTICATION_AUTHORIZATION_IMPLEMENTATION.md`

## Quick Start

### 1. Create a System Admin User

```java
@Autowired
private UserService userService;

@Autowired
private RoleService roleService;

// Initialize system roles (call once on startup)
roleService.initializeSystemRoles();

// Create admin user
User admin = userService.createUser(
    "admin",
    "admin@healthdata.com",
    "SecureAdminPassword123!",
    "System",
    "Administrator",
    "tenant-123"
);

// Assign ADMIN role
userService.assignRolesToUser(admin.getId(), List.of("ADMIN"));
```

### 2. Create Regular Users

```java
// Create provider
User provider = userService.createUser(
    "dr.smith",
    "dr.smith@healthdata.com",
    "ProviderPassword123!",
    "John",
    "Smith",
    "tenant-123"
);

userService.assignRolesToUser(provider.getId(), List.of("PROVIDER"));

// Create care manager
User manager = userService.createUser(
    "jane.manager",
    "jane@healthdata.com",
    "ManagerPassword123!",
    "Jane",
    "Manager",
    "tenant-123"
);

userService.assignRolesToUser(manager.getId(), List.of("CARE_MANAGER"));
```

### 3. User Login

**Endpoint**: `POST /api/auth/login`

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "dr.smith",
    "password": "ProviderPassword123!"
  }'
```

**Response**:
```json
{
  "status": "success",
  "message": "User authenticated successfully",
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "username": "dr.smith",
  "email": "dr.smith@healthdata.com",
  "roles": ["PROVIDER"],
  "authorities": ["patient:read", "patient:write", "report:generate"]
}
```

### 4. Use Access Token in API Requests

```bash
curl -X GET http://localhost:8080/api/patients \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### 5. Refresh Expired Token

**Endpoint**: `POST /api/auth/refresh`

```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }'
```

## Common Operations

### User Management

```java
// Find user
User user = userService.getUserByUsername("dr.smith");
User user = userService.getUserById(userId);

// Update user
userService.updateUser(userId, "Jane", "Smith", "jane@example.com", "+1234567890");

// Change password (user operation)
userService.changePassword(userId, currentPassword, newPassword);

// Reset password (admin operation)
userService.resetPassword(userId, newPassword);

// Get users in tenant
Page<User> users = userService.getUsersByTenant(tenantId, pageable);

// Search users
Page<User> results = userService.searchUsers(tenantId, "John", pageable);

// Get users by role
List<User> providers = userService.getUsersByRole(tenantId, "PROVIDER");
```

### Account Management

```java
// Activate/deactivate
userService.activateUser(userId);
userService.deactivateUser(userId);

// Lock/unlock
userService.lockUser(userId);
userService.unlockUser(userId);

// Delete/restore (soft delete)
userService.deleteUser(userId);
userService.restoreUser(userId);

// Check status
boolean isLocked = userService.isUserLocked(userId);
```

### Role Management

```java
// Create role
Role role = roleService.createRole(
    "BILLING_MANAGER",
    "Manages billing operations",
    "tenant-123"
);

// Get roles
List<Role> roles = roleService.getRolesByTenant("tenant-123");
List<Role> activeRoles = roleService.getActiveRolesByTenant("tenant-123");

// Assign roles
userService.assignRolesToUser(userId, List.of("PROVIDER", "CARE_MANAGER"));

// Check permissions
boolean hasPermission = userService.userHasPermission(userId, "patient:write");
```

### Permission Management

```java
// Add permission
roleService.addPermissionToRole(roleId, "appointment:write");

// Remove permission
roleService.removePermissionFromRole(roleId, "appointment:write");

// Check permission
boolean has = roleService.roleHasPermission(roleId, "patient:read");

// Get roles by permission
List<Role> roles = roleService.getRolesByPermission("patient:write");
```

## Security Best Practices

### 1. Configure Password Policies

```yaml
# application.yml
spring:
  security:
    password:
      min-length: 8
      require-uppercase: true
      require-numbers: true
      require-special: true
      expiry-days: 90
```

### 2. Use Method-Level Security

```java
@PreAuthorize("hasRole('ADMIN')")
public void deleteUser(String userId) {
    userService.deleteUser(userId);
}

@PreAuthorize("hasAuthority('patient:write')")
public void updatePatient(Patient patient) {
    // Update patient
}

@PreAuthorize("hasAnyRole('PROVIDER', 'CARE_MANAGER')")
public Patient getPatient(String patientId) {
    // Return patient
}
```

### 3. Enforce Tenant Isolation

Always pass `tenantId` to service methods:

```java
// ✅ Correct - tenant isolated
User user = userService.getUserByUsernameAndTenant(username, getCurrentTenantId());

// ❌ Wrong - might return user from different tenant
User user = userService.getUserByUsername(username);
```

### 4. Log Security Events

```java
// UserService automatically logs:
// - User creation
// - Login attempts (successful and failed)
// - Failed login attempts
// - Account lock/unlock
// - Password changes
```

## Debugging Tips

### Check User Roles

```java
User user = userService.getUserById(userId);
System.out.println("Roles: " + user.getRoles());
System.out.println("Authorities: " + user.getAuthorities());
System.out.println("Permissions: " + user.getRoles().stream()
    .flatMap(r -> r.getPermissionsAsSet().stream())
    .collect(Collectors.toList()));
```

### Check Account Status

```java
User user = userService.getUserById(userId);
System.out.println("Active: " + user.isActive());
System.out.println("Locked: " + user.isLocked());
System.out.println("Deleted: " + user.isDeleted());
System.out.println("Enabled: " + user.isEnabled());
System.out.println("Last Login: " + user.getLastLogin());
System.out.println("Failed Attempts: " + user.getFailedLoginAttempts());
```

### Verify Permissions

```java
boolean hasPermission = userService.userHasPermission(userId, "patient:write");
if (!hasPermission) {
    List<String> authorities = userService.getUserAuthorities(userId);
    System.out.println("User has authorities: " + authorities);
}
```

## Default System Roles

| Role | Description | Key Permissions |
|------|-------------|-----------------|
| ADMIN | System Administrator | All (user:*, role:*, patient:*, report:*, measure:*, audit:*) |
| PROVIDER | Healthcare Provider | patient:read/write, report:generate, appointment:*, diagnosis:* |
| CARE_MANAGER | Care Coordinator | patient:read/write, care-gap:*, appointment:* |
| PATIENT | Patient | patient:read:own, report:read:own, appointment:read:own |
| ANALYST | Data Analyst | patient:read, report:read/generate/export, measure:read, audit:read |
| QUALITY_OFFICER | Quality Officer | measure:*, report:generate/export, care-gap:*, audit:read |

## Multi-Tenant Example

```java
// Create users in different tenants
User user1 = userService.createUser(
    "john.doe",
    "john@example.com",
    "Password123!",
    "John", "Doe",
    "tenant-123"  // Tenant 1
);

User user2 = userService.createUser(
    "john.doe",   // Same username OK in different tenant
    "john@hospital.com",
    "Password123!",
    "John", "Doe",
    "tenant-456"  // Tenant 2
);

// Both exist but are completely isolated
User found1 = userService.getUserByUsernameAndTenant("john.doe", "tenant-123");  // user1
User found2 = userService.getUserByUsernameAndTenant("john.doe", "tenant-456");  // user2
```

## Error Handling

```java
try {
    User user = userService.createUser(...);
} catch (IllegalArgumentException e) {
    // Username or email already exists
    System.err.println(e.getMessage());
}

try {
    userService.changePassword(userId, wrongPassword, newPassword);
} catch (IllegalArgumentException e) {
    // Current password is incorrect
    System.err.println("Invalid current password");
}

try {
    userService.getUserByUsername(unknownUsername);
} catch (UsernameNotFoundException e) {
    // User not found
    System.err.println("User not found");
}
```

## Performance Optimization

### Indexes
- `idx_username`: Fast username lookup
- `idx_email`: Fast email lookup
- `idx_tenant_id`: Tenant filtering
- `idx_is_active`, `idx_is_locked`: Account status queries
- `idx_deleted_at`: Soft delete filtering
- `idx_user_id`, `idx_role_id`: User-role relationship queries

### Pagination Example

```java
// Get paginated users
Pageable pageable = PageRequest.of(0, 20, Sort.by("username"));
Page<User> page = userService.getUsersByTenant("tenant-123", pageable);

System.out.println("Total: " + page.getTotalElements());
System.out.println("Pages: " + page.getTotalPages());
System.out.println("Current Page: " + page.getNumber());
```

## Next Steps

1. Create Liquibase migration files for database schema
2. Configure password policies in properties
3. Implement additional login endpoints (register, forgot password)
4. Set up audit logging for compliance
5. Create admin management UI
6. Implement email verification flow
7. Add MFA (multi-factor authentication) support
8. Create comprehensive integration tests

## Support & Documentation

Full documentation: `AUTHENTICATION_AUTHORIZATION_IMPLEMENTATION.md`

Key Classes:
- User: `/src/main/java/com/healthdata/shared/security/model/User.java`
- Role: `/src/main/java/com/healthdata/shared/security/model/Role.java`
- UserService: `/src/main/java/com/healthdata/shared/security/service/UserService.java`
- RoleService: `/src/main/java/com/healthdata/shared/security/service/RoleService.java`
