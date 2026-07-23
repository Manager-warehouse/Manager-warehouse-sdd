package com.wms.controller.billing_payment;

import com.wms.dto.request.CreateSupplierPaymentRequest;
import com.wms.dto.response.SupplierPaymentResponse;
import com.wms.entity.access_control.User;
import com.wms.repository.UserRepository;
import com.wms.service.billing_payment.SupplierPaymentService;
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
@RequestMapping("/api/v1/supplier-payments")
@RequiredArgsConstructor
public class SupplierPaymentController {

    private final SupplierPaymentService supplierPaymentService;
    private final UserRepository userRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ADMIN')")
    public ResponseEntity<SupplierPaymentResponse> createSupplierPayment(
            @Valid @RequestBody CreateSupplierPaymentRequest request,
            Principal principal) {
        User actor = getActor(principal);
        SupplierPaymentResponse response = supplierPaymentService.createSupplierPayment(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ACCOUNTANT_MANAGER', 'ADMIN', 'CEO')")
    public ResponseEntity<SupplierPaymentResponse> getSupplierPaymentById(
            @PathVariable Long id,
            Principal principal) {
        User actor = getActor(principal);
        return ResponseEntity.ok(supplierPaymentService.getSupplierPaymentById(id, actor));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ACCOUNTANT_MANAGER', 'ADMIN', 'CEO')")
    public ResponseEntity<List<SupplierPaymentResponse>> getSupplierPayments(
            @RequestParam(required = false, name = "supplierId") Long supplierId,
            @RequestParam(required = false, name = "invoiceId") Long invoiceId,
            Principal principal) {
        User actor = getActor(principal);
        return ResponseEntity.ok(supplierPaymentService.getSupplierPayments(supplierId, invoiceId, actor));
    }

    @PostMapping("/ocr")
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ADMIN')")
    public ResponseEntity<com.wms.dto.response.SupplierPaymentOcrResponse> scanSupplierPaymentOcr(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            Principal principal) {
        User actor = getActor(principal);
        return ResponseEntity.ok(supplierPaymentService.scanSupplierPaymentOcr(file, actor));
    }

    private User getActor(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
        }
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS"));
    }
}
