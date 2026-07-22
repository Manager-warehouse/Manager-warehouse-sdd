package com.wms.controller.billing_payment;


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
import com.wms.dto.request.AccountingPeriodCloseRequest;
import com.wms.dto.request.AccountingPeriodCreateRequest;
import com.wms.dto.response.AccountingPeriodResponse;
import com.wms.entity.access_control.User;
import com.wms.repository.UserRepository;
import com.wms.service.billing_payment.AccountingPeriodService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/v1/accounting-periods")
@RequiredArgsConstructor
public class AccountingPeriodController {

    private final AccountingPeriodService accountingPeriodService;
    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ACCOUNTANT_MANAGER', 'ADMIN', 'CEO', 'WAREHOUSE_MANAGER', 'STOREKEEPER')")
    public ResponseEntity<List<AccountingPeriodResponse>> getAllPeriods(Principal principal) {
        User actor = getActor(principal);
        return ResponseEntity.ok(accountingPeriodService.getAllPeriods(actor));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ACCOUNTANT_MANAGER', 'ADMIN')")
    public ResponseEntity<AccountingPeriodResponse> createPeriod(
            @Valid @RequestBody AccountingPeriodCreateRequest request,
            Principal principal) {
        User actor = getActor(principal);
        AccountingPeriodResponse response = accountingPeriodService.createPeriod(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ACCOUNTANT_MANAGER', 'ADMIN')")
    public ResponseEntity<AccountingPeriodResponse> closePeriod(
            @PathVariable Long id,
            @RequestBody(required = false) AccountingPeriodCloseRequest request,
            Principal principal) {
        User actor = getActor(principal);
        AccountingPeriodResponse response = accountingPeriodService.closePeriod(id, request, actor);
        return ResponseEntity.ok(response);
    }

    private User getActor(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
        }
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS"));
    }
}
