package com.wms.dto.request;


import com.wms.enums.order_fulfillment.OutboundQualityDecision;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeliveryOrderQualityApprovalRequest {

    private OutboundQualityDecision decision = OutboundQualityDecision.ACCEPT;

    @Size(max = 1000)
    private String rejectionReason;

    @Size(max = 1000)
    private String notes;
}
