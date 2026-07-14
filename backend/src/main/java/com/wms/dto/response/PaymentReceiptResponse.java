package com.wms.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wms.enums.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentReceiptResponse {

    private Long id;

    @JsonProperty("payment_number")
    private String paymentNumber;

    @JsonProperty("dealer_id")
    private Long dealerId;

    @JsonProperty("dealer_name")
    private String dealerName;

    @JsonProperty("invoice_id")
    private Long invoiceId;

    @JsonProperty("invoice_number")
    private String invoiceNumber;

    private BigDecimal amount;

    @JsonProperty("payment_date")
    private LocalDate paymentDate;

    @JsonProperty("payment_method")
    private PaymentMethod paymentMethod;

    @JsonProperty("created_by")
    private Long createdById;

    @JsonProperty("created_by_name")
    private String createdByName;

    @JsonProperty("document_date")
    private LocalDate documentDate;

    @JsonProperty("accounting_period_id")
    private Long accountingPeriodId;

    @JsonProperty("accounting_period_name")
    private String accountingPeriodName;

    private String notes;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;
}
