package com.wms.service;

import com.wms.dto.request.DeliveryOrderAllocationRequest;
import com.wms.dto.request.DeliveryOrderCreateRequest;
import com.wms.dto.request.DeliveryOrderCancelRequest;
import com.wms.dto.request.DeliveryOrderPickingPlanRequest;
import com.wms.dto.request.DeliveryOrderPickQcResultRequest;
import com.wms.dto.request.DeliveryOrderQualityApprovalRequest;
import com.wms.dto.request.DeliveryOrderReplacementPlanRequest;
import com.wms.dto.request.DeliveryOrderUpdateRequest;
import com.wms.dto.request.DeliveryOrderWarehouseApprovalRequest;
import com.wms.dto.request.DeliveryOrderWarehouseRejectRequest;
import com.wms.dto.response.DeliveryOrderResponse;
import com.wms.dto.response.PickingCandidateResponse;
import com.wms.entity.User;
import java.util.List;
import java.util.Map;

public interface DeliveryOrderService {
    List<DeliveryOrderResponse> getAllDeliveryOrders(User actor);
    Map<Long, List<PickingCandidateResponse>> getPickingCandidates(Long doId, User actor);
    DeliveryOrderResponse getDeliveryOrderById(Long id, User actor);
    DeliveryOrderResponse createDeliveryOrder(DeliveryOrderCreateRequest request, User actor);
    DeliveryOrderResponse updateDeliveryOrder(Long id, DeliveryOrderUpdateRequest request, User actor);
    DeliveryOrderResponse cancelDeliveryOrder(Long id, DeliveryOrderCancelRequest request, User actor);
    DeliveryOrderResponse saveDeliveryOrderPickingPlan(Long id, DeliveryOrderPickingPlanRequest request, User actor);
    DeliveryOrderResponse saveDeliveryOrderPickQcResult(Long id, DeliveryOrderPickQcResultRequest request, User actor);
    DeliveryOrderResponse saveDeliveryOrderReplacementPlan(Long id, DeliveryOrderReplacementPlanRequest request, User actor);
    DeliveryOrderResponse approveDeliveryOrderQuality(Long id, DeliveryOrderQualityApprovalRequest request, User actor);
    DeliveryOrderResponse approveDeliveryOrderWarehouseRelease(Long id, DeliveryOrderWarehouseApprovalRequest request, User actor);
    DeliveryOrderResponse rejectDeliveryOrderWarehouseRelease(Long id, DeliveryOrderWarehouseRejectRequest request, User actor);
}
