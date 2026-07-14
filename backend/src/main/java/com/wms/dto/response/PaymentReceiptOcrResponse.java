package com.wms.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
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
public class PaymentReceiptOcrResponse {

    private BigDecimal amount;

    @JsonProperty("payment_date")
    private LocalDate paymentDate;

    @JsonProperty("dealer_id")
    private Long dealerId;

    private String notes;

    @JsonProperty("confidence_score")
    private Double confidenceScore;
}
