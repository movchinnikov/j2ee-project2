package com.project.booking.service;

import com.project.booking.domain.entity.*;
import com.project.booking.domain.enums.OrderFrequency;
import com.project.booking.domain.enums.OrderStatus;
import com.project.booking.web.dto.request.CreateOrderRequest;
import com.project.booking.web.dto.response.OrderResponse;
import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final UnitOfWork uow;
    private final PricingService pricingService;
    private final CleanerAssignmentService assignmentService;

    // ── Create Order ──────────────────────────────────────────────────────

    @Transactional
    public Either<String, OrderResponse> createOrder(UUID clientId, CreateOrderRequest req) {
        // Validate property belongs to client
        var propertyOpt = uow.getProperties().findByIdAsOption(req.getPropertyId());
        if (propertyOpt.isEmpty()) return Either.left("Property not found");
        PropertyEntity property = propertyOpt.get();
        if (!property.getClientId().equals(clientId)) return Either.left("Property does not belong to you");

        // Validate service type
        var stOpt = uow.getServiceTypes().findByIdAsOption(req.getServiceTypeId());
        if (stOpt.isEmpty()) return Either.left("Service type not found");
        ServiceTypeEntity serviceType = stOpt.get();

        // Resolve addons
        Set<ServiceAddonEntity> addons = req.getAddonIds() != null && !req.getAddonIds().isEmpty()
                ? uow.getServiceAddons().findAllByIdIn(req.getAddonIds())
                : Set.of();

        // Calculate price
        OrderFrequency frequency = parseFrequency(req.getFrequency());
        var pricing = pricingService.calculate(property, serviceType, addons, frequency);

        // Assign cleaner
        CleanerProfileEntity cleaner = assignmentService.assignCleaner(req.getScheduledDate()).orElse(null);

        OrderEntity order = OrderEntity.builder()
                .clientId(clientId)
                .property(property)
                .serviceType(serviceType)
                .cleaner(cleaner)
                .frequency(frequency)
                .scheduledDate(req.getScheduledDate())
                .addons(addons)
                .basePrice(pricing.basePrice())
                .discountPercent(pricing.discountPercent())
                .addonsPrice(pricing.addonsPrice())
                .totalPrice(pricing.totalPrice())
                .durationHours(pricing.durationHours())
                .clientNotes(req.getNotes())
                .status(cleaner != null ? OrderStatus.CONFIRMED : OrderStatus.PENDING)
                .build();

        OrderEntity saved = uow.getOrders().save(order);
        log.info("Order {} created for client {}, price={}", saved.getId(), clientId, pricing.totalPrice());
        return Either.right(OrderResponse.from(saved));
    }

    // ── Get Orders (Client) ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<OrderResponse> getClientOrders(UUID clientId, Pageable pageable) {
        return uow.getOrders().findAllByClientId(clientId, pageable).map(OrderResponse::from);
    }

    @Transactional(readOnly = true)
    public Either<String, OrderResponse> getOrderById(UUID orderId, UUID clientId) {
        return uow.getOrders().findByIdAsOption(orderId)
                .toEither("Order not found")
                .flatMap(o -> o.getClientId().equals(clientId)
                        ? Either.right(OrderResponse.from(o))
                        : Either.left("Access denied"));
    }

    // ── Cancel ────────────────────────────────────────────────────────────

    @Transactional
    public Either<String, OrderResponse> cancelOrder(UUID orderId, UUID clientId, String reason) {
        return uow.getOrders().findByIdAsOption(orderId)
                .toEither("Order not found")
                .flatMap(order -> {
                    if (!order.getClientId().equals(clientId)) return Either.left("Access denied");
                    if (order.getStatus() == OrderStatus.COMPLETED) return Either.left("Cannot cancel a completed order");
                    if (order.getStatus() == OrderStatus.CANCELLED)  return Either.left("Order already cancelled");
                    order.setStatus(OrderStatus.CANCELLED);
                    order.setCancellationReason(reason);
                    return Either.right(OrderResponse.from(uow.getOrders().save(order)));
                });
    }

    // ── Cleaner: upcoming schedule ────────────────────────────────────────

    @Transactional(readOnly = true)
    public Either<String, List<OrderResponse>> getCleanerSchedule(UUID cleanerUserId) {
        return uow.getCleanerProfiles().findByUserId(cleanerUserId)
                .toEither("Cleaner profile not found")
                .map(profile -> uow.getOrders()
                        .findUpcomingByCleaner(profile.getId(), LocalDateTime.now())
                        .stream().map(OrderResponse::from).toList());
    }

    // ── Cleaner: complete order ───────────────────────────────────────────

    @Transactional
    public Either<String, OrderResponse> completeOrder(UUID orderId, UUID cleanerUserId) {
        return uow.getCleanerProfiles().findByUserId(cleanerUserId)
                .toEither("Cleaner profile not found")
                .flatMap(profile -> uow.getOrders().findByIdAsOption(orderId)
                        .toEither("Order not found")
                        .flatMap(order -> {
                            if (!order.getCleaner().getId().equals(profile.getId()))
                                return Either.left("This order is not assigned to you");
                            if (order.getStatus() != OrderStatus.CONFIRMED && order.getStatus() != OrderStatus.IN_PROGRESS)
                                return Either.left("Order cannot be completed in status: " + order.getStatus());

                            order.setStatus(OrderStatus.COMPLETED);
                            OrderEntity saved = uow.getOrders().save(order);

                            // Record earning (60% of total price goes to cleaner)
                            var earning = EarningEntity.builder()
                                    .cleaner(profile)
                                    .order(saved)
                                    .amount(saved.getTotalPrice().multiply(new java.math.BigDecimal("0.60")))
                                    .build();
                            uow.getEarnings().save(earning);

                            profile.setCompletedOrdersCount(profile.getCompletedOrdersCount() + 1);
                            uow.getCleanerProfiles().save(profile);

                            return Either.right(OrderResponse.from(saved));
                        }));
    }

    // ── Cleaner: request replacement ──────────────────────────────────────

    @Transactional
    public Either<String, OrderResponse> requestReplacement(UUID orderId, UUID cleanerUserId) {
        return uow.getCleanerProfiles().findByUserId(cleanerUserId)
                .toEither("Cleaner profile not found")
                .flatMap(profile -> uow.getOrders().findByIdAsOption(orderId)
                        .toEither("Order not found")
                        .flatMap(order -> {
                            if (!order.getCleaner().getId().equals(profile.getId()))
                                return Either.left("This order is not assigned to you");
                            order.setStatus(OrderStatus.REPLACEMENT_REQUESTED);
                            return Either.right(OrderResponse.from(uow.getOrders().save(order)));
                        }));
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private OrderFrequency parseFrequency(String freq) {
        try {
            return freq != null ? OrderFrequency.valueOf(freq.toUpperCase()) : OrderFrequency.ONE_TIME;
        } catch (IllegalArgumentException e) {
            return OrderFrequency.ONE_TIME;
        }
    }
}
