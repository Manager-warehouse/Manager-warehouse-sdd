package com.wms.controller;

import com.wms.dto.response.CreditAgingReportResponse;
import com.wms.entity.User;
import com.wms.repository.UserRepository;
import com.wms.service.PaymentReceiptService;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/v1/credit")
@RequiredArgsConstructor
public class CreditController {

    private final PaymentReceiptService paymentReceiptService;
    private final UserRepository userRepository;

    @GetMapping("/aging-report")
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ACCOUNTANT_MANAGER', 'ADMIN', 'CEO')")
    public ResponseEntity<List<CreditAgingReportResponse>> getCreditAgingReport(Principal principal) {
        User actor = getActor(principal);
        return ResponseEntity.ok(paymentReceiptService.getCreditAgingReport(actor));
    }

    private User getActor(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
        }
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS"));
    }
}
