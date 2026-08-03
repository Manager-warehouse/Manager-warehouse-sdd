package com.wms.controller.billing_payment;


import com.wms.dto.request.InvoiceCreateRequest;
import com.wms.dto.response.InvoiceResponse;
import com.wms.entity.access_control.User;
import com.wms.repository.UserRepository;
import com.wms.service.billing_payment.InvoiceService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final UserRepository userRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ADMIN')")
    public ResponseEntity<InvoiceResponse> createInvoice(
            @Valid @RequestBody InvoiceCreateRequest request,
            Principal principal) {
        User actor = getActor(principal);
        InvoiceResponse response = invoiceService.createInvoice(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ACCOUNTANT_MANAGER', 'ADMIN', 'CEO')")
    public ResponseEntity<InvoiceResponse> getInvoiceById(
            @PathVariable Long id,
            Principal principal) {
        User actor = getActor(principal);
        return ResponseEntity.ok(invoiceService.getInvoiceById(id, actor));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ACCOUNTANT_MANAGER', 'ADMIN', 'CEO')")
    public ResponseEntity<List<InvoiceResponse>> getInvoices(
            @RequestParam(required = false, name = "dealerId") Long dealerId,
            @RequestParam(required = false, name = "status") String status,
            @RequestParam(required = false, name = "warehouseId") Long warehouseId,
            Principal principal) {
        User actor = getActor(principal);
        return ResponseEntity.ok(invoiceService.getInvoices(dealerId, status, warehouseId, actor));
    }

    private User getActor(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
        }
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS"));
    }
}
