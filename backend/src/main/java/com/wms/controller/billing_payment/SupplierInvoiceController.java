package com.wms.controller.billing_payment;

import com.wms.dto.request.CreateSupplierInvoiceRequest;
import com.wms.dto.response.SupplierInvoiceResponse;
import com.wms.entity.access_control.User;
import com.wms.repository.UserRepository;
import com.wms.service.billing_payment.SupplierInvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/supplier-invoices")
@RequiredArgsConstructor
public class SupplierInvoiceController {

    private final SupplierInvoiceService supplierInvoiceService;
    private final UserRepository userRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ADMIN')")
    public ResponseEntity<SupplierInvoiceResponse> createSupplierInvoice(
            @Valid @RequestBody CreateSupplierInvoiceRequest request,
            Principal principal) {
        User actor = getActor(principal);
        SupplierInvoiceResponse response = supplierInvoiceService.createSupplierInvoice(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ACCOUNTANT_MANAGER', 'ADMIN', 'CEO')")
    public ResponseEntity<SupplierInvoiceResponse> getSupplierInvoiceById(
            @PathVariable Long id,
            Principal principal) {
        User actor = getActor(principal);
        return ResponseEntity.ok(supplierInvoiceService.getSupplierInvoiceById(id, actor));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ACCOUNTANT_MANAGER', 'ADMIN', 'CEO')")
    public ResponseEntity<List<SupplierInvoiceResponse>> getSupplierInvoices(
            @RequestParam(required = false, name = "supplierId") Long supplierId,
            @RequestParam(required = false, name = "status") String status,
            Principal principal) {
        User actor = getActor(principal);
        return ResponseEntity.ok(supplierInvoiceService.getSupplierInvoices(supplierId, status, actor));
    }

    private User getActor(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
        }
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS"));
    }
}
