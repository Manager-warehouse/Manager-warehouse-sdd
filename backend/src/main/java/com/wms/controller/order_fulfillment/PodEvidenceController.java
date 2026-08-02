package com.wms.controller.order_fulfillment;

import com.wms.dto.response.PodEvidenceSignedUrlsResponse;
import com.wms.service.order_fulfillment.DriverDeliveryService;
import com.wms.service.user_context.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/delivery-orders")
@Tag(name = "POD Evidence", description = "Authorized access to private proof-of-delivery evidence")
public class PodEvidenceController {

    private final DriverDeliveryService driverDeliveryService;
    private final CurrentUserService currentUserService;

    public PodEvidenceController(DriverDeliveryService driverDeliveryService,
            CurrentUserService currentUserService) {
        this.driverDeliveryService = driverDeliveryService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/{doId}/pod-evidence/signed-urls")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Generate fresh 15-minute signed URLs for private POD evidence")
    public PodEvidenceSignedUrlsResponse getSignedUrls(@PathVariable Long doId) {
        return driverDeliveryService.getPodEvidenceSignedUrls(doId,
                currentUserService.getRequiredCurrentUser());
    }
}
