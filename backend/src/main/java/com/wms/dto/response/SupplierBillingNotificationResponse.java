package com.wms.dto.response;

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
