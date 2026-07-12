package com.wms.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class PriceHistoryUpdateRequest {

    @NotNull
    @JsonProperty("effective_date")
    private LocalDate effectiveDate;

    @NotNull
    @DecimalMin(value = "0.01", message = "cost_price phải lớn hơn 0")
    @JsonProperty("cost_price")
    private BigDecimal costPrice;

    @NotNull
    @DecimalMin(value = "0.01", message = "selling_price phải lớn hơn 0")
    @JsonProperty("selling_price")
    private BigDecimal sellingPrice;

    @JsonProperty("notes")
    private String notes;
}
