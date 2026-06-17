package com.wms.controller;

import com.wms.dto.request.*;
import com.wms.dto.response.ReturnCreditNoteResponse;
import com.wms.entity.Receipt;
import com.wms.entity.User;
import com.wms.service.CurrentUserService;
import com.wms.service.ReturnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/returns")
@Tag(name = "Customer Returns", description = "Customer returns management and Credit Note processing (Spec 009)")
public class ReturnController {

    private final ReturnService returnService;
    private final CurrentUserService currentUserService;

    public ReturnController(ReturnService returnService,
                            CurrentUserService currentUserService) {
        this.returnService = returnService;
        this.currentUserService = currentUserService;
    }

    @Operation(
        summary = "Thủ kho lập phiếu nháp nhận hàng hoàn từ DO gốc",
        description = "Tạo receipt dạng RETURN ở trạng thái DRAFT. Đơn gốc DO phải ở trạng thái DELIVERED."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Phiếu hoàn hàng được lập nháp thành công"),
        @ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
        @ApiResponse(responseCode = "422", description = "Kỳ kế toán bị khóa hoặc trạng thái DO không hợp lệ")
    })
    @PostMapping
    public ResponseEntity<Receipt> createReturnReceipt(
            @Valid @RequestBody ReturnCreateRequest request) {
        User actor = currentUserService.getRequiredCurrentUser();
        Receipt receipt = returnService.createReturnReceipt(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(receipt);
    }

    @Operation(
        summary = "Thủ kho nhập kết quả kiểm đếm thực tế và kiểm QC (Split QC)",
        description = "Phân loại hàng đạt/lỗi. Số lượng trả thực tế không được vượt quá số lượng đã bán/xuất của DO gốc."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Kết quả QC được lưu, status phiếu thành QC_COMPLETED"),
        @ApiResponse(responseCode = "400", description = "Sai expectedVersion hoặc Passed+Failed không khớp Actual"),
        @ApiResponse(responseCode = "422", description = "Số lượng trả vượt quá số lượng đã bán (RETURN_EXCEEDS_ORIGINAL_SALE)")
    })
    @PutMapping("/{id}/qc")
    public ResponseEntity<Receipt> submitQc(
            @Parameter(description = "ID của return receipt") @PathVariable Long id,
            @Valid @RequestBody ReturnQcRequest request) {
        User actor = currentUserService.getRequiredCurrentUser();
        Receipt receipt = returnService.submitQc(id, request, actor);
        return ResponseEntity.ok(receipt);
    }

    @Operation(
        summary = "Trưởng kho phê duyệt phiếu nhận hàng hoàn",
        description = "Chuyển trạng thái phiếu sang APPROVED. Tạo/liên kết lô hàng hoàn (batch) cho hàng hoàn trả."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Phiếu nhận được duyệt thành công"),
        @ApiResponse(responseCode = "409", description = "Xung đột phiên bản dữ liệu hoặc sai trạng thái phiếu")
    })
    @PutMapping("/{id}/approve")
    public ResponseEntity<Receipt> approveReturn(
            @Parameter(description = "ID của return receipt") @PathVariable Long id,
            @Valid @RequestBody ReceiptDecisionRequest request) {
        User actor = currentUserService.getRequiredCurrentUser();
        Receipt receipt = returnService.approveReturn(id, request, actor);
        return ResponseEntity.ok(receipt);
    }

    @Operation(
        summary = "Thủ kho xác nhận putaway hàng hoàn cất vào ô kệ",
        description = "Cất hàng Đạt vào Bin thường (tăng regular stock) và hàng Lỗi vào Bin Quarantine (tăng quarantine stock)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Putaway hoàn tất, tồn kho khả dụng và quarantine đã tăng"),
        @ApiResponse(responseCode = "422", description = "Chỉ định sai loại ô kệ cho hàng Đạt/Lỗi")
    })
    @PutMapping("/{id}/complete")
    public ResponseEntity<Receipt> completePutaway(
            @Parameter(description = "ID của return receipt") @PathVariable Long id,
            @Valid @RequestBody ReturnPutawayRequest request) {
        User actor = currentUserService.getRequiredCurrentUser();
        Receipt receipt = returnService.completePutaway(id, request, actor);
        return ResponseEntity.ok(receipt);
    }

    @Operation(
        summary = "Kế toán viên lập Credit Note cấn trừ tiền hàng hoàn giảm công nợ cho Đại lý",
        description = "Chỉ thực hiện sau khi phiếu hoàn trả được duyệt APPROVED. Giá trị Credit Note = actualQty * đơn giá gốc DO."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Credit Note lập thành công, balance đại lý đã giảm"),
        @ApiResponse(responseCode = "409", description = "Credit Note cho phiếu hoàn này đã tồn tại trước đó"),
        @ApiResponse(responseCode = "422", description = "Kỳ kế toán bị đóng hoặc sai trạng thái phiếu")
    })
    @PostMapping("/{id}/credit-note")
    public ResponseEntity<ReturnCreditNoteResponse> createCreditNote(
            @Parameter(description = "ID của return receipt") @PathVariable Long id,
            @Valid @RequestBody ReturnCreditNoteRequest request) {
        User actor = currentUserService.getRequiredCurrentUser();
        ReturnCreditNoteResponse response = returnService.createCreditNote(id, request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
