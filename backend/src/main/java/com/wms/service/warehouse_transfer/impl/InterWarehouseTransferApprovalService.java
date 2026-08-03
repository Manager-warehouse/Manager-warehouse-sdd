package com.wms.service.warehouse_transfer.impl;
import com.wms.dto.request.InterWarehouseTransferReasonRequest;
import com.wms.dto.response.InterWarehouseTransferResponse;
import com.wms.entity.access_control.User;
import com.wms.entity.warehouse_transfer.InterWarehouseTransfer;
import com.wms.enums.audit_trail.AuditAction;
import com.wms.enums.warehouse_transfer.InterWarehouseTransferStatus;
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
     * LUỒNG DUYỆT PHIẾU ĐIỀU CHUYỂN:
     * - Các hàm public là hành động chính của trưởng kho nguồn/CEO/Admin: duyệt hoặc từ chối phiếu NEW.
     * - Không có helper riêng trong file này; các rule giữ/trả hàng, kiểm quyền và audit dùng InterWarehouseTransferHelper.
     */
    private final InterWarehouseTransferRepository transferRepository;
    private final InterWarehouseTransferHelper helper;

    @Transactional
    public InterWarehouseTransferResponse approveTransfer(Long id, User actor) {
        // HÀM CHÍNH: duyệt phiếu điều chuyển và giữ hàng khả dụng tại kho nguồn.
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
        // HÀM CHÍNH: từ chối phiếu điều chuyển khi còn NEW, không làm thay đổi tồn kho.
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
