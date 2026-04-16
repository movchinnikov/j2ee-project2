package com.project.booking.repository;

import com.project.booking.domain.entity.EarningEntity;
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

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM EarningEntity e WHERE e.cleaner.id = :cleanerId")
    BigDecimal sumByCleanerId(@Param("cleanerId") UUID cleanerId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM EarningEntity e " +
           "WHERE e.cleaner.id = :cleanerId " +
           "AND YEAR(e.createdAt) = :year AND MONTH(e.createdAt) = :month")
    BigDecimal sumByCleanerIdAndMonth(@Param("cleanerId") UUID cleanerId,
                                       @Param("year") int year,
                                       @Param("month") int month);
}
