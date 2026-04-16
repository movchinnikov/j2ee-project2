package com.project.booking.repository;

import com.project.booking.domain.entity.PropertyEntity;
import io.vavr.control.Option;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface PropertyRepository extends JpaRepository<PropertyEntity, UUID> {
    Page<PropertyEntity> findAllByClientIdAndActiveTrue(UUID clientId, Pageable pageable);
    default Option<PropertyEntity> findByIdAsOption(UUID id) { return Option.ofOptional(findById(id)); }
}
