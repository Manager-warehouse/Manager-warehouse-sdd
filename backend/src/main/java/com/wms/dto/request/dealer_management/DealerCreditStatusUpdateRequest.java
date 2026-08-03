package com.wms.dto.request.dealer_management;


import com.wms.enums.dealer_management.CreditStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DealerCreditStatusUpdateRequest {

    @NotNull
    private CreditStatus creditStatus;
}
