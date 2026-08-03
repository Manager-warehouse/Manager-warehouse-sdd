package com.wms.controller.notification_delivery;


import com.wms.dto.response.BillingNotificationResponse;
import com.wms.entity.access_control.User;
import com.wms.repository.UserRepository;
import com.wms.service.notification_delivery.BillingNotificationService;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/billing-notifications")
@RequiredArgsConstructor
public class BillingNotificationController {

    private final BillingNotificationService billingNotificationService;
    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ACCOUNTANT_MANAGER', 'ADMIN', 'CEO')")
    public ResponseEntity<List<BillingNotificationResponse>> getActiveNotifications(Principal principal) {
        User actor = getActor(principal);
        return ResponseEntity.ok(billingNotificationService.getActiveNotifications(actor));
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ADMIN')")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long id,
            Principal principal) {
        User actor = getActor(principal);
        billingNotificationService.markAsRead(id, actor);
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
