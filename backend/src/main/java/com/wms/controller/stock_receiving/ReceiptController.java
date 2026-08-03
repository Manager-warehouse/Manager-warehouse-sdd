package com.wms.controller.stock_receiving;


import com.wms.dto.request.CreateReceiptRequest;
import com.wms.dto.request.PreReceiveApprovalRequest;
import com.wms.dto.request.ReceiptCancelRequest;
import com.wms.dto.request.ReceiveQcReceiptRequest;
import com.wms.dto.request.ReceiveReceiptRequest;
import com.wms.dto.request.ReceiptReopenRequest;
import com.wms.dto.request.ReceiptQcRequest;
import com.wms.dto.request.ReviseReceiptRequest;
import com.wms.dto.request.StorekeeperReviewRequest;
import com.wms.dto.response.ReceiptResponse;
import com.wms.dto.response.ReceiptQcResponse;
import com.wms.entity.access_control.User;
import com.wms.service.user_context.CurrentUserService;
import com.wms.service.stock_receiving.ReceiptService;
import com.wms.service.stock_receiving.ReceiptQcService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/receipts")
@Tag(name = "Receipts", description = "Inbound receipt drafting, processing, and QC")
public class ReceiptController {

    private final ReceiptService receiptService;
    private final CurrentUserService currentUserService;
    private final ReceiptQcService receiptQcService;

    public ReceiptController(ReceiptService receiptService,
                             CurrentUserService currentUserService,
                             ReceiptQcService receiptQcService) {
        this.receiptService = receiptService;
        this.currentUserService = currentUserService;
        this.receiptQcService = receiptQcService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List receipts for a warehouse")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Receipt list returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid authentication"),
            @ApiResponse(responseCode = "403", description = "User cannot access this warehouse")
    })
    public List<ReceiptResponse> getReceipts(
            @RequestParam(value = "warehouse_id", required = false) Long warehouseIdSnake,
            @RequestParam(value = "warehouseId", required = false) Long warehouseIdCamel,
            @RequestParam(value = "type", required = false) com.wms.enums.stock_receiving.ReceiptType type) {
        Long warehouseId = warehouseIdSnake != null ? warehouseIdSnake : warehouseIdCamel;
        if (warehouseId == null) {
            throw new IllegalArgumentException("Required request parameter 'warehouseId' or 'warehouse_id' is not present");
        }
        User actor = currentUserService.getRequiredCurrentUser();
        return receiptService.getReceiptsByWarehouseAndType(warehouseId, type, actor);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get receipt detail by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Receipt detail returned",
                    content = @Content(schema = @Schema(implementation = ReceiptResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid authentication"),
            @ApiResponse(responseCode = "403", description = "User cannot access this warehouse"),
            @ApiResponse(responseCode = "404", description = "Receipt not found")
    })
    public ReceiptResponse getReceiptById(@PathVariable Long id) {
        User actor = currentUserService.getRequiredCurrentUser();
        return receiptService.getReceiptById(id, actor);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('PLANNER', 'ADMIN')")
    @Operation(summary = "Create supplier purchase receipt draft")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Receipt created",
                    content = @Content(schema = @Schema(implementation = ReceiptResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error or return flow attempt"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid authentication"),
            @ApiResponse(responseCode = "403", description = "Planner cannot access warehouse"),
            @ApiResponse(responseCode = "404", description = "Supplier, warehouse, or product not found"),
            @ApiResponse(responseCode = "409", description = "Receipt number conflict"),
            @ApiResponse(responseCode = "422", description = "Inactive master data or invalid item semantics")
    })
    public ReceiptResponse createReceipt(@Valid @RequestBody CreateReceiptRequest request) {
        User actor = currentUserService.getRequiredCurrentUser();
        return receiptService.createPurchaseReceipt(request, actor);
    }

    @PutMapping("/{id}/pre-receive-approval")
    @PreAuthorize("hasRole('WAREHOUSE_MANAGER')")
    @Operation(summary = "Approve or reject a planned inbound receipt before physical receiving")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pre-receive decision accepted",
                    content = @Content(schema = @Schema(implementation = ReceiptResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid authentication"),
            @ApiResponse(responseCode = "403", description = "Manager cannot access warehouse"),
            @ApiResponse(responseCode = "404", description = "Receipt not found"),
            @ApiResponse(responseCode = "409", description = "Version conflict"),
            @ApiResponse(responseCode = "422", description = "Receipt is not awaiting manager approval")
    })
    public ReceiptResponse decidePreReceiveApproval(@PathVariable Long id,
                                                    @Valid @RequestBody PreReceiveApprovalRequest request) {
        User actor = currentUserService.getRequiredCurrentUser();
        return receiptService.decidePreReceiveApproval(id, request, actor);
    }

    @PutMapping("/{id}/revision")
    @PreAuthorize("hasAnyRole('PLANNER', 'ADMIN')")
    @Operation(summary = "Revise and resubmit a rejected inbound receipt for manager approval")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Receipt revision resubmitted",
                    content = @Content(schema = @Schema(implementation = ReceiptResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid authentication"),
            @ApiResponse(responseCode = "403", description = "Planner cannot access warehouse"),
            @ApiResponse(responseCode = "404", description = "Receipt or product not found"),
            @ApiResponse(responseCode = "409", description = "Version conflict"),
            @ApiResponse(responseCode = "422", description = "Receipt revision is not allowed")
    })
    public ReceiptResponse reviseReceipt(@PathVariable Long id,
                                         @Valid @RequestBody ReviseReceiptRequest request) {
        User actor = currentUserService.getRequiredCurrentUser();
        return receiptService.reviseReceipt(id, request, actor);
    }

    @PutMapping("/{id}/receive")
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'ADMIN')")
    @Operation(summary = "Submit or correct complete physical receipt counts")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Receipt counts accepted",
                    content = @Content(schema = @Schema(implementation = ReceiptResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid authentication"),
            @ApiResponse(responseCode = "403", description = "Warehouse Staff cannot access warehouse"),
            @ApiResponse(responseCode = "404", description = "Receipt or receipt item not found"),
            @ApiResponse(responseCode = "409", description = "Receipt status does not allow receive counting"),
            @ApiResponse(responseCode = "422", description = "Invalid or incomplete count data")
    })
    public ReceiptResponse receiveReceipt(@PathVariable Long id,
                                           @Valid @RequestBody ReceiveReceiptRequest request) {
        User actor = currentUserService.getRequiredCurrentUser();
        return receiptService.receiveReceiptCounts(id, request, actor);
    }

    @PutMapping("/{id}/receive-qc")
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'ADMIN')")
    @Operation(summary = "Warehouse Staff records physical counts and inbound QC")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Receive-QC accepted",
                    content = @Content(schema = @Schema(implementation = ReceiptResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid authentication"),
            @ApiResponse(responseCode = "403", description = "User cannot receive and QC this receipt"),
            @ApiResponse(responseCode = "404", description = "Receipt or receipt item not found"),
            @ApiResponse(responseCode = "409", description = "Version conflict or manager decision already started"),
            @ApiResponse(responseCode = "422", description = "Invalid status or receive-QC quantities")
    })
    public ReceiptResponse receiveAndQcReceipt(@PathVariable Long id,
                                               @Valid @RequestBody ReceiveQcReceiptRequest request) {
        User actor = currentUserService.getRequiredCurrentUser();
        return receiptService.receiveAndQcReceipt(id, request, actor);
    }

    @PutMapping("/{id}/storekeeper-review")
    @PreAuthorize("hasAnyRole('STOREKEEPER', 'ADMIN')")
    @Operation(summary = "Storekeeper reviews Staff count/QC and approves or requests recount")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Storekeeper review accepted",
                    content = @Content(schema = @Schema(implementation = ReceiptResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid authentication"),
            @ApiResponse(responseCode = "403", description = "Storekeeper cannot access warehouse"),
            @ApiResponse(responseCode = "404", description = "Receipt not found"),
            @ApiResponse(responseCode = "409", description = "Version conflict"),
            @ApiResponse(responseCode = "422", description = "Receipt is not pending storekeeper review")
    })
    public ReceiptResponse reviewStorekeeperCountQc(@PathVariable Long id,
                                                    @Valid @RequestBody StorekeeperReviewRequest request) {
        User actor = currentUserService.getRequiredCurrentUser();
        return receiptService.reviewStorekeeperCountQc(id, request, actor);
    }

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

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('PLANNER', 'WAREHOUSE_MANAGER', 'ADMIN')")
    @Operation(summary = "Status-based cancellation of inbound receipt")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Receipt cancelled"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "403", description = "User cannot cancel this receipt"),
            @ApiResponse(responseCode = "409", description = "Version conflict"),
            @ApiResponse(responseCode = "422", description = "Receipt already finalized")
    })
    public ReceiptResponse cancelReceipt(@PathVariable Long id,
                                         @Valid @RequestBody ReceiptCancelRequest request) {
        User actor = currentUserService.getRequiredCurrentUser();
        return receiptService.cancelReceipt(id, request, actor);
    }

    @PostMapping("/{id}/reopen")
    @PreAuthorize("hasAnyRole('STOREKEEPER', 'WAREHOUSE_MANAGER', 'ADMIN')")
    @Operation(summary = "Manager reopen flow for APPROVED or RETURN_TO_SUPPLIER_PENDING receipts")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Receipt reopened"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "403", description = "User cannot reopen this receipt"),
            @ApiResponse(responseCode = "409", description = "Version conflict"),
            @ApiResponse(responseCode = "422", description = "Receipt cannot be reopened")
    })
    public ReceiptResponse reopenReceipt(@PathVariable Long id,
                                         @Valid @RequestBody ReceiptReopenRequest request) {
        User actor = currentUserService.getRequiredCurrentUser();
        return receiptService.reopenReceipt(id, request, actor);
    }
}
