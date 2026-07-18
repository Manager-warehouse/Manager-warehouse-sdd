package com.wms.service.impl;

import com.wms.service.PaymentReceiptService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// NFR-005 (Spec 008): daily credit-hold sweep must run off-peak, duration <= 30s.
// Kept separate from PaymentReceiptServiceImpl so that class stays free of scheduling
// concerns; the actual overdue-detection logic lives in runDailyOverdueHoldJob().
@Component
public class CreditHoldScheduledJob {

    private static final Logger log = LoggerFactory.getLogger(CreditHoldScheduledJob.class);

    private final PaymentReceiptService paymentReceiptService;

    public CreditHoldScheduledJob(PaymentReceiptService paymentReceiptService) {
        this.paymentReceiptService = paymentReceiptService;
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void run() {
        try {
            paymentReceiptService.runDailyOverdueHoldJob();
        } catch (Exception e) {
            log.error("Daily credit-hold sweep failed", e);
        }
    }
}
