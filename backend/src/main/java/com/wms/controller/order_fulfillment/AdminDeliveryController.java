package com.wms.controller.order_fulfillment;


import com.wms.entity.access_control.*;
import com.wms.entity.audit_trail.*;
import com.wms.entity.billing_payment.*;
import com.wms.entity.dealer_management.*;
import com.wms.entity.document_numbering.*;
import com.wms.entity.driver_management.*;
import com.wms.entity.fleet_management.*;
import com.wms.entity.notification_delivery.*;
import com.wms.entity.order_fulfillment.*;
import com.wms.entity.price_management.*;
import com.wms.entity.product_catalog.*;
import com.wms.entity.stock_control.*;
import com.wms.entity.stock_counting.*;
import com.wms.entity.stock_receiving.*;
import com.wms.entity.supplier_management.*;
import com.wms.entity.user_configuration.*;
import com.wms.entity.warehouse_location.*;
import com.wms.entity.warehouse_transfer.*;
import com.wms.enums.access_control.*;
import com.wms.enums.audit_trail.*;
import com.wms.enums.billing_payment.*;
import com.wms.enums.dealer_management.*;
import com.wms.enums.driver_management.*;
import com.wms.enums.fleet_management.*;
import com.wms.enums.notification_delivery.*;
import com.wms.enums.order_fulfillment.*;
import com.wms.enums.price_management.*;
import com.wms.enums.stock_control.*;
import com.wms.enums.stock_counting.*;
import com.wms.enums.stock_receiving.*;
import com.wms.enums.supplier_management.*;
import com.wms.enums.user_configuration.*;
import com.wms.enums.warehouse_location.*;
import com.wms.enums.warehouse_transfer.*;
import com.wms.dto.request.ResetDeliveryOtpRequest;
import com.wms.dto.response.DeliveryOtpResponse;
import com.wms.entity.access_control.User;
import com.wms.service.user_context.CurrentUserService;
import com.wms.service.order_fulfillment.DriverDeliveryService;
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
