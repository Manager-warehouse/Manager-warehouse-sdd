package com.wms.controller.dealer_management;


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
import com.wms.dto.request.dealer_management.DealerCreateRequest;
import com.wms.dto.request.dealer_management.DealerCreditLimitUpdateRequest;
import com.wms.dto.request.dealer_management.DealerCreditStatusUpdateRequest;
import com.wms.dto.request.dealer_management.DealerPaymentTermUpdateRequest;
import com.wms.dto.request.dealer_management.DealerUpdateRequest;
import com.wms.dto.response.dealer_management.DealerResponse;
import com.wms.entity.access_control.User;
import com.wms.service.user_context.CurrentUserService;
import com.wms.service.dealer_management.DealerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dealers")
@Tag(name = "Dealers", description = "Dealer master data and credit controls")
public class DealerController {

    private final DealerService dealerService;
    private final CurrentUserService currentUserService;

    public DealerController(DealerService dealerService,
                            CurrentUserService currentUserService) {
        this.dealerService = dealerService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    @Operation(summary = "List dealers")
    public List<DealerResponse> getAllDealers() {
        return dealerService.getAllDealers();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get dealer detail")
    public DealerResponse getDealerById(@PathVariable Long id) {
        return dealerService.getDealerById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ACCOUNTANT_MANAGER')")
    @Operation(summary = "Create dealer")
    public DealerResponse createDealer(@Valid @RequestBody DealerCreateRequest request) {
        return dealerService.createDealer(request, currentUser());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ACCOUNTANT_MANAGER')")
    @Operation(summary = "Update dealer profile")
    public DealerResponse updateDealer(@PathVariable Long id,
                                       @Valid @RequestBody DealerUpdateRequest request) {
        return dealerService.updateDealer(id, request, currentUser());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ACCOUNTANT_MANAGER')")
    @Operation(summary = "Deactivate dealer")
    public void deactivateDealer(@PathVariable Long id) {
        dealerService.deactivateDealer(id, currentUser());
    }

    @PutMapping("/{id}/reactivate")
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ACCOUNTANT_MANAGER')")
    @Operation(summary = "Reactivate dealer")
    public DealerResponse reactivateDealer(@PathVariable Long id) {
        return dealerService.reactivateDealer(id, currentUser());
    }

    @PutMapping("/{id}/credit-limit")
    @PreAuthorize("hasRole('ACCOUNTANT_MANAGER')")
    @Operation(summary = "Update dealer credit limit")
    public DealerResponse updateCreditLimit(@PathVariable Long id,
                                            @Valid @RequestBody DealerCreditLimitUpdateRequest request) {
        return dealerService.updateCreditLimit(id, request, currentUser());
    }

    @PutMapping("/{id}/payment-term")
    @PreAuthorize("hasRole('ACCOUNTANT_MANAGER')")
    @Operation(summary = "Update dealer payment term")
    public DealerResponse updatePaymentTerm(@PathVariable Long id,
                                            @Valid @RequestBody DealerPaymentTermUpdateRequest request) {
        return dealerService.updatePaymentTerm(id, request, currentUser());
    }

    @PutMapping("/{id}/credit-status")
    @PreAuthorize("hasRole('ACCOUNTANT_MANAGER')")
    @Operation(summary = "Update dealer credit status")
    public DealerResponse updateCreditStatus(@PathVariable Long id,
                                             @Valid @RequestBody DealerCreditStatusUpdateRequest request) {
        return dealerService.updateCreditStatus(id, request, currentUser());
    }

    private User currentUser() {
        return currentUserService.getRequiredCurrentUser();
    }
}
