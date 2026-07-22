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
import com.wms.enums.billing_payment.InvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponse {

    private Long id;

    @JsonProperty("invoice_number")
    private String invoiceNumber;

    @JsonProperty("do_id")
    private Long doId;

    @JsonProperty("do_number")
    private String doNumber;

    @JsonProperty("dealer_id")
    private Long dealerId;

    @JsonProperty("dealer_name")
    private String dealerName;

    @JsonProperty("total_amount")
    private BigDecimal totalAmount;

    @JsonProperty("issue_date")
    private LocalDate issueDate;

    @JsonProperty("due_date")
    private LocalDate dueDate;

    private InvoiceStatus status;

    @JsonProperty("created_by")
    private Long createdById;

    @JsonProperty("created_by_name")
    private String createdByName;

    @JsonProperty("document_date")
    private LocalDate documentDate;

    @JsonProperty("accounting_period_id")
    private Long accountingPeriodId;

    @JsonProperty("accounting_period_name")
    private String accountingPeriodName;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;

    // Proof-of-delivery evidence for reconciliation against this auto-invoiced delivery
    // (feature-accountant-customer-invoicing.md §4.3).
    @JsonProperty("otp_verified_at")
    private OffsetDateTime otpVerifiedAt;

    @JsonProperty("pod_image_url")
    private String podImageUrl;

    @JsonProperty("pod_signature_url")
    private String podSignatureUrl;

    @JsonProperty("pod_timestamp")
    private OffsetDateTime podTimestamp;
}
