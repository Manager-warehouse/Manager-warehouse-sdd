package com.wms.service.billing_payment.impl;

import com.wms.entity.access_control.User;
import com.wms.entity.billing_payment.AccountingPeriod;
import com.wms.entity.notification_delivery.Notification;
import com.wms.enums.access_control.UserRole;
import com.wms.enums.billing_payment.AccountingPeriodStatus;
import com.wms.repository.AccountingPeriodRepository;
import com.wms.repository.NotificationRepository;
import com.wms.repository.UserRepository;
import com.wms.service.user_configuration.SystemConfigService;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// feature-accountant-period-closing.md 3 (Event-driven): once today's date reaches or passes
// MONTHLY_CLOSING_DAY (system_configs, default 25) the system SHALL remind ACCOUNTANT_MANAGER
// to review and close the accounting period. Runs daily; the reminder repeats every day the
// condition still holds, until the period covering today is actually CLOSED.
@Component
public class MonthlyClosingReminderScheduledJob {

    private static final Logger log = LoggerFactory.getLogger(MonthlyClosingReminderScheduledJob.class);
    private static final int DEFAULT_CLOSING_DAY = 25;
    private static final String NOTIFICATION_TYPE = "MONTHLY_CLOSING_REMINDER";

    private final SystemConfigService systemConfigService;
    private final AccountingPeriodRepository accountingPeriodRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    public MonthlyClosingReminderScheduledJob(SystemConfigService systemConfigService,
                                              AccountingPeriodRepository accountingPeriodRepository,
                                              UserRepository userRepository,
                                              NotificationRepository notificationRepository) {
        this.systemConfigService = systemConfigService;
        this.accountingPeriodRepository = accountingPeriodRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void run() {
        try {
            checkAndNotify();
        } catch (Exception e) {
            log.error("Monthly closing reminder job failed", e);
        }
    }

    @Transactional
    public void checkAndNotify() {
        int closingDay = systemConfigService.getIntValue("MONTHLY_CLOSING_DAY", DEFAULT_CLOSING_DAY);
        LocalDate today = LocalDate.now();
        if (today.getDayOfMonth() < closingDay) {
            return;
        }

        Optional<AccountingPeriod> openPeriod = accountingPeriodRepository
                .findPeriodByDateAndStatus(today, AccountingPeriodStatus.OPEN);
        if (openPeriod.isEmpty()) {
            return;
        }

        String message = String.format(
                "Đã đến hoặc quá ngày khóa sổ hàng tháng (ngày %d). Vui lòng rà soát và chốt sổ kỳ kế toán %s.",
                closingDay, openPeriod.get().getPeriodName());

        List<User> managers = userRepository.findByRole(UserRole.ACCOUNTANT_MANAGER);
        for (User manager : managers) {
            notificationRepository.save(Notification.builder()
                    .recipient(manager)
                    .type(NOTIFICATION_TYPE)
                    .referenceType("accounting_periods")
                    .referenceId(openPeriod.get().getId())
                    .message(message)
                    .isRead(false)
                    .createdAt(OffsetDateTime.now())
                    .build());
        }
        log.info("Monthly closing reminder sent to {} accountant manager(s) for period {}",
                managers.size(), openPeriod.get().getPeriodName());
    }
}
