package com.wms.dto.request.dealer_management;


import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DealerCreditLimitUpdateRequest {

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal creditLimit;
}
