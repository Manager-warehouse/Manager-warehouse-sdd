package com.wms.controller;

import com.wms.dto.request.PaymentReceiptCreateRequest;
import com.wms.dto.response.PaymentReceiptResponse;
import com.wms.entity.User;
import com.wms.repository.UserRepository;
import com.wms.service.PaymentReceiptService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/payment-receipts")
@RequiredArgsConstructor
public class PaymentReceiptController {

    private final PaymentReceiptService paymentReceiptService;
    private final UserRepository userRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ADMIN')")
    public ResponseEntity<PaymentReceiptResponse> createPaymentReceipt(
            @Valid @RequestBody PaymentReceiptCreateRequest request,
            Principal principal) {
        User actor = getActor(principal);
        PaymentReceiptResponse response = paymentReceiptService.createPaymentReceipt(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ACCOUNTANT_MANAGER', 'ADMIN', 'CEO')")
    public ResponseEntity<List<PaymentReceiptResponse>> getPaymentReceipts(
            @RequestParam(required = false, name = "dealerId") Long dealerId,
            @RequestParam(required = false, name = "accountingPeriodId") Long accountingPeriodId,
            Principal principal) {
        User actor = getActor(principal);
        return ResponseEntity.ok(paymentReceiptService.getPaymentReceipts(dealerId, accountingPeriodId, actor));
    }

    private User getActor(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
        }
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS"));
    }
}
