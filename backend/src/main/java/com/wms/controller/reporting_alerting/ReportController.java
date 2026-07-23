package com.wms.controller.reporting_alerting;


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
import com.wms.dto.response.CeoDashboardResponse;
import com.wms.dto.response.InventoryValuationResponse;
import com.wms.service.user_context.CurrentUserService;
import com.wms.service.reporting_alerting.ReportService;
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
    @PreAuthorize("hasRole('CEO')")
    @Operation(summary = "Xem 5 chỉ số KPI chiến lược trên CEO Dashboard")
    public CeoDashboardResponse getCeoDashboard() {
        Long currentUserId = currentUserService.getRequiredCurrentUser().getId();
        return reportService.getCeoDashboard(currentUserId);
    }

    @GetMapping("/reports/inventory-valuation")
    @PreAuthorize("hasAnyRole('CEO', 'WAREHOUSE_MANAGER', 'ACCOUNTANT_MANAGER')")
    @Operation(summary = "Xem báo cáo Giá trị tồn kho cuối kỳ")
    public InventoryValuationResponse getInventoryValuation(
            @RequestParam(required = false) Long warehouseId) {
        Long currentUserId = currentUserService.getRequiredCurrentUser().getId();
        return reportService.getInventoryValuation(warehouseId, currentUserId);
    }

}
