package com.project.booking.service;

import com.project.booking.domain.entity.PropertyEntity;
import com.project.booking.domain.entity.ServiceAddonEntity;
import com.project.booking.domain.entity.ServiceTypeEntity;
import com.project.booking.domain.enums.OrderFrequency;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

/**
 * Calculates order price based on property, service type, addons, and frequency.
 *
 * <p>Formula:</p>
 * <pre>
 *   basePrice  = area_sqm × ratePerSqm + bathrooms × 15
 *   addonsTotal = sum of addon prices
 *   discount   = WEEKLY 15% | BIWEEKLY 10% | MONTHLY 5% | ONE_TIME 0%
 *   total      = (basePrice - discount) + addonsTotal
 *   duration   = totalMinutes / 60  (addons contribute duration_minutes)
 * </pre>
 */
@Service
@Slf4j
public class PricingService {

    private static final BigDecimal BATHROOM_RATE = new BigDecimal("15.00");
    private static final BigDecimal AVG_SQM_PER_HOUR = new BigDecimal("20.0");

    public PricingResult calculate(PropertyEntity property,
                                   ServiceTypeEntity serviceType,
                                   Set<ServiceAddonEntity> addons,
                                   OrderFrequency frequency) {

        // Base price from area + bathrooms
        BigDecimal basePrice = property.getAreaSqm()
                .multiply(serviceType.getRatePerSqm())
                .add(BATHROOM_RATE.multiply(BigDecimal.valueOf(property.getBathroomsCount())))
                .setScale(2, RoundingMode.HALF_UP);

        // Frequency discount
        BigDecimal discountPercent = switch (frequency) {
            case WEEKLY   -> new BigDecimal("15");
            case BIWEEKLY -> new BigDecimal("10");
            case MONTHLY  -> new BigDecimal("5");
            case ONE_TIME -> BigDecimal.ZERO;
        };
        BigDecimal discountAmount = basePrice
                .multiply(discountPercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        // Addons
        BigDecimal addonsTotal = addons.stream()
                .map(ServiceAddonEntity::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalPrice = basePrice.subtract(discountAmount).add(addonsTotal);

        // Duration
        double baseMinutes = property.getAreaSqm()
                .divide(AVG_SQM_PER_HOUR, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(60))
                .doubleValue();
        int addonMinutes = addons.stream()
                .mapToInt(ServiceAddonEntity::getDurationMinutes)
                .sum();
        BigDecimal durationHours = BigDecimal.valueOf((baseMinutes + addonMinutes) / 60.0)
                .setScale(2, RoundingMode.HALF_UP);

        log.debug("Price calc: base={} discount={}% addons={} total={} duration={}h",
                basePrice, discountPercent, addonsTotal, totalPrice, durationHours);

        return new PricingResult(basePrice, discountPercent, addonsTotal, totalPrice, durationHours);
    }

    public record PricingResult(
            BigDecimal basePrice,
            BigDecimal discountPercent,
            BigDecimal addonsPrice,
            BigDecimal totalPrice,
            BigDecimal durationHours
    ) {}
}
