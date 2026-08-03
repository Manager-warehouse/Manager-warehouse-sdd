package com.wms.controller.warehouse_transfer;


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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transfer-requests")
@RequiredArgsConstructor
@Tag(name = "Transfer Requests Management", description = "Endpoints for warehouse manager transfer requests and source warehouse approval flow (Spec 005)")
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
    @PreAuthorize("hasRole('WAREHOUSE_MANAGER')")
    public ResponseEntity<TransferRequestResponse> createRequest(@Valid @RequestBody TransferRequestCreateRequest request) {
        User actor = currentUserService.getRequiredCurrentUser();
        TransferRequestResponse response = requestService.createRequest(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật yêu cầu điều chuyển thô (Chỉ sửa khi DRAFT)")
    @PreAuthorize("hasRole('WAREHOUSE_MANAGER')")
    public ResponseEntity<TransferRequestResponse> updateRequest(
            @PathVariable Long id,
            @Valid @RequestBody TransferRequestUpdateRequest request) {
        User actor = currentUserService.getRequiredCurrentUser();
        return ResponseEntity.ok(requestService.updateRequest(id, request, actor));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Hủy mềm yêu cầu điều chuyển DRAFT (hiển thị như xóa đơn)")
    @PreAuthorize("hasRole('WAREHOUSE_MANAGER')")
    public ResponseEntity<TransferRequestResponse> cancelRequest(@PathVariable Long id) {
        User actor = currentUserService.getRequiredCurrentUser();
        return ResponseEntity.ok(requestService.cancelRequest(id, actor));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Gửi yêu cầu điều chuyển cho Quản lý kho nguồn duyệt (DRAFT -> SUBMITTED)")
    @PreAuthorize("hasRole('WAREHOUSE_MANAGER')")
    public ResponseEntity<TransferRequestResponse> submitRequest(@PathVariable Long id) {
        User actor = currentUserService.getRequiredCurrentUser();
        return ResponseEntity.ok(requestService.submitRequest(id, actor));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Quản lý kho nguồn phê duyệt yêu cầu và giữ hàng (SUBMITTED -> APPROVED)")
    @PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER','ADMIN')")
    public ResponseEntity<TransferRequestResponse> approveRequest(@PathVariable Long id) {
        User actor = currentUserService.getRequiredCurrentUser();
        return ResponseEntity.ok(requestService.approveRequest(id, actor));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Quản lý kho nguồn từ chối yêu cầu điều chuyển (SUBMITTED -> REJECTED)")
    @PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER','ADMIN')")
    public ResponseEntity<TransferRequestResponse> rejectRequest(
            @PathVariable Long id,
            @Valid @RequestBody TransferRequestRejectRequest request) {
        User actor = currentUserService.getRequiredCurrentUser();
        return ResponseEntity.ok(requestService.rejectRequest(id, request, actor));
    }

    @PostMapping("/{id}/convert")
    @Operation(summary = "Planner chốt yêu cầu đã duyệt thành phiếu điều chuyển TRF")
    @PreAuthorize("hasAnyRole('PLANNER','ADMIN')")
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
