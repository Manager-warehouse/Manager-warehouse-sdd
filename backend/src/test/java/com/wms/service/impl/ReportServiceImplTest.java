package com.wms.service.impl;

import com.wms.dto.response.CeoDashboardResponse;
import com.wms.dto.response.InventoryValuationResponse;
import com.wms.dto.response.ProductivityReportResponse;
import com.wms.entity.User;
import com.wms.entity.Warehouse;
import com.wms.enums.UserRole;
import com.wms.enums.WarehouseType;
import com.wms.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock WarehouseRepository warehouseRepository;
    @Mock InventoryRepository inventoryRepository;
    @Mock InvoiceRepository invoiceRepository;
    @Mock DeliveryOrderItemRepository deliveryOrderItemRepository;
    @Mock TripRepository tripRepository;
    @Mock UserWarehouseAssignmentRepository userWarehouseAssignmentRepository;
    @Mock AuditLogRepository auditLogRepository;
    @Mock OutboundQcRecordRepository outboundQcRecordRepository;
    @Mock DeliveryOrderItemAllocationRepository deliveryOrderItemAllocationRepository;
    @Mock ReceiptItemRepository receiptItemRepository;
    @Mock DeliveryRepository deliveryRepository;

    ReportServiceImpl service;

    User ceo;
    User accountantManager;
    User warehouseManager;
    Warehouse warehouse;

    @BeforeEach
    void setUp() {
        service = new ReportServiceImpl(
                userRepository, warehouseRepository, inventoryRepository, invoiceRepository,
                deliveryOrderItemRepository, tripRepository, userWarehouseAssignmentRepository,
                auditLogRepository, outboundQcRecordRepository, deliveryOrderItemAllocationRepository,
                receiptItemRepository, deliveryRepository
        );

        ceo = new User();
        ceo.setId(1L);
        ceo.setRole(UserRole.CEO);

        accountantManager = new User();
        accountantManager.setId(2L);
        accountantManager.setRole(UserRole.ACCOUNTANT_MANAGER);

        warehouseManager = new User();
        warehouseManager.setId(3L);
        warehouseManager.setRole(UserRole.WAREHOUSE_MANAGER);

        warehouse = new Warehouse();
        warehouse.setId(10L);
        warehouse.setName("Kho Hải Phòng");
        warehouse.setType(WarehouseType.PHYSICAL);
    }

    @Test
    void getCeoDashboard_unauthorizedRole_throwsException() {
        User staff = new User();
        staff.setId(4L);
        staff.setRole(UserRole.WAREHOUSE_STAFF);

        when(userRepository.findById(4L)).thenReturn(Optional.of(staff));

        assertThatThrownBy(() -> service.getCeoDashboard(4L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ACCESS_DENIED");
    }

    @Test
    void getCeoDashboard_validCeo_returnsResponse() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(ceo));
        when(inventoryRepository.findAll()).thenReturn(List.of());
        when(invoiceRepository.findByIssueDateBetween(any(), any())).thenReturn(List.of());
        when(deliveryOrderItemRepository.findCompletedItemsInPeriod(any(), any())).thenReturn(List.of());
        when(tripRepository.findByStatusAndCompletedAtBetween(any(), any(), any())).thenReturn(List.of());
        when(outboundQcRecordRepository.findAll()).thenReturn(List.of());
        when(receiptItemRepository.findAll()).thenReturn(List.of());
        when(deliveryRepository.findAll()).thenReturn(List.of());

        CeoDashboardResponse response = service.getCeoDashboard(1L);

        assertThat(response).isNotNull();
        assertThat(response.getKpis().getTotalInventoryValue()).isZero();
    }

    @Test
    void getInventoryValuation_validRole_returnsResponse() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(accountantManager));
        when(inventoryRepository.findAll()).thenReturn(List.of());

        InventoryValuationResponse response = service.getInventoryValuation(null, 2L);

        assertThat(response).isNotNull();
        assertThat(response.getSummary().getTotalValuation()).isZero();
    }

    @Test
    void getProductivityReport_validRole_returnsResponse() {
        when(userRepository.findById(3L)).thenReturn(Optional.of(warehouseManager));
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(10L));
        when(warehouseRepository.findById(10L)).thenReturn(Optional.of(warehouse));
        when(outboundQcRecordRepository.findByWarehouseIdAndCreatedAtBetween(any(), any(), any())).thenReturn(List.of());
        when(deliveryOrderItemAllocationRepository.findByWarehouseIdAndCreatedAtBetween(any(), any(), any())).thenReturn(List.of());
        when(tripRepository.findByWarehouseIdAndStatusAndCompletedAtBetween(any(), any(), any(), any())).thenReturn(List.of());
        when(deliveryRepository.findAll()).thenReturn(List.of());

        ProductivityReportResponse response = service.getProductivityReport(10L, LocalDate.now().minusDays(5), LocalDate.now(), 3L);

        assertThat(response).isNotNull();
        assertThat(response.getWarehouseName()).isEqualTo("Kho Hải Phòng");
    }

    @Test
    void exportProductivityReportExcel_returnsBytes() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(accountantManager));
        when(warehouseRepository.findById(10L)).thenReturn(Optional.of(warehouse));
        when(outboundQcRecordRepository.findByWarehouseIdAndCreatedAtBetween(any(), any(), any())).thenReturn(List.of());
        when(deliveryOrderItemAllocationRepository.findByWarehouseIdAndCreatedAtBetween(any(), any(), any())).thenReturn(List.of());
        when(tripRepository.findByWarehouseIdAndStatusAndCompletedAtBetween(any(), any(), any(), any())).thenReturn(List.of());
        when(deliveryRepository.findAll()).thenReturn(List.of());

        byte[] bytes = service.exportProductivityReportExcel(10L, LocalDate.now().minusDays(5), LocalDate.now(), 2L);

        assertThat(bytes).isNotEmpty();
    }

    @Test
    void getProductivityReport_invalidWarehouse_throwsException() {
        when(userRepository.findById(3L)).thenReturn(Optional.of(warehouseManager));
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(10L));

        assertThatThrownBy(() -> service.getProductivityReport(20L, LocalDate.now().minusDays(5), LocalDate.now(), 3L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("WAREHOUSE_SCOPE_FORBIDDEN");
    }
}

