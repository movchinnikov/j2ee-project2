package com.project.iac.service;

import com.project.iac.repository.RefreshTokenRepository;
import com.project.iac.repository.RoleRepository;
import com.project.iac.repository.UserRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Unit of Work pattern — groups all repositories under a single transactional boundary.
 * <p>
 * Services should inject {@code UnitOfWork} instead of individual repositories to ensure
 * consistent transaction management and a clean separation between the domain and persistence layers.
 * </p>
 *
 * <pre>{@code
 *   @Autowired UnitOfWork uow;
 *
 *   @Transactional
 *   public void doSomething() {
 *       var user = uow.users().findByUsernameAsOption("alice").get();
 *       uow.roles().findByNameAsOption("ADMIN").forEach(user::addRole);
 *       uow.users().save(user);
 *   }
 * }</pre>
 */
@Component
@Getter
@RequiredArgsConstructor
@Transactional
public class UnitOfWork {

    private final UserRepository users;
    private final RoleRepository roles;
    private final RefreshTokenRepository refreshTokens;
}
