package com.wms.service.billing_payment;


import com.wms.dto.outbound.AutoInvoiceResult;
import com.wms.entity.access_control.User;
import com.wms.entity.billing_payment.Invoice;
import com.wms.entity.order_fulfillment.Delivery;
import com.wms.entity.order_fulfillment.DeliveryOrder;
import java.time.LocalDate;

public interface AutoInvoiceService {

    AutoInvoiceResult createForConfirmedDelivery(DeliveryOrder deliveryOrder, User actor);

    // Manual backfill path used by InvoiceService when automatic invoicing failed.
    // Shares the same credit-check / period-stamping / numbering logic as the auto path,
    // but requires the Delivery Order to already be COMPLETED and rejects duplicates instead
    // of returning the existing invoice idempotently.
    Invoice createBackfillInvoice(DeliveryOrder deliveryOrder, User actor, LocalDate documentDate);
}
