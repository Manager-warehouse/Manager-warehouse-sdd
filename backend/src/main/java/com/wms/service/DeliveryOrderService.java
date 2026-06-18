package com.wms.service;

import com.wms.dto.request.DeliveryOrderCreateRequest;
import com.wms.dto.request.DeliveryOrderCancelRequest;
import com.wms.dto.request.DeliveryOrderUpdateRequest;
import com.wms.dto.response.DeliveryOrderResponse;
import com.wms.entity.User;
import java.util.List;

public interface DeliveryOrderService {
    List<DeliveryOrderResponse> getAllDeliveryOrders();
    DeliveryOrderResponse getDeliveryOrderById(Long id);
    DeliveryOrderResponse createDeliveryOrder(DeliveryOrderCreateRequest request, User actor);
    DeliveryOrderResponse updateDeliveryOrder(Long id, DeliveryOrderUpdateRequest request, User actor);
    DeliveryOrderResponse cancelDeliveryOrder(Long id, DeliveryOrderCancelRequest request, User actor);
}
