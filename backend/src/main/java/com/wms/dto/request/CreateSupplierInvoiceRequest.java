package com.wms.dto.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSupplierInvoiceRequest {

    @NotNull(message = "Receipt ID is required")
    private Long receiptId;

    // Matches supplier_invoices.supplier_invoice_number VARCHAR(100) - reject before it
    // becomes a raw DB error, and this field can never be edited after creation, so a bad
    // value is permanent (see feature-accountant-supplier-invoicing.md reconciliation note).
    @NotBlank(message = "Supplier invoice number is required")
    @Size(max = 100, message = "SUPPLIER_INVOICE_NUMBER_TOO_LONG")
    private String supplierInvoiceNumber;

    @NotNull(message = "Document date is required")
    private LocalDate documentDate;

    private LocalDate dueDate;

    // Optional. When absent, totalAmount is auto-calculated from receipt_items
    // (unit_cost x actualQty). When present, the Accountant is overriding that
    // calculated estimate with the amount on the supplier's actual paper invoice
    // (they may differ - see feature-accountant-supplier-invoicing.md reconciliation note).
    // Matches supplier_invoices.total_amount NUMERIC(18,2). Unlike payment amounts (which
    // are bounded by the invoice's remaining balance), nothing else naturally caps this -
    // it's a permanent, never-editable field, so reject an absurd value up front.
    @Positive(message = "Confirmed total amount must be positive")
    @Digits(integer = 16, fraction = 2, message = "CONFIRMED_TOTAL_AMOUNT_TOO_LARGE")
    private BigDecimal confirmedTotalAmount;
}
