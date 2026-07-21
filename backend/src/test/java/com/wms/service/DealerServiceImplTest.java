package com.wms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wms.dto.request.DealerCreditStatusUpdateRequest;
import com.wms.entity.Dealer;
import com.wms.entity.User;
import com.wms.enums.CreditStatus;
import com.wms.enums.UserRole;
import com.wms.exception.BusinessRuleViolationException;
import com.wms.mapper.DealerMapper;
import com.wms.repository.DealerRepository;
import com.wms.repository.SystemConfigRepository;
import com.wms.service.impl.DealerServiceImpl;
import com.wms.util.PartnerAuditUtil;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DealerServiceImplTest {

    @Mock
    private DealerRepository dealerRepository;
    @Mock
    private SystemConfigRepository systemConfigRepository;
    @Mock
    private PartnerAuditUtil auditUtil;

    private DealerServiceImpl dealerService;
    private User accountantManager;
    private Dealer dealer;

    @BeforeEach
    void setUp() {
        dealerService = new DealerServiceImpl(dealerRepository, systemConfigRepository,
                new DealerMapper(), auditUtil);
        accountantManager = new User();
        accountantManager.setId(1L);
        accountantManager.setRole(UserRole.ACCOUNTANT_MANAGER);

        dealer = Dealer.builder()
                .id(10L)
                .code("DL-001")
                .creditLimit(BigDecimal.valueOf(50_000_000))
                .currentBalance(BigDecimal.valueOf(30_000_000))
                .creditStatus(CreditStatus.CREDIT_HOLD)
                .isActive(true)
                .build();
    }

    private void stubSave() {
        when(dealerRepository.findById(10L)).thenReturn(Optional.of(dealer));
        when(dealerRepository.save(any(Dealer.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private DealerCreditStatusUpdateRequest activeRequest() {
        DealerCreditStatusUpdateRequest request = new DealerCreditStatusUpdateRequest();
        request.setCreditStatus(CreditStatus.ACTIVE);
        return request;
    }

    @Test
    void updateCreditStatus_allowsLockingToCreditHoldWithoutAnyCheck() {
        dealer.setCreditStatus(CreditStatus.ACTIVE);
        stubSave();
        DealerCreditStatusUpdateRequest request = new DealerCreditStatusUpdateRequest();
        request.setCreditStatus(CreditStatus.CREDIT_HOLD);

        dealerService.updateCreditStatus(10L, request, accountantManager);

        assertThat(dealer.getCreditStatus()).isEqualTo(CreditStatus.CREDIT_HOLD);
    }

    @Test
    void updateCreditStatus_rejectsUnlockWhenBalanceAboveThreshold() {
        // limit*0.8 = 40,000,000; balance 30,000,000 is not below when limit is lowered to make threshold too small
        dealer.setCreditLimit(BigDecimal.valueOf(30_000_000)); // threshold = 24,000,000 < balance 30,000,000
        when(dealerRepository.findById(10L)).thenReturn(Optional.of(dealer));

        assertThatThrownBy(() -> dealerService.updateCreditStatus(10L, activeRequest(), accountantManager))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("not below the unlock threshold");
        verify(dealerRepository, never()).save(any());
    }

    @Test
    void updateCreditStatus_allowsUnlockWhenBalanceBelowThreshold() {
        stubSave();

        dealerService.updateCreditStatus(10L, activeRequest(), accountantManager);

        assertThat(dealer.getCreditStatus()).isEqualTo(CreditStatus.ACTIVE);
    }
}
