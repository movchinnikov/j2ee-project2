package com.project.order.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Outbox Relay for Order Service — polls outbox_events and publishes to Kafka.
 * Runs every 5 seconds with fixed delay.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRelayScheduler {

    private final OutboxEventRepository outboxRepo;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelayString = "${outbox.relay.interval-ms:5000}")
    @Transactional
    public void relay() {
        List<OutboxEventEntity> pending = outboxRepo.findPendingBatch();
        if (pending.isEmpty()) return;

        log.debug("Outbox relay: processing {} pending event(s)", pending.size());

        List<UUID> successIds = new ArrayList<>();

        for (OutboxEventEntity event : pending) {
            try {
                kafkaTemplate.send(event.getEventType(), event.getAggregateId(), event.getPayload())
                        .get();
                successIds.add(event.getId());
                log.info("Outbox relay: published [{}] for {}:{}",
                        event.getEventType(), event.getAggregateType(), event.getAggregateId());
            } catch (Exception e) {
                log.error("Outbox relay: failed to publish event {} [{}]: {}",
                        event.getId(), event.getEventType(), e.getMessage());
                outboxRepo.markFailed(event.getId(), e.getMessage(), LocalDateTime.now());
            }
        }

        if (!successIds.isEmpty()) {
            outboxRepo.markProcessed(successIds, LocalDateTime.now());
        }
    }
}
