package com.project.iac.repository;

import com.project.iac.domain.entity.RoleEntity;
import io.vavr.control.Option;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link RoleEntity}.
 */
@Repository
public interface RoleRepository extends JpaRepository<RoleEntity, UUID> {

    java.util.Optional<RoleEntity> findByName(String name);

    boolean existsByName(String name);

    List<RoleEntity> findAllByNameIn(List<String> names);

    default Option<RoleEntity> findByNameAsOption(String name) {
        return Option.ofOptional(findByName(name));
    }
}
