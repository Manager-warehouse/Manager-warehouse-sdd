package com.wms.dto.response;

import com.wms.enums.order_fulfillment.DeliveryOrderStatus;
import com.wms.enums.order_fulfillment.ReturnedDeliveryFlowStatus;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReturnedGoodsFlowResponse {
    private Long doId;
    private String doNumber;
    private DeliveryOrderStatus deliveryOrderStatus;
    private ReturnedDeliveryFlowStatus flowStatus;
    private List<ReturnedGoodsFlowItemResponse> items;
}
