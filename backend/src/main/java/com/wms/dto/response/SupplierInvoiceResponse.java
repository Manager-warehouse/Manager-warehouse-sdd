package com.wms.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wms.enums.billing_payment.InvoiceStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

// snake_case wire format matches InvoiceResponse (the AR equivalent) and what
// SupplierInvoices.jsx already reads - see feature-accountant-supplier-invoicing.md.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierInvoiceResponse {
    private Long id;
    @JsonProperty("invoice_number")
    private String invoiceNumber;
    @JsonProperty("supplier_invoice_number")
    private String supplierInvoiceNumber;
    @JsonProperty("receipt_id")
    private Long receiptId;
    @JsonProperty("receipt_number")
    private String receiptNumber;
    @JsonProperty("supplier_id")
    private Long supplierId;
    @JsonProperty("supplier_name")
    private String supplierName;
    @JsonProperty("total_amount")
    private BigDecimal totalAmount;
    @JsonProperty("paid_amount")
    private BigDecimal paidAmount;
    @JsonProperty("issue_date")
    private LocalDate issueDate;
    @JsonProperty("due_date")
    private LocalDate dueDate;
    private InvoiceStatus status;
    @JsonProperty("accounting_period_id")
    private Long accountingPeriodId;
    @JsonProperty("document_date")
    private LocalDate documentDate;
    @JsonProperty("created_by_name")
    private String createdByName;
    @JsonProperty("created_at")
    private OffsetDateTime createdAt;
}
