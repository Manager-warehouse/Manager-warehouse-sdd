package com.wms.controller.billing_payment;

import com.wms.dto.request.CorrectionVoucherCreateRequest;
import com.wms.dto.response.CorrectionVoucherResponse;
import com.wms.entity.access_control.User;
import com.wms.enums.billing_payment.CorrectionVoucherReferenceType;
import com.wms.repository.UserRepository;
import com.wms.service.billing_payment.CorrectionVoucherService;
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
@RequestMapping("/api/v1/correction-vouchers")
@RequiredArgsConstructor
public class CorrectionVoucherController {

    private final CorrectionVoucherService correctionVoucherService;
    private final UserRepository userRepository;

    // ACCOUNTANT_MANAGER only: they already hold sole authority over period closing
    // (US-WMS-17), so a correction against a period they closed does not require a
    // separate checker step - see feature-accountant-correction-voucher.md.
    @PostMapping
    @PreAuthorize("hasAnyRole('ACCOUNTANT_MANAGER', 'ADMIN')")
    public ResponseEntity<CorrectionVoucherResponse> createCorrectionVoucher(
            @Valid @RequestBody CorrectionVoucherCreateRequest request,
            Principal principal) {
        User actor = getActor(principal);
        CorrectionVoucherResponse response = correctionVoucherService.createCorrectionVoucher(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ACCOUNTANT_MANAGER', 'ADMIN', 'CEO')")
    public ResponseEntity<List<CorrectionVoucherResponse>> getCorrectionVouchers(
            @RequestParam(required = false) CorrectionVoucherReferenceType referenceType,
            Principal principal) {
        User actor = getActor(principal);
        return ResponseEntity.ok(correctionVoucherService.getCorrectionVouchers(referenceType, actor));
    }

    private User getActor(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
        }
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS"));
    }
}
