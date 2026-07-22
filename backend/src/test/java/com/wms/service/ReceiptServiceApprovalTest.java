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
import com.wms.dto.response.ReceiptActionResponse;
import com.wms.exception.*;
import com.wms.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReceiptService.approveReceipt() — US-WMS-05.
 *
 * <p>
 * Covers: happy path, invalid status, duplicate decision, stale version,
 * forbidden warehouse.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class ReceiptServiceApprovalTest {

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

        private ReceiptValidationService receiptValidationService;
        private ReceiptApprovalService receiptService;

        private User manager;
        private Warehouse warehouse;
        private Receipt qcCompletedReceipt;
        private ReceiptItem receiptItem;
        private Product product;

        @BeforeEach
        void setUp() {
                warehouse = new Warehouse();
                warehouse.setId(10L);
                warehouse.setCode("WH-HP");

                manager = new User();
                manager.setId(5L);
                manager.setEmail("manager@wms.com");
                manager.setRole(UserRole.WAREHOUSE_MANAGER);

                product = new Product();
                product.setId(100L);

                qcCompletedReceipt = new Receipt();
                qcCompletedReceipt.setId(1L);
                qcCompletedReceipt.setReceiptNumber("RCV-2026-001");
                qcCompletedReceipt.setStatus(ReceiptStatus.QC_COMPLETED);
                qcCompletedReceipt.setWarehouse(warehouse);
                qcCompletedReceipt.setDocumentDate(LocalDate.of(2026, 6, 11));
                qcCompletedReceipt.setVersion(3);
                qcCompletedReceipt.setCreatedAt(OffsetDateTime.now());
                qcCompletedReceipt.setUpdatedAt(OffsetDateTime.now());

                Batch batch = new Batch();
                batch.setId(200L);
                batch.setBatchNumber("BCH-100-RCV-2026-001-2026-06-11");
                batch.setProduct(product);
                batch.setWarehouse(warehouse);
                batch.setReceivedDate(LocalDate.of(2026, 6, 11));

                receiptItem = new ReceiptItem();
                receiptItem.setId(10L);
                receiptItem.setProduct(product);
                receiptItem.setActualQty(100);
                receiptItem.setBatch(batch);

                receiptValidationService = new ReceiptValidationService(receiptRepository,
                                userWarehouseAssignmentRepository);
                receiptService = new ReceiptApprovalService(
                                receiptRepository,
                                receiptItemRepository,
                                batchRepository,
                                inventoryRepository,
                                warehouseLocationRepository,
                                receiptValidationService,
                                auditLogService);
        }

        // -----------------------------------------------------------------------
        // Happy path: approve QC_COMPLETED receipt
        // -----------------------------------------------------------------------

        @Test
        void approveReceipt_happyPath_statusBecomesApproved() {
                ReceiptDecisionRequest request = new ReceiptDecisionRequest();
                request.setExpectedVersion(3);

                when(receiptRepository.findById(1L)).thenReturn(Optional.of(qcCompletedReceipt));
                when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(5L)).thenReturn(List.of(10L));
                when(receiptItemRepository.findByReceiptId(1L)).thenReturn(List.of(receiptItem));
                when(batchRepository.findByProductWarehouseAndReceivedDate(100L, 10L, LocalDate.of(2026, 6, 11)))
                                .thenReturn(Optional.of(receiptItem.getBatch()));
                when(receiptItemRepository.save(any(ReceiptItem.class))).thenReturn(receiptItem);
                when(receiptRepository.save(any(Receipt.class))).thenAnswer(i -> i.getArgument(0));

                ReceiptActionResponse response = receiptService.approveReceipt(1L, request, manager);

                assertNotNull(response);
                assertEquals(ReceiptStatus.APPROVED, response.getStatus());
                assertEquals("RCV-2026-001", response.getReceiptNumber());

                verify(receiptRepository).save(argThat(r -> r.getStatus() == ReceiptStatus.APPROVED
                                && r.getApprovedBy().getId().equals(5L)
                                && r.getApprovedAt() != null));
                verify(auditLogService).log(eq(manager), eq(AuditAction.RECEIPT_APPROVE),
                                eq("RECEIPT"), eq(1L), eq("RCV-2026-001"), eq(10L), any(), any());
        }

        @Test
        void approveReceipt_happyPath_doesNotIncreaseInventory() {
                ReceiptDecisionRequest request = new ReceiptDecisionRequest();
                request.setExpectedVersion(3);

                when(receiptRepository.findById(1L)).thenReturn(Optional.of(qcCompletedReceipt));
                when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(5L)).thenReturn(List.of(10L));
                when(receiptItemRepository.findByReceiptId(1L)).thenReturn(List.of(receiptItem));
                when(batchRepository.findByProductWarehouseAndReceivedDate(any(), any(), any()))
                                .thenReturn(Optional.of(receiptItem.getBatch()));
                when(receiptItemRepository.save(any())).thenReturn(receiptItem);
                when(receiptRepository.save(any())).thenAnswer(i -> i.getArgument(0));

                receiptService.approveReceipt(1L, request, manager);

                // Approval must NOT touch inventory
                verify(inventoryRepository, never()).save(any());
                verify(inventoryRepository, never()).findByWarehouseProductBatchLocationForUpdate(any(), any(), any(),
                                any());
        }

        // -----------------------------------------------------------------------
        // Invalid status: receipt not in QC_COMPLETED
        // -----------------------------------------------------------------------

        @Test
        void approveReceipt_draftStatus_throwsBusinessRuleViolation() {
                qcCompletedReceipt.setStatus(ReceiptStatus.DRAFT);
                ReceiptDecisionRequest request = new ReceiptDecisionRequest();
                request.setExpectedVersion(3);

                when(receiptRepository.findById(1L)).thenReturn(Optional.of(qcCompletedReceipt));
                when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(5L)).thenReturn(List.of(10L));

                assertThrows(BusinessRuleViolationException.class,
                                () -> receiptService.approveReceipt(1L, request, manager));

                verify(receiptRepository, never()).save(any());
        }

        // -----------------------------------------------------------------------
        // Duplicate decision: already
        // APPROVED/RETURN_TO_SUPPLIER_PENDING/RETURNED_TO_SUPPLIER
        // -----------------------------------------------------------------------

        @Test
        void approveReceipt_alreadyApproved_throwsReceiptAlreadyDecided() {
                qcCompletedReceipt.setStatus(ReceiptStatus.APPROVED);
                ReceiptDecisionRequest request = new ReceiptDecisionRequest();
                request.setExpectedVersion(3);

                when(receiptRepository.findById(1L)).thenReturn(Optional.of(qcCompletedReceipt));
                when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(5L)).thenReturn(List.of(10L));

                assertThrows(ReceiptAlreadyDecidedException.class,
                                () -> receiptService.approveReceipt(1L, request, manager));
        }

        @Test
        void approveReceipt_alreadyReturnPending_throwsReceiptAlreadyDecided() {
                qcCompletedReceipt.setStatus(ReceiptStatus.RETURN_TO_SUPPLIER_PENDING);
                ReceiptDecisionRequest request = new ReceiptDecisionRequest();
                request.setExpectedVersion(3);

                when(receiptRepository.findById(1L)).thenReturn(Optional.of(qcCompletedReceipt));
                when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(5L)).thenReturn(List.of(10L));

                assertThrows(ReceiptAlreadyDecidedException.class,
                                () -> receiptService.approveReceipt(1L, request, manager));
        }

        // -----------------------------------------------------------------------
        // Stale version (optimistic locking)
        // -----------------------------------------------------------------------

        @Test
        void approveReceipt_staleVersion_throwsBusinessRuleViolation() {
                ReceiptDecisionRequest request = new ReceiptDecisionRequest();
                request.setExpectedVersion(99); // Client has old version 99, current is 3

                when(receiptRepository.findById(1L)).thenReturn(Optional.of(qcCompletedReceipt));
                when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(5L)).thenReturn(List.of(10L));

                BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
                                () -> receiptService.approveReceipt(1L, request, manager));

                assertTrue(ex.getMessage().contains("INVENTORY_VERSION_CONFLICT"));
                verify(receiptRepository, never()).save(any());
        }

        // -----------------------------------------------------------------------
        // Forbidden warehouse: manager not assigned
        // -----------------------------------------------------------------------

        @Test
        void approveReceipt_forbiddenWarehouse_throwsForbiddenReceiptWarehouse() {
                ReceiptDecisionRequest request = new ReceiptDecisionRequest();
                request.setExpectedVersion(3);

                when(receiptRepository.findById(1L)).thenReturn(Optional.of(qcCompletedReceipt));
                when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(5L))
                                .thenReturn(List.of(999L)); // Manager assigned to different warehouse

                assertThrows(ForbiddenReceiptWarehouseException.class,
                                () -> receiptService.approveReceipt(1L, request, manager));

                verify(receiptRepository, never()).save(any());
        }

        // -----------------------------------------------------------------------
        // Batch resolution: reuse existing batch idempotently
        // -----------------------------------------------------------------------

        @Test
        void approveReceipt_existingBatch_reusedNotDuplicated() {
                Batch existingBatch = new Batch();
                existingBatch.setId(200L);
                existingBatch.setBatchNumber("BCH-100-RCV-2026-001-2026-06-11");
                existingBatch.setProduct(product);
                existingBatch.setWarehouse(warehouse);
                existingBatch.setReceivedDate(LocalDate.of(2026, 6, 11));

                ReceiptDecisionRequest request = new ReceiptDecisionRequest();
                request.setExpectedVersion(3);

                when(receiptRepository.findById(1L)).thenReturn(Optional.of(qcCompletedReceipt));
                when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(5L)).thenReturn(List.of(10L));
                when(receiptItemRepository.findByReceiptId(1L)).thenReturn(List.of(receiptItem));
                when(batchRepository.findByProductWarehouseAndReceivedDate(100L, 10L, LocalDate.of(2026, 6, 11)))
                                .thenReturn(Optional.of(existingBatch)); // Existing batch found
                when(receiptItemRepository.save(any())).thenReturn(receiptItem);
                when(receiptRepository.save(any())).thenAnswer(i -> i.getArgument(0));

                receiptService.approveReceipt(1L, request, manager);

                // Should not create new batch since existing one was found
                verify(batchRepository, never()).save(any(Batch.class));
        }
}
