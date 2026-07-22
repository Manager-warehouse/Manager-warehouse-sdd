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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

import com.wms.entity.access_control.User;
import com.wms.enums.access_control.UserRole;
import com.wms.exception.ForbiddenReceiptWarehouseException;
import com.wms.repository.ReceiptRepository;
import com.wms.repository.UserWarehouseAssignmentRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReceiptValidationServiceTest {

    @Mock
    private ReceiptRepository receiptRepository;
    @Mock
    private UserWarehouseAssignmentRepository userWarehouseAssignmentRepository;

    private ReceiptValidationService service;

    @BeforeEach
    void setUp() {
        service = new ReceiptValidationService(receiptRepository, userWarehouseAssignmentRepository);
    }

    private User user(UserRole role) {
        User user = new User();
        user.setId(1L);
        user.setRole(role);
        return user;
    }

    @Test
    void assertWarehouseAccess_allowsAccountantWithoutWarehouseAssignment() {
        assertDoesNotThrow(() -> service.assertWarehouseAccess(user(UserRole.ACCOUNTANT), 20L));
    }

    @Test
    void assertWarehouseAccess_allowsAccountantManagerWithoutWarehouseAssignment() {
        assertDoesNotThrow(() -> service.assertWarehouseAccess(user(UserRole.ACCOUNTANT_MANAGER), 20L));
    }

    @Test
    void assertWarehouseAccess_rejectsUnassignedWarehouseStaff() {
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(1L)).thenReturn(List.of(99L));

        assertThrows(ForbiddenReceiptWarehouseException.class,
                () -> service.assertWarehouseAccess(user(UserRole.WAREHOUSE_STAFF), 20L));
    }

    @Test
    void assertWarehouseAccess_allowsAssignedWarehouseStaff() {
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(1L)).thenReturn(List.of(20L));

        assertDoesNotThrow(() -> service.assertWarehouseAccess(user(UserRole.WAREHOUSE_STAFF), 20L));
    }
}
