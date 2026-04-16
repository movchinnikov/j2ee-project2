package com.project.booking.service;

import com.project.booking.repository.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Unit of Work — groups all booking repositories under one @Transactional boundary.
 */
@Component
@Getter
@RequiredArgsConstructor
@Transactional
public class UnitOfWork {
    private final PropertyRepository properties;
    private final ServiceTypeRepository serviceTypes;
    private final ServiceAddonRepository serviceAddons;
    private final CleanerProfileRepository cleanerProfiles;
    private final OrderRepository orders;
    private final EarningRepository earnings;
}
