package com.wms.controller;

import com.wms.dto.response.CeoDashboardResponse;
import com.wms.dto.response.InventoryValuationResponse;
import com.wms.service.CurrentUserService;
import com.wms.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Reports & Dashboards", description = "Quản lý báo cáo & dashboard quản trị (Spec 010)")
public class ReportController {

    private final ReportService reportService;
    private final CurrentUserService currentUserService;

    @GetMapping("/dashboard/ceo")
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN')")
    @Operation(summary = "Xem 5 chỉ số KPI chiến lược trên CEO Dashboard")
    public CeoDashboardResponse getCeoDashboard() {
        Long currentUserId = currentUserService.getRequiredCurrentUser().getId();
        return reportService.getCeoDashboard(currentUserId);
    }

    @GetMapping("/reports/inventory-valuation")
    @PreAuthorize("hasAnyRole('ACCOUNTANT_MANAGER', 'ADMIN', 'CEO', 'WAREHOUSE_MANAGER')")
    @Operation(summary = "Xem báo cáo Giá trị tồn kho cuối kỳ")
    public InventoryValuationResponse getInventoryValuation(
            @RequestParam(required = false) Long warehouseId) {
        Long currentUserId = currentUserService.getRequiredCurrentUser().getId();
        return reportService.getInventoryValuation(warehouseId, currentUserId);
    }

}
