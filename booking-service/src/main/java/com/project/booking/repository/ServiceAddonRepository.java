package com.project.booking.repository;

import com.project.booking.domain.entity.ServiceAddonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface ServiceAddonRepository extends JpaRepository<ServiceAddonEntity, UUID> {
    List<ServiceAddonEntity> findAllByActiveTrue();
    Set<ServiceAddonEntity> findAllByIdIn(Set<UUID> ids);
}
