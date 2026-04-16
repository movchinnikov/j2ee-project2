package com.project.pricing.repository;

import com.project.pricing.domain.entity.ServiceTypeEntity;
import io.vavr.control.Option;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ServiceTypeRepository extends JpaRepository<ServiceTypeEntity, UUID> {
    List<ServiceTypeEntity> findAllByActiveTrue();
    Option<ServiceTypeEntity> findByCode(String code);
}
