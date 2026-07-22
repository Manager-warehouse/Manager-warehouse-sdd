package com.wms.controller.stock_control;


import com.wms.entity.access_control.*;
import com.wms.entity.audit_trail.*;
import com.wms.entity.billing_payment.*;
import com.wms.entity.dealer_management.*;
import com.wms.entity.document_numbering.*;
import com.wms.entity.driver_management.*;
import com.wms.entity.fleet_management.*;
import com.wms.entity.notification_delivery.*;
import com.wms.entity.order_fulfillment.*;
import com.wms.entity.price_management.*;
import com.wms.entity.product_catalog.*;
import com.wms.entity.stock_control.*;
import com.wms.entity.stock_counting.*;
import com.wms.entity.stock_receiving.*;
import com.wms.entity.supplier_management.*;
import com.wms.entity.user_configuration.*;
import com.wms.entity.warehouse_location.*;
import com.wms.entity.warehouse_transfer.*;
import com.wms.enums.access_control.*;
import com.wms.enums.audit_trail.*;
import com.wms.enums.billing_payment.*;
import com.wms.enums.dealer_management.*;
import com.wms.enums.driver_management.*;
import com.wms.enums.fleet_management.*;
import com.wms.enums.notification_delivery.*;
import com.wms.enums.order_fulfillment.*;
import com.wms.enums.price_management.*;
import com.wms.enums.stock_control.*;
import com.wms.enums.stock_counting.*;
import com.wms.enums.stock_receiving.*;
import com.wms.enums.supplier_management.*;
import com.wms.enums.user_configuration.*;
import com.wms.enums.warehouse_location.*;
import com.wms.enums.warehouse_transfer.*;
import com.wms.dto.response.InventoryAvailabilityResponse;
import com.wms.dto.response.WarehouseStockOverviewResponse;
import com.wms.service.stock_control.InventoryService;
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

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('ADMIN','CEO','PLANNER','DISPATCHER','WAREHOUSE_MANAGER','STOREKEEPER','WAREHOUSE_STAFF')")
    @Operation(summary = "Get warehouse overview KPIs from current inventory and transaction data")
    public WarehouseStockOverviewResponse getOverview(@RequestParam Long warehouseId) {
        return inventoryService.getOverview(warehouseId);
    }
}
