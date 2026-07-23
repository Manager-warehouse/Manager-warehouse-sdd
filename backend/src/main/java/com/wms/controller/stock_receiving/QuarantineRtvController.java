package com.wms.controller.stock_receiving;


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
import com.wms.dto.request.ReceiptRtvConfirmRequest;
import com.wms.dto.request.ReceiptRtvCreateRequest;
import com.wms.dto.response.RtvActionResponse;
import com.wms.entity.access_control.User;
import com.wms.service.user_context.CurrentUserService;
import com.wms.service.stock_receiving.QuarantineRtvService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for quarantine Return To Vendor (RTV) operations (US-WMS-04).
 */
@RestController
@RequestMapping("/api/v1/receipts")
@Tag(name = "Quarantine RTV", description = "Quarantine handling and Return-To-Vendor (RTV) management (Spec 003)")
public class QuarantineRtvController {

    private final QuarantineRtvService quarantineRtvService;
    private final CurrentUserService currentUserService;

    public QuarantineRtvController(QuarantineRtvService quarantineRtvService,
                                   CurrentUserService currentUserService) {
        this.quarantineRtvService = quarantineRtvService;
        this.currentUserService = currentUserService;
    }

    @Operation(
        summary = "Trưởng kho lập phiếu trả NCC (RTV) và sinh Debit Note tự động",
        description = "Chỉ áp dụng cho phiếu QC_FAILED. Tạo RETURN_TO_VENDOR adjustment ở trạng thái pending "
            + "và Debit Note do hệ thống tự tạo. KHÔNG trừ tồn Quarantine tại bước này. "
            + "Chỉ cho phép 1 RTV per receipt — duplicate bị từ chối HTTP 409."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "RTV và Debit Note được tạo thành công"),
        @ApiResponse(responseCode = "400", description = "Thiếu reason hoặc expectedVersion"),
        @ApiResponse(responseCode = "403", description = "FORBIDDEN_RECEIPT_WAREHOUSE"),
        @ApiResponse(responseCode = "404", description = "Phiếu nhập không tồn tại"),
        @ApiResponse(responseCode = "409", description = "RTV_ALREADY_EXISTS — RTV đã tồn tại cho phiếu này"),
        @ApiResponse(responseCode = "422", description = "Phiếu không ở trạng thái QC_FAILED hoặc không có items")
    })
    @PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER', 'ADMIN', 'CEO')")
    @PostMapping("/{id}/rtv")
    public ResponseEntity<RtvActionResponse> createRtv(
            @Parameter(description = "ID phiếu nhập QC_FAILED") @PathVariable Long id,
            @Valid @RequestBody ReceiptRtvCreateRequest request) {
        User actor = currentUserService.getRequiredCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(quarantineRtvService.createRtv(id, request, actor));
    }

    @Operation(
        summary = "Storekeeper xác nhận đã giao trả NCC và trừ tồn Quarantine",
        description = "returnedQty PHẢI bằng đúng tổng số lượng quarantine của phiếu — partial bị từ chối HTTP 422. "
            + "Sau khi xác nhận: adjustment được đánh dấu confirmed, quarantine inventory bị trừ đúng toàn bộ số lượng. "
            + "Trạng thái phiếu vẫn giữ nguyên QC_FAILED."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "RTV đã xác nhận, quarantine inventory đã trừ"),
        @ApiResponse(responseCode = "400", description = "Thiếu returnedQty hoặc expectedVersion"),
        @ApiResponse(responseCode = "403", description = "FORBIDDEN_RECEIPT_WAREHOUSE"),
        @ApiResponse(responseCode = "404", description = "Phiếu hoặc pending RTV không tồn tại"),
        @ApiResponse(responseCode = "409", description = "RTV đã được xác nhận trước đó (RTV_ALREADY_CONFIRMED)"),
        @ApiResponse(responseCode = "422", description = "RTV_QUANTITY_MISMATCH — số lượng trả không khớp với quarantine")
    })
    @PreAuthorize("hasAnyRole('STOREKEEPER', 'WAREHOUSE_MANAGER', 'ADMIN', 'CEO')")
    @PutMapping("/{id}/rtv/confirm")
    public ResponseEntity<RtvActionResponse> confirmRtv(
            @Parameter(description = "ID phiếu nhập QC_FAILED") @PathVariable Long id,
            @Valid @RequestBody ReceiptRtvConfirmRequest request) {
        User actor = currentUserService.getRequiredCurrentUser();
        return ResponseEntity.ok(quarantineRtvService.confirmRtv(id, request, actor));
    }
}
