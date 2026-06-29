package com.wms.service.transfer.impl;

import com.wms.dto.request.*;
import com.wms.dto.response.InterWarehouseTransferResponse;
import com.wms.entity.InterWarehouseTransfer;
import com.wms.entity.User;
import com.wms.exception.BusinessRuleViolationException;
import com.wms.repository.InterWarehouseTransferRepository;
import com.wms.service.transfer.InterWarehouseTransferService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InterWarehouseTransferServiceImpl implements InterWarehouseTransferService {

    private final InterWarehouseTransferRepository transferRepository;
    private final InterWarehouseTransferHelper helper;
    private final InterWarehouseTransferPlanningService planningService;
    private final InterWarehouseTransferApprovalService approvalService;
    private final InterWarehouseTransferShippingService shippingService;
    private final InterWarehouseTransferReceivingService receivingService;

    @Override
    @Transactional
    public List<InterWarehouseTransferResponse> getAllTransfers(User actor) {
        // Load warehouse assignments once to avoid N+1 queries in canViewTransfer
        List<Long> actorWarehouseIds = helper.loadWarehouseIds(actor);
        return transferRepository.findAllByOrderByCreatedAtDesc().stream()
                .peek(helper::applyTripDeadlineRules)
                .filter(transfer -> helper.canViewTransfer(actor, actorWarehouseIds, transfer))
                .map(transfer -> helper.toResponseEager(transfer))
                .toList();
    }

    @Override
    @Transactional
    public InterWarehouseTransferResponse getTransferById(Long id, User actor) {
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        helper.applyTripDeadlineRules(transfer);
        if (!helper.canViewTransfer(actor, transfer)) {
            throw new BusinessRuleViolationException("WAREHOUSE_SCOPE_REQUIRED");
        }
        return helper.toResponse(transfer);
    }

    @Override
    public InterWarehouseTransferResponse createTransfer(InterWarehouseTransferCreateRequest request, User actor) {
        return planningService.createTransfer(request, actor);
    }

    @Override
    public InterWarehouseTransferResponse updateTransfer(Long id, InterWarehouseTransferUpdateRequest request,
            User actor) {
        return planningService.updateTransfer(id, request, actor);
    }

    @Override
    public InterWarehouseTransferResponse cancelTransfer(Long id, InterWarehouseTransferReasonRequest request,
            User actor) {
        return planningService.cancelTransfer(id, request, actor);
    }

    @Override
    public InterWarehouseTransferResponse approveTransfer(Long id, User actor) {
        return approvalService.approveTransfer(id, actor);
    }

    @Override
    public InterWarehouseTransferResponse rejectTransfer(Long id, InterWarehouseTransferReasonRequest request,
            User actor) {
        return approvalService.rejectTransfer(id, request, actor);
    }

    @Override
    public InterWarehouseTransferResponse assignTrip(Long id, InterWarehouseTransferTripAssignRequest request,
            User actor) {
        return shippingService.assignTrip(id, request, actor);
    }

    @Override
    public InterWarehouseTransferResponse shipTransfer(Long id, User actor) {
        return shippingService.shipTransfer(id, actor);
    }

    @Override
    public InterWarehouseTransferResponse unshipTransfer(Long id, User actor) {
        return shippingService.unshipTransfer(id, actor);
    }

    @Override
    public InterWarehouseTransferResponse departTransfer(Long id, User actor) {
        return shippingService.departTransfer(id, actor);
    }

    @Override
    public InterWarehouseTransferResponse receiveCount(Long id, InterWarehouseTransferReceiveCountRequest request,
            User actor) {
        return receivingService.receiveCount(id, request, actor);
    }

    @Override
    public InterWarehouseTransferResponse receiveCheck(Long id, InterWarehouseTransferReceiveCheckRequest request,
            User actor) {
        return receivingService.receiveCheck(id, request, actor);
    }

    @Override
    public InterWarehouseTransferResponse finalReceive(Long id, InterWarehouseTransferFinalReceiveRequest request,
            User actor) {
        return receivingService.finalReceive(id, request, actor);
    }

    @Override
    public InterWarehouseTransferResponse returnToSource(Long id, User actor) {
        return receivingService.returnToSource(id, actor);
    }

    @Override
    public InterWarehouseTransferResponse quarantineReject(Long id, InterWarehouseTransferRejectRequest request,
            User actor) {
        return receivingService.quarantineReject(id, request, actor);
    }

    @Override
    public InterWarehouseTransferResponse requestReturn(Long id, TransferReturnRequest request, User actor) {
        return receivingService.requestReturn(id, request, actor);
    }

    @Override
    public InterWarehouseTransferResponse approveReturn(Long id, User actor) {
        return receivingService.approveReturn(id, actor);
    }

    @Override
    public InterWarehouseTransferResponse rejectReturn(Long id, TransferReturnRejectRequest request, User actor) {
        return receivingService.rejectReturn(id, request, actor);
    }
}
