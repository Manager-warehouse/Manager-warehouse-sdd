package com.wms.controller.order_fulfillment;

import com.wms.dto.request.SplitDeliveryPlanCreateRequest;
import com.wms.dto.request.SplitDeliveryPlanCancelRequest;
import com.wms.dto.request.SplitDeliveryPlanUpdateRequest;
import com.wms.dto.request.SplitLegFailureRequest;
import com.wms.dto.request.TripCompleteRequest;
import com.wms.dto.response.SplitLegMilestoneResponse;
import com.wms.dto.response.SplitDeliveryPlanResponse;
import com.wms.entity.access_control.User;
import com.wms.service.order_fulfillment.SplitDeliveryPlanService;
import com.wms.service.user_context.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/split-delivery-plans")
@Tag(name = "Split Delivery Plans", description = "One Delivery Order dispatched by multiple vehicles")
public class SplitDeliveryPlanController {

    private final SplitDeliveryPlanService splitDeliveryPlanService;
    private final CurrentUserService currentUserService;

    public SplitDeliveryPlanController(SplitDeliveryPlanService splitDeliveryPlanService,
            CurrentUserService currentUserService) {
        this.splitDeliveryPlanService = splitDeliveryPlanService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('DISPATCHER')")
    @Operation(summary = "Create a split delivery plan for one Delivery Order")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Split delivery plan created",
                    content = @Content(schema = @Schema(implementation = SplitDeliveryPlanResponse.class))),
            @ApiResponse(responseCode = "409", description = "Delivery Order is already assigned", content = @Content),
            @ApiResponse(responseCode = "422", description = "Allocation or vehicle readiness rule failed", content = @Content)
    })
    public SplitDeliveryPlanResponse createPlan(@Valid @RequestBody SplitDeliveryPlanCreateRequest request) {
        return splitDeliveryPlanService.createPlan(request, currentUser());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DISPATCHER')")
    @Operation(summary = "Update a planned split delivery plan before departure")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Split delivery plan updated",
                    content = @Content(schema = @Schema(implementation = SplitDeliveryPlanResponse.class))),
            @ApiResponse(responseCode = "422", description = "Split delivery plan is not editable", content = @Content)
    })
    public SplitDeliveryPlanResponse updatePlan(@PathVariable Long id,
            @Valid @RequestBody SplitDeliveryPlanUpdateRequest request) {
        return splitDeliveryPlanService.updatePlan(id, request, currentUser());
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('DISPATCHER')")
    @Operation(summary = "Cancel a planned split delivery plan before departure")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Split delivery plan cancelled",
                    content = @Content(schema = @Schema(implementation = SplitDeliveryPlanResponse.class))),
            @ApiResponse(responseCode = "422", description = "Split delivery plan is not cancellable", content = @Content)
    })
    public SplitDeliveryPlanResponse cancelPlan(@PathVariable Long id,
            @Valid @RequestBody SplitDeliveryPlanCancelRequest request) {
        return splitDeliveryPlanService.cancelPlan(id, request.getCancelReason(), currentUser());
    }

    @PutMapping("/{id}/depart")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Lead driver confirms coordinated departure for every split delivery leg")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Split delivery plan departed",
                    content = @Content(schema = @Schema(implementation = SplitDeliveryPlanResponse.class))),
            @ApiResponse(responseCode = "403", description = "Authenticated driver is not the lead driver", content = @Content),
            @ApiResponse(responseCode = "422", description = "Split resources or Delivery Order are not ready", content = @Content)
    })
    public SplitDeliveryPlanResponse departPlan(@PathVariable Long id) {
        return splitDeliveryPlanService.departPlan(id, currentUser());
    }

    @PutMapping("/{planId}/dealer-arrival")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Lead driver confirms the whole split convoy arrived at the dealer")
    public SplitLegMilestoneResponse confirmDealerArrival(@PathVariable Long planId) {
        return splitDeliveryPlanService.confirmDealerArrival(planId, currentUser());
    }

    @PutMapping("/{planId}/handover")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Lead driver confirms whole split Delivery Order handover")
    public SplitLegMilestoneResponse confirmHandover(@PathVariable Long planId) {
        return splitDeliveryPlanService.confirmHandover(planId, currentUser());
    }

    @PutMapping("/{planId}/fail-delivery")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Lead driver reports failure for the whole split Delivery Order")
    public SplitLegMilestoneResponse failDelivery(@PathVariable Long planId,
            @Valid @RequestBody SplitLegFailureRequest request) {
        return splitDeliveryPlanService.failDelivery(planId, request, currentUser());
    }

    @PutMapping("/{planId}/complete")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Lead driver confirms the whole split convoy returned to the source warehouse")
    public SplitLegMilestoneResponse completePlan(@PathVariable Long planId,
            @Valid @RequestBody(required = false) TripCompleteRequest request) {
        return splitDeliveryPlanService.completePlan(planId, request, currentUser());
    }

    private User currentUser() {
        return currentUserService.getRequiredCurrentUser();
    }
}
