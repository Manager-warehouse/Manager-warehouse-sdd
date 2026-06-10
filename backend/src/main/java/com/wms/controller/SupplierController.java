package com.wms.controller;

import com.wms.dto.request.SupplierCreateRequest;
import com.wms.dto.request.SupplierUpdateRequest;
import com.wms.dto.response.SupplierReceivedOrderDetailResponse;
import com.wms.dto.response.SupplierReceivedOrderResponse;
import com.wms.dto.response.SupplierResponse;
import com.wms.entity.User;
import com.wms.service.CurrentUserService;
import com.wms.service.SupplierService;
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
@RequestMapping("/api/v1/suppliers")
@Tag(name = "Suppliers", description = "Supplier master data and received-order history")
public class SupplierController {

    private final SupplierService supplierService;
    private final CurrentUserService currentUserService;

    public SupplierController(SupplierService supplierService,
                              CurrentUserService currentUserService) {
        this.supplierService = supplierService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    @Operation(summary = "List suppliers")
    public List<SupplierResponse> getAllSuppliers() {
        return supplierService.getAllSuppliers();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get supplier detail")
    public SupplierResponse getSupplierById(@PathVariable Long id) {
        return supplierService.getSupplierById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ACCOUNTANT_MANAGER')")
    @Operation(summary = "Create supplier")
    public SupplierResponse createSupplier(@Valid @RequestBody SupplierCreateRequest request) {
        return supplierService.createSupplier(request, currentUser());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ACCOUNTANT_MANAGER')")
    @Operation(summary = "Update supplier")
    public SupplierResponse updateSupplier(@PathVariable Long id,
                                           @Valid @RequestBody SupplierUpdateRequest request) {
        return supplierService.updateSupplier(id, request, currentUser());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ACCOUNTANT_MANAGER')")
    @Operation(summary = "Deactivate supplier")
    public void deactivateSupplier(@PathVariable Long id) {
        supplierService.deactivateSupplier(id, currentUser());
    }

    @PutMapping("/{id}/reactivate")
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ACCOUNTANT_MANAGER')")
    @Operation(summary = "Reactivate supplier")
    public SupplierResponse reactivateSupplier(@PathVariable Long id) {
        return supplierService.reactivateSupplier(id, currentUser());
    }

    @GetMapping("/{id}/received-orders")
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ACCOUNTANT_MANAGER', 'ADMIN', 'CEO', 'WAREHOUSE_MANAGER')")
    @Operation(summary = "List supplier received orders")
    public List<SupplierReceivedOrderResponse> getReceivedOrders(@PathVariable Long id) {
        return supplierService.getReceivedOrders(id);
    }

    @GetMapping("/{id}/received-orders/{orderId}")
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ACCOUNTANT_MANAGER', 'ADMIN', 'CEO', 'WAREHOUSE_MANAGER')")
    @Operation(summary = "Get supplier received order detail")
    public SupplierReceivedOrderDetailResponse getReceivedOrderDetail(@PathVariable Long id,
                                                                      @PathVariable Long orderId) {
        return supplierService.getReceivedOrderDetail(id, orderId);
    }

    private User currentUser() {
        return currentUserService.getRequiredCurrentUser();
    }
}
