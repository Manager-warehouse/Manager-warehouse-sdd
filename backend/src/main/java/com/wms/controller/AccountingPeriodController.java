package com.wms.controller;

import com.wms.dto.request.AccountingPeriodCloseRequest;
import com.wms.dto.request.AccountingPeriodCreateRequest;
import com.wms.dto.response.AccountingPeriodResponse;
import com.wms.entity.User;
import com.wms.repository.UserRepository;
import com.wms.service.AccountingPeriodService;
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
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ACCOUNTANT_MANAGER', 'ADMIN', 'CEO')")
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
