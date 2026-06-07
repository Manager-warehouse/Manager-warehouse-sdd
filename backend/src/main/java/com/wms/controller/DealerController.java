package com.wms.controller;

import com.wms.dto.request.DealerCreateRequest;
import com.wms.dto.request.DealerCreditLimitUpdateRequest;
import com.wms.dto.request.DealerCreditStatusUpdateRequest;
import com.wms.dto.request.DealerPaymentTermUpdateRequest;
import com.wms.dto.request.DealerUpdateRequest;
import com.wms.dto.response.DealerResponse;
import com.wms.entity.User;
import com.wms.service.CurrentUserService;
import com.wms.service.DealerService;
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
    @PreAuthorize("hasRole('ACCOUNTANT')")
    @Operation(summary = "Create dealer")
    public DealerResponse createDealer(@Valid @RequestBody DealerCreateRequest request) {
        return dealerService.createDealer(request, currentUser());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ACCOUNTANT')")
    @Operation(summary = "Update dealer profile")
    public DealerResponse updateDealer(@PathVariable Long id,
                                       @Valid @RequestBody DealerUpdateRequest request) {
        return dealerService.updateDealer(id, request, currentUser());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ACCOUNTANT')")
    @Operation(summary = "Deactivate dealer")
    public void deactivateDealer(@PathVariable Long id) {
        dealerService.deactivateDealer(id, currentUser());
    }

    @PutMapping("/{id}/reactivate")
    @PreAuthorize("hasRole('ACCOUNTANT')")
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
