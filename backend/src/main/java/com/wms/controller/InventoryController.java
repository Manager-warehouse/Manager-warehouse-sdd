package com.wms.controller;

import com.wms.dto.response.InventoryAvailabilityResponse;
import com.wms.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/warehouse-stock")
@Tag(name = "Warehouse Stock", description = "Read-only inventory availability queries")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/availability")
    @PreAuthorize("hasAnyRole('ADMIN','CEO','PLANNER','DISPATCHER','WAREHOUSE_MANAGER','STOREKEEPER','WAREHOUSE_STAFF','DRIVER')")
    @Operation(summary = "Get total, reserved, and available stock by warehouse and product")
    public InventoryAvailabilityResponse getAvailability(@RequestParam Long warehouseId,
                                                         @RequestParam Long productId) {
        return inventoryService.getAvailability(warehouseId, productId);
    }
}
