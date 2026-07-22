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
import com.wms.dto.response.QuarantineItemResponse;
import com.wms.entity.access_control.User;
import com.wms.service.user_context.CurrentUserService;
import com.wms.service.stock_receiving.QuarantineRtvService;
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
