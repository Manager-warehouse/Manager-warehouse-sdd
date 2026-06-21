package com.wms.service.transfer.impl;

import com.wms.dto.request.InterWarehouseTransferReasonRequest;
import com.wms.dto.response.InterWarehouseTransferResponse;
import com.wms.entity.*;
import com.wms.enums.InterWarehouseTransferStatus;
import com.wms.enums.AuditAction;
import com.wms.repository.InterWarehouseTransferRepository;
import java.time.OffsetDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InterWarehouseTransferApprovalService {

    private final InterWarehouseTransferRepository transferRepository;
    private final InterWarehouseTransferHelper helper;

    @Transactional
    public InterWarehouseTransferResponse approveTransfer(Long id, User actor) {
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
