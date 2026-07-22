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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.wms.dto.request.AccountingPeriodCloseRequest;
import com.wms.entity.billing_payment.AccountingPeriod;
import com.wms.entity.access_control.User;
import com.wms.enums.billing_payment.AccountingPeriodStatus;
import com.wms.enums.access_control.UserRole;
import com.wms.exception.UnprocessableEntityException;
import com.wms.repository.AccountingPeriodRepository;
import com.wms.service.billing_payment.impl.AccountingPeriodServiceImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountingPeriodServiceImplTest {

    @Mock
    private AccountingPeriodRepository accountingPeriodRepository;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private EntityManager entityManager;

    private AccountingPeriodServiceImpl service;
    private User accountantManager;
    private AccountingPeriod openPeriod;

    @BeforeEach
    void setUp() {
        service = new AccountingPeriodServiceImpl(accountingPeriodRepository, auditLogService, entityManager);
        accountantManager = new User();
        accountantManager.setId(1L);
        accountantManager.setRole(UserRole.ACCOUNTANT_MANAGER);
        openPeriod = AccountingPeriod.builder()
                .id(10L)
                .periodName("2026-06")
                .status(AccountingPeriodStatus.OPEN)
                .build();
    }

    @SuppressWarnings("unchecked")
    private void stubAllChecksClean() {
        TypedQuery<Long> countQuery = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(countQuery);
        when(countQuery.setParameter(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(0L);
    }

    @Test
    void closePeriod_succeedsWhenNoPendingDocumentsExist() {
        when(accountingPeriodRepository.findById(10L)).thenReturn(Optional.of(openPeriod));
        stubAllChecksClean();
        when(accountingPeriodRepository.save(org.mockito.ArgumentMatchers.any(AccountingPeriod.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.closePeriod(10L, new AccountingPeriodCloseRequest(), accountantManager);

        assertEquals(AccountingPeriodStatus.CLOSED, response.getStatus());
        assertEquals(accountantManager.getId(), response.getClosedById());
    }

    @Test
    @SuppressWarnings("unchecked")
    void closePeriod_reportsBlockingReceiptsWithSampleNumbers() {
        when(accountingPeriodRepository.findById(10L)).thenReturn(Optional.of(openPeriod));

        // Receipt is the first check in validateNoPendingDocuments and throws immediately,
        // so no other entity's query is ever issued in this scenario.
        TypedQuery<Long> receiptCountQuery = mock(TypedQuery.class);
        when(entityManager.createQuery(contains("from Receipt"), eq(Long.class))).thenReturn(receiptCountQuery);
        when(receiptCountQuery.setParameter(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(receiptCountQuery);
        when(receiptCountQuery.getSingleResult()).thenReturn(2L);

        TypedQuery<String> receiptSampleQuery = mock(TypedQuery.class);
        when(entityManager.createQuery(contains("from Receipt"), eq(String.class))).thenReturn(receiptSampleQuery);
        when(receiptSampleQuery.setParameter(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(receiptSampleQuery);
        when(receiptSampleQuery.setMaxResults(anyInt())).thenReturn(receiptSampleQuery);
        when(receiptSampleQuery.getResultList()).thenReturn(List.of("RN-1", "RN-2"));

        UnprocessableEntityException ex = assertThrows(UnprocessableEntityException.class,
                () -> service.closePeriod(10L, new AccountingPeriodCloseRequest(), accountantManager));

        assertTrue(ex.getMessage().contains("2 pending/unapproved inbound receipts"));
        assertTrue(ex.getMessage().contains("RN-1, RN-2"));
    }

    @Test
    void closePeriod_rejectsAlreadyClosedPeriod() {
        AccountingPeriod closed = AccountingPeriod.builder()
                .id(10L)
                .periodName("2026-06")
                .status(AccountingPeriodStatus.CLOSED)
                .build();
        when(accountingPeriodRepository.findById(10L)).thenReturn(Optional.of(closed));

        assertThrows(UnprocessableEntityException.class,
                () -> service.closePeriod(10L, new AccountingPeriodCloseRequest(), accountantManager));
    }
}
