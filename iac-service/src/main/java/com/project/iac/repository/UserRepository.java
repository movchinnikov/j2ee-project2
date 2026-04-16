package com.project.iac.repository;

import com.project.iac.domain.entity.UserEntity;
import io.vavr.control.Option;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for {@link UserEntity}.
 * Returns Vavr {@link Option} for nullable lookups to eliminate nulls.
 */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    /**
     * Find by username — returns Vavr Option to force null-safety.
     */
    @Query("SELECT u FROM UserEntity u LEFT JOIN FETCH u.roles WHERE u.username = :username")
    java.util.Optional<UserEntity> findByUsernameWithRoles(@Param("username") String username);

    java.util.Optional<UserEntity> findByUsername(String username);

    java.util.Optional<UserEntity> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM UserEntity u LEFT JOIN FETCH u.roles")
    Page<UserEntity> findAllWithRoles(Pageable pageable);

    /**
     * Vavr-wrapped lookup — use in service layer for cleaner Option chaining.
     */
    default Option<UserEntity> findByUsernameAsOption(String username) {
        return Option.ofOptional(findByUsername(username));
    }

    default Option<UserEntity> findByEmailAsOption(String email) {
        return Option.ofOptional(findByEmail(email));
    }

    default Option<UserEntity> findByIdAsOption(UUID id) {
        return Option.ofOptional(findById(id));
    }
}
