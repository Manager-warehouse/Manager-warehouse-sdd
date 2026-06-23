package com.wms.dto.response;

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
    private Long doId;
    private String doNumber;
    private Long dealerId;
    private String dealerName;
    private Long warehouseId;
    private OffsetDateTime deliveredAt;
    private BigDecimal totalAmountEstimate;
    private BillingNotificationInvoiceStatus invoiceStatus;
    private BillingNotificationStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime readAt;
}
