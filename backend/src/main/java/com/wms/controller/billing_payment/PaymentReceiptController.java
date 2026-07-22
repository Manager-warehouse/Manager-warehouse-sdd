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
import com.wms.dto.request.PaymentReceiptCreateRequest;
import com.wms.dto.response.PaymentReceiptOcrResponse;
import com.wms.dto.response.PaymentReceiptResponse;
import com.wms.entity.access_control.User;
import com.wms.repository.UserRepository;
import com.wms.service.billing_payment.OcrService;
import com.wms.service.billing_payment.PaymentReceiptService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/payment-receipts")
@RequiredArgsConstructor
public class PaymentReceiptController {

    private final PaymentReceiptService paymentReceiptService;
    private final UserRepository userRepository;
    private final OcrService ocrService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ADMIN')")
    public ResponseEntity<PaymentReceiptResponse> createPaymentReceipt(
            @Valid @RequestBody PaymentReceiptCreateRequest request,
            Principal principal) {
        User actor = getActor(principal);
        PaymentReceiptResponse response = paymentReceiptService.createPaymentReceipt(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/ocr", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ADMIN')")
    public ResponseEntity<PaymentReceiptOcrResponse> scanPaymentReceipt(
            @RequestParam("file") MultipartFile file,
            Principal principal) {
        User actor = getActor(principal);

        // Validation: content type
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/png") && !contentType.equals("image/jpeg") && !contentType.equals("image/jpg"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_FILE_TYPE");
        }

        // Validation: file size (max 5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FILE_TOO_LARGE");
        }

        PaymentReceiptOcrResponse response = ocrService.processOcr(file, actor);
        return ResponseEntity.ok(response);
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
