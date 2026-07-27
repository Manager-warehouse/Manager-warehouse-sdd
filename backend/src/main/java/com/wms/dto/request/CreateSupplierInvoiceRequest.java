package com.wms.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSupplierInvoiceRequest {

    @NotNull(message = "Receipt ID is required")
    private Long receiptId;

    @NotBlank(message = "Supplier invoice number is required")
    private String supplierInvoiceNumber;

    @NotNull(message = "Document date is required")
    private LocalDate documentDate;

    private LocalDate dueDate;

    private String notes;

    // Optional. When absent, totalAmount is auto-calculated from receipt_items
    // (unit_cost x actualQty). When present, the Accountant is overriding that
    // calculated estimate with the amount on the supplier's actual paper invoice
    // (they may differ - see feature-accountant-supplier-invoicing.md reconciliation note).
    @Positive(message = "Confirmed total amount must be positive")
    private BigDecimal confirmedTotalAmount;
}
