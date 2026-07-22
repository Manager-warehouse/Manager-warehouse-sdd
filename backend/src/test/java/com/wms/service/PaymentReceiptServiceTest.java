@ -3,8 +3,6 @@ package com.wms.service;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
@ -17,12 +15,10 @@ import com.wms.enums.UserRole;
import com.wms.exception.BusinessRuleViolationException;
import com.wms.mapper.DealerMapper;
import com.wms.repository.DealerRepository;
import com.wms.repository.InvoiceRepository;
import com.wms.repository.SystemConfigRepository;
import com.wms.service.impl.DealerServiceImpl;
import com.wms.util.PartnerAuditUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
@ -38,8 +34,6 @@ class DealerServiceImplTest {
    @Mock
    private SystemConfigRepository systemConfigRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private PartnerAuditUtil auditUtil;

    private DealerServiceImpl dealerService;
@ -49,7 +43,7 @@ class DealerServiceImplTest {
    @BeforeEach
    void setUp() {
        dealerService = new DealerServiceImpl(dealerRepository, systemConfigRepository,
                invoiceRepository, new DealerMapper(), auditUtil);
        accountantManager = new User();
        accountantManager.setId(1L);
        accountantManager.setRole(UserRole.ACCOUNTANT_MANAGER);
@ -100,23 +94,8 @@ class DealerServiceImplTest {
    }

    @Test
    void updateCreditStatus_rejectsUnlockWhenInvoiceStillOverdue() {
        // threshold = 50,000,000 * 0.8 = 40,000,000; balance 30,000,000 is below -> passes balance check
        when(dealerRepository.findById(10L)).thenReturn(Optional.of(dealer));
        when(invoiceRepository.existsByDealerIdAndStatusInAndDueDateBefore(
                eq(10L), anyIterable(), any(LocalDate.class))).thenReturn(true);

        assertThatThrownBy(() -> dealerService.updateCreditStatus(10L, activeRequest(), accountantManager))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("overdue");
        verify(dealerRepository, never()).save(any());
    }

    @Test
    void updateCreditStatus_allowsUnlockWhenBalanceLowAndNoOverdueInvoice() {
        stubSave();
        when(invoiceRepository.existsByDealerIdAndStatusInAndDueDateBefore(
                eq(10L), anyIterable(), any(LocalDate.class))).thenReturn(false);

        dealerService.updateCreditStatus(10L, activeRequest(), accountantManager);

