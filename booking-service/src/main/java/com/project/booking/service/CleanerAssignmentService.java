package com.project.booking.service;

import com.project.booking.domain.entity.CleanerProfileEntity;
import com.project.booking.repository.CleanerProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Optional;

/**
 * Assigns the most available cleaner to an order.
 * Strategy: pick available cleaner with fewest upcoming orders.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CleanerAssignmentService {

    private final CleanerProfileRepository cleanerRepo;

    @Transactional(readOnly = true)
    public Optional<CleanerProfileEntity> assignCleaner(LocalDateTime scheduledDate) {
        return cleanerRepo.findAllByIsAvailableTrue()
                .stream()
                .min(Comparator.comparingInt(CleanerProfileEntity::getCompletedOrdersCount))
                .map(cleaner -> {
                    log.info("Assigned cleaner {} (userId={}) for date {}",
                            cleaner.getId(), cleaner.getUserId(), scheduledDate);
                    return cleaner;
                });
    }
}
