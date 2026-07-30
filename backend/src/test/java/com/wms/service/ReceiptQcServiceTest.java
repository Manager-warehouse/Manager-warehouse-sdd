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

import com.wms.dto.request.ReceiptQcItemRequest;
import com.wms.dto.request.ReceiptQcRequest;
import com.wms.dto.response.ReceiptQcResponse;
import com.wms.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
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
        itemReq.setQualityPassedQty(90);
        itemReq.setQualityFailedQty(5);
        itemReq.setQcFailureReason("Scratch");

        ReceiptQcRequest request = new ReceiptQcRequest();
        request.setAction(ReceiptQcRequest.QcAction.SUBMIT);
        request.setExpectedVersion(0);
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
        request.setExpectedVersion(0);

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
    void processQc_confirm_failed_noFailedQty_noInventoryMutation() {
        item.setQcResult(QcResult.FAILED);
        item.setQualityFailedQty(5);

        when(userRepository.findByEmail("storekeeper@wms.com")).thenReturn(Optional.of(storekeeperActor));
        when(receiptValidationService.loadReceiptForUpdate(100L)).thenReturn(receipt);
        when(receiptItemRepository.findByReceiptId(100L)).thenReturn(List.of(item));

        ReceiptQcRequest request = new ReceiptQcRequest();
        request.setAction(ReceiptQcRequest.QcAction.CONFIRM);
        request.setExpectedVersion(0);

        ReceiptQcResponse response = receiptQcService.processQc(100L, request, "storekeeper@wms.com");

        assertThat(response).isNotNull();
        assertThat(receipt.getStatus()).isEqualTo(ReceiptStatus.QC_FAILED);

        verify(receiptRepository).save(receipt);
        verify(auditLogService).log(eq(storekeeperActor), eq(AuditAction.RECEIPT_QC_CONFIRM), eq("Receipt"), eq(100L),
                any(), any(), any(), any());
        verifyNoInteractions(locationRepository, batchRepository, inventoryRepository);
    }

    @Test
    void processQc_confirm_failed_withFailedQty_marksQuarantineReadyOnly() {
        item.setQcResult(QcResult.FAILED);
        item.setQualityFailedQty(5);
        receipt.setDocumentDate(LocalDate.now());

        when(userRepository.findByEmail("storekeeper@wms.com")).thenReturn(Optional.of(storekeeperActor));
        when(receiptValidationService.loadReceiptForUpdate(100L)).thenReturn(receipt);
        when(receiptItemRepository.findByReceiptId(100L)).thenReturn(List.of(item));

        ReceiptQcRequest request = new ReceiptQcRequest();
        request.setAction(ReceiptQcRequest.QcAction.CONFIRM);
        request.setExpectedVersion(0);

        ReceiptQcResponse response = receiptQcService.processQc(100L, request, "storekeeper@wms.com");

        assertThat(response).isNotNull();
        assertThat(receipt.getStatus()).isEqualTo(ReceiptStatus.QC_FAILED);
        assertThat(item.getQuarantineReadyQty()).isEqualTo(5);

        verify(receiptItemRepository).save(item);
        verifyNoInteractions(locationRepository, batchRepository, inventoryRepository);
    }

    @Test
    void processQc_confirm_defectRate30PercentOrMore_stillMarksQcFailed() {
        item.setQcResult(QcResult.FAILED);
        item.setSampleQty(10);
        item.setSamplePassedQty(2);
        item.setSampleFailedQty(8);
        item.setQualityFailedQty(80);

        when(userRepository.findByEmail("storekeeper@wms.com")).thenReturn(Optional.of(storekeeperActor));
        when(receiptValidationService.loadReceiptForUpdate(100L)).thenReturn(receipt);
        when(receiptItemRepository.findByReceiptId(100L)).thenReturn(List.of(item));

        ReceiptQcRequest request = new ReceiptQcRequest();
        request.setAction(ReceiptQcRequest.QcAction.CONFIRM);
        request.setExpectedVersion(0);

        ReceiptQcResponse response = receiptQcService.processQc(100L, request, "storekeeper@wms.com");

        assertThat(response).isNotNull();
        assertThat(receipt.getStatus()).isEqualTo(ReceiptStatus.QC_FAILED);
        assertThat(item.getQuarantineReadyQty()).isEqualTo(80);

        verify(receiptRepository).save(receipt);
        verify(auditLogService).log(eq(storekeeperActor), eq(AuditAction.RECEIPT_QC_CONFIRM), eq("Receipt"), eq(100L),
                any(), any(), any(), any());
        verifyNoInteractions(locationRepository, batchRepository, inventoryRepository);
    }
}
