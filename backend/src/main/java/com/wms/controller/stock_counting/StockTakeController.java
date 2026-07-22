package com.wms.controller.stock_counting;


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
import com.wms.dto.request.CreateStockTakeRequest;
import com.wms.dto.request.StockTakeCountRequest;
import com.wms.dto.request.StockTakeRejectRequest;
import com.wms.dto.response.StockTakeResponse;
import com.wms.dto.response.StockTakeSummaryResponse;
import com.wms.entity.access_control.User;
import com.wms.enums.stock_counting.StockTakeStatus;
import com.wms.service.user_context.CurrentUserService;
import com.wms.service.stock_counting.StockTakeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for StockTake & Adjustment operations (Spec 006, US-WMS-13).
 */
@RestController
@RequestMapping("/api/v1/stocktakes")
@Tag(name = "StockTake", description = "Kiểm kê & Điều chỉnh Tồn kho (Spec 006)")
public class StockTakeController {

    private final StockTakeService stockTakeService;
    private final CurrentUserService currentUserService;

    public StockTakeController(StockTakeService stockTakeService,
                               CurrentUserService currentUserService) {
        this.stockTakeService = stockTakeService;
        this.currentUserService = currentUserService;
    }

    // ─── List ─────────────────────────────────────────────────────────────────

    @Operation(summary = "Danh sách phiếu kiểm kê (phân trang)", description = "Lấy danh sách phiếu kiểm kê của một kho, lọc theo status tùy chọn, hỗ trợ phân trang. Roles: WAREHOUSE_MANAGER, STOREKEEPER, CEO, ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Thành công"),
        @ApiResponse(responseCode = "401", description = "Chưa xác thực"),
        @ApiResponse(responseCode = "403", description = "Không đủ role hoặc không có quyền truy cập kho")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER', 'STOREKEEPER', 'CEO', 'ADMIN')")
    public ResponseEntity<Page<StockTakeSummaryResponse>> getStockTakes(
            @RequestParam("warehouse_id") Long warehouseId,
            @RequestParam(value = "status", required = false) StockTakeStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        User actor = currentUserService.getRequiredCurrentUser();
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(stockTakeService.getStockTakes(warehouseId, status, actor, pageRequest));
    }

    // ─── Detail ───────────────────────────────────────────────────────────────

    @Operation(summary = "Chi tiết phiếu kiểm kê", description = "Lấy chi tiết phiếu kiểm kê kèm tất cả items.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Thành công"),
        @ApiResponse(responseCode = "404", description = "Không tìm thấy phiếu kiểm kê")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER', 'STOREKEEPER', 'CEO', 'ADMIN')")
    public ResponseEntity<StockTakeResponse> getStockTakeById(@PathVariable Long id) {
        User actor = currentUserService.getRequiredCurrentUser();
        return ResponseEntity.ok(stockTakeService.getStockTakeById(id, actor));
    }

    // ─── Create ───────────────────────────────────────────────────────────────

    @Operation(summary = "Tạo phiếu kiểm kê mới", description = "Thủ kho tạo phiếu kiểm kê. Trạng thái ban đầu: DRAFT. Chưa khóa kệ. Roles: STOREKEEPER, ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Tạo thành công"),
        @ApiResponse(responseCode = "400", description = "Thiếu trường bắt buộc"),
        @ApiResponse(responseCode = "422", description = "Kỳ kế toán đã đóng (ACCOUNTING_PERIOD_CLOSED)")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('STOREKEEPER', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<StockTakeResponse> createStockTake(
            @Valid @RequestBody CreateStockTakeRequest request) {
        User actor = currentUserService.getRequiredCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(stockTakeService.createStockTake(request, actor));
    }

    // ─── Start ────────────────────────────────────────────────────────────────

    @Operation(summary = "Bắt đầu kiểm kê", description = "Chuyển DRAFT → IN_PROGRESS và khóa tất cả vị trí kệ. Roles: STOREKEEPER, ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Bắt đầu thành công"),
        @ApiResponse(responseCode = "422", description = "Trạng thái không hợp lệ")
    })
    @PutMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('STOREKEEPER', 'ADMIN')")
    public ResponseEntity<StockTakeResponse> startStockTake(@PathVariable Long id) {
        User actor = currentUserService.getRequiredCurrentUser();
        return ResponseEntity.ok(stockTakeService.startStockTake(id, actor));
    }

    // ─── Count ────────────────────────────────────────────────────────────────

    @Operation(summary = "Nhập số đếm thực tế", description = "Nhập actual_qty cho từng item. Hệ thống tự tính variance. Roles: STOREKEEPER, ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ghi nhận thành công"),
        @ApiResponse(responseCode = "400", description = "actual_qty âm (INVALID_COUNT_QTY) hoặc thiếu ghi chú lỗi NV (EMPLOYEE_FAULT_REASON_REQUIRED)"),
        @ApiResponse(responseCode = "422", description = "Phiếu không ở trạng thái IN_PROGRESS")
    })
    @PutMapping("/{id}/count")
    @PreAuthorize("hasAnyRole('STOREKEEPER', 'ADMIN')")
    public ResponseEntity<StockTakeResponse> recordCount(
            @PathVariable Long id,
            @Valid @RequestBody StockTakeCountRequest request) {
        User actor = currentUserService.getRequiredCurrentUser();
        return ResponseEntity.ok(stockTakeService.recordCount(id, request, actor));
    }

    // ─── Complete ─────────────────────────────────────────────────────────────

    @Operation(summary = "Hoàn tất đếm & trình duyệt", description = "Tính tổng chênh lệch, xác định cấp duyệt. Nếu AUTO thì phê duyệt ngay. Roles: STOREKEEPER, ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Thành công"),
        @ApiResponse(responseCode = "400", description = "Còn item chưa có actual_qty (INCOMPLETE_COUNT)")
    })
    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('STOREKEEPER', 'ADMIN')")
    public ResponseEntity<StockTakeResponse> completeStockTake(@PathVariable Long id) {
        User actor = currentUserService.getRequiredCurrentUser();
        return ResponseEntity.ok(stockTakeService.completeStockTake(id, actor));
    }

    // ─── Cancel ───────────────────────────────────────────────────────────────

    @Operation(summary = "Hủy phiếu kiểm kê", description = "Hủy phiếu khi DRAFT hoặc IN_PROGRESS. Giải phóng lock nếu đang IN_PROGRESS. Roles: STOREKEEPER, WAREHOUSE_MANAGER, ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Hủy thành công"),
        @ApiResponse(responseCode = "422", description = "Không thể hủy (STOCK_TAKE_NOT_CANCELLABLE)")
    })
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('STOREKEEPER', 'WAREHOUSE_MANAGER', 'ADMIN')")
    public ResponseEntity<StockTakeResponse> cancelStockTake(@PathVariable Long id) {
        User actor = currentUserService.getRequiredCurrentUser();
        return ResponseEntity.ok(stockTakeService.cancelStockTake(id, actor));
    }

    // ─── Approve (Manager) ────────────────────────────────────────────────────

    @Operation(summary = "Trưởng kho duyệt chênh lệch", description = "Duyệt phiếu kiểm kê ở cấp MANAGER. CEO cũng có thể gọi endpoint này.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Phê duyệt thành công"),
        @ApiResponse(responseCode = "403", description = "Không đúng cấp duyệt (APPROVAL_LEVEL_MISMATCH)"),
        @ApiResponse(responseCode = "409", description = "Đã được duyệt (STOCK_TAKE_ALREADY_APPROVED)"),
        @ApiResponse(responseCode = "422", description = "Kỳ kế toán đã đóng (ACCOUNTING_PERIOD_CLOSED)")
    })
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER', 'CEO', 'ADMIN')")
    public ResponseEntity<StockTakeResponse> approveStockTake(@PathVariable Long id) {
        User actor = currentUserService.getRequiredCurrentUser();
        return ResponseEntity.ok(stockTakeService.approveStockTake(id, actor));
    }

    // ─── Reject (Manager) ─────────────────────────────────────────────────────

    @Operation(summary = "Trưởng kho từ chối chênh lệch", description = "Từ chối phiếu kiểm kê. Phiếu quay về REJECTED, Thủ kho có thể sửa và nộp lại.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Từ chối thành công"),
        @ApiResponse(responseCode = "400", description = "Thiếu rejection_reason"),
        @ApiResponse(responseCode = "403", description = "APPROVAL_LEVEL_MISMATCH")
    })
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('WAREHOUSE_MANAGER', 'ADMIN')")
    public ResponseEntity<StockTakeResponse> rejectStockTake(
            @PathVariable Long id,
            @Valid @RequestBody StockTakeRejectRequest request) {
        User actor = currentUserService.getRequiredCurrentUser();
        return ResponseEntity.ok(stockTakeService.rejectStockTake(id, request, actor));
    }

    // ─── Approve (CEO) ────────────────────────────────────────────────────────

    @Operation(summary = "CEO duyệt chênh lệch", description = "CEO duyệt phiếu kiểm kê ở cấp CEO (hoặc khi cần override cấp MANAGER).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Phê duyệt thành công"),
        @ApiResponse(responseCode = "403", description = "Chỉ CEO mới có thể gọi endpoint này"),
        @ApiResponse(responseCode = "422", description = "Kỳ kế toán đã đóng")
    })
    @PutMapping("/{id}/approve-ceo")
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN')")
    public ResponseEntity<StockTakeResponse> approveCeoStockTake(@PathVariable Long id) {
        User actor = currentUserService.getRequiredCurrentUser();
        return ResponseEntity.ok(stockTakeService.approveCeoStockTake(id, actor));
    }

    // ─── Reject (CEO) ─────────────────────────────────────────────────────────

    @Operation(summary = "CEO từ chối chênh lệch", description = "CEO từ chối phiếu kiểm kê ở cấp CEO.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Từ chối thành công"),
        @ApiResponse(responseCode = "400", description = "Thiếu rejection_reason"),
        @ApiResponse(responseCode = "403", description = "Chỉ CEO mới có thể gọi endpoint này")
    })
    @PutMapping("/{id}/reject-ceo")
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN')")
    public ResponseEntity<StockTakeResponse> rejectCeoStockTake(
            @PathVariable Long id,
            @Valid @RequestBody StockTakeRejectRequest request) {
        User actor = currentUserService.getRequiredCurrentUser();
        return ResponseEntity.ok(stockTakeService.rejectCeoStockTake(id, request, actor));
    }
}
