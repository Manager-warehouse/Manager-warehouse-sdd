package com.wms.controller;

import com.wms.dto.request.TripCancelRequest;
import com.wms.dto.request.TripCompleteRequest;
import com.wms.dto.request.TripCreateRequest;
import com.wms.dto.request.TripDepartRequest;
import com.wms.dto.request.TripUpdateRequest;
import com.wms.dto.response.TripResponse;
import com.wms.entity.User;
import com.wms.service.CurrentUserService;
import com.wms.service.TripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
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
@RequestMapping("/api/v1/trips")
@Tag(name = "Trips", description = "Outbound trip dispatch workflow")
public class TripController {

    private final TripService tripService;
    private final CurrentUserService currentUserService;

    public TripController(TripService tripService, CurrentUserService currentUserService) {
        this.tripService = tripService;
        this.currentUserService = currentUserService;
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
    public TripResponse completeTrip(@PathVariable Long id,
                                     @Valid @RequestBody TripCompleteRequest request) {
        return tripService.completeTrip(id, request, currentUser());
    }

    private User currentUser() {
        return currentUserService.getRequiredCurrentUser();
    }
}
