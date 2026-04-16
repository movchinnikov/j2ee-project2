package com.project.order.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Writes an event to the outbox table within the caller's active @Transactional boundary.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxEventRepository outboxRepo;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    public void publish(String aggregateType, String aggregateId, String topic, Object eventPayload) {
        try {
            String json = objectMapper.writeValueAsString(eventPayload);
            OutboxEventEntity entry = OutboxEventEntity.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(topic)
                    .payload(json)
                    .status("PENDING")
                    .build();
            outboxRepo.save(entry);
            log.debug("Outbox: queued event [{}] for {}:{}", topic, aggregateType, aggregateId);
        } catch (JsonProcessingException e) {
            log.error("Outbox: failed to serialise event [{}]: {}", topic, e.getMessage());
            throw new IllegalStateException("Event serialisation failed", e);
        }
    }
}
