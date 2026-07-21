package com.wms.service.impl;

import com.wms.dto.response.CeoDashboardResponse;
import com.wms.dto.response.InventoryValuationResponse;
import com.wms.entity.*;
import com.wms.enums.DeliveryOrderStatus;
import com.wms.enums.InvoiceStatus;
import com.wms.enums.TripStatus;
import com.wms.enums.UserRole;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.*;
import com.wms.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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
    private final InventoryRepository inventoryRepository;
    private final InvoiceRepository invoiceRepository;
    private final DeliveryOrderItemRepository deliveryOrderItemRepository;
    private final TripRepository tripRepository;
    private final OutboundQcRecordRepository outboundQcRecordRepository;
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

        if (user.getRole() != UserRole.ACCOUNTANT_MANAGER && user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.CEO && user.getRole() != UserRole.WAREHOUSE_MANAGER) {
            throw new IllegalArgumentException("ACCESS_DENIED");
        }

        // Ghi Audit Log ngoại lệ
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

}
