package com.wms.service.impl;

import com.wms.dto.response.CeoDashboardResponse;
import com.wms.dto.response.InventoryValuationResponse;
import com.wms.dto.response.ProductivityReportResponse;
import com.wms.entity.*;
import com.wms.enums.AuditAction;
import com.wms.enums.DeliveryOrderStatus;
import com.wms.enums.InvoiceStatus;
import com.wms.enums.TripStatus;
import com.wms.enums.UserRole;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.*;
import com.wms.service.ReportService;
import com.wms.util.AuditLogUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final UserRepository userRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryRepository inventoryRepository;
    private final InvoiceRepository invoiceRepository;
    private final DeliveryOrderItemRepository deliveryOrderItemRepository;
    private final TripRepository tripRepository;
    private final UserWarehouseAssignmentRepository userWarehouseAssignmentRepository;
    private final AuditLogRepository auditLogRepository;
    private final OutboundQcRecordRepository outboundQcRecordRepository;
    private final DeliveryOrderItemAllocationRepository deliveryOrderItemAllocationRepository;
    private final ReceiptItemRepository receiptItemRepository;
    private final DeliveryRepository deliveryRepository;

    @Override
    @Transactional
    public CeoDashboardResponse getCeoDashboard(Long currentUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentUserId));

        if (user.getRole() != UserRole.CEO && user.getRole() != UserRole.ACCOUNTANT_MANAGER && user.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("ACCESS_DENIED");
        }

        // Ghi Audit Log ngoại lệ
        recordReportViewAuditLog(user, "CEO_DASHBOARD", "Truy cập CEO Dashboard quản trị", null, Map.of());

        OffsetDateTime now = OffsetDateTime.now();
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth = today.with(TemporalAdjusters.lastDayOfMonth());

        OffsetDateTime startOfMonthDateTime = startOfMonth.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime endOfMonthDateTime = endOfMonth.atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC);

        // 1. Tính tổng giá trị tồn kho
        List<Inventory> activeInventories = inventoryRepository.findAll().stream()
                .filter(i -> i.getLocation() != null && !i.getLocation().getIsQuarantine() && i.getTotalQty().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());

        BigDecimal totalInventoryValue = activeInventories.stream()
                .map(i -> i.getTotalQty().multiply(i.getCostPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. Tính P&L (Doanh thu, Giá vốn, Chi phí vận hành)
        // Revenue = Tổng totalAmount của các hóa đơn phát hành trong tháng
        List<Invoice> invoicesInMonth = invoiceRepository.findByIssueDateBetween(startOfMonth, endOfMonth);
        BigDecimal revenue = invoicesInMonth.stream()
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // COGS = Tổng unitCost * qcPassQty cho các DO đã hoàn thành trong tháng
        List<DeliveryOrderItem> completedDoItems = deliveryOrderItemRepository.findCompletedItemsInPeriod(startOfMonthDateTime, endOfMonthDateTime);
        BigDecimal cogs = completedDoItems.stream()
                .map(item -> {
                    BigDecimal unitCost = item.getUnitCost() != null ? item.getUnitCost() : BigDecimal.ZERO;
                    BigDecimal qcPass = item.getQcPassQty() != null ? item.getQcPassQty() : BigDecimal.ZERO;
                    return unitCost.multiply(qcPass);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Operating Costs = Ước lượng 500.000đ mỗi chuyến xe hoàn thành trong tháng
        List<Trip> completedTripsInMonth = tripRepository.findByStatusAndCompletedAtBetween(TripStatus.COMPLETED, startOfMonthDateTime, endOfMonthDateTime);
        int completedTripsCount = completedTripsInMonth.size();
        BigDecimal operatingCosts = BigDecimal.valueOf(completedTripsCount).multiply(BigDecimal.valueOf(500000));

        BigDecimal netProfit = revenue.subtract(cogs).subtract(operatingCosts);

        // 3. QC Failure Rate trong tháng
        // Lấy số liệu outbound QC
        List<OutboundQcRecord> outboundQcRecords = outboundQcRecordRepository.findAll().stream()
                .filter(r -> r.getCreatedAt().isAfter(startOfMonthDateTime) && r.getCreatedAt().isBefore(endOfMonthDateTime))
                .collect(Collectors.toList());

        BigDecimal outboundFail = outboundQcRecords.stream()
                .map(r -> r.getQcFailQty() != null ? r.getQcFailQty() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal outboundPass = outboundQcRecords.stream()
                .map(r -> r.getQcPassQty() != null ? r.getQcPassQty() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Lấy số liệu inbound QC
        List<ReceiptItem> inboundReceiptItems = receiptItemRepository.findAll().stream()
                .filter(i -> i.getReceipt() != null && i.getReceipt().getUpdatedAt() != null &&
                        i.getReceipt().getUpdatedAt().isAfter(startOfMonthDateTime) && i.getReceipt().getUpdatedAt().isBefore(endOfMonthDateTime))
                .collect(Collectors.toList());

        BigDecimal inboundFail = inboundReceiptItems.stream()
                .map(i -> i.getSampleFailedQty() != null ? BigDecimal.valueOf(i.getSampleFailedQty()) : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal inboundPass = inboundReceiptItems.stream()
                .map(i -> i.getSamplePassedQty() != null ? BigDecimal.valueOf(i.getSamplePassedQty()) : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalFail = outboundFail.add(inboundFail);
        BigDecimal totalQC = totalFail.add(outboundPass).add(inboundPass);
        BigDecimal qcFailureRate = BigDecimal.ZERO;
        if (totalQC.compareTo(BigDecimal.ZERO) > 0) {
            qcFailureRate = totalFail.divide(totalQC, 4, RoundingMode.HALF_UP);
        }

        // 4. OTD Rate trong tháng
        List<Delivery> deliveriesInMonth = deliveryRepository.findAll().stream()
                .filter(d -> d.getDeliveredAt() != null && d.getDeliveredAt().isAfter(startOfMonthDateTime) && d.getDeliveredAt().isBefore(endOfMonthDateTime))
                .collect(Collectors.toList());

        int totalDeliveries = deliveriesInMonth.size();
        long onTimeDeliveries = deliveriesInMonth.stream()
                .filter(d -> {
                    if (d.getDeliveryOrder() == null || d.getDeliveryOrder().getExpectedDeliveryDate() == null) {
                        return true;
                    }
                    LocalDate expected = d.getDeliveryOrder().getExpectedDeliveryDate();
                    LocalDate actual = d.getDeliveredAt().toLocalDate();
                    return actual.isBefore(expected) || actual.isEqual(expected);
                })
                .count();

        BigDecimal onTimeDeliveryRate = BigDecimal.ONE;
        if (totalDeliveries > 0) {
            onTimeDeliveryRate = BigDecimal.valueOf(onTimeDeliveries).divide(BigDecimal.valueOf(totalDeliveries), 4, RoundingMode.HALF_UP);
        }

        // 5. Top 5 đại lý nợ quá hạn
        List<Invoice> overdueInvoices = invoiceRepository.findByStatusNotAndDueDateBefore(InvoiceStatus.PAID, today);
        Map<Dealer, BigDecimal> dealerDebtMap = new HashMap<>();
        Map<Dealer, Integer> dealerMaxOverdueDays = new HashMap<>();

        for (Invoice invoice : overdueInvoices) {
            Dealer dealer = invoice.getDealer();
            BigDecimal overdueAmt = invoice.getTotalAmount(); // Giả định UNPAID/PARTIALLY_PAID tính totalAmount của hóa đơn đó
            dealerDebtMap.put(dealer, dealerDebtMap.getOrDefault(dealer, BigDecimal.ZERO).add(overdueAmt));

            int overdueDays = (int) (today.toEpochDay() - invoice.getDueDate().toEpochDay());
            if (overdueDays > dealerMaxOverdueDays.getOrDefault(dealer, 0)) {
                dealerMaxOverdueDays.put(dealer, overdueDays);
            }
        }

        List<CeoDashboardResponse.DebtorInfo> topDebtors = dealerDebtMap.entrySet().stream()
                .map(entry -> CeoDashboardResponse.DebtorInfo.builder()
                        .dealerId(entry.getKey().getId())
                        .dealerName(entry.getKey().getName())
                        .overdueAmount(entry.getValue())
                        .maxOverdueDays(dealerMaxOverdueDays.getOrDefault(entry.getKey(), 0))
                        .build())
                .sorted(Comparator.comparing(CeoDashboardResponse.DebtorInfo::getOverdueAmount).reversed())
                .limit(5)
                .collect(Collectors.toList());

        return CeoDashboardResponse.builder()
                .asOfTime(now)
                .kpis(CeoDashboardResponse.Kpis.builder()
                        .totalInventoryValue(totalInventoryValue)
                        .pAndL(CeoDashboardResponse.PAndL.builder()
                                .period(DateTimeFormatter.ofPattern("yyyy-MM").format(today))
                                .revenue(revenue)
                                .cogs(cogs)
                                .operatingCosts(operatingCosts)
                                .netProfit(netProfit)
                                .build())
                        .qcFailureRate(qcFailureRate)
                        .onTimeDeliveryRate(onTimeDeliveryRate)
                        .build())
                .topDebtors(topDebtors)
                .build();
    }

    @Override
    @Transactional
    public InventoryValuationResponse getInventoryValuation(Long warehouseId, Long currentUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentUserId));

        if (user.getRole() != UserRole.ACCOUNTANT_MANAGER && user.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("ACCESS_DENIED");
        }

        // Ghi Audit Log ngoại lệ
        recordReportViewAuditLog(user, "INVENTORY_VALUATION", "Xem báo cáo Giá trị tồn kho", warehouseId, Map.of());

        List<Inventory> inventories = inventoryRepository.findAll().stream()
                .filter(i -> i.getLocation() != null && !i.getLocation().getIsQuarantine() && i.getTotalQty().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());

        if (warehouseId != null) {
            inventories = inventories.stream()
                    .filter(i -> i.getWarehouse().getId().equals(warehouseId))
                    .collect(Collectors.toList());
        }

        List<InventoryValuationResponse.ValuationRecord> records = new ArrayList<>();
        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalValuation = BigDecimal.ZERO;

        for (Inventory i : inventories) {
            BigDecimal qty = i.getTotalQty();
            BigDecimal cost = i.getCostPrice();
            BigDecimal valuation = qty.multiply(cost);

            totalQty = totalQty.add(qty);
            totalValuation = totalValuation.add(valuation);

            records.add(InventoryValuationResponse.ValuationRecord.builder()
                    .warehouseId(i.getWarehouse().getId())
                    .warehouseName(i.getWarehouse().getName())
                    .productId(i.getProduct().getId())
                    .productSku(i.getProduct().getSku())
                    .productName(i.getProduct().getName())
                    .batchNumber(i.getBatch() != null ? i.getBatch().getBatchNumber() : "N/A")
                    .totalQty(qty)
                    .unitCost(cost)
                    .valuationAmount(valuation)
                    .build());
        }

        return InventoryValuationResponse.builder()
                .generatedAt(OffsetDateTime.now())
                .filters(InventoryValuationResponse.Filters.builder().warehouseId(warehouseId).build())
                .summary(InventoryValuationResponse.Summary.builder()
                        .totalItems(records.size())
                        .totalQty(totalQty)
                        .totalValuation(totalValuation)
                        .build())
                .records(records)
                .build();
    }

    @Override
    @Transactional
    public ProductivityReportResponse getProductivityReport(Long warehouseId, LocalDate startDate, LocalDate endDate, Long currentUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentUserId));

        if (user.getRole() != UserRole.WAREHOUSE_MANAGER && user.getRole() != UserRole.ACCOUNTANT_MANAGER && user.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("ACCESS_DENIED");
        }

        if (user.getRole() == UserRole.WAREHOUSE_MANAGER) {
            List<Long> assignedWarehouseIds = userWarehouseAssignmentRepository.findWarehouseIdsByUserId(currentUserId);
            if (!assignedWarehouseIds.contains(warehouseId)) {
                throw new IllegalArgumentException("WAREHOUSE_SCOPE_FORBIDDEN");
            }
        }

        // Ghi Audit Log ngoại lệ
        recordReportViewAuditLog(user, "PRODUCTIVITY", "Xem báo cáo năng suất nhân viên", warehouseId,
                Map.of("start_date", startDate.toString(), "end_date", endDate.toString()));

        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + warehouseId));

        OffsetDateTime start = startDate.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime end = endDate.atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC);

        // 1. Staff Productivity (Picking)
        List<OutboundQcRecord> qcRecords = outboundQcRecordRepository.findByWarehouseIdAndCreatedAtBetween(warehouseId, start, end);
        Map<User, Integer> staffPickCount = new HashMap<>();
        Map<User, BigDecimal> staffPickQty = new HashMap<>();

        for (OutboundQcRecord r : qcRecords) {
            User staff = r.getCreatedBy();
            if (staff != null && staff.getRole() == UserRole.WAREHOUSE_STAFF) {
                staffPickCount.put(staff, staffPickCount.getOrDefault(staff, 0) + 1);
                BigDecimal picked = r.getPickedQty() != null ? r.getPickedQty() : BigDecimal.ZERO;
                staffPickQty.put(staff, staffPickQty.getOrDefault(staff, BigDecimal.ZERO).add(picked));
            }
        }

        List<ProductivityReportResponse.StaffProductivity> staffList = staffPickCount.entrySet().stream()
                .map(entry -> ProductivityReportResponse.StaffProductivity.builder()
                        .employeeCode(entry.getKey().getCode())
                        .fullName(entry.getKey().getFullName())
                        .role(entry.getKey().getRole().name())
                        .pickingRunsCount(entry.getValue())
                        .totalPickedQty(staffPickQty.getOrDefault(entry.getKey(), BigDecimal.ZERO))
                        .build())
                .collect(Collectors.toList());

        // 2. Storekeeper Productivity (Plans and QC Check)
        List<DeliveryOrderItemAllocation> allocations = deliveryOrderItemAllocationRepository.findByWarehouseIdAndCreatedAtBetween(warehouseId, start, end);
        Map<User, Integer> skPlanCount = new HashMap<>();
        Map<User, BigDecimal> skQcQty = new HashMap<>();

        for (DeliveryOrderItemAllocation a : allocations) {
            User sk = a.getCreatedBy();
            if (sk != null && sk.getRole() == UserRole.STOREKEEPER) {
                skPlanCount.put(sk, skPlanCount.getOrDefault(sk, 0) + 1);
            }
        }

        for (OutboundQcRecord r : qcRecords) {
            // Outbound QC kiểm tra bởi storekeeper/inspector
            User sk = r.getCreatedBy();
            if (sk != null && sk.getRole() == UserRole.STOREKEEPER) {
                BigDecimal checked = (r.getQcPassQty() != null ? r.getQcPassQty() : BigDecimal.ZERO)
                        .add(r.getQcFailQty() != null ? r.getQcFailQty() : BigDecimal.ZERO);
                skQcQty.put(sk, skQcQty.getOrDefault(sk, BigDecimal.ZERO).add(checked));
            }
        }

        // Gom danh sách storekeepers
        Set<User> allSks = new HashSet<>();
        allSks.addAll(skPlanCount.keySet());
        allSks.addAll(skQcQty.keySet());

        List<ProductivityReportResponse.StorekeeperProductivity> skList = allSks.stream()
                .map(sk -> ProductivityReportResponse.StorekeeperProductivity.builder()
                        .employeeCode(sk.getCode())
                        .fullName(sk.getFullName())
                        .role(sk.getRole().name())
                        .pickingPlansCreated(skPlanCount.getOrDefault(sk, 0))
                        .totalQcCheckedQty(skQcQty.getOrDefault(sk, BigDecimal.ZERO))
                        .build())
                .collect(Collectors.toList());

        // 3. Driver Productivity (Trips completed and successful deliveries)
        List<Trip> completedTrips = tripRepository.findByWarehouseIdAndStatusAndCompletedAtBetween(warehouseId, TripStatus.COMPLETED, start, end);
        Map<User, Integer> driverTripCount = new HashMap<>();
        Map<User, Integer> driverDeliveryCount = new HashMap<>();

        for (Trip trip : completedTrips) {
            Driver driver = trip.getDriver();
            if (driver != null && driver.getUser() != null) {
                User dUser = driver.getUser();
                driverTripCount.put(dUser, driverTripCount.getOrDefault(dUser, 0) + 1);
            }
        }

        List<Delivery> successfulDeliveries = deliveryRepository.findAll().stream()
                .filter(d -> d.getTrip() != null && d.getTrip().getWarehouse().getId().equals(warehouseId)
                        && d.getStatus().equals("DELIVERED") && d.getDeliveredAt() != null
                        && d.getDeliveredAt().isAfter(start) && d.getDeliveredAt().isBefore(end))
                .collect(Collectors.toList());

        for (Delivery d : successfulDeliveries) {
            Driver driver = d.getDriver();
            if (driver != null && driver.getUser() != null) {
                User dUser = driver.getUser();
                driverDeliveryCount.put(dUser, driverDeliveryCount.getOrDefault(dUser, 0) + 1);
            }
        }

        Set<User> allDrivers = new HashSet<>();
        allDrivers.addAll(driverTripCount.keySet());
        allDrivers.addAll(driverDeliveryCount.keySet());

        List<ProductivityReportResponse.DriverProductivity> driverList = allDrivers.stream()
                .map(d -> ProductivityReportResponse.DriverProductivity.builder()
                        .employeeCode(d.getCode())
                        .fullName(d.getFullName())
                        .role("DRIVER")
                        .tripsCompleted(driverTripCount.getOrDefault(d, 0))
                        .successfulDeliveries(driverDeliveryCount.getOrDefault(d, 0))
                        .build())
                .collect(Collectors.toList());

        return ProductivityReportResponse.builder()
                .warehouseId(warehouseId)
                .warehouseName(warehouse.getName())
                .startDate(startDate.toString())
                .endDate(endDate.toString())
                .staffProductivity(staffList)
                .storekeeperProductivity(skList)
                .driverProductivity(driverList)
                .build();
    }

    @Override
    @Transactional
    public byte[] exportProductivityReportExcel(Long warehouseId, LocalDate startDate, LocalDate endDate, Long currentUserId) {
        ProductivityReportResponse data = getProductivityReport(warehouseId, startDate, endDate, currentUserId);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            
            // Style chung cho Header
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // Tab 1: Staff Productivity
            Sheet sheet1 = workbook.createSheet("Staff_Productivity");
            Row h1 = sheet1.createRow(0);
            String[] cols1 = {"Mã nhân viên", "Tên nhân viên", "Vai trò", "Số lượt lấy hàng", "Tổng số lượng sản phẩm lấy"};
            for (int i = 0; i < cols1.length; i++) {
                Cell cell = h1.createCell(i);
                cell.setCellValue(cols1[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx1 = 1;
            for (ProductivityReportResponse.StaffProductivity p : data.getStaffProductivity()) {
                Row row = sheet1.createRow(rowIdx1++);
                row.createCell(0).setCellValue(p.getEmployeeCode());
                row.createCell(1).setCellValue(p.getFullName());
                row.createCell(2).setCellValue(p.getRole());
                row.createCell(3).setCellValue(p.getPickingRunsCount());
                row.createCell(4).setCellValue(p.getTotalPickedQty().doubleValue());
            }
            for (int i = 0; i < cols1.length; i++) sheet1.autoSizeColumn(i);

            // Tab 2: Storekeeper Productivity
            Sheet sheet2 = workbook.createSheet("Storekeeper_Productivity");
            Row h2 = sheet2.createRow(0);
            String[] cols2 = {"Mã thủ kho", "Tên thủ kho", "Vai trò", "Số picking plans đã lập", "Tổng sản lượng QC checked"};
            for (int i = 0; i < cols2.length; i++) {
                Cell cell = h2.createCell(i);
                cell.setCellValue(cols2[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx2 = 1;
            for (ProductivityReportResponse.StorekeeperProductivity p : data.getStorekeeperProductivity()) {
                Row row = sheet2.createRow(rowIdx2++);
                row.createCell(0).setCellValue(p.getEmployeeCode());
                row.createCell(1).setCellValue(p.getFullName());
                row.createCell(2).setCellValue(p.getRole());
                row.createCell(3).setCellValue(p.getPickingPlansCreated());
                row.createCell(4).setCellValue(p.getTotalQcCheckedQty().doubleValue());
            }
            for (int i = 0; i < cols2.length; i++) sheet2.autoSizeColumn(i);

            // Tab 3: Driver Productivity
            Sheet sheet3 = workbook.createSheet("Driver_Productivity");
            Row h3 = sheet3.createRow(0);
            String[] cols3 = {"Mã tài xế", "Tên tài xế", "Vai trò", "Số chuyến xe hoàn thành", "Số đơn giao thành công"};
            for (int i = 0; i < cols3.length; i++) {
                Cell cell = h3.createCell(i);
                cell.setCellValue(cols3[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx3 = 1;
            for (ProductivityReportResponse.DriverProductivity p : data.getDriverProductivity()) {
                Row row = sheet3.createRow(rowIdx3++);
                row.createCell(0).setCellValue(p.getEmployeeCode());
                row.createCell(1).setCellValue(p.getFullName());
                row.createCell(2).setCellValue(p.getRole());
                row.createCell(3).setCellValue(p.getTripsCompleted());
                row.createCell(4).setCellValue(p.getSuccessfulDeliveries());
            }
            for (int i = 0; i < cols3.length; i++) sheet3.autoSizeColumn(i);

            workbook.write(bos);
            return bos.toByteArray();

        } catch (IOException e) {
            log.error("Failed to generate Excel file for productivity report", e);
            throw new RuntimeException("EXPORT_FAILED");
        }
    }

    private void recordReportViewAuditLog(User user, String reportType, String description, Long warehouseId, Map<String, Object> filters) {
        Warehouse warehouse = null;
        if (warehouseId != null) {
            warehouse = warehouseRepository.findById(warehouseId).orElse(null);
        }

        Map<String, Object> newValues = new HashMap<>();
        newValues.put("report_type", reportType);
        newValues.put("filters", filters);

        AuditLog auditLog = AuditLog.builder()
                .actor(user)
                .actorRole(user.getRole() != null ? user.getRole().name() : "ADMIN")
                .action(AuditAction.VIEW_REPORT)
                .entityType("REPORT")
                .entityId(0L)
                .description(description)
                .warehouse(warehouse)
                .oldValue(null)
                .newValue(AuditLogUtil.toJson(newValues))
                .timestamp(OffsetDateTime.now())
                .build();

        auditLogRepository.save(auditLog);
    }
}
