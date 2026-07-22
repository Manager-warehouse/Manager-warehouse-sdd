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
import com.wms.dto.request.CreateReceiptRequest;
import com.wms.dto.request.ReceiveReceiptRequest;
import com.wms.dto.request.ReceiptQcRequest;
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
    @PreAuthorize("hasRole('PLANNER')")
    @Operation(summary = "Create supplier purchase receipt draft")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Receipt created",
                    content = @Content(schema = @Schema(implementation = ReceiptResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error or return flow attempt"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid authentication"),
            @ApiResponse(responseCode = "403", description = "Planner cannot access warehouse"),
            @ApiResponse(responseCode = "404", description = "Supplier, warehouse, or product not found"),
            @ApiResponse(responseCode = "409", description = "Duplicate source reference"),
            @ApiResponse(responseCode = "422", description = "Inactive master data or invalid item semantics")
    })
    public ReceiptResponse createReceipt(@Valid @RequestBody CreateReceiptRequest request) {
        User actor = currentUserService.getRequiredCurrentUser();
        return receiptService.createPurchaseReceipt(request, actor);
    }

    @PutMapping("/{id}/receive")
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'STOREKEEPER', 'WAREHOUSE_MANAGER', 'ADMIN')")
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
