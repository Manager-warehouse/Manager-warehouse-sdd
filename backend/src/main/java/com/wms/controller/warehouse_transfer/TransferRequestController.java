package com.wms.controller.warehouse_transfer;


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
import com.wms.dto.request.TransferRequestCreateRequest;
import com.wms.dto.request.TransferRequestRejectRequest;
import com.wms.dto.request.TransferRequestUpdateRequest;
import com.wms.dto.response.TransferRequestResponse;
import com.wms.dto.response.WarehouseStockLookupResponse;
import com.wms.entity.access_control.User;
import com.wms.service.user_context.CurrentUserService;
import com.wms.service.warehouse_transfer.TransferRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transfer-requests")
@RequiredArgsConstructor
@Tag(name = "Transfer Requests Management", description = "Endpoints for warehouse manager transfer requests and CEO approval flow (Spec 005)")
public class TransferRequestController {

    private final TransferRequestService requestService;
    private final CurrentUserService currentUserService;

    @GetMapping
    @Operation(summary = "Lấy danh sách các yêu cầu điều chuyển")
    @PreAuthorize("hasAnyRole('ADMIN','CEO','PLANNER','WAREHOUSE_MANAGER')")
    public ResponseEntity<List<TransferRequestResponse>> getAllRequests() {
        User actor = currentUserService.getRequiredCurrentUser();
        return ResponseEntity.ok(requestService.getAllRequests(actor));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết yêu cầu điều chuyển theo ID")
    @PreAuthorize("hasAnyRole('ADMIN','CEO','PLANNER','WAREHOUSE_MANAGER')")
    public ResponseEntity<TransferRequestResponse> getRequestById(@PathVariable Long id) {
        User actor = currentUserService.getRequiredCurrentUser();
        return ResponseEntity.ok(requestService.getRequestById(id, actor));
    }

    @PostMapping
    @Operation(summary = "Tạo mới yêu cầu điều chuyển thô (DRAFT)")
    @PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER','ADMIN')")
    public ResponseEntity<TransferRequestResponse> createRequest(@Valid @RequestBody TransferRequestCreateRequest request) {
        User actor = currentUserService.getRequiredCurrentUser();
        TransferRequestResponse response = requestService.createRequest(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật yêu cầu điều chuyển thô (Chỉ sửa khi DRAFT)")
    @PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER','ADMIN')")
    public ResponseEntity<TransferRequestResponse> updateRequest(
            @PathVariable Long id,
            @Valid @RequestBody TransferRequestUpdateRequest request) {
        User actor = currentUserService.getRequiredCurrentUser();
        return ResponseEntity.ok(requestService.updateRequest(id, request, actor));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Hủy mềm yêu cầu điều chuyển DRAFT (hiển thị như xóa đơn)")
    @PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER','ADMIN')")
    public ResponseEntity<TransferRequestResponse> cancelRequest(@PathVariable Long id) {
        User actor = currentUserService.getRequiredCurrentUser();
        return ResponseEntity.ok(requestService.cancelRequest(id, actor));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Gửi yêu cầu điều chuyển cho CEO duyệt (DRAFT -> SUBMITTED)")
    @PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER','ADMIN')")
    public ResponseEntity<TransferRequestResponse> submitRequest(@PathVariable Long id) {
        User actor = currentUserService.getRequiredCurrentUser();
        return ResponseEntity.ok(requestService.submitRequest(id, actor));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "CEO phê duyệt yêu cầu điều chuyển (SUBMITTED -> APPROVED)")
    @PreAuthorize("hasAnyRole('CEO','ADMIN')")
    public ResponseEntity<TransferRequestResponse> approveRequest(@PathVariable Long id) {
        User actor = currentUserService.getRequiredCurrentUser();
        return ResponseEntity.ok(requestService.approveRequest(id, actor));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "CEO từ chối yêu cầu điều chuyển (SUBMITTED -> REJECTED)")
    @PreAuthorize("hasAnyRole('CEO','ADMIN')")
    public ResponseEntity<TransferRequestResponse> rejectRequest(
            @PathVariable Long id,
            @Valid @RequestBody TransferRequestRejectRequest request) {
        User actor = currentUserService.getRequiredCurrentUser();
        return ResponseEntity.ok(requestService.rejectRequest(id, request, actor));
    }

    @PostMapping("/{id}/convert")
    @Operation(summary = "Planner convert yêu cầu điều chuyển đã duyệt thành phiếu điều chuyển TRF NEW")
    @PreAuthorize("hasAnyRole('PLANNER','CEO','ADMIN')")
    public ResponseEntity<TransferRequestResponse> convertToTransfer(@PathVariable Long id) {
        User actor = currentUserService.getRequiredCurrentUser();
        return ResponseEntity.ok(requestService.convertToTransfer(id, actor));
    }

    @GetMapping("/stock-lookup")
    @Operation(summary = "Xem tồn kho khả dụng của sản phẩm tại các kho khác (không tính Quarantine)")
    @PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER','PLANNER','CEO','ADMIN')")
    public ResponseEntity<List<WarehouseStockLookupResponse>> stockLookup(@RequestParam Long productId) {
        User actor = currentUserService.getRequiredCurrentUser();
        return ResponseEntity.ok(requestService.stockLookup(productId, actor));
    }
}
