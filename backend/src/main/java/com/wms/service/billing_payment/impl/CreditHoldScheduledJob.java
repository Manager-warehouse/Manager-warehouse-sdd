package com.wms.service.billing_payment.impl;


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
import com.wms.service.billing_payment.PaymentReceiptService;
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
