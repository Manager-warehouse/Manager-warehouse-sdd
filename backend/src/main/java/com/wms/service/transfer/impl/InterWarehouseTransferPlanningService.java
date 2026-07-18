package com.wms.service.transfer.impl;

import com.wms.dto.request.InterWarehouseTransferCreateRequest;
import com.wms.dto.request.InterWarehouseTransferItemRequest;
import com.wms.dto.request.InterWarehouseTransferReasonRequest;
import com.wms.dto.request.InterWarehouseTransferUpdateRequest;
import com.wms.dto.response.InterWarehouseTransferResponse;
import com.wms.entity.*;
import com.wms.enums.InterWarehouseTransferStatus;
import com.wms.enums.AuditAction;
import com.wms.exception.BusinessRuleViolationException;
import com.wms.repository.InterWarehouseTransferItemRepository;
import com.wms.repository.InterWarehouseTransferRepository;
import com.wms.service.AccountingPeriodService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InterWarehouseTransferPlanningService {

    private final InterWarehouseTransferRepository transferRepository;
    private final InterWarehouseTransferItemRepository transferItemRepository;
    private final InterWarehouseTransferHelper helper;
    private final AccountingPeriodService accountingPeriodService;

    @Transactional
    public InterWarehouseTransferResponse createTransfer(InterWarehouseTransferCreateRequest request, User actor) {
        helper.ensureWarehouseScope(actor, request.sourceWarehouseId());
        ensureDifferentWarehouses(request.sourceWarehouseId(), request.destinationWarehouseId());
        ensureUniqueExternalInstruction(request.externalInstructionCode(), request.sourceWarehouseId(),
                request.destinationWarehouseId(), request.documentDate(), null);

        OffsetDateTime now = OffsetDateTime.now();
        InterWarehouseTransfer transfer = new InterWarehouseTransfer();
        transfer.setTransferNumber(helper.generateTransferNumber());
        applyTransferFields(transfer, request.externalInstructionCode(), request.sourceWarehouseId(),
                request.destinationWarehouseId(), request.documentDate(), request.plannedDate(), request.notes());
        transfer.setStatus(InterWarehouseTransferStatus.NEW);
        transfer.setCreatedBy(actor);
        transfer.setCreatedAt(now);
        transfer.setUpdatedAt(now);

        InterWarehouseTransfer saved = transferRepository.save(transfer);
        replaceItems(saved, request.items());
        helper.audit(saved, actor, AuditAction.CREATE, Map.of(), helper.snapshot(saved));
        return helper.toResponse(saved);
    }

    @Transactional
    public InterWarehouseTransferResponse updateTransfer(Long id, InterWarehouseTransferUpdateRequest request, User actor) {
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        helper.requireStatus(transfer, InterWarehouseTransferStatus.NEW);
        helper.ensureWarehouseScope(actor, transfer.getSourceWarehouse().getId());
        helper.ensureWarehouseScope(actor, request.sourceWarehouseId());
        ensureDifferentWarehouses(request.sourceWarehouseId(), request.destinationWarehouseId());
        ensureUniqueExternalInstruction(request.externalInstructionCode(), request.sourceWarehouseId(),
                request.destinationWarehouseId(), request.documentDate(), id);
        Map<String, Object> before = helper.snapshot(transfer);

        applyTransferFields(transfer, request.externalInstructionCode(), request.sourceWarehouseId(),
                request.destinationWarehouseId(), request.documentDate(), request.plannedDate(), request.notes());
        transfer.setUpdatedAt(OffsetDateTime.now());
        replaceItems(transfer, request.items());
        InterWarehouseTransfer saved = transferRepository.save(transfer);
        helper.audit(saved, actor, AuditAction.UPDATE, before, helper.snapshot(saved));
        return helper.toResponse(saved);
    }

    @Transactional
    public InterWarehouseTransferResponse cancelTransfer(Long id, InterWarehouseTransferReasonRequest request, User actor) {
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        helper.ensureWarehouseScope(actor, transfer.getSourceWarehouse().getId());
        Map<String, Object> before = helper.snapshot(transfer);
        if (transfer.getStatus() == InterWarehouseTransferStatus.APPROVED) {
            ensureNotLoaded(transfer);
            helper.releaseReservations(transfer);
        } else if (transfer.getStatus() != InterWarehouseTransferStatus.NEW) {
            throw new BusinessRuleViolationException("TRANSFER_CANCEL_NOT_ALLOWED");
        }
        transfer.setStatus(InterWarehouseTransferStatus.CANCELLED);
        transfer.setRejectionReason(helper.requiredReason(request, "CANCEL_REASON_REQUIRED"));
        transfer.setUpdatedAt(OffsetDateTime.now());
        InterWarehouseTransfer saved = transferRepository.save(transfer);
        helper.audit(saved, actor, AuditAction.TRANSFER_CANCEL, before, helper.snapshot(saved));
        return helper.toResponse(saved);
    }

    private void ensureDifferentWarehouses(Long sourceWarehouseId, Long destinationWarehouseId) {
        if (Objects.equals(sourceWarehouseId, destinationWarehouseId)) {
            throw new BusinessRuleViolationException("SOURCE_DESTINATION_MUST_DIFFER");
        }
    }

    private void ensureUniqueExternalInstruction(String code, Long sourceWarehouseId, Long destinationWarehouseId,
                                                 java.time.LocalDate documentDate, Long currentId) {
        boolean exists = currentId == null
                ? transferRepository.existsByExternalInstructionCodeAndSourceWarehouseIdAndDestinationWarehouseIdAndDocumentDateAndStatusNotIn(
                    code.trim(), sourceWarehouseId, destinationWarehouseId, documentDate, InterWarehouseTransferHelper.DUPLICATE_IGNORED_STATUSES)
                : transferRepository.existsByExternalInstructionCodeAndSourceWarehouseIdAndDestinationWarehouseIdAndDocumentDateAndStatusNotInAndIdNot(
                    code.trim(), sourceWarehouseId, destinationWarehouseId, documentDate, InterWarehouseTransferHelper.DUPLICATE_IGNORED_STATUSES, currentId);
        if (exists) {
            throw new BusinessRuleViolationException("DUPLICATE_EXTERNAL_INSTRUCTION");
        }
    }

    private void applyTransferFields(InterWarehouseTransfer transfer, String externalInstructionCode, Long sourceWarehouseId,
                                     Long destinationWarehouseId, java.time.LocalDate documentDate,
                                     java.time.LocalDate plannedDate, String notes) {
        transfer.setExternalInstructionCode(externalInstructionCode.trim());
        transfer.setSourceWarehouse(helper.reference(Warehouse.class, sourceWarehouseId));
        transfer.setDestinationWarehouse(helper.reference(Warehouse.class, destinationWarehouseId));
        transfer.setDocumentDate(documentDate);
        transfer.setAccountingPeriod(accountingPeriodService.resolveOpenPeriod(documentDate));
        transfer.setPlannedDate(plannedDate);
        transfer.setNotes(notes);
    }

    private void replaceItems(InterWarehouseTransfer transfer, List<InterWarehouseTransferItemRequest> requests) {
        transferItemRepository.deleteByTransferId(transfer.getId());
        for (InterWarehouseTransferItemRequest request : requests) {
            InterWarehouseTransferItem item = new InterWarehouseTransferItem();
            item.setTransfer(transfer);
            item.setProduct(helper.reference(Product.class, request.productId()));
            item.setSourceLocation(request.sourceLocationId() == null ? null : helper.reference(WarehouseLocation.class, request.sourceLocationId()));
            item.setDestinationLocation(request.destinationLocationId() == null ? null : helper.reference(WarehouseLocation.class, request.destinationLocationId()));
            item.setPlannedQty(request.plannedQty());
            transferItemRepository.save(item);
        }
    }

    private void ensureNotLoaded(InterWarehouseTransfer transfer) {
        if (helper.items(transfer).stream().anyMatch(item -> item.getSentQty() != null)) {
            throw new BusinessRuleViolationException("UNSHIP_REQUIRED_BEFORE_CANCEL");
        }
    }
}
