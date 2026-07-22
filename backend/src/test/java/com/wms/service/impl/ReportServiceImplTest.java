package com.wms.service.impl;


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

import com.wms.dto.response.CeoDashboardResponse;
import com.wms.dto.response.InventoryValuationResponse;
import com.wms.entity.access_control.User;
import com.wms.entity.warehouse_location.Warehouse;
import com.wms.enums.access_control.UserRole;
import com.wms.enums.warehouse_location.WarehouseType;
import com.wms.repository.DeliveryOrderItemRepository;
import com.wms.repository.DeliveryRepository;
import com.wms.repository.InventoryRepository;
import com.wms.repository.InvoiceRepository;
import com.wms.repository.OutboundQcRecordRepository;
import com.wms.repository.ReceiptItemRepository;
import com.wms.repository.TripRepository;
import com.wms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock InventoryRepository inventoryRepository;
    @Mock InvoiceRepository invoiceRepository;
    @Mock DeliveryOrderItemRepository deliveryOrderItemRepository;
    @Mock TripRepository tripRepository;
    @Mock OutboundQcRecordRepository outboundQcRecordRepository;
    @Mock ReceiptItemRepository receiptItemRepository;
    @Mock DeliveryRepository deliveryRepository;

    ReportServiceImpl service;

    User ceo;
    User accountantManager;
    Warehouse warehouse;

    @BeforeEach
    void setUp() {
        service = new ReportServiceImpl(
                userRepository, inventoryRepository, invoiceRepository,
                deliveryOrderItemRepository, tripRepository,
                outboundQcRecordRepository,
                receiptItemRepository, deliveryRepository
        );

        ceo = new User();
        ceo.setId(1L);
        ceo.setRole(UserRole.CEO);

        accountantManager = new User();
        accountantManager.setId(2L);
        accountantManager.setRole(UserRole.ACCOUNTANT_MANAGER);

        warehouse = new Warehouse();
        warehouse.setId(10L);
        warehouse.setName("Kho Hai Phong");
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
}
