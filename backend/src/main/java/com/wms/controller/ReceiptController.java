package com.wms.controller;

import com.wms.dto.request.*;
import com.wms.dto.response.ReceiptDetailResponse;
import com.wms.dto.response.ReceiptResponse;
import com.wms.entity.User;
import com.wms.service.CurrentUserService;
import com.wms.service.ReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/receipts")
@Tag(name = "Receipts", description = "Inbound receipt lifecycle: create → receive → QC → approve/reject → RTV → putaway")
public class ReceiptController {

    private final ReceiptService receiptService;
    private final CurrentUserService currentUserService;

    public ReceiptController(ReceiptService receiptService,
                             CurrentUserService currentUserService) {
        this.receiptService = receiptService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CEO','WAREHOUSE_MANAGER','STOREKEEPER','WAREHOUSE_STAFF','PLANNER')")
    @Operation(summary = "List purchase receipts by warehouse")
    public List<ReceiptResponse> list(@RequestParam Long warehouseId) {
        return receiptService.listByWarehouse(warehouseId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CEO','WAREHOUSE_MANAGER','STOREKEEPER','WAREHOUSE_STAFF','PLANNER','ACCOUNTANT','ACCOUNTANT_MANAGER')")
    @Operation(summary = "Get receipt detail")
    public ReceiptDetailResponse get(@PathVariable Long id) {
        return receiptService.get(id);
    }

    /** US-WMS-02: Planner lập lệnh nhập kho → PENDING_RECEIPT */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('PLANNER','ADMIN')")
    @Operation(summary = "Planner tạo lệnh nhập kho (PENDING_RECEIPT)")
    public ReceiptDetailResponse create(@Valid @RequestBody ReceiptCreateRequest request) {
        return receiptService.create(request, currentUser());
    }

    /** US-WMS-03a: Warehouse Staff nhập số lượng thực tế → DRAFT */
    @PutMapping("/{id}/receive")
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF','STOREKEEPER','ADMIN')")
    @Operation(summary = "Warehouse Staff ghi nhận số lượng thực tế (DRAFT)")
    public ReceiptDetailResponse receive(@PathVariable Long id,
                                         @Valid @RequestBody ReceiptReceiveRequest request) {
        return receiptService.receive(id, request, currentUser());
    }

    /**
     * US-WMS-03b: QC flow (2 bước dùng chung 1 endpoint)
     * action=SUBMIT  → WAREHOUSE_STAFF ghi kết quả mẫu QC
     * action=CONFIRM → STOREKEEPER kết luận → QC_COMPLETED hoặc QC_FAILED
     */
    @PutMapping("/{id}/qc")
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF','STOREKEEPER','ADMIN')")
    @Operation(summary = "QC inbound: SUBMIT (staff) hoặc CONFIRM (storekeeper)")
    public ReceiptDetailResponse qc(@PathVariable Long id,
                                    @Valid @RequestBody ReceiptQcRequest request) {
        return receiptService.qc(id, request, currentUser());
    }

    /** US-WMS-05: Trưởng kho duyệt → APPROVED, tạo batch, cộng inventory */
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER','ADMIN')")
    @Operation(summary = "Trưởng kho duyệt nhập kho (APPROVED)")
    public ReceiptDetailResponse approve(@PathVariable Long id,
                                          @Valid @RequestBody ReceiptApproveRequest request) {
        return receiptService.approve(id, request, currentUser());
    }

    /** US-WMS-05: Trưởng kho từ chối → REJECTED */
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER','ADMIN')")
    @Operation(summary = "Trưởng kho từ chối nhập kho (REJECTED)")
    public ReceiptDetailResponse reject(@PathVariable Long id,
                                         @Valid @RequestBody ReceiptRejectRequest request) {
        return receiptService.reject(id, request, currentUser());
    }

    /** US-WMS-04: Trưởng kho lập phiếu trả hàng NCC + Debit Note */
    @PostMapping("/{id}/rtv")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER','ADMIN')")
    @Operation(summary = "Return to Vendor: tạo Adjustment + Debit Note, trừ quarantine inventory")
    public ReceiptDetailResponse rtv(@PathVariable Long id,
                                      @Valid @RequestBody ReceiptRtvRequest request) {
        return receiptService.rtv(id, request, currentUser());
    }

    /** US-WMS-03a: Storekeeper xác nhận cất hàng vào Bin Location */
    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('STOREKEEPER','ADMIN')")
    @Operation(summary = "Storekeeper xác nhận putaway vào Bin Location")
    public ReceiptDetailResponse putaway(@PathVariable Long id,
                                          @Valid @RequestBody ReceiptPutawayRequest request) {
        return receiptService.putaway(id, request, currentUser());
    }

    private User currentUser() {
        return currentUserService.getRequiredCurrentUser();
    }
}
