package com.wms.service;

import com.wms.entity.access_control.User;
import com.wms.entity.billing_payment.AccountingPeriod;
import com.wms.entity.notification_delivery.Notification;
import com.wms.enums.access_control.UserRole;
import com.wms.enums.billing_payment.AccountingPeriodStatus;
import com.wms.repository.AccountingPeriodRepository;
import com.wms.repository.NotificationRepository;
import com.wms.repository.UserRepository;
import com.wms.service.billing_payment.impl.MonthlyClosingReminderScheduledJob;
import com.wms.service.user_configuration.SystemConfigService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonthlyClosingReminderScheduledJobTest {

    @Mock private SystemConfigService systemConfigService;
    @Mock private AccountingPeriodRepository accountingPeriodRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationRepository notificationRepository;

    private MonthlyClosingReminderScheduledJob job;
    private AccountingPeriod openPeriod;
    private User manager;

    @BeforeEach
    void setUp() {
        job = new MonthlyClosingReminderScheduledJob(
                systemConfigService, accountingPeriodRepository, userRepository, notificationRepository);

        openPeriod = new AccountingPeriod();
        openPeriod.setId(5L);
        openPeriod.setPeriodName("2026-07");
        openPeriod.setStatus(AccountingPeriodStatus.OPEN);

        manager = new User();
        manager.setId(9L);
        manager.setRole(UserRole.ACCOUNTANT_MANAGER);
    }

    @Test
    @DisplayName("Gửi nhắc nhở khi ngày hiện tại đạt/vượt ngày khóa sổ và kỳ đang OPEN")
    void checkAndNotify_sendsReminderWhenPastClosingDayAndPeriodOpen() {
        // closingDay = 1 guarantees today's day-of-month is always >= 1, making this
        // deterministic regardless of when the test actually runs.
        when(systemConfigService.getIntValue(eq("MONTHLY_CLOSING_DAY"), anyInt())).thenReturn(1);
        when(accountingPeriodRepository.findPeriodByDateAndStatus(any(LocalDate.class), eq(AccountingPeriodStatus.OPEN)))
                .thenReturn(Optional.of(openPeriod));
        when(userRepository.findByRole(UserRole.ACCOUNTANT_MANAGER)).thenReturn(List.of(manager));

        job.checkAndNotify();

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getRecipient()).isSameAs(manager);
        assertThat(captor.getValue().getReferenceId()).isEqualTo(5L);
        assertThat(captor.getValue().getMessage()).contains("2026-07");
    }

    @Test
    @DisplayName("Không gửi nhắc nhở khi chưa tới ngày khóa sổ cấu hình")
    void checkAndNotify_skipsWhenBeforeClosingDay() {
        // closingDay = 32 can never be reached by a real day-of-month, so this deterministically
        // exercises the "not yet due" branch regardless of when the test runs.
        when(systemConfigService.getIntValue(eq("MONTHLY_CLOSING_DAY"), anyInt())).thenReturn(32);

        job.checkAndNotify();

        verify(accountingPeriodRepository, never()).findPeriodByDateAndStatus(any(), any());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Không gửi nhắc nhở khi không còn kỳ kế toán OPEN cho ngày hiện tại")
    void checkAndNotify_skipsWhenNoOpenPeriodCoversToday() {
        when(systemConfigService.getIntValue(eq("MONTHLY_CLOSING_DAY"), anyInt())).thenReturn(1);
        when(accountingPeriodRepository.findPeriodByDateAndStatus(any(LocalDate.class), eq(AccountingPeriodStatus.OPEN)))
                .thenReturn(Optional.empty());

        job.checkAndNotify();

        verify(userRepository, never()).findByRole(any());
        verify(notificationRepository, never()).save(any());
    }
}
