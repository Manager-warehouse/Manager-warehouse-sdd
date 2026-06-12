package com.wms.controller;

import com.wms.dto.request.ReceiptQcRequest;
import com.wms.dto.response.ReceiptQcResponse;
import com.wms.service.ReceiptQcService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/receipts")
@RequiredArgsConstructor
@Tag(name = "Receipts", description = "Quản lý phiếu nhập kho và kiểm định chất lượng")
public class ReceiptController {

    private final ReceiptQcService receiptQcService;

    /**
     * PUT /api/v1/receipts/{id}/qc
     *
     * - action=SUBMIT (WAREHOUSE_STAFF): ghi nhận kết quả QC mẫu từng item.
     * - action=CONFIRM (STOREKEEPER): kết luận QC, chuyển trạng thái receipt.
     */
    @PutMapping("/{id}/qc")
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'STOREKEEPER', 'WAREHOUSE_MANAGER', 'ADMIN')")
    @Operation(
        summary = "Kiểm định chất lượng inbound",
        description = "SUBMIT: Nhân viên kho ghi kết quả QC mẫu. CONFIRM: Storekeeper kết luận và chuyển trạng thái phiếu."
    )
    public ResponseEntity<ReceiptQcResponse> processQc(
            @PathVariable Long id,
            @Valid @RequestBody ReceiptQcRequest request,
            Authentication authentication) {
        ReceiptQcResponse response = receiptQcService.processQc(id, request, authentication.getName());
        return ResponseEntity.ok(response);
    }
}
