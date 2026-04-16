package com.project.iac.bootstrap;

import com.project.iac.domain.entity.RoleEntity;
import com.project.iac.domain.entity.UserEntity;
import com.project.iac.service.UnitOfWork;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs on application startup to ensure a Super Admin user exists.
 * Idempotent — safe to run on every restart.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UnitOfWork uow;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.superadmin.username}")
    private String superAdminUsername;

    @Value("${app.superadmin.email}")
    private String superAdminEmail;

    @Value("${app.superadmin.password}")
    private String superAdminPassword;

    @Value("${app.superadmin.first-name}")
    private String superAdminFirstName;

    @Value("${app.superadmin.last-name}")
    private String superAdminLastName;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("=== IAC Bootstrap: Checking Super Admin user ===");

        if (uow.getUsers().existsByUsername(superAdminUsername)) {
            log.info("Super Admin '{}' already exists — skipping creation.", superAdminUsername);
            return;
        }

        RoleEntity superAdminRole = uow.getRoles().findByNameAsOption("SUPER_ADMIN")
                .getOrElseThrow(() -> new IllegalStateException(
                        "SUPER_ADMIN role not found — ensure Flyway migration ran successfully."));

        RoleEntity adminRole = uow.getRoles().findByNameAsOption("ADMIN")
                .getOrElse(() -> null);

        UserEntity superAdmin = UserEntity.builder()
                .username(superAdminUsername)
                .email(superAdminEmail)
                .passwordHash(passwordEncoder.encode(superAdminPassword))
                .firstName(superAdminFirstName)
                .lastName(superAdminLastName)
                .enabled(true)
                .build();

        superAdmin.addRole(superAdminRole);
        if (adminRole != null) {
            superAdmin.addRole(adminRole);
        }

        uow.getUsers().save(superAdmin);

        log.info("=======================================================");
        log.info("  Super Admin created successfully!");
        log.info("  Username : {}", superAdminUsername);
        log.info("  Email    : {}", superAdminEmail);
        log.info("  Password : {}", superAdminPassword);
        log.info("  Roles    : SUPER_ADMIN, ADMIN");
        log.info("=======================================================");
    }
}
