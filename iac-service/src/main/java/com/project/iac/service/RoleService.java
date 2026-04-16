package com.project.iac.service;

import com.project.iac.domain.entity.RoleEntity;
import com.project.iac.domain.entity.UserEntity;
import com.project.iac.outbox.OutboxPublisher;
import com.project.iac.web.dto.response.UserResponse;
import com.project.shared.event.UserRoleAssignedEvent;
import com.project.shared.kafka.KafkaTopics;
import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Role management service — assign/revoke roles, list roles.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoleService {

    private final UnitOfWork uow;
    private final OutboxPublisher outboxPublisher;

    @Transactional
    public Either<String, UserResponse> assignRole(String username, String roleName) {
        return uow.getUsers().findByUsernameAsOption(username)
                .toEither("User not found: " + username)
                .flatMap(user ->
                        uow.getRoles().findByNameAsOption(roleName)
                                .toEither("Role not found: " + roleName)
                                .map(role -> {
                                    if (user.getRoles().stream().anyMatch(r -> r.getName().equals(roleName))) {
                                        return user; // already has role, idempotent
                                    }
                                    user.addRole(role);
                                    UserEntity saved = uow.getUsers().save(user);
                                    log.info("Assigned role {} to user {}", roleName, username);

                                    // ── Outbox: publish UserRoleAssignedEvent ───────────────
                                    outboxPublisher.publish(
                                            "User",
                                            saved.getId().toString(),
                                            KafkaTopics.USER_ROLE_ASSIGNED,
                                            UserRoleAssignedEvent.builder()
                                                    .userId(saved.getId())
                                                    .username(saved.getUsername())
                                                    .roleName(roleName)
                                                    .assignedAt(LocalDateTime.now())
                                                    .build()
                                    );
                                    return saved;
                                })
                )
                .map(UserResponse::from);
    }

    @Transactional
    public Either<String, UserResponse> revokeRole(String username, String roleName) {
        if ("SUPER_ADMIN".equals(roleName)) {
            return Either.left("SUPER_ADMIN role cannot be revoked");
        }
        return uow.getUsers().findByUsernameAsOption(username)
                .toEither("User not found: " + username)
                .flatMap(user ->
                        uow.getRoles().findByNameAsOption(roleName)
                                .toEither("Role not found: " + roleName)
                                .map(role -> {
                                    user.removeRole(role);
                                    UserEntity saved = uow.getUsers().save(user);
                                    log.info("Revoked role {} from user {}", roleName, username);
                                    return saved;
                                })
                )
                .map(UserResponse::from);
    }

    @Transactional(readOnly = true)
    public List<String> getAllRoles() {
        return uow.getRoles().findAll()
                .stream()
                .map(RoleEntity::getName)
                .toList();
    }
}
