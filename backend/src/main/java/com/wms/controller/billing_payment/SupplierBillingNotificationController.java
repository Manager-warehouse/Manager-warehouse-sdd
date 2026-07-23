package com.wms.controller.billing_payment;

import com.wms.dto.response.SupplierBillingNotificationResponse;
import com.wms.entity.access_control.User;
import com.wms.repository.UserRepository;
import com.wms.service.billing_payment.SupplierBillingNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/supplier-billing-notifications")
@RequiredArgsConstructor
public class SupplierBillingNotificationController {

    private final SupplierBillingNotificationService supplierBillingNotificationService;
    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ACCOUNTANT_MANAGER', 'ADMIN')")
    public ResponseEntity<List<SupplierBillingNotificationResponse>> getPendingNotifications(Principal principal) {
        User actor = getActor(principal);
        return ResponseEntity.ok(supplierBillingNotificationService.getPendingNotifications(actor));
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ACCOUNTANT_MANAGER', 'ADMIN')")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id, Principal principal) {
        User actor = getActor(principal);
        supplierBillingNotificationService.markAsRead(id, actor);
        return ResponseEntity.ok().build();
    }

    private User getActor(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
        }
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS"));
    }
}
