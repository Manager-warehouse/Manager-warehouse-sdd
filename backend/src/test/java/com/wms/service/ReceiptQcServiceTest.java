package com.wms.service;

import com.wms.dto.request.ReceiptQcItemRequest;
import com.wms.dto.request.ReceiptQcRequest;
import com.wms.dto.response.ReceiptQcResponse;
import com.wms.entity.*;
import com.wms.enums.*;
import com.wms.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReceiptQcServiceTest {

    @Mock
    private ReceiptRepository receiptRepository;
    @Mock
    private ReceiptItemRepository receiptItemRepository;
    @Mock
    private WarehouseLocationRepository locationRepository;
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private BatchRepository batchRepository;
    @Mock
    private ReceiptValidationService receiptValidationService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReceiptQcService receiptQcService;

    private User staffActor;
    private User storekeeperActor;
    private Receipt receipt;
    private ReceiptItem item;

    @BeforeEach
    void setUp() {
        staffActor = User.builder()
                .id(1L)
                .email("staff@wms.com")
                .role(UserRole.WAREHOUSE_STAFF)
                .build();

        storekeeperActor = User.builder()
                .id(2L)
                .email("storekeeper@wms.com")
                .role(UserRole.STOREKEEPER)
                .build();

        Warehouse warehouse = Warehouse.builder().id(10L).build();

        receipt = Receipt.builder()
                .id(100L)
                .receiptNumber("RCV-001")
                .warehouse(warehouse)
                .status(ReceiptStatus.DRAFT)
                .build();

        item = ReceiptItem.builder()
                .id(200L)
                .receipt(receipt)
                .product(Product.builder().id(30L).sku("PROD-1").name("Product 1").build())
                .expectedQty(100)
                .actualQty(95)
                .unitCost(BigDecimal.TEN)
                .build();
    }

    @Test
    void processQc_submit_success() {
        when(userRepository.findByEmail("staff@wms.com")).thenReturn(Optional.of(staffActor));
        when(receiptValidationService.loadReceiptForUpdate(100L)).thenReturn(receipt);
        when(receiptItemRepository.findByIdAndReceiptId(200L, 100L)).thenReturn(Optional.of(item));
        when(receiptItemRepository.findByReceiptId(100L)).thenReturn(List.of(item));

        ReceiptQcItemRequest itemReq = new ReceiptQcItemRequest();
        itemReq.setReceiptItemId(200L);
        itemReq.setQcPassedQty(90);
        itemReq.setQcFailedQty(5);
        itemReq.setQcFailureReason("Scratch");

        ReceiptQcRequest request = new ReceiptQcRequest();
        request.setAction(ReceiptQcRequest.QcAction.SUBMIT);
        request.setItems(List.of(itemReq));

        ReceiptQcResponse response = receiptQcService.processQc(100L, request, "staff@wms.com");

        assertThat(response).isNotNull();
        assertThat(response.getReceiptId()).isEqualTo(100L);
        assertThat(item.getSamplePassedQty()).isEqualTo(90);
        assertThat(item.getSampleFailedQty()).isEqualTo(5);
        assertThat(item.getQcResult()).isEqualTo(QcResult.FAILED); // sample failed > 0 -> FAILED

        verify(receiptValidationService).assertWarehouseAssignment(staffActor, 100L);
        verify(receiptValidationService).assertRole(staffActor, UserRole.WAREHOUSE_STAFF, "RECEIPT_QC_SUBMIT");
        verify(auditLogService).log(eq(staffActor), eq(AuditAction.RECEIPT_QC_SUBMIT), eq("Receipt"), eq(100L), any(),
                any(), any(), any());
    }

    @Test
    void processQc_confirm_passed_success() {
        item.setQcResult(QcResult.PASSED);

        when(userRepository.findByEmail("storekeeper@wms.com")).thenReturn(Optional.of(storekeeperActor));
        when(receiptValidationService.loadReceiptForUpdate(100L)).thenReturn(receipt);
        when(receiptItemRepository.findByReceiptId(100L)).thenReturn(List.of(item));

        ReceiptQcRequest request = new ReceiptQcRequest();
        request.setAction(ReceiptQcRequest.QcAction.CONFIRM);

        ReceiptQcResponse response = receiptQcService.processQc(100L, request, "storekeeper@wms.com");

        assertThat(response).isNotNull();
        assertThat(receipt.getStatus()).isEqualTo(ReceiptStatus.QC_COMPLETED);

        verify(receiptValidationService).assertWarehouseAssignment(storekeeperActor, 100L);
        verify(receiptValidationService).assertRole(storekeeperActor, UserRole.STOREKEEPER, "RECEIPT_QC_CONFIRM");
        verify(receiptRepository).save(receipt);
        verify(auditLogService).log(eq(storekeeperActor), eq(AuditAction.RECEIPT_QC_CONFIRM), eq("Receipt"), eq(100L),
                any(), any(), any(), any());
    }

    @Test
    void processQc_confirm_failed_marksReceiptWithoutInventoryMutation() {
        item.setQcResult(QcResult.FAILED);

        when(userRepository.findByEmail("storekeeper@wms.com")).thenReturn(Optional.of(storekeeperActor));
        when(receiptValidationService.loadReceiptForUpdate(100L)).thenReturn(receipt);
        when(receiptItemRepository.findByReceiptId(100L)).thenReturn(List.of(item));

        ReceiptQcRequest request = new ReceiptQcRequest();
        request.setAction(ReceiptQcRequest.QcAction.CONFIRM);

        ReceiptQcResponse response = receiptQcService.processQc(100L, request, "storekeeper@wms.com");

        assertThat(response).isNotNull();
        assertThat(receipt.getStatus()).isEqualTo(ReceiptStatus.QC_FAILED);

        verify(receiptRepository).save(receipt);
        verify(auditLogService).log(eq(storekeeperActor), eq(AuditAction.RECEIPT_QC_CONFIRM), eq("Receipt"), eq(100L),
                any(), any(), any(), any());
        verifyNoInteractions(locationRepository, batchRepository, inventoryRepository);
    }
}
