package com.project.booking.service;

import com.project.booking.domain.entity.EarningEntity;
import com.project.booking.web.dto.response.EarningResponse;
import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EarningService {

    private final UnitOfWork uow;

    @Transactional(readOnly = true)
    public Either<String, EarningResponse.Summary> getSummary(UUID cleanerUserId) {
        return uow.getCleanerProfiles().findByUserId(cleanerUserId)
                .toEither("Cleaner profile not found")
                .map(profile -> {
                    BigDecimal total = uow.getEarnings().sumByCleanerId(profile.getId());
                    List<EarningEntity> history = uow.getEarnings()
                            .findAllByCleanerIdOrderByCreatedAtDesc(profile.getId());
                    return EarningResponse.Summary.builder()
                            .totalEarned(total)
                            .completedOrders(profile.getCompletedOrdersCount())
                            .history(history.stream().map(EarningResponse::from).toList())
                            .build();
                });
    }

    @Transactional(readOnly = true)
    public Either<String, BigDecimal> getMonthlyTotal(UUID cleanerUserId, int year, int month) {
        return uow.getCleanerProfiles().findByUserId(cleanerUserId)
                .toEither("Cleaner profile not found")
                .map(profile -> uow.getEarnings().sumByCleanerIdAndMonth(profile.getId(), year, month));
    }
}
