package com.wms.service.impl;

import com.wms.dto.response.CeoDashboardResponse;
import com.wms.dto.response.InventoryValuationResponse;
import com.wms.entity.User;
import com.wms.entity.Warehouse;
import com.wms.enums.UserRole;
import com.wms.enums.WarehouseType;
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
