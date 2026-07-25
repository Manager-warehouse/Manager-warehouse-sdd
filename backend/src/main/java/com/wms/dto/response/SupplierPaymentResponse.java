package com.wms.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wms.enums.billing_payment.PaymentMethod;
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
public class SupplierPaymentResponse {
    private Long id;
    @JsonProperty("payment_number")
    private String paymentNumber;
    @JsonProperty("supplier_id")
    private Long supplierId;
    @JsonProperty("supplier_name")
    private String supplierName;
    @JsonProperty("supplier_invoice_id")
    private Long supplierInvoiceId;
    @JsonProperty("invoice_number")
    private String invoiceNumber;
    private BigDecimal amount;
    @JsonProperty("payment_date")
    private LocalDate paymentDate;
    @JsonProperty("payment_method")
    private PaymentMethod paymentMethod;
    @JsonProperty("accounting_period_id")
    private Long accountingPeriodId;
    @JsonProperty("document_date")
    private LocalDate documentDate;
    private String notes;
    @JsonProperty("created_by_name")
    private String createdByName;
    @JsonProperty("created_at")
    private OffsetDateTime createdAt;
}
