package com.wms.dto.request;

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
    private LocalDate effectiveDate;

    @NotNull
    private LocalDate endDate;

    @NotNull
    @DecimalMin(value = "0.01", message = "cost_price phải lớn hơn 0")
    private BigDecimal costPrice;

    @NotNull
    @DecimalMin(value = "0.01", message = "selling_price phải lớn hơn 0")
    private BigDecimal sellingPrice;

    private String notes;
}
