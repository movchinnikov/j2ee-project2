package com.project.pricing.service;

import com.project.pricing.domain.entity.ServiceAddonEntity;
import com.project.pricing.domain.entity.ServiceTypeEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

/**
 * Pure pricing calculation engine — no DB writes, no side effects.
 *
 * Formula:
 *   base     = areaSqm × ratePerSqm + bathrooms × 15
 *   discount = WEEKLY 15% | BIWEEKLY 10% | MONTHLY 5% | ONE_TIME 0%
 *   addons   = sum(addon.price)
 *   total    = base × (1 - discount/100) + addons
 *   duration = (areaSqm/20 * 60 + addonMinutes) / 60 hours
 */
@Service @Slf4j
public class PricingEngine {

    private static final BigDecimal BATHROOM_RATE = new BigDecimal("15.00");
    private static final BigDecimal AVG_SQM_PER_HOUR = new BigDecimal("20.0");

    public PricingResult calculate(BigDecimal areaSqm,
                                   int bathrooms,
                                   ServiceTypeEntity serviceType,
                                   Set<ServiceAddonEntity> addons,
                                   String frequency) {

        BigDecimal base = areaSqm.multiply(serviceType.getRatePerSqm())
                .add(BATHROOM_RATE.multiply(BigDecimal.valueOf(bathrooms)))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal discountPct = switch (frequency.toUpperCase()) {
            case "WEEKLY"   -> new BigDecimal("15");
            case "BIWEEKLY" -> new BigDecimal("10");
            case "MONTHLY"  -> new BigDecimal("5");
            default         -> BigDecimal.ZERO;
        };

        BigDecimal discountAmt = base.multiply(discountPct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal addonsTotal = addons.stream()
                .map(ServiceAddonEntity::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);

        BigDecimal total = base.subtract(discountAmt).add(addonsTotal).setScale(2, RoundingMode.HALF_UP);

        double baseMin  = areaSqm.divide(AVG_SQM_PER_HOUR, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(60)).doubleValue();
        int addonMin = addons.stream().mapToInt(ServiceAddonEntity::getDurationMinutes).sum();
        BigDecimal durationHours = BigDecimal.valueOf((baseMin + addonMin) / 60.0)
                .setScale(2, RoundingMode.HALF_UP);

        log.debug("Pricing: base={} disc={}% addons={} total={} dur={}h",
                base, discountPct, addonsTotal, total, durationHours);

        return new PricingResult(base, discountPct, addonsTotal, total, durationHours,
                serviceType.getCode(), serviceType.getName());
    }

    public record PricingResult(
            BigDecimal basePrice,
            BigDecimal discountPercent,
            BigDecimal addonsPrice,
            BigDecimal totalPrice,
            BigDecimal durationHours,
            String serviceTypeCode,
            String serviceTypeName
    ) {}
}
