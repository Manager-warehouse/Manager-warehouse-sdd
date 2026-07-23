package com.wms.dto.request;

import com.wms.enums.order_fulfillment.ReturnedGoodsQcDecision;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReturnedGoodsApprovalRequest {
    @NotNull
    private ReturnedGoodsQcDecision decision;

    private String rejectionReason;

    private String notes;
}
