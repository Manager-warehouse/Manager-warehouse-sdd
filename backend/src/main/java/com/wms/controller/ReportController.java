package com.wms.controller;

import com.wms.dto.response.CeoDashboardResponse;
import com.wms.dto.response.InventoryValuationResponse;
import com.wms.dto.response.ProductivityReportResponse;
import com.wms.service.CurrentUserService;
import com.wms.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Reports & Dashboards", description = "Quản lý báo cáo & dashboard quản trị (Spec 010)")
public class ReportController {

    private final ReportService reportService;
    private final CurrentUserService currentUserService;

    @GetMapping("/dashboard/ceo")
    @PreAuthorize("hasAnyRole('CEO', 'ACCOUNTANT_MANAGER', 'ADMIN')")
    @Operation(summary = "Xem 5 chỉ số KPI chiến lược trên CEO Dashboard")
    public CeoDashboardResponse getCeoDashboard() {
        Long currentUserId = currentUserService.getRequiredCurrentUser().getId();
        return reportService.getCeoDashboard(currentUserId);
    }

    @GetMapping("/reports/inventory-valuation")
    @PreAuthorize("hasAnyRole('ACCOUNTANT_MANAGER', 'ADMIN')")
    @Operation(summary = "Xem báo cáo Giá trị tồn kho cuối kỳ")
    public InventoryValuationResponse getInventoryValuation(
            @RequestParam(required = false) Long warehouseId) {
        Long currentUserId = currentUserService.getRequiredCurrentUser().getId();
        return reportService.getInventoryValuation(warehouseId, currentUserId);
    }

    @GetMapping("/reports/productivity")
    @PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER', 'ACCOUNTANT_MANAGER', 'ADMIN')")
    @Operation(summary = "Xem báo cáo năng suất nhân viên kho (JSON)")
    public ProductivityReportResponse getProductivityReport(
            @RequestParam Long warehouseId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Long currentUserId = currentUserService.getRequiredCurrentUser().getId();
        return reportService.getProductivityReport(warehouseId, startDate, endDate, currentUserId);
    }

    @GetMapping("/reports/productivity/export")
    @PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER', 'ACCOUNTANT_MANAGER', 'ADMIN')")
    @Operation(summary = "Xuất file Excel báo cáo năng suất nhân viên kho")
    public ResponseEntity<byte[]> exportProductivityReport(
            @RequestParam Long warehouseId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Long currentUserId = currentUserService.getRequiredCurrentUser().getId();
        byte[] excelBytes = reportService.exportProductivityReportExcel(warehouseId, startDate, endDate, currentUserId);

        
        String filename = String.format("Productivity_Report_WH%d_%s_%s.xlsx", warehouseId, startDate, endDate);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelBytes);
    }
}
