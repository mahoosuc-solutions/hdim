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

import java.util.Set;

/**
 * Seeds a demo tenant and three demo users on first startup.
 * Idempotent: skips any tenant or user that already exists.
 *
 * <p>Controlled by {@code hdim.demo-tenant.enabled} (default {@code true}).
 * In production deployments, set {@code hdim.demo-tenant.enabled=false}.</p>
 */
@Component
@EnableConfigurationProperties(DemoTenantProperties.class)
public class DemoTenantBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoTenantBootstrap.class);

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DemoTenantProperties properties;

    public DemoTenantBootstrap(TenantRepository tenantRepository,
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
            log.info("Demo tenant seeding is disabled (hdim.demo-tenant.enabled=false). Skipping.");
            return;
        }

        String tenantId = properties.getId();

        if (tenantRepository.existsByIdIgnoreCase(tenantId)) {
            log.info("Demo tenant '{}' already exists. Skipping seed.", tenantId);
            return;
        }

        // Create and activate the demo tenant
        Tenant tenant = Tenant.builder()
            .id(tenantId)
            .name(properties.getName())
            .build();
        tenant.activate();
        tenantRepository.save(tenant);
        log.info("Created demo tenant: id='{}', name='{}'", tenantId, properties.getName());

        // Encode the shared demo password once
        String encodedPassword = passwordEncoder.encode(properties.getAdminPassword());

        // Admin user: ADMIN + EVALUATOR roles
        createUserIfMissing(
            properties.getAdminUsername(),
            properties.getAdminEmail(),
            "Demo", "Admin",
            encodedPassword,
            Set.of(UserRole.ADMIN, UserRole.EVALUATOR),
            tenantId
        );

        // Analyst user: ANALYST + EVALUATOR roles
        createUserIfMissing(
            properties.getAnalystUsername(),
            properties.getAnalystEmail(),
            "Demo", "Analyst",
            encodedPassword,
            Set.of(UserRole.ANALYST, UserRole.EVALUATOR),
            tenantId
        );

        // Viewer user: VIEWER role only
        createUserIfMissing(
            properties.getViewerUsername(),
            properties.getViewerEmail(),
            "Demo", "Viewer",
            encodedPassword,
            Set.of(UserRole.VIEWER),
            tenantId
        );

        log.info("Demo tenant seed complete: tenant='{}', users=[{}, {}, {}]",
            tenantId,
            properties.getAdminUsername(),
            properties.getAnalystUsername(),
            properties.getViewerUsername());
    }

    /**
     * Creates a user if one with the given username does not already exist.
     */
    private void createUserIfMissing(String username,
                                     String email,
                                     String firstName,
                                     String lastName,
                                     String encodedPassword,
                                     Set<UserRole> roles,
                                     String tenantId) {
        if (userRepository.existsByUsername(username)) {
            log.info("Demo user '{}' already exists. Skipping.", username);
            return;
        }

        User user = User.builder()
            .username(username)
            .email(email)
            .firstName(firstName)
            .lastName(lastName)
            .passwordHash(encodedPassword)
            .roles(roles)
            .tenantIds(Set.of(tenantId))
            .active(true)
            .emailVerified(true)
            .mfaEnabled(false)
            .forcePasswordChange(true)
            .failedLoginAttempts(0)
            .build();

        userRepository.save(user);
        log.info("Created demo user: username='{}', roles={}", username, roles);
    }
}
