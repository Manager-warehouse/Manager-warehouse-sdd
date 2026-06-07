package com.wms.dto.request;

import com.wms.enums.CreditStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DealerCreditStatusUpdateRequest {

    @NotNull
    private CreditStatus creditStatus;
}
