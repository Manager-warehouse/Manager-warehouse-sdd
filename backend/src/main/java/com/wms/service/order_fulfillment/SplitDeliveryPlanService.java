package com.wms.service.order_fulfillment;

import com.wms.dto.request.SplitDeliveryPlanCreateRequest;
import com.wms.dto.request.SplitDeliveryPlanUpdateRequest;
import com.wms.dto.request.SplitLegFailureRequest;
import com.wms.dto.response.SplitLegMilestoneResponse;
import com.wms.dto.response.SplitDeliveryPlanResponse;
import com.wms.entity.access_control.User;

public interface SplitDeliveryPlanService {
    SplitDeliveryPlanResponse createPlan(SplitDeliveryPlanCreateRequest request, User actor);

    SplitDeliveryPlanResponse updatePlan(Long id, SplitDeliveryPlanUpdateRequest request, User actor);

    SplitDeliveryPlanResponse cancelPlan(Long id, String cancelReason, User actor);

    SplitDeliveryPlanResponse confirmDriverReadiness(Long id, User actor);

    SplitDeliveryPlanResponse departPlan(Long id, User actor);

    SplitLegMilestoneResponse confirmDealerArrival(Long planId, Long legId, User actor);

    SplitLegMilestoneResponse confirmHandover(Long planId, Long legId, User actor);

    SplitLegMilestoneResponse failDeliveryLeg(Long planId, Long legId, SplitLegFailureRequest request, User actor);
}
