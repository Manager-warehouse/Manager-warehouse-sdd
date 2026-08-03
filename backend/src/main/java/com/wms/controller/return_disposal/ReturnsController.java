package com.wms.controller.return_disposal;


import com.wms.dto.request.CreateCreditNoteRequest;
import com.wms.dto.request.CreateReturnRequest;
import com.wms.dto.request.ReturnQcRequest;
import com.wms.dto.response.CreditNoteResponse;
import com.wms.dto.response.ReceiptActionResponse;
import com.wms.entity.access_control.User;
import com.wms.entity.dealer_management.Dealer;
import com.wms.entity.order_fulfillment.Delivery;
import com.wms.entity.stock_receiving.Receipt;
import com.wms.service.return_disposal.ReturnsService;
import com.wms.service.user_context.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/returns")
@Tag(name = "Customer Returns", description = "Customer returns management and dealer credit notes (Spec 009)")
public class ReturnsController {

    private final ReturnsService returnsService;
    private final CurrentUserService currentUserService;

    public ReturnsController(ReturnsService returnsService, CurrentUserService currentUserService) {
        this.returnsService = returnsService;
        this.currentUserService = currentUserService;
    }

    @Operation(
            summary = "Lập phiếu hoàn trả hàng từ đại lý",
            description = "Tạo phiếu nhập trả hàng (Receipt loại RETURN) ở trạng thái DRAFT. " +
                    "Kiểm tra xem số lượng trả có vượt quá số lượng thực tế đã bán trong Delivery Order gốc không."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tạo phiếu hoàn trả thành công"),
            @ApiResponse(responseCode = "400", description = "Thông tin không hợp lệ hoặc vượt quá giới hạn bán gốc"),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập kho"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy DO hoặc đại lý"),
            @ApiResponse(responseCode = "409", description = "Đại lý trả hàng không khớp với DO gốc")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('STOREKEEPER', 'WAREHOUSE_MANAGER', 'ADMIN', 'CEO')")
    public ResponseEntity<ReceiptActionResponse> createReturnReceipt(
            @Valid @RequestBody CreateReturnRequest request) {
        User actor = currentUserService.getRequiredCurrentUser();
        ReceiptActionResponse response = returnsService.createReturnReceipt(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Ghi nhận kết quả QC phân tách và nhập kho hàng trả",
            description = "Phân tách hàng trả thực tế: hàng đạt chuẩn (passed) đưa vào regular bin và tăng tồn kho thường; " +
                    "hàng lỗi (failed) đưa vào quarantine location và tăng tồn kho cách ly. " +
                    "Cập nhật trạng thái phiếu trả hàng thành APPROVED."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Phân tách QC và nhập kho thành công"),
            @ApiResponse(responseCode = "400", description = "Thông tin không hợp lệ"),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy phiếu hoặc vị trí"),
            @ApiResponse(responseCode = "409", description = "Xung đột phiên bản dữ liệu (optimistic locking)")
    })
    @PutMapping("/{id}/qc")
    @PreAuthorize("hasAnyRole('STOREKEEPER', 'WAREHOUSE_MANAGER', 'ADMIN', 'CEO')")
    public ResponseEntity<ReceiptActionResponse> processReturnQc(
            @Parameter(description = "ID của phiếu trả hàng") @PathVariable Long id,
            @Valid @RequestBody ReturnQcRequest request) {
        User actor = currentUserService.getRequiredCurrentUser();
        ReceiptActionResponse response = returnsService.processReturnQc(id, request, actor);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Tạo Credit Note hoàn trả công nợ cho đại lý",
            description = "Sinh Credit Note hoàn trả tiền và khấu trừ trực tiếp vào công nợ hiện tại của Dealer. " +
                    "Chỉ thực hiện cho các phiếu trả hàng đã được duyệt APPROVED."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sinh Credit Note và khấu trừ công nợ thành công"),
            @ApiResponse(responseCode = "400", description = "Phiếu chưa được duyệt hoặc không hợp lệ"),
            @ApiResponse(responseCode = "403", description = "Không có vai trò kế toán hoặc quản lý"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy phiếu"),
            @ApiResponse(responseCode = "409", description = "Credit Note đã tồn tại cho phiếu hoàn trả này (CREDIT_NOTE_ALREADY_EXISTS)")
    })
    @PostMapping("/{id}/credit-note")
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ACCOUNTANT_MANAGER', 'ADMIN', 'CEO')")
    public ResponseEntity<CreditNoteResponse> createCreditNote(
            @Parameter(description = "ID của phiếu trả hàng") @PathVariable Long id,
            @Valid @RequestBody CreateCreditNoteRequest request) {
        User actor = currentUserService.getRequiredCurrentUser();
        CreditNoteResponse response = returnsService.createCreditNote(id, request, actor);
        return ResponseEntity.ok(response);
    }
}
