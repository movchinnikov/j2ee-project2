package com.project.booking.repository;

import com.project.booking.domain.entity.OrderEntity;
import com.project.booking.domain.enums.OrderStatus;
import io.vavr.control.Option;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    Page<OrderEntity> findAllByClientId(UUID clientId, Pageable pageable);

    @Query("SELECT o FROM OrderEntity o WHERE o.cleaner.id = :cleanerId " +
           "AND o.status NOT IN ('CANCELLED', 'COMPLETED') " +
           "AND o.scheduledDate >= :from ORDER BY o.scheduledDate ASC")
    List<OrderEntity> findUpcomingByCleaner(@Param("cleanerId") UUID cleanerId,
                                             @Param("from") LocalDateTime from);

    @Query("SELECT o FROM OrderEntity o WHERE o.cleaner.id = :cleanerId " +
           "AND o.scheduledDate BETWEEN :start AND :end ORDER BY o.scheduledDate ASC")
    List<OrderEntity> findByCleanerAndDateRange(@Param("cleanerId") UUID cleanerId,
                                                 @Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end);

    long countByCleanerIdAndStatus(UUID cleanerId, OrderStatus status);

    default Option<OrderEntity> findByIdAsOption(UUID id) { return Option.ofOptional(findById(id)); }
}
