package com.wms.service.order_fulfillment;

import com.wms.dto.request.SplitDeliveryPlanCreateRequest;
import com.wms.dto.request.SplitDeliveryPlanUpdateRequest;
import com.wms.dto.request.SplitLegFailureRequest;
import com.wms.dto.request.TripCompleteRequest;
import com.wms.dto.response.SplitLegMilestoneResponse;
import com.wms.dto.response.SplitDeliveryPlanResponse;
import com.wms.entity.access_control.User;

public interface SplitDeliveryPlanService {
    SplitDeliveryPlanResponse createPlan(SplitDeliveryPlanCreateRequest request, User actor);

    SplitDeliveryPlanResponse updatePlan(Long id, SplitDeliveryPlanUpdateRequest request, User actor);

    SplitDeliveryPlanResponse cancelPlan(Long id, String cancelReason, User actor);

    SplitDeliveryPlanResponse departPlan(Long id, User actor);

    SplitLegMilestoneResponse confirmDealerArrival(Long planId, User actor);

    SplitLegMilestoneResponse confirmHandover(Long planId, User actor);

    SplitLegMilestoneResponse failDelivery(Long planId, SplitLegFailureRequest request, User actor);

    SplitLegMilestoneResponse completePlan(Long planId, TripCompleteRequest request, User actor);
}
