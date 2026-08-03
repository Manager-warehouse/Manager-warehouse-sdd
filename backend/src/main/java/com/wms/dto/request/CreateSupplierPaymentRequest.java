package com.wms.dto.request;

import com.wms.enums.billing_payment.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSupplierPaymentRequest {

    @NotNull(message = "Supplier ID is required")
    private Long supplierId;

    @NotNull(message = "Supplier invoice ID is required")
    private Long supplierInvoiceId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Payment amount must be greater than 0")
    private BigDecimal amount;

    // Sole date on this request - drives both the payment record and which accounting
    // period it posts into (matches how PaymentReceiptCreateRequest works for AR; there
    // is no separate "document date" hidden behind this one).
    @NotNull(message = "Payment date is required")
    private LocalDate paymentDate;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    private String notes;
}
