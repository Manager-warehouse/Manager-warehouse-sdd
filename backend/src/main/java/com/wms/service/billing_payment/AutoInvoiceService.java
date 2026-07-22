package com.wms.service.billing_payment;


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
import com.wms.dto.outbound.AutoInvoiceResult;
import com.wms.entity.order_fulfillment.DeliveryOrder;
import com.wms.entity.billing_payment.Invoice;
import com.wms.entity.access_control.User;
import java.time.LocalDate;

public interface AutoInvoiceService {

    AutoInvoiceResult createForConfirmedDelivery(DeliveryOrder deliveryOrder, User actor);

    // Manual backfill path used by InvoiceService when automatic invoicing failed.
    // Shares the same credit-check / period-stamping / numbering logic as the auto path,
    // but requires the Delivery Order to already be COMPLETED and rejects duplicates instead
    // of returning the existing invoice idempotently.
    Invoice createBackfillInvoice(DeliveryOrder deliveryOrder, User actor, LocalDate documentDate);
}
