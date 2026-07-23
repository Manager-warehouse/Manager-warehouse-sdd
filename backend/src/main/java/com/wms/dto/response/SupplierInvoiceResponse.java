package com.wms.dto.response;

import com.wms.enums.billing_payment.InvoiceStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierInvoiceResponse {
    private Long id;
    private String invoiceNumber;
    private String supplierInvoiceNumber;
    private Long receiptId;
    private String receiptNumber;
    private Long supplierId;
    private String supplierName;
    private BigDecimal totalAmount;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private InvoiceStatus status;
    private Long accountingPeriodId;
    private LocalDate documentDate;
    private String createdByName;
    private OffsetDateTime createdAt;
}
