package com.project.order.service;

import com.project.order.client.PricingClient;
import com.project.order.client.PricingClient.PriceRequest;
import com.project.order.client.PropertyClient;
import com.project.order.domain.entity.CleanerProfileEntity;
import com.project.order.domain.entity.EarningEntity;
import com.project.order.domain.entity.OrderEntity;
import com.project.order.outbox.OutboxPublisher;
import com.project.order.repository.CleanerProfileRepository;
import com.project.order.repository.EarningRepository;
import com.project.order.repository.OrderRepository;
import com.project.shared.event.OrderCancelledEvent;
import com.project.shared.event.OrderCompletedEvent;
import com.project.shared.event.OrderConfirmedEvent;
import com.project.shared.event.OrderCreatedEvent;
import com.project.shared.kafka.KafkaTopics;
import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service @RequiredArgsConstructor @Slf4j
public class OrderService {

    private final OrderRepository orderRepo;
    private final CleanerProfileRepository cleanerRepo;
    private final EarningRepository earningRepo;
    private final PricingClient pricingClient;
    private final PropertyClient propertyClient;
    private final OutboxPublisher outboxPublisher;

    /**
     * Round-robin counter — persists in JVM memory.
     */
    private final AtomicInteger rrCounter = new AtomicInteger(0);

    // ── Series helpers ─────────────────────────────────────────────────────

    /** Number of additional occurrences to generate per frequency */
    private static int occurrencesFor(String freq) {
        return switch (freq.toUpperCase()) {
            case "WEEKLY"    -> 7;   // 8 total (first + 7 more) = ~2 months
            case "BIWEEKLY"  -> 5;   // 6 total = ~3 months
            case "MONTHLY"   -> 2;   // 3 total = ~3 months
            default          -> 0;   // ONE_TIME — no siblings
        };
    }

    /** Days between occurrences */
    private static long daysIntervalFor(String freq) {
        return switch (freq.toUpperCase()) {
            case "WEEKLY"   -> 7;
            case "BIWEEKLY" -> 14;
            case "MONTHLY"  -> 30;
            default         -> 0;
        };
    }

    // ── Create ────────────────────────────────────────────────────────────

    @Transactional
    public Either<String, List<OrderEntity>> createOrder(
            UUID clientId,
            UUID propertyId,
            String serviceTypeCode,
            Set<String> addonCodes,
            String frequency,
            LocalDateTime scheduledDate,
            String notes,
            String bearerToken) {

        // 1. Validate property
        var propInfo = propertyClient.validateProperty(propertyId, clientId, bearerToken);
        if (propInfo == null) return Either.left("Property not found or does not belong to you");

        // 2. Calculate price
        var priceReq = new PriceRequest();
        priceReq.setAreaSqm(propInfo.getAreaSqm());
        priceReq.setBathroomsCount(propInfo.getBathroomsCount());
        priceReq.setServiceTypeCode(serviceTypeCode);
        priceReq.setAddonCodes(addonCodes);
        priceReq.setFrequency(frequency != null ? frequency : "ONE_TIME");

        var pricing = pricingClient.calculatePrice(priceReq);
        if (pricing == null) return Either.left("Pricing service unavailable — try again later");

        String freq = frequency != null ? frequency.toUpperCase() : "ONE_TIME";
        int extraCount = occurrencesFor(freq);
        int seriesSize = extraCount + 1;
        long intervalDays = daysIntervalFor(freq);
        String addonsStr = addonCodes != null ? String.join(",", addonCodes) : "";

        // 3. Save first order
        OrderEntity first = orderRepo.save(OrderEntity.builder()
                .clientId(clientId).propertyId(propertyId)
                .propertyName(propInfo.getName())
                .serviceTypeCode(pricing.getServiceTypeCode())
                .serviceTypeName(pricing.getServiceTypeName())
                .frequency(freq).scheduledDate(scheduledDate)
                .addonsSnapshot(addonsStr)
                .basePrice(pricing.getBasePrice()).discountPercent(pricing.getDiscountPercent())
                .addonsPrice(pricing.getAddonsPrice()).totalPrice(pricing.getTotalPrice())
                .durationHours(pricing.getDurationHours())
                .clientNotes(notes).status("PENDING")
                .seriesIndex(0).seriesSize(seriesSize)
                .build());

        List<OrderEntity> series = new ArrayList<>();
        series.add(first);

        // 4. Generate sibling orders for recurring frequencies
        for (int i = 1; i <= extraCount; i++) {
            LocalDateTime siblingDate = scheduledDate.plusDays(intervalDays * i);
            OrderEntity sibling = orderRepo.save(OrderEntity.builder()
                    .clientId(clientId).propertyId(propertyId)
                    .propertyName(propInfo.getName())
                    .serviceTypeCode(pricing.getServiceTypeCode())
                    .serviceTypeName(pricing.getServiceTypeName())
                    .frequency(freq).scheduledDate(siblingDate)
                    .addonsSnapshot(addonsStr)
                    .basePrice(pricing.getBasePrice()).discountPercent(pricing.getDiscountPercent())
                    .addonsPrice(pricing.getAddonsPrice()).totalPrice(pricing.getTotalPrice())
                    .durationHours(pricing.getDurationHours())
                    .clientNotes(notes).status("PENDING")
                    .parentOrderId(first.getId())
                    .seriesIndex(i).seriesSize(seriesSize)
                    .build());
            series.add(sibling);
        }

        // Update first order's seriesSize after siblings knowm
        if (extraCount > 0) {
            log.info("Created recurring series: {} orders, freq={}, first={}", seriesSize, freq, first.getId());
        }

        // Outbox: publish only for the first order
        outboxPublisher.publish("Order", first.getId().toString(), KafkaTopics.ORDER_CREATED,
                OrderCreatedEvent.builder()
                        .orderId(first.getId()).clientId(first.getClientId())
                        .propertyId(first.getPropertyId()).propertyName(first.getPropertyName())
                        .serviceTypeCode(first.getServiceTypeCode()).serviceTypeName(first.getServiceTypeName())
                        .frequency(first.getFrequency()).totalPrice(first.getTotalPrice())
                        .scheduledDate(first.getScheduledDate()).createdAt(LocalDateTime.now())
                        .build());

        return Either.right(series);
    }

    // ── Pay ───────────────────────────────────────────────────────────────

    @Transactional
    public Either<String, OrderEntity> payOrder(UUID orderId, UUID clientId) {
        return orderRepo.findByIdAsOption(orderId)
                .toEither("Order not found")
                .flatMap(order -> {
                    if (!order.getClientId().equals(clientId))
                        return Either.left("Access denied");
                    if (!"PENDING".equals(order.getStatus()))
                        return Either.left("Only PENDING orders can be paid (current: " + order.getStatus() + ")");

                    CleanerProfileEntity cleaner = assignCleanerRoundRobin();
                    if (cleaner == null)
                        return Either.left("No available cleaners at the moment — try again later");

                    order.setCleaner(cleaner);
                    order.setStatus("CONFIRMED");
                    OrderEntity saved = orderRepo.save(order);
                    log.info("Order {} paid → CONFIRMED, cleaner={}", orderId, cleaner.getId());

                    outboxPublisher.publish("Order", saved.getId().toString(), KafkaTopics.ORDER_CONFIRMED,
                            OrderConfirmedEvent.builder()
                                    .orderId(saved.getId()).clientId(saved.getClientId())
                                    .cleanerProfileId(cleaner.getId()).cleanerUserId(cleaner.getUserId())
                                    .totalPrice(saved.getTotalPrice())
                                    .scheduledDate(saved.getScheduledDate())
                                    .confirmedAt(LocalDateTime.now())
                                    .build());

                    return Either.right(saved);
                });
    }

    /** Round-robin assignment */
    private CleanerProfileEntity assignCleanerRoundRobin() {
        List<CleanerProfileEntity> available = cleanerRepo.findAllByIsAvailableTrue()
                .stream()
                .sorted(Comparator.comparing(c -> c.getId().toString()))
                .toList();
        if (available.isEmpty()) return null;
        int idx = rrCounter.getAndUpdate(i -> (i + 1) % available.size());
        CleanerProfileEntity chosen = available.get(idx);
        log.info("Round-robin: picked cleaner {} (index {}/{})", chosen.getId(), idx, available.size());
        return chosen;
    }

    // ── Client queries ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<OrderEntity> getClientOrders(UUID clientId, Pageable pageable) {
        return orderRepo.findAllByClientId(clientId, pageable);
    }

    @Transactional(readOnly = true)
    public Either<String, OrderEntity> getClientOrder(UUID orderId, UUID clientId) {
        return orderRepo.findByIdAsOption(orderId)
                .toEither("Order not found")
                .flatMap(o -> o.getClientId().equals(clientId) ? Either.right(o) : Either.left("Access denied"));
    }

    /** Returns full series (parent + all siblings) */
    @Transactional(readOnly = true)
    public Either<String, List<OrderEntity>> getOrderSeries(UUID orderId, UUID clientId) {
        return orderRepo.findByIdAsOption(orderId)
                .toEither("Order not found")
                .flatMap(o -> {
                    if (!o.getClientId().equals(clientId)) return Either.left("Access denied");
                    UUID parentId = o.getParentOrderId() != null ? o.getParentOrderId() : o.getId();
                    return Either.right(orderRepo.findSeriesByParentId(parentId));
                });
    }

    // ── Cancel ────────────────────────────────────────────────────────────

    @Transactional
    public Either<String, OrderEntity> cancelOrder(UUID orderId, UUID clientId, String reason) {
        return orderRepo.findByIdAsOption(orderId)
                .toEither("Order not found")
                .flatMap(o -> {
                    if (!o.getClientId().equals(clientId)) return Either.left("Access denied");
                    if ("COMPLETED".equals(o.getStatus()))  return Either.left("Cannot cancel completed order");
                    if ("CANCELLED".equals(o.getStatus()))  return Either.left("Already cancelled");
                    o.setStatus("CANCELLED");
                    o.setCancellationReason(reason);
                    OrderEntity saved = orderRepo.save(o);

                    outboxPublisher.publish("Order", saved.getId().toString(), KafkaTopics.ORDER_CANCELLED,
                            OrderCancelledEvent.builder()
                                    .orderId(saved.getId()).clientId(saved.getClientId())
                                    .cleanerProfileId(saved.getCleaner() != null ? saved.getCleaner().getId() : null)
                                    .cancellationReason(reason).previousStatus("CONFIRMED")
                                    .cancelledAt(LocalDateTime.now()).build());

                    return Either.right(saved);
                });
    }

    // ── Admin queries ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<OrderEntity> getAllOrders(Pageable pageable) {
        return orderRepo.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<OrderEntity> getAllOrdersByStatus(String status, Pageable pageable) {
        return orderRepo.findAllByStatus(status, pageable);
    }

    // ── Cleaner: schedule ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Either<String, List<OrderEntity>> getCleanerSchedule(UUID cleanerUserId) {
        return cleanerRepo.findByUserId(cleanerUserId)
                .toEither("Cleaner profile not found")
                .map(p -> orderRepo.findUpcomingByCleaner(p.getId(), LocalDateTime.now()));
    }

    // ── Cleaner: complete ─────────────────────────────────────────────────

    @Transactional
    public Either<String, OrderEntity> completeOrder(UUID orderId, UUID cleanerUserId) {
        return cleanerRepo.findByUserId(cleanerUserId)
                .toEither("Cleaner profile not found")
                .flatMap(profile -> orderRepo.findByIdAsOption(orderId)
                        .toEither("Order not found")
                        .flatMap(order -> {
                            if (order.getCleaner() == null || !order.getCleaner().getId().equals(profile.getId()))
                                return Either.left("Order not assigned to you");
                            if (!"CONFIRMED".equals(order.getStatus()) && !"IN_PROGRESS".equals(order.getStatus()))
                                return Either.left("Cannot complete order in status: " + order.getStatus());

                            order.setStatus("COMPLETED");
                            OrderEntity saved = orderRepo.save(order);

                            var earning = EarningEntity.builder()
                                    .cleaner(profile).order(saved)
                                    .amount(saved.getTotalPrice().multiply(new BigDecimal("0.60")))
                                    .build();
                            earningRepo.save(earning);

                            profile.setCompletedOrdersCount(profile.getCompletedOrdersCount() + 1);
                            cleanerRepo.save(profile);

                            outboxPublisher.publish("Order", saved.getId().toString(), KafkaTopics.ORDER_COMPLETED,
                                    OrderCompletedEvent.builder()
                                            .orderId(saved.getId()).clientId(saved.getClientId())
                                            .cleanerProfileId(profile.getId()).cleanerUserId(profile.getUserId())
                                            .totalPrice(saved.getTotalPrice())
                                            .cleanerEarning(earning.getAmount())
                                            .completedAt(LocalDateTime.now()).build());

                            return Either.right(saved);
                        }));
    }

    // ── Cleaner: replacement ──────────────────────────────────────────────

    @Transactional
    public Either<String, OrderEntity> requestReplacement(UUID orderId, UUID cleanerUserId) {
        return cleanerRepo.findByUserId(cleanerUserId)
                .toEither("Cleaner profile not found")
                .flatMap(profile -> orderRepo.findByIdAsOption(orderId)
                        .toEither("Order not found")
                        .flatMap(order -> {
                            if (order.getCleaner() == null || !order.getCleaner().getId().equals(profile.getId()))
                                return Either.left("Order not assigned to you");
                            CleanerProfileEntity replacement = cleanerRepo.findAllByIsAvailableTrue()
                                    .stream()
                                    .filter(c -> !c.getId().equals(profile.getId()))
                                    .sorted(Comparator.comparing(c -> c.getId().toString()))
                                    .findFirst().orElse(null);
                            order.setStatus(replacement != null ? "CONFIRMED" : "REPLACEMENT_REQUESTED");
                            if (replacement != null) order.setCleaner(replacement);
                            return Either.right(orderRepo.save(order));
                        }));
    }

    // ── Cleaner: earnings ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Either<String, BigDecimal> getTotalEarnings(UUID cleanerUserId) {
        return cleanerRepo.findByUserId(cleanerUserId)
                .toEither("Cleaner profile not found")
                .map(p -> earningRepo.sumByCleanerId(p.getId()));
    }

    @Transactional(readOnly = true)
    public Either<String, BigDecimal> getMonthlyEarnings(UUID cleanerUserId, int year, int month) {
        return cleanerRepo.findByUserId(cleanerUserId)
                .toEither("Cleaner profile not found")
                .map(p -> earningRepo.sumByCleanerIdAndMonth(p.getId(), year, month));
    }

    // ── Cleaner profile ───────────────────────────────────────────────────

    @Transactional
    public CleanerProfileEntity getOrCreateProfile(UUID userId) {
        return cleanerRepo.findByUserId(userId)
                .getOrElse(() -> cleanerRepo.save(
                        CleanerProfileEntity.builder().userId(userId).build()));
    }

    @Transactional
    public void setAvailability(UUID userId, boolean available) {
        cleanerRepo.findByUserId(userId).forEach(p -> {
            p.setAvailable(available);
            cleanerRepo.save(p);
        });
    }
}
