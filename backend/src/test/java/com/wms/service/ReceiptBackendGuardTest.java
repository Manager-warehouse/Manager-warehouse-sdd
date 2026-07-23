package com.wms.service;


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
import com.wms.service.user_configuration.*;
import com.wms.service.user_configuration.impl.*;
import com.wms.service.audit_trail.*;
import com.wms.service.access_control.*;
import com.wms.service.dealer_management.*;
import com.wms.service.dealer_management.impl.*;
import com.wms.service.billing_payment.*;
import com.wms.service.billing_payment.impl.*;
import com.wms.service.stock_receiving.*;
import com.wms.service.stock_control.*;
import com.wms.service.stock_control.impl.*;
import com.wms.service.notification_delivery.*;
import com.wms.service.notification_delivery.impl.*;
import com.wms.service.order_fulfillment.*;
import com.wms.service.order_fulfillment.impl.*;
import com.wms.service.price_management.*;
import com.wms.service.price_management.impl.*;
import com.wms.service.reporting_alerting.*;
import com.wms.service.reporting_alerting.impl.*;
import com.wms.service.return_disposal.*;
import com.wms.service.stock_counting.*;
import com.wms.service.fleet_management.*;
import com.wms.service.fleet_management.impl.*;
import com.wms.service.warehouse_location.*;
import com.wms.service.warehouse_location.impl.*;

import com.wms.dto.request.ReceiptDecisionRequest;
import com.wms.dto.request.ReceiptRtvConfirmRequest;
import com.wms.dto.request.ReceiptRtvCreateRequest;
import com.wms.entity.stock_receiving.Receipt;
import com.wms.entity.access_control.User;
import com.wms.entity.warehouse_location.Warehouse;
import com.wms.enums.stock_receiving.ReceiptStatus;
import com.wms.enums.access_control.UserRole;
import com.wms.exception.BusinessRuleViolationException;
import com.wms.exception.ForbiddenReceiptWarehouseException;
import com.wms.repository.AdjustmentRepository;
import com.wms.repository.BatchRepository;
import com.wms.repository.DebitNoteRepository;
import com.wms.repository.InventoryRepository;
import com.wms.repository.ReceiptItemRepository;
import com.wms.repository.ReceiptRepository;
import com.wms.repository.UserWarehouseAssignmentRepository;
import com.wms.repository.WarehouseLocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReceiptBackendGuardTest {

    @Mock
    private ReceiptRepository receiptRepository;
    @Mock
    private ReceiptItemRepository receiptItemRepository;
    @Mock
    private BatchRepository batchRepository;
    @Mock
    private AdjustmentRepository adjustmentRepository;
    @Mock
    private DebitNoteRepository debitNoteRepository;
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private WarehouseLocationRepository warehouseLocationRepository;
    @Mock
    private UserWarehouseAssignmentRepository userWarehouseAssignmentRepository;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private com.wms.repository.QuarantineRecordRepository quarantineRecordRepository;
    @Mock
    private com.wms.repository.PriceHistoryRepository priceHistoryRepository;
    @Mock
    private AccountingPeriodService accountingPeriodService;

    private ReceiptValidationService receiptValidationService;
    private ReceiptApprovalService approvalService;
    private QuarantineRtvService rtvService;

    private Receipt qcFailedReceipt;
    private User storekeeper;
    private User manager;

    @BeforeEach
    void setUp() {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(10L);

        storekeeper = new User();
        storekeeper.setId(6L);
        storekeeper.setRole(UserRole.STOREKEEPER);

        manager = new User();
        manager.setId(5L);
        manager.setRole(UserRole.WAREHOUSE_MANAGER);

        qcFailedReceipt = receipt(2L, "RCV-QC-FAILED", ReceiptStatus.QC_FAILED, warehouse, 4);

        receiptValidationService = new ReceiptValidationService(receiptRepository, userWarehouseAssignmentRepository);
        approvalService = new ReceiptApprovalService(
                receiptRepository,
                receiptItemRepository,
                batchRepository,
                inventoryRepository,
                warehouseLocationRepository,
                receiptValidationService,
                auditLogService);
        rtvService = new QuarantineRtvService(
                receiptRepository,
                receiptItemRepository,
                adjustmentRepository,
                debitNoteRepository,
                inventoryRepository,
                receiptValidationService,
                auditLogService,
                quarantineRecordRepository,
                priceHistoryRepository,
                accountingPeriodService);
    }

    @Test
    void approve_withStorekeeperRole_throwsForbidden() {
        ReceiptDecisionRequest request = new ReceiptDecisionRequest();
        request.setExpectedVersion(3);

        assertThrows(ForbiddenReceiptWarehouseException.class,
                () -> approvalService.approveReceipt(1L, request, storekeeper));

        verify(receiptRepository, never()).save(any());
    }

    @Test
    void createRtv_staleReceiptVersion_throwsVersionConflictBeforeCreatingDocuments() {
        ReceiptRtvCreateRequest request = new ReceiptRtvCreateRequest();
        request.setExpectedVersion(99);
        request.setReason("Hàng lỗi");

        when(receiptRepository.findById(2L)).thenReturn(Optional.of(qcFailedReceipt));
        when(receiptRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(qcFailedReceipt));
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(5L)).thenReturn(List.of(10L));

        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
                () -> rtvService.createRtv(2L, request, manager));

        assertTrue(ex.getMessage().contains("INVENTORY_VERSION_CONFLICT"));
        verify(adjustmentRepository, never()).save(any());
        verify(debitNoteRepository, never()).save(any());
    }

    @Test
    void confirmRtv_staleReceiptVersion_throwsVersionConflictBeforeInventoryDeduction() {
        ReceiptRtvConfirmRequest request = new ReceiptRtvConfirmRequest();
        request.setExpectedVersion(99);
        request.setReturnedQty(java.math.BigDecimal.TEN);

        when(receiptRepository.findById(2L)).thenReturn(Optional.of(qcFailedReceipt));
        when(receiptRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(qcFailedReceipt));
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(6L)).thenReturn(List.of(10L));

        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
                () -> rtvService.confirmRtv(2L, request, storekeeper));

        assertTrue(ex.getMessage().contains("INVENTORY_VERSION_CONFLICT"));
        verify(inventoryRepository, never()).save(any());
        verify(adjustmentRepository, never()).save(any());
    }

    private Receipt receipt(Long id, String number, ReceiptStatus status, Warehouse warehouse, Integer version) {
        Receipt receipt = new Receipt();
        receipt.setId(id);
        receipt.setReceiptNumber(number);
        receipt.setStatus(status);
        receipt.setWarehouse(warehouse);
        receipt.setDocumentDate(LocalDate.now());
        receipt.setVersion(version);
        receipt.setCreatedAt(OffsetDateTime.now());
        receipt.setUpdatedAt(OffsetDateTime.now());
        return receipt;
    }
}
