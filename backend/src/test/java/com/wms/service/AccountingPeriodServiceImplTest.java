package com.wms.service;

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
import com.wms.entity.AccountingPeriod;
import com.wms.entity.User;
import com.wms.enums.AccountingPeriodStatus;
import com.wms.enums.UserRole;
import com.wms.exception.UnprocessableEntityException;
import com.wms.repository.AccountingPeriodRepository;
import com.wms.service.impl.AccountingPeriodServiceImpl;
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
