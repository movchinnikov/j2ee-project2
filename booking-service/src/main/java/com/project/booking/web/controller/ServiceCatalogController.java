package com.project.booking.web.controller;

import com.project.booking.domain.entity.ServiceAddonEntity;
import com.project.booking.domain.entity.ServiceTypeEntity;
import com.project.booking.service.UnitOfWork;
import com.project.booking.web.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Catalog", description = "Public service catalog — no auth required")
public class ServiceCatalogController {

    private final UnitOfWork uow;

    @Operation(summary = "List all cleaning service types with prices")
    @GetMapping("/services")
    public ResponseEntity<ApiResponse<List<ServiceTypeEntity>>> getServices() {
        return ResponseEntity.ok(ApiResponse.ok(uow.getServiceTypes().findAllByActiveTrue()));
    }

    @Operation(summary = "List all available service addons")
    @GetMapping("/addons")
    public ResponseEntity<ApiResponse<List<ServiceAddonEntity>>> getAddons() {
        return ResponseEntity.ok(ApiResponse.ok(uow.getServiceAddons().findAllByActiveTrue()));
    }
}
