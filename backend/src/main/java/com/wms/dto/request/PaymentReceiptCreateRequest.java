package com.wms.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wms.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentReceiptCreateRequest {

    @NotNull(message = "Dealer ID is required")
    @JsonProperty("dealer_id")
    private Long dealerId;

    @NotNull(message = "Invoice ID is required")
    @JsonProperty("invoice_id")
    private Long invoiceId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotNull(message = "Payment date is required")
    @JsonProperty("payment_date")
    private LocalDate paymentDate;

    @NotNull(message = "Payment method is required")
    @JsonProperty("payment_method")
    private PaymentMethod paymentMethod;

    private String notes;
}
