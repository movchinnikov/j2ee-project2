package com.project.order.repository;

import com.project.order.domain.entity.CleanerProfileEntity;
import io.vavr.control.Option;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface CleanerProfileRepository extends JpaRepository<CleanerProfileEntity, UUID> {
    Option<CleanerProfileEntity> findByUserId(UUID userId);
    List<CleanerProfileEntity> findAllByIsAvailableTrue();
    default Option<CleanerProfileEntity> findByIdAsOption(UUID id) { return Option.ofOptional(findById(id)); }
}
