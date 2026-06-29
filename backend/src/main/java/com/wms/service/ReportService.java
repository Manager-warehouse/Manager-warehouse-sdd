package com.wms.service;

import com.wms.dto.response.CeoDashboardResponse;
import com.wms.dto.response.InventoryValuationResponse;
import com.wms.dto.response.ProductivityReportResponse;

import java.time.LocalDate;

public interface ReportService {
    CeoDashboardResponse getCeoDashboard(Long currentUserId);
    InventoryValuationResponse getInventoryValuation(Long warehouseId, Long currentUserId);
    ProductivityReportResponse getProductivityReport(Long warehouseId, LocalDate startDate, LocalDate endDate, Long currentUserId);
    byte[] exportProductivityReportExcel(Long warehouseId, LocalDate startDate, LocalDate endDate, Long currentUserId);
}
