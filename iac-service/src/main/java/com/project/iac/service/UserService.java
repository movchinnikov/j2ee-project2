package com.project.iac.service;

import com.project.iac.domain.entity.UserEntity;
import com.project.iac.web.dto.response.UserResponse;
import io.vavr.control.Either;
import io.vavr.control.Option;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * User management service — profile retrieval, listing, deletion.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UnitOfWork uow;

    @Transactional(readOnly = true)
    public Either<String, UserResponse> getByUsername(String username) {
        return uow.getUsers().findByUsernameAsOption(username)
                .toEither("User not found: " + username)
                .map(UserResponse::from);
    }

    @Transactional(readOnly = true)
    public Either<String, UserResponse> getById(UUID id) {
        return uow.getUsers().findByIdAsOption(id)
                .toEither("User not found: " + id)
                .map(UserResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return uow.getUsers().findAllWithRoles(pageable)
                .map(UserResponse::from);
    }

    @Transactional
    public Either<String, Void> deleteUser(UUID id) {
        Option<UserEntity> userOpt = uow.getUsers().findByIdAsOption(id);
        if (userOpt.isEmpty()) {
            return Either.left("User not found: " + id);
        }
        UserEntity user = userOpt.get();
        // Prevent deletion of super admin
        boolean isSuperAdmin = user.getRoles().stream()
                .anyMatch(r -> r.getName().equals("SUPER_ADMIN"));
        if (isSuperAdmin) {
            return Either.left("Cannot delete the Super Admin user");
        }
        uow.getUsers().delete(user);
        log.info("Deleted user: {}", id);
        return Either.right(null);
    }
}
