package com.wms.dto.response;


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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wms.enums.billing_payment.BillingNotificationInvoiceStatus;
import com.wms.enums.billing_payment.BillingNotificationStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class BillingNotificationResponse {
    private Long id;

    @JsonProperty("do_id")
    private Long doId;

    @JsonProperty("do_number")
    private String doNumber;

    @JsonProperty("dealer_id")
    private Long dealerId;

    @JsonProperty("dealer_name")
    private String dealerName;

    @JsonProperty("warehouse_id")
    private Long warehouseId;

    @JsonProperty("delivered_at")
    private OffsetDateTime deliveredAt;

    @JsonProperty("total_amount_estimate")
    private BigDecimal totalAmountEstimate;

    @JsonProperty("invoice_status")
    private BillingNotificationInvoiceStatus invoiceStatus;

    private BillingNotificationStatus status;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("read_at")
    private OffsetDateTime readAt;

    @JsonProperty("otp_verified_at")
    private OffsetDateTime otpVerifiedAt;

    @JsonProperty("pod_image_url")
    private String podImageUrl;

    @JsonProperty("pod_signature_url")
    private String podSignatureUrl;

    @JsonProperty("pod_timestamp")
    private OffsetDateTime podTimestamp;
}
