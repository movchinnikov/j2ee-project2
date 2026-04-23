package com.project.order.repository;

import com.project.order.domain.entity.OrderEntity;
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

    Page<OrderEntity> findAll(Pageable pageable);

    Page<OrderEntity> findAllByStatus(String status, Pageable pageable);

    /** All orders in a series (parentOrderId = given id OR id = given id) */
    @Query("SELECT o FROM OrderEntity o WHERE o.parentOrderId = :pid OR o.id = :pid ORDER BY o.scheduledDate ASC")
    List<OrderEntity> findSeriesByParentId(@Param("pid") UUID parentId);

    /** Cleaner's upcoming orders */
    @Query("SELECT o FROM OrderEntity o WHERE o.cleaner.id = :cid " +
            "AND o.status NOT IN ('CANCELLED','COMPLETED') " +
            "AND o.scheduledDate >= :from ORDER BY o.scheduledDate ASC")
    List<OrderEntity> findUpcomingByCleaner(@Param("cid") UUID cleanerId, @Param("from") LocalDateTime from);

    default Option<OrderEntity> findByIdAsOption(UUID id) {
        return Option.ofOptional(findById(id));
    }
}
