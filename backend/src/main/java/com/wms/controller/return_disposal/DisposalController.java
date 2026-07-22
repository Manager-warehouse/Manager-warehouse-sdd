package com.wms.controller.return_disposal;


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
import com.wms.dto.request.DisposalRequest;
import com.wms.dto.response.DisposalResponse;
import com.wms.dto.response.PendingDisposalResponse;
import com.wms.entity.access_control.User;
import com.wms.service.user_context.CurrentUserService;
import com.wms.service.return_disposal.DisposalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Scrap Disposal", description = "Quarantine scrap disposal and damage report management (Spec 009)")
public class DisposalController {

    private final DisposalService disposalService;
    private final CurrentUserService currentUserService;

    public DisposalController(DisposalService disposalService, CurrentUserService currentUserService) {
        this.disposalService = disposalService;
        this.currentUserService = currentUserService;
    }

    @Operation(
            summary = "Tạo yêu cầu tiêu hủy hàng lỗi cách ly",
            description = "Tạo báo cáo hỏng (Damage Report) và Adjustment tương ứng. " +
                    "Nếu giá trị hàng tiêu hủy < 5M VND, tự động phê duyệt và trừ tồn kho quarantine."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tạo yêu cầu thành công"),
            @ApiResponse(responseCode = "400", description = "Thông tin không hợp lệ"),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập kho hoặc vai trò không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy receipt item"),
            @ApiResponse(responseCode = "409", description = "Sản phẩm đã được làm thủ tục tiêu hủy trước đó (ALREADY_DISPOSED)")
    })
    @PostMapping("/receipts/{receiptItemId}/dispose")
    public ResponseEntity<DisposalResponse> createDisposal(
            @Parameter(description = "ID của receipt item cần tiêu hủy") @PathVariable Long receiptItemId,
            @Valid @RequestBody DisposalRequest request) {
        User actor = currentUserService.getRequiredCurrentUser();
        DisposalResponse response = disposalService.createDisposalRequest(receiptItemId, request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Tạo yêu cầu tiêu hủy hàng điều chuyển cách ly",
            description = "Tạo báo cáo hỏng và Adjustment tương ứng từ QuarantineRecord. " +
                    "Nếu giá trị < 5M VND, tự động phê duyệt và trừ tồn kho quarantine."
    )
    @PostMapping("/quarantine/{quarantineRecordId}/dispose")
    public ResponseEntity<DisposalResponse> createDisposalFromQuarantine(
            @Parameter(description = "ID của quarantine record cần tiêu hủy") @PathVariable Long quarantineRecordId,
            @Valid @RequestBody DisposalRequest request) {
        User actor = currentUserService.getRequiredCurrentUser();
        DisposalResponse response = disposalService.createDisposalFromQuarantine(quarantineRecordId, request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Lấy danh sách các yêu cầu tiêu hủy chờ duyệt",
            description = "Lấy toàn bộ các adjustment tiêu hủy đang ở trạng thái pending."
    )
    @GetMapping("/disposals/pending")
    public ResponseEntity<List<PendingDisposalResponse>> getPendingDisposals() {
        User actor = currentUserService.getRequiredCurrentUser();
        List<PendingDisposalResponse> responses = disposalService.getPendingDisposals(actor);
        return ResponseEntity.ok(responses);
    }

    @Operation(
            summary = "Phê duyệt yêu cầu tiêu hủy hàng cách ly",
            description = "Phê duyệt adjustment tiêu hủy, đồng thời trừ tồn kho quarantine và cập nhật dung lượng vị trí. " +
                    "Hàng có giá trị > 100M VND chỉ CEO mới được duyệt."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Phê duyệt thành công"),
            @ApiResponse(responseCode = "400", description = "Thông tin không hợp lệ"),
            @ApiResponse(responseCode = "403", description = "Vai trò không hợp lệ hoặc cần phê duyệt cấp CEO"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy adjustment"),
            @ApiResponse(responseCode = "409", description = "Yêu cầu đã được phê duyệt trước đó")
    })
    @PutMapping("/disposal/{adjustmentId}/approve")
    public ResponseEntity<DisposalResponse> approveDisposal(
            @Parameter(description = "ID của adjustment tiêu hủy cần duyệt") @PathVariable Long adjustmentId) {
        User actor = currentUserService.getRequiredCurrentUser();
        DisposalResponse response = disposalService.approveDisposal(adjustmentId, actor);
        return ResponseEntity.ok(response);
    }
}
