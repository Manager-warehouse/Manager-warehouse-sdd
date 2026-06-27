package com.wms.service;

import com.wms.dto.request.DisposalRequest;
import com.wms.dto.response.DisposalResponse;
import com.wms.dto.response.PendingDisposalResponse;
import com.wms.entity.*;
import com.wms.enums.AdjustmentType;
import com.wms.enums.ReceiptStatus;
import com.wms.enums.ReceiptType;
import com.wms.enums.UserRole;
import com.wms.exception.BusinessRuleViolationException;
import com.wms.exception.ForbiddenReceiptWarehouseException;
import com.wms.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisposalServiceTest {

    @Mock private ReceiptItemRepository receiptItemRepository;
    @Mock private DamageReportRepository damageReportRepository;
    @Mock private AdjustmentRepository adjustmentRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private WarehouseLocationRepository warehouseLocationRepository;
    @Mock private PriceHistoryRepository priceHistoryRepository;
    @Mock private UserWarehouseAssignmentRepository userWarehouseAssignmentRepository;
    @Mock private ReceiptValidationService receiptValidationService;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private DisposalService disposalService;

    private User actor;
    private ReceiptItem item;
    private Receipt receipt;
    private Warehouse warehouse;
    private WarehouseLocation location;
    private Product product;
    private Batch batch;

    @BeforeEach
    void setUp() {
        actor = User.builder().id(1L).role(UserRole.WAREHOUSE_STAFF).fullName("Staff Test").build();
        warehouse = Warehouse.builder().id(10L).name("HP Warehouse").build();
        location = WarehouseLocation.builder().id(20L).isQuarantine(true).currentVolumeM3(BigDecimal.ZERO).currentWeightKg(BigDecimal.ZERO).build();
        product = Product.builder().id(30L).sku("POT-001").volumeM3(BigDecimal.valueOf(0.1)).weightKg(BigDecimal.valueOf(1.0)).build();
        batch = Batch.builder().id(40L).build();

        receipt = Receipt.builder()
                .id(100L)
                .warehouse(warehouse)
                .status(ReceiptStatus.QC_FAILED)
                .build();

        item = ReceiptItem.builder()
                .id(200L)
                .receipt(receipt)
                .product(product)
                .batch(batch)
                .location(location)
                .sampleFailedQty(10)
                .unitCost(BigDecimal.valueOf(100000)) // 100K each -> Total 1M (Auto approval)
                .build();
    }

    @Test
    void createDisposalRequest_autoApproved_success() {
        when(receiptItemRepository.findById(200L)).thenReturn(Optional.of(item));
        when(adjustmentRepository.existsByReferenceTypeAndReferenceIdAndType(any(), any(), any())).thenReturn(false);
        when(damageReportRepository.save(any(DamageReport.class))).thenAnswer(invocation -> {
            DamageReport dr = invocation.getArgument(0);
            dr.setId(400L);
            return dr;
        });
        when(adjustmentRepository.save(any(Adjustment.class))).thenAnswer(invocation -> {
            Adjustment adj = invocation.getArgument(0);
            adj.setId(500L);
            return adj;
        });
        
        Inventory inventory = Inventory.builder()
                .warehouse(warehouse)
                .product(product)
                .batch(batch)
                .location(location)
                .totalQty(BigDecimal.valueOf(15))
                .reservedQty(BigDecimal.ZERO)
                .build();
        when(inventoryRepository.findByWarehouseProductBatchLocationForUpdate(any(), any(), any(), any()))
                .thenReturn(Optional.of(inventory));

        DisposalRequest req = DisposalRequest.builder().cause("Vỡ móp").imageUrl("http://img").build();
        DisposalResponse response = disposalService.createDisposalRequest(200L, req, actor);

        assertThat(response).isNotNull();
        assertThat(response.isAutoApproved()).isTrue();
        assertThat(inventory.getTotalQty()).isEqualByComparingTo(BigDecimal.valueOf(5)); // 15 - 10 = 5

        verify(inventoryRepository).save(inventory);
        verify(warehouseLocationRepository).save(location);
        verify(receiptValidationService).assertWarehouseAccess(actor, 10L);
    }

    @Test
    void createDisposalRequest_requiresApproval_success() {
        item.setUnitCost(BigDecimal.valueOf(1000000)); // 1M each -> Total 10M (Requires manager approval)
        when(receiptItemRepository.findById(200L)).thenReturn(Optional.of(item));
        when(damageReportRepository.save(any(DamageReport.class))).thenAnswer(invocation -> {
            DamageReport dr = invocation.getArgument(0);
            dr.setId(400L);
            return dr;
        });
        when(adjustmentRepository.save(any(Adjustment.class))).thenAnswer(invocation -> {
            Adjustment adj = invocation.getArgument(0);
            adj.setId(500L);
            return adj;
        });

        DisposalRequest req = DisposalRequest.builder().cause("Rỉ sét nặng").build();
        DisposalResponse response = disposalService.createDisposalRequest(200L, req, actor);

        assertThat(response).isNotNull();
        assertThat(response.isAutoApproved()).isFalse(); // > 5M -> Needs approval

        verifyNoInteractions(inventoryRepository); // No stock deduction until approval
    }

    @Test
    void approveDisposal_manager_success() {
        Adjustment adjustment = Adjustment.builder()
                .id(500L)
                .adjustmentNumber("ADJ-001")
                .warehouse(warehouse)
                .product(product)
                .batch(batch)
                .location(location)
                .quantityAdjustment(BigDecimal.valueOf(-10))
                .type(AdjustmentType.DISPOSAL)
                .referenceId(200L)
                .reason("Hỏng")
                .build();

        User manager = User.builder().id(2L).role(UserRole.WAREHOUSE_MANAGER).build();
        when(adjustmentRepository.findById(500L)).thenReturn(Optional.of(adjustment));
        when(receiptItemRepository.findById(200L)).thenReturn(Optional.of(item));

        Inventory inventory = Inventory.builder()
                .warehouse(warehouse)
                .product(product)
                .batch(batch)
                .location(location)
                .totalQty(BigDecimal.valueOf(10))
                .reservedQty(BigDecimal.ZERO)
                .build();
        when(inventoryRepository.findByWarehouseProductBatchLocationForUpdate(any(), any(), any(), any()))
                .thenReturn(Optional.of(inventory));

        DisposalResponse response = disposalService.approveDisposal(500L, manager);

        assertThat(response).isNotNull();
        assertThat(adjustment.getApprovedBy()).isEqualTo(manager);
        assertThat(inventory.getTotalQty()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void approveDisposal_highValueRequiresCeo_failsForManager() {
        item.setUnitCost(BigDecimal.valueOf(12000000)); // 12M each -> Total 120M (Exceeds 100M threshold)
        Adjustment adjustment = Adjustment.builder()
                .id(500L)
                .adjustmentNumber("ADJ-001")
                .warehouse(warehouse)
                .product(product)
                .batch(batch)
                .location(location)
                .quantityAdjustment(BigDecimal.valueOf(-10))
                .type(AdjustmentType.DISPOSAL)
                .referenceId(200L)
                .build();

        User manager = User.builder().id(2L).role(UserRole.WAREHOUSE_MANAGER).build();
        when(adjustmentRepository.findById(500L)).thenReturn(Optional.of(adjustment));
        when(receiptItemRepository.findById(200L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> disposalService.approveDisposal(500L, manager))
                .isInstanceOf(ForbiddenReceiptWarehouseException.class)
                .hasMessageContaining("CEO approval is required");
    }

    @Test
    void createDisposalRequest_returnReceiptApproved_success() {
        receipt.setType(ReceiptType.RETURN);
        receipt.setStatus(ReceiptStatus.APPROVED);

        when(receiptItemRepository.findById(200L)).thenReturn(Optional.of(item));
        when(adjustmentRepository.existsByReferenceTypeAndReferenceIdAndType(any(), any(), any())).thenReturn(false);
        when(damageReportRepository.save(any(DamageReport.class))).thenAnswer(invocation -> {
            DamageReport dr = invocation.getArgument(0);
            dr.setId(400L);
            return dr;
        });
        when(adjustmentRepository.save(any(Adjustment.class))).thenAnswer(invocation -> {
            Adjustment adj = invocation.getArgument(0);
            adj.setId(500L);
            return adj;
        });
        
        Inventory inventory = Inventory.builder()
                .warehouse(warehouse)
                .product(product)
                .batch(batch)
                .location(location)
                .totalQty(BigDecimal.valueOf(15))
                .reservedQty(BigDecimal.ZERO)
                .build();
        when(inventoryRepository.findByWarehouseProductBatchLocationForUpdate(any(), any(), any(), any()))
                .thenReturn(Optional.of(inventory));

        DisposalRequest req = DisposalRequest.builder().cause("Hỏng khi trả hàng").build();
        DisposalResponse response = disposalService.createDisposalRequest(200L, req, actor);

        assertThat(response).isNotNull();
        assertThat(response.isAutoApproved()).isTrue();
        assertThat(inventory.getTotalQty()).isEqualByComparingTo(BigDecimal.valueOf(5));
    }
}
