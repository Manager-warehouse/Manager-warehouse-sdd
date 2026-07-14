package com.wms.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wms.enums.BillingNotificationInvoiceStatus;
import com.wms.enums.BillingNotificationStatus;
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
