# JWT Security Integration Examples

## Real-World Usage Patterns

### 1. Creating a UserDetailsService

```java
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.healthdata.shared.security.model.UserPrincipal;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomUserDetailsService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                    "User not found: " + username));

        return UserPrincipal.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toList()))
                .enabled(user.isEnabled())
                .accountNonLocked(!user.isLocked())
                .credentialsNonExpired(user.isCredentialsNotExpired())
                .accountNonExpired(user.isAccountNotExpired())
                .build();
    }
}
```

### 2. Patient Controller with JWT Security

```java
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.healthdata.shared.security.util.JwtUtils;
import com.healthdata.shared.security.model.UserPrincipal;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    /**
     * Get all patients - requires PROVIDER or ADMIN role
     */
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN')")
    @GetMapping
    public ResponseEntity<Page<PatientDto>> getAllPatients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        String currentUser = JwtUtils.getCurrentUsername();
        log.info("User {} requesting patient list", currentUser);

        Page<PatientDto> patients = patientService.getPatients(
                PageRequest.of(page, size));

        return ResponseEntity.ok(patients);
    }

    /**
     * Get patient by ID - requires appropriate role
     */
    @PreAuthorize("hasAnyRole('PROVIDER', 'PATIENT', 'ADMIN', 'CARE_MANAGER')")
    @GetMapping("/{patientId}")
    public ResponseEntity<PatientDto> getPatient(@PathVariable String patientId) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();

        // PATIENT role can only see their own record
        if (principal.hasRole("PATIENT") && !isOwnRecord(patientId, principal.getUserId())) {
            throw new AccessDeniedException("Cannot access other patients' records");
        }

        PatientDto patient = patientService.getPatient(patientId);
        return ResponseEntity.ok(patient);
    }

    /**
     * Create patient - requires PROVIDER or ADMIN role
     */
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN')")
    @PostMapping
    public ResponseEntity<PatientDto> createPatient(@RequestBody CreatePatientRequest request) {

        String creator = JwtUtils.getCurrentUsername();
        log.info("User {} creating new patient", creator);

        PatientDto created = patientService.createPatient(request, creator);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update patient - requires PROVIDER or ADMIN role
     */
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN')")
    @PutMapping("/{patientId}")
    public ResponseEntity<PatientDto> updatePatient(
            @PathVariable String patientId,
            @RequestBody UpdatePatientRequest request) {

        String updater = JwtUtils.getCurrentUsername();
        log.info("User {} updating patient {}", updater, patientId);

        PatientDto updated = patientService.updatePatient(patientId, request, updater);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete patient - requires ADMIN role
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{patientId}")
    public ResponseEntity<Void> deletePatient(@PathVariable String patientId) {

        String deleter = JwtUtils.getCurrentUsername();
        log.info("User {} deleting patient {}", deleter, patientId);

        patientService.deletePatient(patientId);
        return ResponseEntity.noContent().build();
    }

    private boolean isOwnRecord(String patientId, String userId) {
        // Check if patient ID matches user's ID
        return patientId.equals(userId);
    }
}
```

### 3. Quality Measure Service with Role-Based Calculation

```java
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import com.healthdata.shared.security.util.JwtUtils;

@Service
@Transactional
public class QualityMeasureService {

    private final QualityMeasureRepository repository;
    private final AuditLogger auditLogger;

    /**
     * Calculate measures for a population - tracks who initiated calculation
     */
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN', 'CARE_MANAGER')")
    public MeasureResults calculateMeasures(String cohortId) {

        String initiator = JwtUtils.getCurrentUsername();
        long startTime = System.currentTimeMillis();

        log.info("User {} initiating measure calculation for cohort {}",
                initiator, cohortId);

        try {
            MeasureResults results = performCalculation(cohortId);

            // Audit log with user information
            auditLogger.log(AuditEvent.builder()
                    .user(initiator)
                    .action("CALCULATE_MEASURES")
                    .resourceId(cohortId)
                    .status("SUCCESS")
                    .duration(System.currentTimeMillis() - startTime)
                    .build());

            return results;

        } catch (Exception ex) {
            log.error("Measure calculation failed for user {}: {}",
                    initiator, ex.getMessage());

            auditLogger.log(AuditEvent.builder()
                    .user(initiator)
                    .action("CALCULATE_MEASURES")
                    .resourceId(cohortId)
                    .status("FAILED")
                    .error(ex.getMessage())
                    .build());

            throw ex;
        }
    }

    /**
     * Export measures - restricted by role
     */
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN')")
    public byte[] exportMeasures(String measureId, String format) {

        if (!JwtUtils.hasAnyRole("ADMIN", "PROVIDER")) {
            throw new AccessDeniedException("Export not allowed for your role");
        }

        MeasureEntity measure = repository.findById(measureId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Measure not found: " + measureId));

        if (format.equalsIgnoreCase("PDF")) {
            return exportAsPdf(measure);
        } else if (format.equalsIgnoreCase("EXCEL")) {
            return exportAsExcel(measure);
        } else {
            throw new InvalidArgumentException("Unsupported format: " + format);
        }
    }

    private MeasureResults performCalculation(String cohortId) {
        // Implementation here
        return new MeasureResults();
    }

    private byte[] exportAsPdf(MeasureEntity measure) {
        // PDF export logic
        return new byte[0];
    }

    private byte[] exportAsExcel(MeasureEntity measure) {
        // Excel export logic
        return new byte[0];
    }
}
```

### 4. Care Gap Workflow with Complex Authorization

```java
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import com.healthdata.shared.security.util.JwtUtils;

@Service
@Transactional
public class CareGapService {

    private final CareGapRepository repository;
    private final NotificationService notificationService;

    /**
     * Identify care gaps - PROVIDER or CARE_MANAGER
     */
    @PreAuthorize("hasAnyRole('PROVIDER', 'CARE_MANAGER')")
    public List<CareGapDto> identifyGaps(String patientId) {

        List<CareGapEntity> gaps = repository.findUnresolvedGaps(patientId);

        // Enrich with provider details
        return gaps.stream()
                .map(this::toCareGapDto)
                .collect(Collectors.toList());
    }

    /**
     * Assign gap to care manager - PROVIDER or ADMIN only
     */
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN')")
    public void assignGap(String gapId, String careManagerId) {

        String assignedBy = JwtUtils.getCurrentUsername();

        CareGapEntity gap = repository.findById(gapId)
                .orElseThrow(() -> new ResourceNotFoundException("Gap not found"));

        gap.setAssignedTo(careManagerId);
        gap.setAssignedBy(assignedBy);
        gap.setAssignedAt(LocalDateTime.now());
        repository.save(gap);

        log.info("Gap {} assigned to {} by {}", gapId, careManagerId, assignedBy);

        // Notify care manager
        notificationService.notifyGapAssignment(gapId, careManagerId);
    }

    /**
     * Close care gap - PROVIDER, CARE_MANAGER, or ADMIN
     */
    @PreAuthorize("hasAnyRole('PROVIDER', 'CARE_MANAGER', 'ADMIN')")
    public void closeGap(String gapId, String closureReason) {

        String closedBy = JwtUtils.getCurrentUsername();
        CareGapEntity gap = repository.findById(gapId)
                .orElseThrow(() -> new ResourceNotFoundException("Gap not found"));

        // CARE_MANAGER can only close gaps assigned to them
        if (JwtUtils.hasRole("CARE_MANAGER") &&
                !gap.getAssignedTo().equals(JwtUtils.getCurrentUsername())) {
            throw new AccessDeniedException(
                "Can only close gaps assigned to you");
        }

        gap.setStatus("CLOSED");
        gap.setClosedBy(closedBy);
        gap.setClosedAt(LocalDateTime.now());
        gap.setClosureReason(closureReason);
        repository.save(gap);

        log.info("Gap {} closed by {}", gapId, closedBy);

        // Notify stakeholders
        notificationService.notifyGapClosed(gapId);
    }

    /**
     * View gap details - role-based visibility
     */
    public CareGapDetailDto viewGap(String gapId) {

        CareGapEntity gap = repository.findById(gapId)
                .orElseThrow(() -> new ResourceNotFoundException("Gap not found"));

        String currentUser = JwtUtils.getCurrentUsername();

        // Check access permissions
        if (!hasAccessToGap(gap, currentUser)) {
            throw new AccessDeniedException("Cannot access this care gap");
        }

        return CareGapDetailDto.from(gap);
    }

    private boolean hasAccessToGap(CareGapEntity gap, String userId) {
        // ADMIN has access to all
        if (JwtUtils.hasRole("ADMIN")) {
            return true;
        }

        // PROVIDER has access to their patients' gaps
        if (JwtUtils.hasRole("PROVIDER")) {
            return gap.getPatient().getProvider().getId().equals(userId);
        }

        // CARE_MANAGER has access to gaps assigned to them
        if (JwtUtils.hasRole("CARE_MANAGER")) {
            return gap.getAssignedTo().equals(userId);
        }

        // PATIENT can see their own gaps
        if (JwtUtils.hasRole("PATIENT")) {
            return gap.getPatient().getId().equals(userId);
        }

        return false;
    }

    private CareGapDto toCareGapDto(CareGapEntity gap) {
        return CareGapDto.builder()
                .id(gap.getId())
                .patientId(gap.getPatient().getId())
                .description(gap.getDescription())
                .status(gap.getStatus())
                .priority(gap.getPriority())
                .build();
    }
}
```

### 5. Admin Controller with Strict Access

```java
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.healthdata.shared.security.util.JwtUtils;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final AuditLogger auditLogger;

    /**
     * Get all users - ADMIN only
     */
    @GetMapping("/users")
    public ResponseEntity<Page<UserDto>> getAllUsers(
            @RequestParam(defaultValue = "0") int page) {

        String admin = JwtUtils.getCurrentUsername();
        log.info("Admin {} accessing user list", admin);

        auditLogger.log(AuditEvent.builder()
                .user(admin)
                .action("VIEW_ALL_USERS")
                .status("SUCCESS")
                .build());

        return ResponseEntity.ok(userService.getAllUsers(
                PageRequest.of(page, 50)));
    }

    /**
     * Create user - ADMIN only
     */
    @PostMapping("/users")
    public ResponseEntity<UserDto> createUser(@RequestBody CreateUserRequest request) {

        String admin = JwtUtils.getCurrentUsername();
        log.info("Admin {} creating user {}", admin, request.getUsername());

        UserDto created = userService.createUser(request);

        auditLogger.log(AuditEvent.builder()
                .user(admin)
                .action("CREATE_USER")
                .resourceId(created.getId())
                .details(created.getUsername())
                .status("SUCCESS")
                .build());

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update user role - ADMIN only
     */
    @PutMapping("/users/{userId}/role")
    public ResponseEntity<UserDto> updateUserRole(
            @PathVariable String userId,
            @RequestBody UpdateRoleRequest request) {

        String admin = JwtUtils.getCurrentUsername();

        // Prevent admin from downgrading themselves
        if (userId.equals(admin) && !request.getRoles().contains("ADMIN")) {
            throw new InvalidOperationException(
                "Cannot remove ADMIN role from current user");
        }

        log.info("Admin {} updating role for user {}", admin, userId);

        UserDto updated = userService.updateUserRole(userId, request.getRoles());

        auditLogger.log(AuditEvent.builder()
                .user(admin)
                .action("UPDATE_USER_ROLE")
                .resourceId(userId)
                .details(String.join(",", request.getRoles()))
                .status("SUCCESS")
                .build());

        return ResponseEntity.ok(updated);
    }

    /**
     * Disable user account - ADMIN only
     */
    @PostMapping("/users/{userId}/disable")
    public ResponseEntity<Void> disableUser(@PathVariable String userId) {

        String admin = JwtUtils.getCurrentUsername();

        if (userId.equals(admin)) {
            throw new InvalidOperationException(
                "Cannot disable current user");
        }

        userService.disableUser(userId);

        auditLogger.log(AuditEvent.builder()
                .user(admin)
                .action("DISABLE_USER")
                .resourceId(userId)
                .status("SUCCESS")
                .build());

        return ResponseEntity.noContent().build();
    }

    /**
     * View audit logs - ADMIN only
     */
    @GetMapping("/audit-logs")
    public ResponseEntity<Page<AuditEventDto>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page) {

        String admin = JwtUtils.getCurrentUsername();
        log.info("Admin {} accessing audit logs", admin);

        Page<AuditEventDto> logs = auditLogger.getLogs(
                PageRequest.of(page, 100));

        return ResponseEntity.ok(logs);
    }
}
```

### 6. Notification Service with User Preferences

```java
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import com.healthdata.shared.security.util.JwtUtils;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository repository;
    private final EmailService emailService;

    /**
     * Get user's notification preferences
     */
    @PreAuthorize("isAuthenticated()")
    public NotificationPreferencesDto getPreferences() {

        String userId = JwtUtils.getCurrentUsername();

        NotificationPreference prefs = repository.findByUserId(userId)
                .orElse(NotificationPreference.createDefault(userId));

        return NotificationPreferencesDto.from(prefs);
    }

    /**
     * Update notification preferences
     */
    @PreAuthorize("isAuthenticated()")
    public void updatePreferences(NotificationPreferencesDto preferences) {

        String userId = JwtUtils.getCurrentUsername();

        NotificationPreference prefs = repository.findByUserId(userId)
                .orElse(NotificationPreference.createDefault(userId));

        prefs.setEmailNotifications(preferences.isEmailNotifications());
        prefs.setSmsNotifications(preferences.isSmsNotifications());
        prefs.setPushNotifications(preferences.isPushNotifications());
        prefs.setFrequency(preferences.getFrequency());

        repository.save(prefs);

        log.info("Updated notification preferences for user {}", userId);
    }

    /**
     * Send notification - triggered by system
     */
    public void sendNotification(String userId, NotificationMessage message) {

        NotificationPreference prefs = repository.findByUserId(userId)
                .orElse(NotificationPreference.createDefault(userId));

        if (prefs.isEmailNotifications()) {
            emailService.sendEmail(userId, message);
        }

        // Log notification sent
        log.info("Notification sent to user {} via email", userId);
    }
}
```

## Integration Patterns

### Pattern 1: Context-Aware Repositories

```java
@Repository
public interface PatientRepository extends JpaRepository<Patient, String> {

    // Only return patients belonging to current provider
    @Query("SELECT p FROM Patient p WHERE p.provider.id = :providerId")
    List<Patient> findByProviderId(@Param("providerId") String providerId);
}

// Usage in service
@Service
public class PatientService {

    @Autowired
    private PatientRepository repository;

    public List<Patient> getMyPatients() {
        String providerId = JwtUtils.getCurrentUsername();
        return repository.findByProviderId(providerId);
    }
}
```

### Pattern 2: Audit Trail

```java
@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    private String id;
    private String user;
    private String action;
    private String resourceId;
    private LocalDateTime timestamp = LocalDateTime.now();
    private String status;
    private String details;
}

// Usage
@Component
@Aspect
public class AuditAspect {

    @Around("@annotation(com.healthdata.Auditable)")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        String user = JwtUtils.getCurrentUsername();
        String method = joinPoint.getSignature().getName();

        Object result = joinPoint.proceed();

        auditRepository.save(AuditLog.builder()
                .user(user)
                .action(method)
                .status("SUCCESS")
                .build());

        return result;
    }
}
```

### Pattern 3: Request Logging

```java
@Component
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String user = JwtUtils.getCurrentUsername();
        String path = request.getRequestURI();
        String method = request.getMethod();

        log.info("User {} {} {}", user, method, path);
        filterChain.doFilter(request, response);
    }
}
```

## Best Practices

1. **Always use JwtUtils** for consistent user context access
2. **Log user actions** for security audit trail
3. **Check roles early** in service methods
4. **Throw AccessDeniedException** for authorization failures
5. **Never expose user details** in error messages
6. **Cache user principals** when possible
7. **Validate input** before authorization checks
8. **Use @PreAuthorize** at method level
9. **Implement audit logging** for sensitive operations
10. **Document role requirements** in API documentation

---

These examples demonstrate production-ready patterns for using the JWT security implementation in real-world scenarios.
