package com.wms.controller;

import com.wms.dto.response.QuarantineItemResponse;
import com.wms.entity.User;
import com.wms.service.CurrentUserService;
import com.wms.service.QuarantineRtvService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/quarantine")
@Tag(name = "Quarantine Management", description = "Quarantine workspace management and stock queries")
public class QuarantineController {

    private final QuarantineRtvService quarantineRtvService;
    private final CurrentUserService currentUserService;

    public QuarantineController(QuarantineRtvService quarantineRtvService,
                                CurrentUserService currentUserService) {
        this.quarantineRtvService = quarantineRtvService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/items")
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'STOREKEEPER', 'WAREHOUSE_MANAGER', 'ADMIN')")
    @Operation(
        summary = "Lấy danh sách hàng hóa lỗi cách ly (Quarantine Workspace)",
        description = "Lấy danh sách các sản phẩm đang bị cách ly (lỗi QC) chưa làm thủ tục xuất trả NCC (RTV)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Danh sách hàng cách ly trả về thành công"),
        @ApiResponse(responseCode = "403", description = "Không được phép truy cập kho này")
    })
    public ResponseEntity<List<QuarantineItemResponse>> getQuarantineItems(
            @RequestParam Long warehouseId) {
        User actor = currentUserService.getRequiredCurrentUser();
        List<QuarantineItemResponse> response = quarantineRtvService.getQuarantineItems(warehouseId, actor);
        return ResponseEntity.ok(response);
    }
}
