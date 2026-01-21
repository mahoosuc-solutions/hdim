# Spring Security & RBAC - Authorization Guide

> **This is a comprehensive guide for authorization in HDIM microservices.**
> **Every endpoint requires @PreAuthorize; unauthorized access is a security vulnerability.**

---

## Overview

### What is This Skill?

Spring Security is a Java framework for authentication (who you are) and authorization (what you can do). RBAC (Role-Based Access Control) assigns permissions to roles (ADMIN, EVALUATOR, ANALYST, VIEWER) and users to roles.

**Example:**
- ADMIN role can CREATE patients
- EVALUATOR role can VIEW and RUN measures
- VIEWER role can only VIEW reports (read-only)

### Why is This Important for HDIM?

Healthcare systems require strict access control. Users should only access data they're authorized to see:
- Clinician cannot delete care plans
- Analyst cannot modify measure definitions
- VIEWER cannot create users

Without proper authorization:
- Data breaches (unauthorized access)
- Compliance violations (HIPAA requires access control)
- Accidental data corruption (user modifies wrong data)

### Business Impact

- **Security:** Prevent unauthorized access and data breaches
- **Compliance:** HIPAA requires role-based access control and audit trails
- **Data Integrity:** Users can only perform permitted operations
- **Audit Trail:** Track who accessed/modified what data (HIPAA requirement)

### Key Services Using This Skill

All 51 HDIM services use Spring Security:
- Gateway (8001) - Validates JWT, injects authority headers
- Patient Service (8084) - Protects endpoints with @PreAuthorize
- Quality Measure Service (8087) - Role-based measure access
- Care Gap Service (8086) - Role-based care gap modification

### Estimated Learning Time

1.5-2 weeks (hands-on role/authorization implementation)

---

## Key Concepts

### Concept 1: Authentication vs. Authorization

**Authentication:** Verifying identity (who you are)
- Credentials: username/password or JWT token
- Gateway validates JWT, injects X-Auth-* headers

**Authorization:** Controlling access (what you can do)
- Role-based permissions
- @PreAuthorize enforces permissions on endpoints

### Concept 2: Role Hierarchy

HDIM defines role hierarchy:
```
SUPER_ADMIN (can do everything)
  ↓
ADMIN (tenant admin)
  ├─ EVALUATOR (run measures, create care plans)
  ├─ ANALYST (view reports, export data)
  └─ VIEWER (read-only access)
```

### Concept 3: @PreAuthorize Annotation

**Definition:** Spring Security annotation that checks user has required role before executing method.

**Example:**
```java
@PostMapping
@PreAuthorize("hasRole('ADMIN')")  // Only ADMIN can POST
public ResponseEntity<PatientResponse> createPatient(...) {
}
```

### Concept 4: Multi-Tenant Authorization

Authorization must respect tenant boundaries:
- User authenticated for tenant A
- Cannot access tenant B's data even with correct role
- Repository filters by tenant_id enforces this

### Concept 5: Method-Level Security

@PreAuthorize, @PostAuthorize enable security at method level:
- @PreAuthorize: Check permission BEFORE method executes
- @PostAuthorize: Check permission AFTER method executes (filter results)

---

## Implementation Guide

### Step 1: Configure Spring Security

```java
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)  // Enable @PreAuthorize
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().authenticated()
            )
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .addFilterBefore(
                new TrustedHeaderAuthFilter(),
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }
}
```

### Step 2: Create Trusted Header Filter

```java
@Component
public class TrustedHeaderAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Gateway validates JWT and injects these headers
        String userId = request.getHeader("X-Auth-User-ID");
        String tenantId = request.getHeader("X-Tenant-ID");
        String rolesHeader = request.getHeader("X-Auth-Roles");

        if (userId != null && tenantId != null && rolesHeader != null) {
            // Parse roles from header (comma-separated: "ADMIN,EVALUATOR")
            String[] roleArray = rolesHeader.split(",");
            List<GrantedAuthority> authorities = Arrays.stream(roleArray)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.trim()))
                .collect(Collectors.toList());

            // Create authentication token
            UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    authorities
                );

            // Store in security context
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
```

### Step 3: Protect Endpoints with @PreAuthorize

```java
@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class PatientController {
    private final PatientService patientService;

    // ✅ ADMIN only
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PatientResponse> createPatient(
            @RequestBody CreatePatientRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(patientService.createPatient(request, tenantId));
    }

    // ✅ ADMIN or EVALUATOR
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    public ResponseEntity<PatientResponse> getPatient(
            @PathVariable String id,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ResponseEntity.ok(patientService.getPatient(id, tenantId));
    }

    // ✅ Any authenticated user (ADMIN, EVALUATOR, ANALYST, VIEWER)
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PatientResponse>> listPatients(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ResponseEntity.ok(patientService.listPatients(tenantId));
    }

    // ✅ ADMIN only (delete is dangerous)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePatient(
            @PathVariable String id,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        patientService.deletePatient(id, tenantId);
        return ResponseEntity.noContent().build();
    }
}
```

### Step 4: Define Role Hierarchy

```java
@Component
public class RoleHierarchyConfig {

    @Bean
    public RoleHierarchy roleHierarchy() {
        RoleHierarchyImpl roleHierarchy = new RoleHierarchyImpl();
        roleHierarchy.setHierarchy(
            "ROLE_SUPER_ADMIN > ROLE_ADMIN\n" +
            "ROLE_ADMIN > ROLE_EVALUATOR\n" +
            "ROLE_ADMIN > ROLE_ANALYST\n" +
            "ROLE_EVALUATOR > ROLE_VIEWER\n" +
            "ROLE_ANALYST > ROLE_VIEWER"
        );
        return roleHierarchy;
    }
}
```

### Step 5: Test Authorization

```java
@SpringBootTest
@AutoConfigureMockMvc
class AuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    // ✅ ADMIN can create
    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanCreatePatient() throws Exception {
        mockMvc.perform(post("/api/v1/patients")
                .header("X-Tenant-ID", "anthem")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isCreated());
    }

    // ❌ VIEWER cannot create
    @Test
    @WithMockUser(roles = "VIEWER")
    void viewerCannotCreatePatient() throws Exception {
        mockMvc.perform(post("/api/v1/patients")
                .header("X-Tenant-ID", "anthem")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isForbidden());
    }

    // ✅ EVALUATOR can read
    @Test
    @WithMockUser(roles = "EVALUATOR")
    void evaluatorCanReadPatient() throws Exception {
        mockMvc.perform(get("/api/v1/patients")
                .header("X-Tenant-ID", "anthem"))
            .andExpect(status().isOk());
    }
}
```

---

## Best Practices

- ✅ **DO add @PreAuthorize to every endpoint**
  - Why: Default is deny-all; explicit is safer
  - Example: `@PreAuthorize("hasRole('ADMIN')")`

- ✅ **DO use role hierarchy**
  - Why: ADMIN > EVALUATOR > VIEWER; prevents duplication
  - Example: If ADMIN can do EVALUATOR tasks, configure hierarchy

- ✅ **DO test authorization enforcement**
  - Why: Authorization bugs are security vulnerabilities
  - Example: Test that VIEWER cannot POST

- ✅ **DO combine with multi-tenant filtering**
  - Why: Role check + tenant check provides defense in depth
  - Example: User has ADMIN role for tenant A; cannot access tenant B

- ❌ **DON'T skip authorization on internal endpoints**
  - Why: Internal endpoints still need protection (compromised service = breach)
  - Example: Every endpoint, even internal, should have @PreAuthorize

- ❌ **DON'T hardcode role names**
  - Why: Refactoring requires code changes; use constants
  - Example: `@PreAuthorize("hasRole('ADMIN')")` should use constant

- ❌ **DON'T trust client-supplied roles**
  - Why: Client can lie about their role
  - Example: X-Auth-Roles header validated by gateway (trusted source)

---

## Role Reference

| Role | Permissions |
|------|-----------|
| SUPER_ADMIN | Full system access |
| ADMIN | Tenant-level admin, create users, modify measures |
| EVALUATOR | Run measures, create care plans, update projections |
| ANALYST | View reports, export data, access analytics |
| VIEWER | Read-only access to all resources |

---

## Common Authorization Patterns

### Pattern 1: Role-Based Endpoint Access

```java
@PostMapping
@PreAuthorize("hasRole('ADMIN')")  // Only ADMIN
public ResponseEntity<Void> createUser(...) { }

@GetMapping
@PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")  // ADMIN or EVALUATOR
public ResponseEntity<List<?>> listPatients(...) { }

@GetMapping
@PreAuthorize("isAuthenticated()")  // Any authenticated user
public ResponseEntity<?> getProfile(...) { }
```

### Pattern 2: Tenant + Role Authorization

```java
@PostMapping
@PreAuthorize("hasRole('ADMIN') and #tenantId == authentication.principal.tenantId")
public ResponseEntity<Void> createPatient(
        @RequestBody PatientRequest request,
        @RequestParam String tenantId) { }
```

### Pattern 3: Resource Owner Authorization

```java
@PutMapping("/{id}")
@PreAuthorize("hasRole('ADMIN') or @ownershipService.isOwner(#id, authentication.principal.id)")
public ResponseEntity<Void> updatePatient(
        @PathVariable String id,
        @RequestBody PatientRequest request) { }
```

---

## Troubleshooting

### Issue: "AccessDeniedException: Access is denied"
**Cause:** User doesn't have required role
**Solution:** Add user to correct role or adjust @PreAuthorize

### Issue: "NullPointerException on authentication.principal"
**Cause:** User not authenticated
**Solution:** Add @PreAuthorize("isAuthenticated()") to require authentication

### Issue: "All endpoints returning 403 Forbidden"
**Cause:** SecurityFilterChain rejecting all requests
**Solution:** Verify authentication filter is working and roles are correctly set

---

## References

- Spring Security Documentation
- HDIM Multi-Tenant Architecture Guide
- HIPAA Access Control Requirements

---

**Last Updated:** January 20, 2026
**Difficulty Level:** ⭐⭐⭐ (3/5 stars)
**Time Investment:** 1.5-2 weeks
**Prerequisite Skills:** Spring Boot 3.x, Testing & QA

---

**← [Skills Hub](../README.md)** | **→ [Next: Apache Kafka Event Streaming](../07-messaging/kafka-event-streaming.md)**
