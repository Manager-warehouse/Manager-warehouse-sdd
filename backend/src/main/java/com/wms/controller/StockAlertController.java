package com.wms.controller;

import com.wms.dto.response.StockAlertResponse;
import com.wms.service.CurrentUserService;
import com.wms.service.StockAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/alerts/low-stock")
@RequiredArgsConstructor
@Tag(name = "Stock Alerts", description = "Quản lý cảnh báo tồn kho thấp (Spec 010)")
public class StockAlertController {

    private final StockAlertService stockAlertService;
    private final CurrentUserService currentUserService;

    @GetMapping
    @PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER', 'PLANNER', 'ADMIN', 'CEO')")
    @Operation(summary = "Xem danh sách cảnh báo tồn kho thấp")
    public Page<StockAlertResponse> getLowStockAlerts(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Boolean isResolved,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        Long currentUserId = currentUserService.getRequiredCurrentUser().getId();
        return stockAlertService.getLowStockAlerts(warehouseId, productId, isResolved, page, size, currentUserId);
    }

}
