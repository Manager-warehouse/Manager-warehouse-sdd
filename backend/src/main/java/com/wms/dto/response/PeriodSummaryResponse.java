package com.wms.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wms.enums.billing_payment.AccountingPeriodStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Compiled AR/AP/COGS/pricing view for a single accounting period - the "what actually
// happened financially this period" report that closing a period alone doesn't provide.
// Correction vouchers are deliberately not included here: the existing GET
// /correction-vouchers response already carries original_period_id, and the frontend
// already knows how to filter by it (PeriodClosing.jsx), so this endpoint stays focused
// on the four document types that carry a direct accounting_period_id FK plus COGS/pricing.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeriodSummaryResponse {

    @JsonProperty("period_id")
    private Long periodId;

    @JsonProperty("period_name")
    private String periodName;

    @JsonProperty("start_date")
    private LocalDate startDate;

    @JsonProperty("end_date")
    private LocalDate endDate;

    private AccountingPeriodStatus status;

    @JsonProperty("invoice_count")
    private int invoiceCount;

    @JsonProperty("invoice_total")
    private BigDecimal invoiceTotal;

    private List<InvoiceResponse> invoices;

    @JsonProperty("payment_count")
    private int paymentCount;

    @JsonProperty("payment_total")
    private BigDecimal paymentTotal;

    private List<PaymentReceiptResponse> payments;

    @JsonProperty("supplier_invoice_count")
    private int supplierInvoiceCount;

    @JsonProperty("supplier_invoice_total")
    private BigDecimal supplierInvoiceTotal;

    @JsonProperty("supplier_invoices")
    private List<SupplierInvoiceResponse> supplierInvoices;

    @JsonProperty("supplier_payment_count")
    private int supplierPaymentCount;

    @JsonProperty("supplier_payment_total")
    private BigDecimal supplierPaymentTotal;

    @JsonProperty("supplier_payments")
    private List<SupplierPaymentResponse> supplierPayments;

    private BigDecimal cogs;

    @JsonProperty("gross_margin")
    private BigDecimal grossMargin;

    @JsonProperty("price_change_count")
    private int priceChangeCount;

    @JsonProperty("price_changes")
    private List<PriceHistoryResponse> priceChanges;
}
