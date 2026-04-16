package com.project.booking.repository;

import com.project.booking.domain.entity.ServiceTypeEntity;
import io.vavr.control.Option;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ServiceTypeRepository extends JpaRepository<ServiceTypeEntity, UUID> {
    List<ServiceTypeEntity> findAllByActiveTrue();
    default Option<ServiceTypeEntity> findByIdAsOption(UUID id) { return Option.ofOptional(findById(id)); }
}
