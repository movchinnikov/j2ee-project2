package com.project.order.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {

    @Query("SELECT e FROM OutboxEventEntity e WHERE e.status = 'PENDING' ORDER BY e.createdAt ASC LIMIT 50")
    List<OutboxEventEntity> findPendingBatch();

    @Modifying
    @Query("UPDATE OutboxEventEntity e SET e.status = 'PROCESSED', e.processedAt = :now WHERE e.id IN :ids")
    void markProcessed(@Param("ids") List<UUID> ids, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE OutboxEventEntity e SET e.status = 'FAILED', e.errorMessage = :error, e.processedAt = :now WHERE e.id = :id")
    void markFailed(@Param("id") UUID id, @Param("error") String error, @Param("now") LocalDateTime now);
}
