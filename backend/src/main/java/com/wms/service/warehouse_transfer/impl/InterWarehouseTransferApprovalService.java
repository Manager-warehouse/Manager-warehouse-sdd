package com.wms.service.warehouse_transfer.impl;
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

import com.wms.dto.request.InterWarehouseTransferReasonRequest;
import com.wms.dto.response.InterWarehouseTransferResponse;
import com.wms.enums.warehouse_transfer.InterWarehouseTransferStatus;
import com.wms.enums.audit_trail.AuditAction;
import com.wms.repository.InterWarehouseTransferRepository;
import java.time.OffsetDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phụ trách bước trưởng kho nguồn duyệt hoặc từ chối phiếu.
 * Khi duyệt, hệ thống giữ hàng trong kho cho phiếu này; khi từ chối, phiếu bị đóng kèm lý do.
 */
@Service
@RequiredArgsConstructor
public class InterWarehouseTransferApprovalService {

    /*
     * Bước duyệt của kho nguồn. Khi phiếu còn mới, trưởng kho nguồn quyết định cho chuyển hàng hay không.
     * Duyệt là thời điểm giữ hàng thật trong kho; từ chối thì không động tới tồn kho.
     */
    private final InterWarehouseTransferRepository transferRepository;
    private final InterWarehouseTransferHelper helper;

    @Transactional
    public InterWarehouseTransferResponse approveTransfer(Long id, User actor) {
        // Phiếu mới -> đã duyệt: giữ hàng khả dụng theo nguyên tắc xuất trước trước khi điều phối viên gán xe.
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        helper.requireStatus(transfer, InterWarehouseTransferStatus.NEW);
        helper.ensureWarehouseScope(actor, transfer.getSourceWarehouse().getId());
        Map<String, Object> before = helper.snapshot(transfer);

        helper.allocateReservations(transfer);

        transfer.setStatus(InterWarehouseTransferStatus.APPROVED);
        transfer.setApprovedBy(actor);
        transfer.setApprovedAt(OffsetDateTime.now());
        transfer.setUpdatedAt(OffsetDateTime.now());

        InterWarehouseTransfer saved = transferRepository.save(transfer);
        helper.audit(saved, actor, AuditAction.TRANSFER_APPROVE, before, helper.snapshot(saved));
        return helper.toResponse(saved);
    }

    @Transactional
    public InterWarehouseTransferResponse rejectTransfer(Long id, InterWarehouseTransferReasonRequest request, User actor) {
        // Phiếu mới -> bị từ chối: không động tồn kho, bắt buộc có lý do để truy vết quyết định của trưởng kho.
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        helper.requireStatus(transfer, InterWarehouseTransferStatus.NEW);
        helper.ensureWarehouseScope(actor, transfer.getSourceWarehouse().getId());
        Map<String, Object> before = helper.snapshot(transfer);

        transfer.setStatus(InterWarehouseTransferStatus.REJECTED);
        transfer.setRejectedBy(actor);
        transfer.setRejectedAt(OffsetDateTime.now());
        transfer.setRejectionReason(helper.requiredReason(request, "REJECTION_REASON_REQUIRED"));
        transfer.setUpdatedAt(OffsetDateTime.now());

        InterWarehouseTransfer saved = transferRepository.save(transfer);
        helper.audit(saved, actor, AuditAction.TRANSFER_REJECT, before, helper.snapshot(saved));
        return helper.toResponse(saved);
    }
}
