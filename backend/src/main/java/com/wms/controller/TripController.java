package com.wms.controller;

import com.wms.dto.request.TripCancelRequest;
import com.wms.dto.request.TripCompleteRequest;
import com.wms.dto.request.TripCreateRequest;
import com.wms.dto.request.TripDepartRequest;
import com.wms.dto.request.TripUpdateRequest;
import com.wms.dto.request.ConfirmDeliveryRequest;
import com.wms.dto.request.DeliveryOtpRequest;
import com.wms.dto.request.FailDeliveryRequest;
import com.wms.dto.response.DeliveryAttemptResponse;
import com.wms.dto.response.DeliveryOtpResponse;
import com.wms.dto.response.TripDriverViewResponse;
import com.wms.dto.response.TripResponse;
import com.wms.entity.User;
import com.wms.enums.TripStatus;
import com.wms.service.CurrentUserService;
import com.wms.service.DriverDeliveryService;
import com.wms.service.TripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/trips")
@Tag(name = "Trips", description = "Outbound trip dispatch workflow")
public class TripController {

    private final TripService tripService;
    private final DriverDeliveryService driverDeliveryService;
    private final CurrentUserService currentUserService;

    public TripController(TripService tripService,
                          DriverDeliveryService driverDeliveryService,
                          CurrentUserService currentUserService) {
        this.tripService = tripService;
        this.driverDeliveryService = driverDeliveryService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DISPATCHER','WAREHOUSE_MANAGER','ADMIN','CEO')")
    @Operation(summary = "List outbound trips for dispatch planning")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trips returned for assigned warehouse scope"),
            @ApiResponse(responseCode = "403", description = "User is not assigned to the requested warehouse", content = @Content)
    })
    public List<TripResponse> listTrips(@RequestParam(required = false) Long warehouseId,
                                        @RequestParam(required = false) TripStatus status) {
        return tripService.listTrips(warehouseId, status, currentUser());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Get assigned trip detail for driver mobile")
    public TripDriverViewResponse getDriverTrip(@PathVariable Long id) {
        return driverDeliveryService.getAssignedTrip(id, currentUser());
    }

    @GetMapping("/driver")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "List assigned trips for driver mobile",
            description = "Returns mixed DELIVERY and TRANSFER trip summaries assigned to the authenticated driver. "
                    + "TRANSFER rows include source/destination warehouse codes and transfer line count; "
                    + "DELIVERY rows include dealer delivery stop count and POD/OTP delivery orders."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assigned driver trips returned",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = TripDriverViewResponse.class)))),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not a driver", content = @Content)
    })
    public List<TripDriverViewResponse> listDriverTrips() {
        return driverDeliveryService.listMyTrips(currentUser());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('DISPATCHER')")
    @Operation(summary = "Create a planned outbound trip")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Trip created in PLANNED status"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload", content = @Content),
            @ApiResponse(responseCode = "403", description = "Dispatcher is not assigned to the trip warehouse", content = @Content),
            @ApiResponse(responseCode = "409", description = "Active-trip conflict", content = @Content),
            @ApiResponse(responseCode = "422", description = "Trip validation failed", content = @Content)
    })
    public TripResponse createTrip(@Valid @RequestBody TripCreateRequest request) {
        return tripService.createTrip(request, currentUser());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DISPATCHER')")
    @Operation(summary = "Update a planned outbound trip")
    public TripResponse updateTrip(@PathVariable Long id,
                                   @Valid @RequestBody TripUpdateRequest request) {
        return tripService.updateTrip(id, request, currentUser());
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('DISPATCHER')")
    @Operation(summary = "Cancel a planned outbound trip")
    public TripResponse cancelTrip(@PathVariable Long id,
                                   @Valid @RequestBody TripCancelRequest request) {
        return tripService.cancelTrip(id, request, currentUser());
    }

    @PutMapping("/{id}/depart")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Confirm trip departure and dispatch staged goods")
    public TripResponse departTrip(@PathVariable Long id,
                                   @Valid @RequestBody TripDepartRequest request) {
        return tripService.departTrip(id, request, currentUser());
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Confirm vehicle return and complete the trip")
    public TripDriverViewResponse completeTrip(@PathVariable Long id,
                                               @Valid @RequestBody TripCompleteRequest request) {
        return driverDeliveryService.completeTrip(id, request, currentUser());
    }

    @PostMapping("/{tripId}/delivery-orders/{doId}/pod-evidence")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Upload POD evidence for a delivery order")
    public DeliveryAttemptResponse uploadPodEvidence(@PathVariable Long tripId,
                                                     @PathVariable Long doId,
                                                     @RequestParam MultipartFile goodsImage,
                                                     @RequestParam MultipartFile signDocumentImage,
                                                     @RequestParam(required = false) String notes) {
        return driverDeliveryService.uploadPodEvidence(
                tripId, doId, goodsImage, signDocumentImage, notes, currentUser());
    }

    @PostMapping("/{tripId}/delivery-orders/{doId}/delivery-otp")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Request or resend delivery OTP")
    public DeliveryOtpResponse requestDeliveryOtp(@PathVariable Long tripId,
                                                  @PathVariable Long doId,
                                                  @Valid @RequestBody DeliveryOtpRequest request) {
        return driverDeliveryService.requestDeliveryOtp(tripId, doId, request, currentUser());
    }

    @PutMapping("/{tripId}/delivery-orders/{doId}/confirm-delivery")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Confirm delivery with dealer OTP")
    public DeliveryAttemptResponse confirmDelivery(@PathVariable Long tripId,
                                                   @PathVariable Long doId,
                                                   @Valid @RequestBody ConfirmDeliveryRequest request) {
        return driverDeliveryService.confirmDelivery(tripId, doId, request, currentUser());
    }

    @PutMapping("/{tripId}/delivery-orders/{doId}/fail-delivery")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Record failed or refused delivery")
    public DeliveryAttemptResponse failDelivery(@PathVariable Long tripId,
                                                @PathVariable Long doId,
                                                @Valid @RequestBody FailDeliveryRequest request) {
        return driverDeliveryService.failDelivery(tripId, doId, request, currentUser());
    }

    private User currentUser() {
        return currentUserService.getRequiredCurrentUser();
    }
}
