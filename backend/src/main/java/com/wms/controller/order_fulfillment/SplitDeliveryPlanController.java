package com.wms.controller.order_fulfillment;

import com.wms.dto.request.SplitDeliveryPlanCreateRequest;
import com.wms.dto.request.SplitDeliveryPlanCancelRequest;
import com.wms.dto.request.SplitDeliveryPlanUpdateRequest;
import com.wms.dto.request.SplitLegFailureRequest;
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

    @PutMapping("/{id}/driver-readiness")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Confirm driver readiness for a split delivery leg")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Driver readiness accepted",
                    content = @Content(schema = @Schema(implementation = SplitDeliveryPlanResponse.class))),
            @ApiResponse(responseCode = "403", description = "Driver is not assigned to this split plan", content = @Content)
    })
    public SplitDeliveryPlanResponse confirmDriverReadiness(@PathVariable Long id) {
        return splitDeliveryPlanService.confirmDriverReadiness(id, currentUser());
    }

    @PutMapping("/{planId}/legs/{legId}/dealer-arrival")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Confirm assigned split leg arrived at the dealer")
    public SplitLegMilestoneResponse confirmDealerArrival(@PathVariable Long planId, @PathVariable Long legId) {
        return splitDeliveryPlanService.confirmDealerArrival(planId, legId, currentUser());
    }

    @PutMapping("/{planId}/legs/{legId}/handover")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Confirm assigned split leg handed over goods after every leg arrived")
    public SplitLegMilestoneResponse confirmHandover(@PathVariable Long planId, @PathVariable Long legId) {
        return splitDeliveryPlanService.confirmHandover(planId, legId, currentUser());
    }

    @PutMapping("/{planId}/legs/{legId}/fail-delivery")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Fail the whole split Delivery Order from one assigned leg")
    public SplitLegMilestoneResponse failDeliveryLeg(@PathVariable Long planId, @PathVariable Long legId,
            @Valid @RequestBody SplitLegFailureRequest request) {
        return splitDeliveryPlanService.failDeliveryLeg(planId, legId, request, currentUser());
    }

    private User currentUser() {
        return currentUserService.getRequiredCurrentUser();
    }
}
