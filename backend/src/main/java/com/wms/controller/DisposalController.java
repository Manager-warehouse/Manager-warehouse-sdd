package com.wms.controller;

import com.wms.dto.request.DisposalApproveRequest;
import com.wms.dto.request.DisposalCreateRequest;
import com.wms.dto.response.DisposalApproveResponse;
import com.wms.dto.response.DisposalCreateResponse;
import com.wms.entity.User;
import com.wms.service.CurrentUserService;
import com.wms.service.DisposalService;
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
@RequestMapping("/api/v1/disposal")
@Tag(name = "Quarantine Disposal", description = "Quarantine disposal proposal and approval management (Spec 009)")
public class DisposalController {

    private final DisposalService disposalService;
    private final CurrentUserService currentUserService;

    public DisposalController(DisposalService disposalService,
                              CurrentUserService currentUserService) {
        this.disposalService = disposalService;
        this.currentUserService = currentUserService;
    }

    @Operation(
        summary = "Trưởng kho lập biên bản đề xuất hủy hàng lỗi từ Quarantine Zone",
        description = "Chỉ áp dụng cho hàng đang ở vị trí Quarantine. Tạo damage_reports và pending adjustments (type = 'DISPOSAL'). "
            + "KHÔNG trừ tồn Quarantine tại bước này."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Biên bản đề xuất tiêu hủy được tạo thành công"),
        @ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
        @ApiResponse(responseCode = "403", description = "Người dùng không được phân quyền vào kho này"),
        @ApiResponse(responseCode = "404", description = "Sản phẩm, lô hoặc vị trí không tồn tại trong Quarantine"),
        @ApiResponse(responseCode = "422", description = "Số lượng đề xuất vượt quá số lượng tồn Quarantine hiện tại")
    })
    @PostMapping
    public ResponseEntity<DisposalCreateResponse> createProposal(
            @Valid @RequestBody DisposalCreateRequest request) {
        User actor = currentUserService.getRequiredCurrentUser();
        DisposalCreateResponse response = disposalService.createProposal(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
        summary = "Phê duyệt tiêu hủy và giảm trừ tồn kho Quarantine",
        description = "Nếu giá trị tiêu hủy <= 100M VND, Trưởng kho có thể duyệt. Nếu > 100M VND, chỉ CEO mới có quyền duyệt."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tiêu hủy đã được phê duyệt, tồn kho đã trừ"),
        @ApiResponse(responseCode = "400", description = "Thiếu expectedVersion"),
        @ApiResponse(responseCode = "403", description = "Vượt hạn mức duyệt của Trưởng kho hoặc sai phân quyền"),
        @ApiResponse(responseCode = "404", description = "Đề xuất tiêu hủy không tồn tại"),
        @ApiResponse(responseCode = "422", description = "Tồn kho Quarantine thiếu hụt không đủ trừ")
    })
    @PutMapping("/{id}/approve")
    public ResponseEntity<DisposalApproveResponse> approveDisposal(
            @Parameter(description = "ID của Adjustment tiêu hủy") @PathVariable Long id,
            @Valid @RequestBody DisposalApproveRequest request) {
        User actor = currentUserService.getRequiredCurrentUser();
        DisposalApproveResponse response = disposalService.approveDisposal(id, request, actor);
        return ResponseEntity.ok(response);
    }
}
