package com.healthdata.authentication.service;

import com.healthdata.audit.models.AuditAction;
import com.healthdata.audit.models.AuditEvent;
import com.healthdata.audit.models.AuditOutcome;
import com.healthdata.audit.service.AuditService;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * Service for tenant management operations.
 * Handles tenant registration, activation, suspension, and deactivation.
 */
@Slf4j
@Service
public class TenantService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;  // Optional - may be null if audit module not available

    public TenantService(
            TenantRepository tenantRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Autowired(required = false) AuditService auditService) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    /**
     * Register a new tenant with an admin user.
     * Creates tenant and admin user atomically in a single transaction.
     *
     * @param request Tenant registration details
     * @return Tenant and admin user information
     * @throws TenantAlreadyExistsException if tenant ID already exists
     */
    @Transactional
    public TenantRegistrationResponse registerTenant(TenantRegistrationRequest request) {
        log.info("Tenant registration attempt: {}", request.getTenantId());

        // 1. Validate tenant doesn't already exist
        if (tenantRepository.existsById(request.getTenantId())) {
            log.warn("Tenant already exists: {}", request.getTenantId());
            throw new TenantAlreadyExistsException(request.getTenantId());
        }

        // 2. Create tenant
        Tenant tenant = Tenant.builder()
            .id(request.getTenantId())
            .name(request.getTenantName())
            .status(TenantStatus.ACTIVE)
            .build();
        tenant = tenantRepository.save(tenant);
        log.info("Tenant created: {} ({})", tenant.getId(), tenant.getName());

        // 3. Check for duplicate username/email BEFORE attempting to save
        if (userRepository.existsByUsername(request.getAdminUser().getUsername())) {
            log.warn("Duplicate username during tenant registration: {}",
                request.getAdminUser().getUsername());
            throw UserAlreadyExistsException.forUsername(request.getAdminUser().getUsername());
        }

        if (userRepository.existsByEmail(request.getAdminUser().getEmail())) {
            log.warn("Duplicate email during tenant registration: {}",
                request.getAdminUser().getEmail());
            throw UserAlreadyExistsException.forEmail(request.getAdminUser().getEmail());
        }

        // 4. Create admin user with ADMIN role and access to this tenant
        String passwordHash = passwordEncoder.encode(request.getAdminUser().getPassword());

        User adminUser = User.builder()
            .username(request.getAdminUser().getUsername())
            .email(request.getAdminUser().getEmail())
            .passwordHash(passwordHash)
            .firstName(request.getAdminUser().getFirstName())
            .lastName(request.getAdminUser().getLastName())
            .tenantIds(Set.of(tenant.getId()))
            .roles(Set.of(UserRole.ADMIN))
            .active(true)
            .emailVerified(false)
            .failedLoginAttempts(0)
            .build();

        adminUser = userRepository.save(adminUser);
        log.info("Admin user created: {} (ID: {}) for tenant: {}",
            adminUser.getUsername(), adminUser.getId(), tenant.getId());

        // 5. Audit tenant creation (if audit service is available)
        if (auditService != null) {
            auditService.logAuditEvent(AuditEvent.builder()
                .tenantId(tenant.getId())
                .userId(adminUser.getId().toString())
                .username(adminUser.getUsername())
                .action(AuditAction.CREATE)
                .resourceType("Tenant")
                .resourceId(tenant.getId())
                .outcome(AuditOutcome.SUCCESS)
                .serviceName("TenantService")
                .methodName("registerTenant")
                .build());
        } else {
            log.debug("Audit service not available - skipping audit log for tenant creation: {}", tenant.getId());
        }

        log.info("Tenant registration successful: {} with admin user: {}",
            tenant.getId(), adminUser.getUsername());

        // 6. Build and return response
        return TenantRegistrationResponse.builder()
            .tenantId(tenant.getId())
            .tenantName(tenant.getName())
            .status(tenant.getStatus())
            .createdAt(tenant.getCreatedAt())
            .adminUser(TenantRegistrationResponse.AdminUserInfo.builder()
                .userId(adminUser.getId())
                .username(adminUser.getUsername())
                .email(adminUser.getEmail())
                .roles(new HashSet<>(adminUser.getRoles()))
                .tenantIds(new HashSet<>(adminUser.getTenantIds()))
                .build())
            .build();
    }

    /**
     * Activate a suspended tenant.
     *
     * @param tenantId Tenant ID to activate
     */
    @Transactional
    public void activateTenant(String tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));

        if (!tenant.canBeReactivated()) {
            throw new IllegalStateException(
                "Tenant cannot be reactivated from status: " + tenant.getStatus());
        }

        tenant.activate();
        tenantRepository.save(tenant);
        log.info("Tenant activated: {}", tenantId);
    }

    /**
     * Suspend a tenant (temporary suspension).
     *
     * @param tenantId Tenant ID to suspend
     */
    @Transactional
    public void suspendTenant(String tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));

        tenant.suspend();
        tenantRepository.save(tenant);
        log.info("Tenant suspended: {}", tenantId);
    }

    /**
     * Deactivate a tenant permanently.
     *
     * @param tenantId Tenant ID to deactivate
     */
    @Transactional
    public void deactivateTenant(String tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));

        tenant.deactivate();
        tenantRepository.save(tenant);
        log.info("Tenant deactivated: {}", tenantId);
    }
}
