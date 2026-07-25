package com.wms.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierBillingNotificationResponse {
    private Long id;
    private Long receiptId;
    // frontend reads notif.receiptOrderNumber || notif.receipt_number (SupplierInvoices.jsx
    // tab 1); receiptOrderNumber never existed, so this rename is what actually makes the
    // "So Phieu Nhap" column render instead of showing blank. Other fields here stay plain
    // camelCase on purpose - the same frontend tab already reads those correctly as-is.
    @JsonProperty("receipt_number")
    private String receiptNumber;
    private Long supplierId;
    private String supplierName;
    private Long warehouseId;
    private OffsetDateTime completedAt;
    private BigDecimal totalAmountEstimate;
    private String invoiceStatus;
    private String status;
    private String recipientRole;
    private OffsetDateTime readAt;
    private OffsetDateTime createdAt;
}
