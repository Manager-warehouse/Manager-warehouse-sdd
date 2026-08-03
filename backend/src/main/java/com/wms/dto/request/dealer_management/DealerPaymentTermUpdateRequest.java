package com.wms.dto.request.dealer_management;


import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DealerPaymentTermUpdateRequest {

    @NotNull
    private Integer paymentTermDays;
}
