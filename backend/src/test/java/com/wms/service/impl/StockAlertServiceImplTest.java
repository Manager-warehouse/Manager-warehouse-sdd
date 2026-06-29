package com.wms.service.impl;

import com.wms.dto.response.StockAlertResponse;
import com.wms.entity.*;
import com.wms.enums.AlertType;
import com.wms.enums.UserRole;
import com.wms.enums.WarehouseType;
import com.wms.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockAlertServiceImplTest {

    @Mock StockAlertRepository stockAlertRepository;
    @Mock InventoryRepository inventoryRepository;
    @Mock WarehouseProductReservationRepository warehouseProductReservationRepository;
    @Mock ProductRepository productRepository;
    @Mock WarehouseRepository warehouseRepository;
    @Mock SystemConfigRepository systemConfigRepository;
    @Mock UserRepository userRepository;
    @Mock UserWarehouseAssignmentRepository userWarehouseAssignmentRepository;
    @Mock NotificationRepository notificationRepository;

    StockAlertServiceImpl service;

    Warehouse warehouse;
    Product product;
    User manager;

    @BeforeEach
    void setUp() {
        service = new StockAlertServiceImpl(
                stockAlertRepository, inventoryRepository, warehouseProductReservationRepository,
                productRepository, warehouseRepository, systemConfigRepository,
                userRepository, userWarehouseAssignmentRepository, notificationRepository
        );

        warehouse = new Warehouse();
        warehouse.setId(1L);
        warehouse.setName("Kho Hải Phòng");
        warehouse.setType(WarehouseType.PHYSICAL);

        product = new Product();
        product.setId(10L);
        product.setSku("POT-001");
        product.setName("Nồi inox 20cm");
        product.setReorderPoint(new BigDecimal("20"));

        manager = new User();
        manager.setId(2L);
        manager.setRole(UserRole.WAREHOUSE_MANAGER);
    }

    @Test
    void checkAndTriggerAlert_lowStock_triggersNewAlert() {
        // Tồn khả dụng = 15 < 20 (reorder point) -> Cần trigger alert
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(inventoryRepository.sumValidAvailableQty(1L, 10L)).thenReturn(new BigDecimal("15"));
        when(warehouseProductReservationRepository.findByWarehouseIdAndProductId(1L, 10L))
                .thenReturn(Optional.empty());
        when(stockAlertRepository.findByWarehouseIdAndProductIdAndAlertTypeAndIsResolved(1L, 10L, AlertType.LOW_STOCK, false))
                .thenReturn(Optional.empty());
        when(userWarehouseAssignmentRepository.findWarehouseManagersByWarehouseId(1L))
                .thenReturn(List.of(manager));
        when(userRepository.findByRole(UserRole.PLANNER))
                .thenReturn(List.of());

        service.checkAndTriggerAlert(1L, 10L);

        verify(stockAlertRepository, times(1)).save(any(StockAlert.class));
        verify(notificationRepository, atLeastOnce()).save(any(Notification.class));

    }

    @Test
    void checkAndTriggerAlert_sufficientStock_resolvesActiveAlert() {
        // Tồn khả dụng = 25 >= 20 (reorder point) và đang có active alert -> Cần resolve alert
        StockAlert activeAlert = new StockAlert();
        activeAlert.setId(100L);
        activeAlert.setWarehouse(warehouse);
        activeAlert.setProduct(product);
        activeAlert.setAlertType(AlertType.LOW_STOCK);
        activeAlert.setIsResolved(false);

        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(inventoryRepository.sumValidAvailableQty(1L, 10L)).thenReturn(new BigDecimal("25"));
        when(warehouseProductReservationRepository.findByWarehouseIdAndProductId(1L, 10L))
                .thenReturn(Optional.empty());
        when(stockAlertRepository.findByWarehouseIdAndProductIdAndAlertTypeAndIsResolved(1L, 10L, AlertType.LOW_STOCK, false))
                .thenReturn(Optional.of(activeAlert));
        when(userWarehouseAssignmentRepository.findWarehouseManagersByWarehouseId(1L))
                .thenReturn(List.of(manager));
        when(userRepository.findByRole(UserRole.PLANNER))
                .thenReturn(List.of());

        service.checkAndTriggerAlert(1L, 10L);

        assertThat(activeAlert.getIsResolved()).isTrue();
        assertThat(activeAlert.getResolvedAt()).isNotNull();
        verify(stockAlertRepository, times(1)).save(activeAlert);
        verify(notificationRepository, atLeastOnce()).save(any(Notification.class));
    }


    @Test
    void getLowStockAlerts_unauthorizedWarehouse_throwsException() {
        User user = new User();
        user.setId(5L);
        user.setRole(UserRole.WAREHOUSE_MANAGER);

        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(5L)).thenReturn(List.of(2L)); // Chỉ được phép xem kho 2

        assertThatThrownBy(() -> service.getLowStockAlerts(1L, null, false, 0, 10, 5L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("WAREHOUSE_SCOPE_FORBIDDEN");
    }

    @Test
    void getLowStockAlerts_authorizedWarehouse_returnsPage() {
        User user = new User();
        user.setId(5L);
        user.setRole(UserRole.WAREHOUSE_MANAGER);

        StockAlert alert = StockAlert.builder()
                .id(1L)
                .warehouse(warehouse)
                .product(product)
                .currentQty(BigDecimal.TEN)
                .reorderPoint(BigDecimal.valueOf(20))
                .alertType(AlertType.LOW_STOCK)
                .isResolved(false)
                .build();

        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(5L)).thenReturn(List.of(1L));
        when(stockAlertRepository.findWithFilters(eq(1L), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(alert)));

        Page<StockAlertResponse> result = service.getLowStockAlerts(1L, null, false, 0, 10, 5L);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getProductSku()).isEqualTo("POT-001");
    }
}
