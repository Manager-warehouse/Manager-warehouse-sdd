package com.wms.controller;

import com.wms.dto.request.TransferRequestCreateRequest;
import com.wms.dto.request.TransferRequestRejectRequest;
import com.wms.dto.request.TransferRequestUpdateRequest;
import com.wms.dto.response.TransferRequestResponse;
import com.wms.dto.response.WarehouseStockLookupResponse;
import com.wms.entity.User;
import com.wms.service.CurrentUserService;
import com.wms.service.transfer.TransferRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<TransferRequestResponse>> getAllRequests() {
        User actor = currentUserService.getRequiredCurrentUser();
        return ResponseEntity.ok(requestService.getAllRequests(actor));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết yêu cầu điều chuyển theo ID")
    public ResponseEntity<TransferRequestResponse> getRequestById(@PathVariable Long id) {
        User actor = currentUserService.getRequiredCurrentUser();
        return ResponseEntity.ok(requestService.getRequestById(id, actor));
    }

    @PostMapping
    @Operation(summary = "Tạo mới yêu cầu điều chuyển thô (DRAFT)")
    public ResponseEntity<TransferRequestResponse> createRequest(@Valid @RequestBody TransferRequestCreateRequest request) {
        User actor = currentUserService.getRequiredCurrentUser();
        TransferRequestResponse response = requestService.createRequest(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật yêu cầu điều chuyển thô (Chỉ sửa khi DRAFT)")
    public ResponseEntity<TransferRequestResponse> updateRequest(
            @PathVariable Long id,
            @Valid @RequestBody TransferRequestUpdateRequest request) {
        User actor = currentUserService.getRequiredCurrentUser();
        return ResponseEntity.ok(requestService.updateRequest(id, request, actor));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Gửi yêu cầu điều chuyển cho CEO duyệt (DRAFT -> SUBMITTED)")
    public ResponseEntity<TransferRequestResponse> submitRequest(@PathVariable Long id) {
        User actor = currentUserService.getRequiredCurrentUser();
        return ResponseEntity.ok(requestService.submitRequest(id, actor));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "CEO phê duyệt yêu cầu điều chuyển (SUBMITTED -> APPROVED)")
    public ResponseEntity<TransferRequestResponse> approveRequest(@PathVariable Long id) {
        User actor = currentUserService.getRequiredCurrentUser();
        return ResponseEntity.ok(requestService.approveRequest(id, actor));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "CEO từ chối yêu cầu điều chuyển (SUBMITTED -> REJECTED)")
    public ResponseEntity<TransferRequestResponse> rejectRequest(
            @PathVariable Long id,
            @Valid @RequestBody TransferRequestRejectRequest request) {
        User actor = currentUserService.getRequiredCurrentUser();
        return ResponseEntity.ok(requestService.rejectRequest(id, request, actor));
    }

    @PostMapping("/{id}/convert")
    @Operation(summary = "Planner convert yêu cầu điều chuyển đã duyệt thành phiếu điều chuyển TRF NEW")
    public ResponseEntity<TransferRequestResponse> convertToTransfer(@PathVariable Long id) {
        User actor = currentUserService.getRequiredCurrentUser();
        return ResponseEntity.ok(requestService.convertToTransfer(id, actor));
    }

    @GetMapping("/stock-lookup")
    @Operation(summary = "Xem tồn kho khả dụng của sản phẩm tại các kho khác (không tính Quarantine)")
    public ResponseEntity<List<WarehouseStockLookupResponse>> stockLookup(@RequestParam Long productId) {
        User actor = currentUserService.getRequiredCurrentUser();
        return ResponseEntity.ok(requestService.stockLookup(productId, actor));
    }
}
