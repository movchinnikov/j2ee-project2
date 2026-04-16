package com.project.order.repository;

import com.project.order.domain.entity.EarningEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface EarningRepository extends JpaRepository<EarningEntity, UUID> {
    List<EarningEntity> findAllByCleanerIdOrderByCreatedAtDesc(UUID cleanerId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM EarningEntity e WHERE e.cleaner.id = :cid")
    BigDecimal sumByCleanerId(@Param("cid") UUID cleanerId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM EarningEntity e " +
           "WHERE e.cleaner.id = :cid AND YEAR(e.createdAt) = :y AND MONTH(e.createdAt) = :m")
    BigDecimal sumByCleanerIdAndMonth(@Param("cid") UUID cleanerId, @Param("y") int year, @Param("m") int month);
}
