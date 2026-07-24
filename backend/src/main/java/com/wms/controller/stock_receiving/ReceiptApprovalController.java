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
import com.wms.dto.request.ReceiptDecisionRequest;
import com.wms.dto.request.ReceiptPutawayRequest;
import com.wms.dto.request.ReceiptReturnConfirmRequest;
import com.wms.dto.response.ReceiptActionResponse;
import com.wms.entity.access_control.User;
import com.wms.service.user_context.CurrentUserService;
import com.wms.service.stock_receiving.ReceiptApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for inbound receipt approval and putaway operations (US-WMS-05).
 */
@RestController
@RequestMapping("/api/v1/receipts")
@Tag(name = "Receipt Approval", description = "Inbound receipt approval, rejection, and putaway management (Spec 003)")
public class ReceiptApprovalController {

    private final ReceiptApprovalService receiptApprovalService;
    private final CurrentUserService currentUserService;

    public ReceiptApprovalController(ReceiptApprovalService receiptApprovalService,
                                     CurrentUserService currentUserService) {
        this.receiptApprovalService = receiptApprovalService;
        this.currentUserService = currentUserService;
    }

    @Operation(
        summary = "Duyệt nhập kho (Trưởng kho)",
        description = "Trưởng kho duyệt phiếu nhập ở trạng thái QC_COMPLETED. "
            + "Tạo batch theo product+receipt+date, cập nhật trạng thái thành APPROVED, "
            + "unlock putaway. KHÔNG tăng inventory tại bước này."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Phiếu nhập được duyệt thành công"),
        @ApiResponse(responseCode = "400", description = "Thiếu tham số bắt buộc (EXPECTED_VERSION_REQUIRED)"),
        @ApiResponse(responseCode = "403", description = "Trưởng kho không được phân quyền vào kho này (FORBIDDEN_RECEIPT_WAREHOUSE)"),
        @ApiResponse(responseCode = "404", description = "Phiếu nhập không tồn tại"),
        @ApiResponse(responseCode = "409", description = "Phiếu đã được duyệt/từ chối hoặc version conflict")
    })
    @PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER', 'ADMIN', 'CEO')")
    @PutMapping("/{id}/approve")
    public ResponseEntity<ReceiptActionResponse> approveReceipt(
            @Parameter(description = "ID phiếu nhập") @PathVariable Long id,
            @Valid @RequestBody ReceiptDecisionRequest request) {
        User actor = currentUserService.getRequiredCurrentUser();
        return ResponseEntity.ok(receiptApprovalService.approveReceipt(id, request, actor));
    }

    @Operation(
        summary = "Từ chối nhập kho (Trưởng kho)",
        description = "Trưởng kho từ chối phiếu nhập ở trạng thái QC_COMPLETED. "
            + "Chuyển trạng thái sang RETURN_TO_SUPPLIER_PENDING, lưu lý do. "
            + "KHÔNG tạo batch, RTV, Debit Note hay inventory."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Phiếu nhập bị từ chối, chờ xe NCC đến nhận hàng"),
        @ApiResponse(responseCode = "400", description = "Thiếu lý do từ chối hoặc expectedVersion"),
        @ApiResponse(responseCode = "403", description = "FORBIDDEN_RECEIPT_WAREHOUSE"),
        @ApiResponse(responseCode = "404", description = "Phiếu nhập không tồn tại"),
        @ApiResponse(responseCode = "409", description = "RECEIPT_ALREADY_DECIDED hoặc version conflict")
    })
    @PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER', 'ADMIN', 'CEO')")
    @PutMapping("/{id}/reject")
    public ResponseEntity<ReceiptActionResponse> rejectReceipt(
            @Parameter(description = "ID phiếu nhập") @PathVariable Long id,
            @Valid @RequestBody ReceiptDecisionRequest request) {
        User actor = currentUserService.getRequiredCurrentUser();
        return ResponseEntity.ok(receiptApprovalService.rejectReceipt(id, request, actor));
    }

    @Operation(
        summary = "Storekeeper xác nhận đã bàn giao hàng bị từ chối cho NCC",
        description = "Chuyển trạng thái từ RETURN_TO_SUPPLIER_PENDING sang RETURNED_TO_SUPPLIER. "
            + "KHÔNG tạo inventory, batch, RTV, hay Debit Note."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Xác nhận bàn giao thành công, phiếu đóng"),
        @ApiResponse(responseCode = "400", description = "Thiếu expectedVersion"),
        @ApiResponse(responseCode = "403", description = "FORBIDDEN_RECEIPT_WAREHOUSE"),
        @ApiResponse(responseCode = "404", description = "Phiếu nhập không tồn tại"),
        @ApiResponse(responseCode = "409", description = "Phiếu không ở trạng thái RETURN_TO_SUPPLIER_PENDING hoặc version conflict")
    })
    @PreAuthorize("hasAnyRole('STOREKEEPER', 'WAREHOUSE_MANAGER', 'ADMIN', 'CEO')")
    @PutMapping("/{id}/return-to-supplier/confirm")
    public ResponseEntity<ReceiptActionResponse> confirmReturnToSupplier(
            @Parameter(description = "ID phiếu nhập") @PathVariable Long id,
            @Valid @RequestBody ReceiptReturnConfirmRequest request) {
        User actor = currentUserService.getRequiredCurrentUser();
        return ResponseEntity.ok(receiptApprovalService.confirmReturnToSupplier(id, request, actor));
    }

    @Operation(
        summary = "Storekeeper hoàn tất putaway vào Bin thường (tăng inventory)",
        description = "Sau khi phiếu được APPROVED, Storekeeper cất hàng vào Bin không phải Quarantine. "
            + "Đây là bước duy nhất tăng regular inventory.total_qty. "
            + "Bin phải có is_quarantine = false. Kiểm tra capacity trước khi tăng."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Putaway hoàn tất, regular inventory đã tăng"),
        @ApiResponse(responseCode = "400", description = "Thiếu expectedVersion hoặc locationId"),
        @ApiResponse(responseCode = "403", description = "FORBIDDEN_RECEIPT_WAREHOUSE"),
        @ApiResponse(responseCode = "404", description = "Phiếu nhập hoặc location không tồn tại"),
        @ApiResponse(responseCode = "409", description = "Phiếu không ở trạng thái APPROVED hoặc version conflict"),
        @ApiResponse(responseCode = "422", description = "Location là Quarantine hoặc inventory invariant bị vi phạm")
    })
    @PreAuthorize("hasAnyRole('STOREKEEPER', 'WAREHOUSE_MANAGER', 'ADMIN', 'CEO')")
    @PutMapping("/{id}/complete")
    public ResponseEntity<ReceiptActionResponse> completePutaway(
            @Parameter(description = "ID phiếu nhập") @PathVariable Long id,
            @Valid @RequestBody ReceiptPutawayRequest request) {
        User actor = currentUserService.getRequiredCurrentUser();
        return ResponseEntity.ok(receiptApprovalService.completePutaway(id, request, actor));
    }
}
