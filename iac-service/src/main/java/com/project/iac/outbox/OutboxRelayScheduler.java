package com.project.iac.outbox;

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
 * Outbox Relay — polls the outbox_events table and publishes PENDING events to Kafka.
 *
 * Runs every 5 seconds. Processes events in batches of 50.
 * On success: marks event PROCESSED.
 * On failure: marks event FAILED with error message (retried on next run).
 *
 * This ensures at-least-once delivery even if the service crashes between
 * writing the event and publishing it.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRelayScheduler {

    private final OutboxEventRepository outboxRepo;
    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * Fixed-delay ensures we don't start a new relay run before the previous one finishes.
     */
    @Scheduled(fixedDelayString = "${outbox.relay.interval-ms:5000}")
    @Transactional
    public void relay() {
        List<OutboxEventEntity> pending = outboxRepo.findPendingBatch();
        if (pending.isEmpty()) return;

        log.debug("Outbox relay: processing {} pending event(s)", pending.size());

        List<UUID> successIds = new ArrayList<>();

        for (OutboxEventEntity event : pending) {
            try {
                // Use aggregateId as Kafka partition key for ordering guarantees per entity
                kafkaTemplate.send(event.getEventType(), event.getAggregateId(), event.getPayload())
                        .get(); // blocking send — ensures delivery before marking processed
                successIds.add(event.getId());
                log.info("Outbox relay: published [{}] for {}:{}", event.getEventType(),
                        event.getAggregateType(), event.getAggregateId());
            } catch (Exception e) {
                log.error("Outbox relay: failed to publish event {} [{}]: {}",
                        event.getId(), event.getEventType(), e.getMessage());
                outboxRepo.markFailed(event.getId(), e.getMessage(), LocalDateTime.now());
            }
        }

        if (!successIds.isEmpty()) {
            outboxRepo.markProcessed(successIds, LocalDateTime.now());
            log.debug("Outbox relay: marked {} event(s) as PROCESSED", successIds.size());
        }
    }
}
