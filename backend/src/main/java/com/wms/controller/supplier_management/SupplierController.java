package com.wms.controller.supplier_management;


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
import com.wms.dto.request.supplier_management.SupplierCreateRequest;
import com.wms.dto.request.supplier_management.SupplierUpdateRequest;
import com.wms.dto.response.supplier_management.SupplierReceivedOrderDetailResponse;
import com.wms.dto.response.supplier_management.SupplierReceivedOrderResponse;
import com.wms.dto.response.supplier_management.SupplierResponse;
import com.wms.entity.access_control.User;
import com.wms.service.user_context.CurrentUserService;
import com.wms.service.supplier_management.SupplierService;
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
