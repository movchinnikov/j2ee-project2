package com.project.order.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.project.order.domain.entity.CleanerProfileEntity;
import com.project.order.repository.CleanerProfileRepository;
import com.project.shared.event.UserRegisteredEvent;
import com.project.shared.event.UserRoleAssignedEvent;
import com.project.shared.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kafka consumer for user-domain events produced by IAC Service.
 *
 * Listens to:
 *   - {@code user.registered}   → auto-creates CleanerProfile if user registered as CLEANER
 *   - {@code user.role_assigned} → auto-creates CleanerProfile when CLEANER role is assigned
 *
 * This decouples CleanerProfile creation from the "first API call" approach,
 * ensuring cleaners are ready to be assigned orders as soon as they register.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

    private final CleanerProfileRepository cleanerRepo;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    // ── user.registered ───────────────────────────────────────────────────

    @KafkaListener(
            topics = KafkaTopics.USER_REGISTERED,
            groupId = "order-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onUserRegistered(String payload) {
        try {
            UserRegisteredEvent event = objectMapper.readValue(payload, UserRegisteredEvent.class);
            log.info("Received user.registered for userId={}, roles={}", event.getUserId(), event.getRoles());

            if (event.getRoles() != null && event.getRoles().contains("CLEANER")) {
                ensureCleanerProfile(event.getUserId());
            }
        } catch (Exception e) {
            log.error("Failed to process user.registered event: {}", e.getMessage());
        }
    }

    // ── user.role_assigned ────────────────────────────────────────────────

    @KafkaListener(
            topics = KafkaTopics.USER_ROLE_ASSIGNED,
            groupId = "order-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onUserRoleAssigned(String payload) {
        try {
            UserRoleAssignedEvent event = objectMapper.readValue(payload, UserRoleAssignedEvent.class);
            log.info("Received user.role_assigned for userId={}, role={}", event.getUserId(), event.getRoleName());

            if ("CLEANER".equals(event.getRoleName())) {
                ensureCleanerProfile(event.getUserId());
            }
        } catch (Exception e) {
            log.error("Failed to process user.role_assigned event: {}", e.getMessage());
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────

    /**
     * Idempotently ensures a CleanerProfile exists for the given userId.
     * If it already exists (e.g. created via API call), nothing happens.
     */
    private void ensureCleanerProfile(java.util.UUID userId) {
        if (cleanerRepo.findByUserId(userId).isDefined()) {
            log.debug("CleanerProfile already exists for userId={}, skipping", userId);
            return;
        }
        CleanerProfileEntity profile = CleanerProfileEntity.builder()
                .userId(userId)
                .build();
        cleanerRepo.save(profile);
        log.info("Auto-created CleanerProfile for userId={}", userId);
    }
}
