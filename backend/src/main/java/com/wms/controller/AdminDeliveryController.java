package com.wms.controller;

import com.wms.dto.request.ResetDeliveryOtpRequest;
import com.wms.dto.response.DeliveryOtpResponse;
import com.wms.entity.User;
import com.wms.service.CurrentUserService;
import com.wms.service.DriverDeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/delivery-orders")
@Tag(name = "Admin Delivery", description = "Admin delivery support actions")
public class AdminDeliveryController {

    private final DriverDeliveryService driverDeliveryService;
    private final CurrentUserService currentUserService;

    public AdminDeliveryController(DriverDeliveryService driverDeliveryService,
                                   CurrentUserService currentUserService) {
        this.driverDeliveryService = driverDeliveryService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/{doId}/delivery-otp/reset")
    @PreAuthorize("hasAnyRole('ADMIN','CEO')")
    @Operation(summary = "Reset a locked delivery OTP")
    public DeliveryOtpResponse resetDeliveryOtp(@PathVariable Long doId,
                                                @Valid @RequestBody ResetDeliveryOtpRequest request) {
        return driverDeliveryService.resetDeliveryOtp(doId, request, currentUser());
    }

    private User currentUser() {
        return currentUserService.getRequiredCurrentUser();
    }
}
