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

import com.wms.dto.request.*;
import com.wms.dto.response.InterWarehouseTransferResponse;
import com.wms.exception.BusinessRuleViolationException;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.*;
import com.wms.repository.product_catalog.ProductRepository;
import com.wms.util.PartnerAuditUtil;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phụ trách giai đoạn nhận hàng và xử lý ngoại lệ tại kho nhận hoặc kho nguồn khi xe quay đầu.
 * Class này xử lý các bước: đếm hàng nhận, QC tại kho nhận, đề xuất vị trí nhập kho,
 * duyệt nhập kho cuối, đưa hàng lỗi vào khu cách ly, ghi hồ sơ chênh lệch và xử lý xe quay đầu khi giao sai mã hàng.
 */
@Service
@RequiredArgsConstructor
public class InterWarehouseTransferReceivingService {
    private static final String PUTAWAY_PLAN_PREFIX = "TRANSFER_PUTAWAY_PLAN:";

    /*
     * LUỒNG NHẬN HÀNG, QC NHẬN, NHẬP KHO VÀ QUAY ĐẦU:
     * - Các hàm public là hành động chính trên giao diện: đếm hàng, QC nhận, nhập kho cuối, cách ly, yêu cầu/duyệt/bác quay đầu.
     * - Các hàm private là hàm hỗ trợ: validate từng dòng, kiểm deadline, resolve putaway, chuyển tồn từ transit về kho/quarantine.
     *
     * Xử lý phiếu đang trên đường. Nếu luồng bình thường thì kho đích nhận hàng;
     * nếu xe quay đầu thì kho nguồn nhận lại hàng.
     * Thứ tự đúng: công nhân đếm hàng -> thủ kho kiểm/QC -> quản lý duyệt nhập kho cuối.
     * Hàng đạt được nhập vào vị trí thường, hàng lỗi vào khu cách ly, thiếu/thừa được ghi thành hồ sơ chênh lệch.
     */
    private final InterWarehouseTransferRepository transferRepository;
    private final InterWarehouseTransferItemRepository transferItemRepository;
    private final InterWarehouseTransferAllocationRepository allocationRepository;
    private final InventoryRepository inventoryRepository;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseLocationRepository locationRepository;
    private final AdjustmentRepository adjustmentRepository;
    private final PartnerAuditUtil auditUtil;
    private final InterWarehouseTransferHelper helper;
    private final QuarantineRecordRepository quarantineRecordRepository;
    private final DiscrepancyIncidentRepository discrepancyIncidentRepository;
    private final DiscrepancyHoldEntryRepository discrepancyHoldEntryRepository;
    private final ProductRepository productRepository;
    private final WrongSkuReportRepository wrongSkuReportRepository;
    private final WrongSkuReportItemRepository wrongSkuReportItemRepository;

    @Transactional
    public InterWarehouseTransferResponse receiveCount(Long id, InterWarehouseTransferReceiveCountRequest request, User actor) {
        // HÀM CHÍNH: công nhân kho nhận nhập số lượng thực nhận sau khi có bàn giao.
        // Công nhân nhập số lượng thực nhận. Xe phải được xác nhận đã đến và đã bàn giao ảnh/chứng từ trước.
        // Nếu số thực nhận khác số đã gửi thì phải nhập lý do để truy vết chênh lệch.
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        helper.requireStatus(transfer, InterWarehouseTransferStatus.IN_TRANSIT);
        helper.ensureWarehouseScope(actor, transfer.isReturned() ? transfer.getSourceWarehouse().getId() : transfer.getDestinationWarehouse().getId());
        ensureDestinationReceivingNotOverdue(transfer);

        if (Boolean.TRUE.equals(transfer.isReturned())) {
            // Validate: xe quay đầu phải được tài xế xác nhận đã về kho nguồn trước khi kho nguồn đếm hàng.
            if (transfer.getReturnArrivedAt() == null) {
                throw new BusinessRuleViolationException("RETURN_ARRIVE_REQUIRED");
            }
            // Validate: kho nguồn phải có ảnh/bản ghi bàn giao hàng quay về trước khi nhập số lượng nhận.
            if (transfer.getReturnArrivalHandoverAt() == null) {
                throw new BusinessRuleViolationException("RETURN_HANDOVER_REQUIRED");
            }
        } else {
            // Validate: xe phải đến kho đích trước khi công nhân kho đích nhập số lượng nhận.
            if (transfer.getDriverArrivedAt() == null) {
                throw new BusinessRuleViolationException("DRIVER_ARRIVE_REQUIRED");
            }
            // Validate: kho đích phải nhận bàn giao có ảnh trước khi đếm, tránh ghi nhận hàng chưa thật sự bàn giao.
            if (transfer.getArrivalHandoverAt() == null) {
                throw new BusinessRuleViolationException("ARRIVAL_HANDOVER_REQUIRED");
            }
        }

        Map<Long, InterWarehouseTransferItem> itemById = helper.itemMap(transfer);
        // Validate: dữ liệu đếm phải có đủ mọi dòng hàng trong phiếu, không cho bỏ sót dòng.
        if (request.items().size() != itemById.size()) {
            throw new BusinessRuleViolationException("RECEIVE_COUNT_ITEMS_REQUIRED");
        }
        Set<Long> countedItemIds = new HashSet<>();
        Map<String, Object> before = helper.snapshot(transfer);
        for (InterWarehouseTransferReceiveCountItemRequest line : request.items()) {
            // Validate: mỗi dòng hàng chỉ được nhập số đếm một lần trong cùng lần gửi dữ liệu.
            if (!countedItemIds.add(line.transferItemId())) {
                throw new BusinessRuleViolationException("DUPLICATE_RECEIVE_COUNT_ITEM");
            }
            ensureWholeQuantity(line.receivedQty());
            InterWarehouseTransferItem item = helper.requireItem(itemById, line.transferItemId());
            // Validate: số đếm khác số đã gửi là chênh lệch tại điểm nhận, bắt buộc nhập lý do.
            if (line.receivedQty().compareTo(item.getSentQty()) != 0 && helper.isBlank(line.issueReason())) {
                throw new BusinessRuleViolationException("ISSUE_REASON_REQUIRED");
            }
            item.setWorkerReceivedQty(line.receivedQty());
            item.setIssueReason(line.issueReason());
            transferItemRepository.save(item);
        }
        helper.audit(transfer, actor, AuditAction.TRANSFER_RECEIVE_COUNT, before, helper.snapshot(transfer));
        return helper.toResponse(transfer);
    }

    @Transactional
    public InterWarehouseTransferResponse receiveCheck(Long id, InterWarehouseTransferReceiveCheckRequest request, User actor) {
        // HÀM CHÍNH: thủ kho/QL kho nhận kiểm đếm lại và ghi kết quả QC nhận.
        // Thủ kho kiểm lại số công nhân đã đếm và làm QC. Bắt buộc có ảnh QC;
        // tổng số đạt và số lỗi phải bằng số thủ kho xác nhận.
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        helper.requireStatus(transfer, InterWarehouseTransferStatus.IN_TRANSIT);
        helper.ensureWarehouseScope(actor, transfer.isReturned() ? transfer.getSourceWarehouse().getId() : transfer.getDestinationWarehouse().getId());
        ensureDestinationReceivingNotOverdue(transfer);
        // Validate: bước QC nhận bắt buộc có ảnh để CEO hoặc quản lý kho xem lại bằng chứng.
        if (helper.isBlank(request.qcPhotoRef())) {
            throw new BusinessRuleViolationException("RECEIVE_QC_PHOTO_REQUIRED");
        }
        Map<Long, InterWarehouseTransferItem> itemById = helper.itemMap(transfer);
        // Validate: QC phải kiểm đủ mọi dòng hàng đã gửi trong phiếu.
        if (request.items().size() != itemById.size()) {
            throw new BusinessRuleViolationException("RECEIVE_CHECK_ITEMS_REQUIRED");
        }
        Set<Long> checkedItemIds = new HashSet<>();
        Map<String, Object> before = helper.snapshot(transfer);
        for (InterWarehouseTransferReceiveCheckItemRequest line : request.items()) {
            // Validate: mỗi dòng hàng chỉ có một kết quả QC trong cùng lần gửi dữ liệu.
            if (!checkedItemIds.add(line.transferItemId())) {
                throw new BusinessRuleViolationException("DUPLICATE_RECEIVE_CHECK_ITEM");
            }
            ensureWholeQuantity(line.confirmedQty());
            ensureWholeQuantity(line.qcPassedQty());
            ensureWholeQuantity(line.qcFailedQty());
            InterWarehouseTransferItem item = helper.requireItem(itemById, line.transferItemId());
            validateReceiveCheckLine(transfer, item, line);
            item.setReceivedQty(line.confirmedQty());
            item.setQcPassedQty(line.qcPassedQty());
            item.setQcFailedQty(line.qcFailedQty());
            item.setDestinationLocation(line.destinationLocationId() == null
                    ? null
                    : helper.reference(WarehouseLocation.class, line.destinationLocationId()));
            item.setCheckerNote(line.checkerNote());
            item.setQcFailureReason(line.qcFailureReason());
            item.setCheckedBy(actor);
            item.setCheckedAt(OffsetDateTime.now());
            item.setVarianceQty(line.confirmedQty().subtract(item.getSentQty()));
            transferItemRepository.save(item);
        }
        transfer.setReceiveQcPhotoRef(request.qcPhotoRef());
        transfer.setUpdatedAt(OffsetDateTime.now());
        helper.audit(transfer, actor, AuditAction.TRANSFER_RECEIVE_CHECK, before, helper.snapshot(transfer));
        return helper.toResponse(transfer);
    }

    @Transactional
    public InterWarehouseTransferResponse finalReceive(Long id, InterWarehouseTransferFinalReceiveRequest request, User actor) {
        // HÀM CHÍNH: thủ kho nộp putaway plan hoặc quản lý duyệt nhập kho cuối.
        // Nhập kho cuối có 2 bước: thủ kho nộp kế hoạch đưa hàng vào vị trí,
        // sau đó quản lý kho/CEO/Admin duyệt thì hệ thống mới ghi tăng tồn và đóng phiếu.
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        helper.ensureWarehouseScope(actor, transfer.isReturned() ? transfer.getSourceWarehouse().getId() : transfer.getDestinationWarehouse().getId());
        ensureAllChecked(transfer);
        // Validate: thủ kho chỉ được nộp kế hoạch nhập vị trí; người duyệt cuối phải là quản lý kho/CEO/Admin.
        if (transfer.getStatus() == InterWarehouseTransferStatus.IN_TRANSIT
                && actor.getRole() == UserRole.STOREKEEPER) {
            return submitPutawayPlan(transfer, request, actor);
        }
        helper.requireStatus(transfer, InterWarehouseTransferStatus.PUTAWAY_PENDING_APPROVAL);
        // Validate: thủ kho không được tự duyệt nhập kho cuối cho kế hoạch do chính mình nộp.
        if (actor.getRole() == UserRole.STOREKEEPER) {
            throw new BusinessRuleViolationException("WAREHOUSE_MANAGER_APPROVAL_REQUIRED");
        }
        InterWarehouseTransferFinalReceiveRequest approvedRequest = request.putawayItems() == null
                ? new InterWarehouseTransferFinalReceiveRequest(request.discrepancyReason(), parsePutawayPlan(transfer.getNotes()))
                : request;
        boolean discrepancy = hasReceiveDiscrepancy(transfer) || hasPutawayDiscrepancy(transfer, approvedRequest);
        String discrepancyReason = helper.isBlank(request.discrepancyReason())
                ? transfer.getDiscrepancyReason()
                : request.discrepancyReason();
        // Validate: nếu có thiếu/thừa hoặc kế hoạch nhập vị trí không khớp số hàng QC đạt thì bắt buộc có lý do.
        if (discrepancy && helper.isBlank(discrepancyReason)) {
            throw new BusinessRuleViolationException("DISCREPANCY_REASON_REQUIRED");
        }
        Map<String, Object> before = helper.snapshot(transfer);
        moveTransitToDestination(transfer, new InterWarehouseTransferFinalReceiveRequest(discrepancyReason,
                approvedRequest.putawayItems()), actor);
        transfer.setStatus(discrepancy ? InterWarehouseTransferStatus.COMPLETED_WITH_DISCREPANCY : InterWarehouseTransferStatus.COMPLETED);
        transfer.setDiscrepancyReason(discrepancyReason);
        transfer.setConfirmedBy(actor);
        transfer.setConfirmedAt(OffsetDateTime.now());
        transfer.setActualReceivedDate(OffsetDateTime.now().toLocalDate());
        transfer.setUpdatedAt(OffsetDateTime.now());
        transfer.getTrip().getDriver().setStatus(DriverStatus.AVAILABLE);
        transfer.getTrip().getVehicle().setStatus(VehicleStatus.AVAILABLE);
        transfer.getTrip().setStatus(TripStatus.COMPLETED);
        transfer.getTrip().setUpdatedAt(OffsetDateTime.now());
        InterWarehouseTransfer saved = transferRepository.save(transfer);
        helper.audit(saved, actor, AuditAction.TRANSFER_FINAL_RECEIVE, before, helper.snapshot(saved));
        return helper.toResponse(saved);
    }

    private InterWarehouseTransferResponse submitPutawayPlan(InterWarehouseTransfer transfer,
                                                             InterWarehouseTransferFinalReceiveRequest request,
                                                             User actor) {
        // Thủ kho chỉ đề xuất vị trí đặt hàng; tồn kho chưa tăng cho tới khi quản lý duyệt cuối.
        // Validate: nếu có hàng đạt QC thì phải có kế hoạch cất kệ; nếu toàn bộ hàng lỗi QC thì cho gửi plan rỗng để quản lý duyệt đưa vào quarantine.
        if ((request.putawayItems() == null || request.putawayItems().isEmpty()) && hasQcPassedStock(transfer)) {
            throw new BusinessRuleViolationException("PUTAWAY_PLAN_REQUIRED");
        }
        Map<Long, List<PutawayTarget>> plans = resolveFinalPutawayPlans(transfer, request);
        boolean discrepancy = hasReceiveDiscrepancy(transfer) || hasPutawayDiscrepancy(transfer, request);
        String discrepancyReason = resolveDiscrepancyReason(transfer, request.discrepancyReason());
        // Validate: chỉ bắt nhập ở bước cất kệ khi trước đó chưa có lý do count/QC nào để kế thừa.
        if (discrepancy && helper.isBlank(discrepancyReason)) {
            throw new BusinessRuleViolationException("DISCREPANCY_REASON_REQUIRED");
        }
        Map<String, Object> before = helper.snapshot(transfer);
        for (InterWarehouseTransferItem item : helper.items(transfer)) {
            List<PutawayTarget> targets = plans.get(item.getId());
            if (targets != null && !targets.isEmpty()) {
                item.setDestinationLocation(targets.get(0).location());
                transferItemRepository.save(item);
            }
        }
        transfer.setStatus(InterWarehouseTransferStatus.PUTAWAY_PENDING_APPROVAL);
        transfer.setDiscrepancyReason(discrepancyReason);
        transfer.setNotes(serializePutawayPlan(request.putawayItems() == null ? List.of() : request.putawayItems()));
        transfer.setUpdatedAt(OffsetDateTime.now());
        InterWarehouseTransfer saved = transferRepository.save(transfer);
        helper.audit(saved, actor, AuditAction.TRANSFER_FINAL_RECEIVE, before, helper.snapshot(saved));
        return helper.toResponse(saved);
    }

    private String resolveDiscrepancyReason(InterWarehouseTransfer transfer, String requestReason) {
        // Lý do chênh lệch có thể đã được nhập ở bước count/QC; bước cất kệ chỉ cần nhập thêm nếu chưa có căn cứ.
        if (!helper.isBlank(requestReason)) {
            return requestReason.trim();
        }
        return helper.items(transfer).stream()
                .map(item -> !helper.isBlank(item.getIssueReason()) ? item.getIssueReason() : item.getQcFailureReason())
                .filter(reason -> !helper.isBlank(reason))
                .findFirst()
                .map(String::trim)
                .orElse(null);
    }

    private boolean hasQcPassedStock(InterWarehouseTransfer transfer) {
        // Chỉ hàng đạt QC mới cần kế hoạch cất kệ thường; hàng lỗi đi quarantine khi quản lý duyệt.
        return helper.items(transfer).stream()
                .anyMatch(item -> helper.zero(item.getQcPassedQty()).signum() > 0);
    }

    private boolean hasReceiveDiscrepancy(InterWarehouseTransfer transfer) {
        // Chênh lệch nhận xảy ra khi số thủ kho xác nhận khác số đã gửi từ kho nguồn.
        return helper.items(transfer).stream()
                .anyMatch(item -> helper.zero(item.getVarianceQty()).compareTo(BigDecimal.ZERO) != 0);
    }

    private boolean hasPutawayDiscrepancy(InterWarehouseTransfer transfer,
                                          InterWarehouseTransferFinalReceiveRequest request) {
        // Chênh lệch nhập vị trí xảy ra khi tổng số đưa vào vị trí không bằng số hàng QC đạt.
        if (request.putawayItems() == null) {
            return false;
        }
        Map<Long, BigDecimal> plannedQtyByItem = request.putawayItems().stream()
                .collect(java.util.stream.Collectors.toMap(
                        InterWarehouseTransferFinalPutawayItemRequest::transferItemId,
                        item -> item.allocations().stream()
                                .map(InterWarehouseTransferPutawayAllocationRequest::quantity)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)));
        return helper.items(transfer).stream()
                .filter(item -> helper.zero(item.getQcPassedQty()).signum() > 0)
                .anyMatch(item -> helper.zero(plannedQtyByItem.get(item.getId()))
                        .compareTo(helper.zero(item.getQcPassedQty())) != 0);
    }

    private String serializePutawayPlan(List<InterWarehouseTransferFinalPutawayItemRequest> plans) {
        // Lưu tạm kế hoạch nhập vị trí vào ghi chú để quản lý có thể duyệt lại mà không cần gửi lại toàn bộ dữ liệu.
        return PUTAWAY_PLAN_PREFIX + plans.stream()
                .map(item -> item.transferItemId() + "=" + item.allocations().stream()
                        .map(allocation -> allocation.locationId() + ":" + allocation.quantity())
                        .collect(java.util.stream.Collectors.joining(",")))
                .collect(java.util.stream.Collectors.joining(";"));
    }

    private List<InterWarehouseTransferFinalPutawayItemRequest> parsePutawayPlan(String notes) {
        // Quản lý duyệt cuối có thể dùng lại kế hoạch thủ kho đã nộp trước đó.
        if (notes == null || !notes.startsWith(PUTAWAY_PLAN_PREFIX)) {
            throw new BusinessRuleViolationException("PUTAWAY_PLAN_REQUIRED");
        }
        String body = notes.substring(PUTAWAY_PLAN_PREFIX.length());
        if (helper.isBlank(body)) {
            return List.of();
        }
        java.util.ArrayList<InterWarehouseTransferFinalPutawayItemRequest> items = new java.util.ArrayList<>();
        for (String itemPart : body.split(";")) {
            String[] pair = itemPart.split("=", 2);
            if (pair.length != 2) {
                throw new BusinessRuleViolationException("PUTAWAY_PLAN_INVALID");
            }
            java.util.ArrayList<InterWarehouseTransferPutawayAllocationRequest> allocations = new java.util.ArrayList<>();
            for (String allocationPart : pair[1].split(",")) {
                String[] allocation = allocationPart.split(":", 2);
                if (allocation.length != 2) {
                    throw new BusinessRuleViolationException("PUTAWAY_PLAN_INVALID");
                }
                allocations.add(new InterWarehouseTransferPutawayAllocationRequest(
                        Long.valueOf(allocation[0]), new BigDecimal(allocation[1])));
            }
            items.add(new InterWarehouseTransferFinalPutawayItemRequest(Long.valueOf(pair[0]), allocations));
        }
        return items;
    }

    @Transactional
    public InterWarehouseTransferResponse returnToSource(Long id, TransferReturnRequest request, User actor) {
        // HÀM CHÍNH: quản lý kho nguồn chủ động yêu cầu xe quay đầu trước khi kho đích nhận bàn giao.
        // Quản lý kho nguồn chủ động cho xe quay đầu khi hàng còn trên đường,
        // trước khi kho đích xác nhận xe đến hoặc nhận bàn giao.
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        helper.requireStatus(transfer, InterWarehouseTransferStatus.IN_TRANSIT);
        // Validate: chỉ trưởng kho nguồn được chủ động yêu cầu xe quay đầu; CEO/Admin không tạo yêu cầu thay nghiệp vụ kho.
        if (actor.getRole() != UserRole.WAREHOUSE_MANAGER) {
            throw new BusinessRuleViolationException("WAREHOUSE_MANAGER_ROLE_REQUIRED");
        }
        ensureManagerCanRequestReturn(transfer, actor);

        // Validate: quay đầu xe luôn cần lý do để lưu trên phiếu và lịch sử thao tác.
        if (helper.isBlank(request.reason())) {
            throw new BusinessRuleViolationException("RETURN_REASON_REQUIRED");
        }
        ensureReturnNotAlreadyInProgress(transfer);
        // Validate: kho nguồn chỉ được cho xe quay đầu khi kho đích chưa xác nhận xe đến và chưa nhận bàn giao.
        if (transfer.getDriverArrivedAt() != null || transfer.getArrivalHandoverAt() != null) {
            throw new BusinessRuleViolationException("SOURCE_RETURN_ONLY_BEFORE_DESTINATION_ARRIVAL");
        }

        Map<String, Object> before = helper.snapshot(transfer);
        transfer.setReturned(true);
        transfer.setReturnReason(request.reason());
        transfer.setUpdatedAt(OffsetDateTime.now());

        if (request.wrongSkuItems() != null && !request.wrongSkuItems().isEmpty()) {
            WrongSkuReport report = WrongSkuReport.builder()
                    .transfer(transfer)
                    .status("APPROVED") // Tự duyệt vì yêu cầu này do quản lý kho nguồn hoặc người lập phiếu tạo ra.
                    .reportedBy(actor)
                    .reportedAt(OffsetDateTime.now())
                    .managerDecisionBy(actor)
                    .managerDecisionAt(OffsetDateTime.now())
                    .build();
            report = wrongSkuReportRepository.save(report);

            saveWrongSkuItems(transfer, report, request.wrongSkuItems());
        }

        InterWarehouseTransfer saved = transferRepository.save(transfer);
        helper.audit(saved, actor, AuditAction.TRANSFER_RETURN_TO_SOURCE, before, helper.snapshot(saved));
        return helper.toResponse(saved);
    }

    private void ensureManagerCanRequestReturn(InterWarehouseTransfer transfer, User actor) {
        // Quản lý chỉ được yêu cầu quay đầu cho phiếu thuộc kho nguồn mình phụ trách.
        List<Long> warehouseIds = helper.loadWarehouseIds(actor);
        Long sourceWarehouseId = transfer.getSourceWarehouse().getId();
        // Validate: quản lý không thuộc kho nguồn thì không được yêu cầu quay đầu xe.
        if (!warehouseIds.contains(sourceWarehouseId)) {
            throw new BusinessRuleViolationException("WAREHOUSE_SCOPE_REQUIRED");
        }
    }

    @Transactional
    public InterWarehouseTransferResponse quarantineReject(Long id, InterWarehouseTransferRejectRequest request, User actor) {
        // HÀM CHÍNH: kho nhận từ chối toàn bộ và đưa hàng đang vận chuyển vào khu cách ly.
        // Từ chối toàn bộ chỉ sau khi xe đã đến, đã bàn giao và công nhân đã đếm hàng.
        // Toàn bộ hàng đang trên xe sẽ được đưa vào khu cách ly để xử lý sau.
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        helper.requireStatus(transfer, InterWarehouseTransferStatus.IN_TRANSIT);
        Long targetWarehouseId = transfer.isReturned() ? transfer.getSourceWarehouse().getId() : transfer.getDestinationWarehouse().getId();
        helper.ensureWarehouseScope(actor, targetWarehouseId);

        // Validate: từ chối/cách ly toàn bộ phải có lý do rõ ràng.
        if (helper.isBlank(request.getRejectionReason())) {
            throw new BusinessRuleViolationException("REJECTION_REASON_REQUIRED");
        }
        ensureQuarantineRejectGate(transfer);

        Map<String, Object> before = helper.snapshot(transfer);
        transfer.setRejectionReason(request.getRejectionReason());

        moveTransitToQuarantine(transfer, actor);

        for (InterWarehouseTransferItem item : helper.items(transfer)) {
            BigDecimal qty = item.getSentQty() != null ? item.getSentQty() : item.getPlannedQty();
            item.setReceivedQty(qty);
            item.setQcPassedQty(BigDecimal.ZERO);
            item.setQcFailedQty(qty);
            item.setCheckerNote(request.getRejectionReason());
            item.setQcFailureReason(request.getRejectionReason());
            item.setCheckedBy(actor);
            item.setCheckedAt(OffsetDateTime.now());
            item.setVarianceQty(BigDecimal.ZERO);
            transferItemRepository.save(item);
        }

        transfer.setStatus(InterWarehouseTransferStatus.QUARANTINED);
        transfer.setRejectedBy(actor);
        transfer.setRejectedAt(OffsetDateTime.now());
        transfer.setUpdatedAt(OffsetDateTime.now());

        if (transfer.getTrip() != null) {
            transfer.getTrip().getDriver().setStatus(DriverStatus.AVAILABLE);
            transfer.getTrip().getVehicle().setStatus(VehicleStatus.AVAILABLE);
            transfer.getTrip().setStatus(TripStatus.COMPLETED);
            transfer.getTrip().setUpdatedAt(OffsetDateTime.now());
        }

        InterWarehouseTransfer saved = transferRepository.save(transfer);
        helper.audit(saved, actor, AuditAction.TRANSFER_QUARANTINE_REJECT, before, helper.snapshot(saved));
        return helper.toResponse(saved);
    }

    private void validateReceiveCheckLine(InterWarehouseTransfer transfer, InterWarehouseTransferItem item, InterWarehouseTransferReceiveCheckItemRequest line) {
        // Validate một dòng QC nhận: phải có số công nhân đếm trước, nếu thủ kho xác nhận lệch thì phải ghi chú,
        // hàng lỗi phải có lý do và kho nhận phải có khu cách ly.
        // Validate: không được QC khi công nhân chưa nhập số đếm ban đầu.
        if (item.getWorkerReceivedQty() == null) {
            throw new BusinessRuleViolationException("WORKER_COUNT_REQUIRED");
        }
        // Validate: thủ kho xác nhận khác số công nhân đếm thì phải ghi note giải trình.
        if (line.confirmedQty().compareTo(item.getWorkerReceivedQty()) != 0 && helper.isBlank(line.checkerNote())) {
            throw new BusinessRuleViolationException("CHECKER_NOTE_REQUIRED");
        }
        // Validate: tổng số đạt và số lỗi phải khớp số thủ kho xác nhận, không để thất thoát ngoài QC.
        if (line.qcPassedQty().add(line.qcFailedQty()).compareTo(line.confirmedQty()) != 0) {
            throw new BusinessRuleViolationException("QC_TOTAL_MUST_MATCH_CONFIRMED_QTY");
        }
        // Validate: có hàng lỗi QC thì bắt buộc nhập lý do để tạo hồ sơ cách ly và truy vết sau này.
        if (line.qcFailedQty().signum() > 0 && helper.isBlank(line.qcFailureReason())) {
            throw new BusinessRuleViolationException("QC_FAILURE_REASON_REQUIRED");
        }
        Long targetWarehouseId = transfer.isReturned() ? transfer.getSourceWarehouse().getId() : transfer.getDestinationWarehouse().getId();
        if (line.destinationLocationId() != null) {
            validateDestinationLocation(line.destinationLocationId(), targetWarehouseId);
        }
        if (line.qcFailedQty().signum() > 0) {
            boolean hasQuarantine = !locationRepository.findByWarehouseIdAndIsQuarantineTrueAndIsActiveTrue(targetWarehouseId).isEmpty();
            // Validate: kho nhận phải cấu hình khu cách ly trước khi cho ghi nhận hàng lỗi QC.
            if (!hasQuarantine) {
                throw new BusinessRuleViolationException("QUARANTINE_LOCATION_NOT_CONFIGURED");
            }
        }
    }

    private void ensureDestinationReceivingNotOverdue(InterWarehouseTransfer transfer) {
        // Chặn nhận hàng trễ hạn ở kho đích. Khi xe quay đầu về kho nguồn thì không dùng kiểm tra quá hạn này.
        // Validate: phiếu quá hạn thời gian dự kiến không được nhận bình thường, phải xử lý như ngoại lệ.
        if (!Boolean.TRUE.equals(transfer.isReturned()) && helper.isTripOverdue(transfer)) {
            throw new BusinessRuleViolationException("TRANSFER_TRIP_OVERDUE");
        }
    }

    private void validateDestinationLocation(Long locationId, Long targetWarehouseId) {
        // Hàng QC đạt phải vào vị trí thường đang hoạt động của kho nhận, không được đưa vào khu cách ly.
        WarehouseLocation destination = locationRepository.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Destination location not found: " + locationId));
        // Validate: vị trí nhập phải thuộc đúng kho nhận và đang hoạt động.
        if (!Objects.equals(destination.getWarehouse().getId(), targetWarehouseId)
                || Boolean.FALSE.equals(destination.getIsActive())) {
            throw new BusinessRuleViolationException("INVALID_DESTINATION_LOCATION");
        }
        // Validate: hàng QC đạt không được nhập vào vị trí cách ly.
        if (Boolean.TRUE.equals(destination.getIsQuarantine())) {
            throw new BusinessRuleViolationException("QC_PASSED_BIN_MUST_NOT_BE_QUARANTINE");
        }
    }

    private void ensureQuarantineRejectGate(InterWarehouseTransfer transfer) {
        // Cách ly toàn bộ chỉ chạy sau khi xe đã đến, đã bàn giao và đã có số công nhân đếm để chứng minh hàng có thật.
        if (Boolean.TRUE.equals(transfer.isReturned())) {
            // Validate: xe quay đầu phải về tới kho nguồn trước khi cách ly toàn bộ.
            if (transfer.getReturnArrivedAt() == null) {
                throw new BusinessRuleViolationException("RETURN_ARRIVE_REQUIRED");
            }
            // Validate: xe quay đầu phải có bàn giao tại kho nguồn trước khi cách ly.
            if (transfer.getReturnArrivalHandoverAt() == null) {
                throw new BusinessRuleViolationException("RETURN_HANDOVER_REQUIRED");
            }
        } else {
            // Validate: luồng thường phải có tài xế đến kho đích trước khi từ chối và cách ly.
            if (transfer.getDriverArrivedAt() == null) {
                throw new BusinessRuleViolationException("DRIVER_ARRIVE_REQUIRED");
            }
            // Validate: luồng thường phải có bàn giao tại kho đích trước khi từ chối và cách ly.
            if (transfer.getArrivalHandoverAt() == null) {
                throw new BusinessRuleViolationException("ARRIVAL_HANDOVER_REQUIRED");
            }
        }
        // Validate: phải có số công nhân đếm để biết số lượng thực tế cần đưa vào khu cách ly.
        if (helper.items(transfer).stream().anyMatch(item -> item.getWorkerReceivedQty() == null)) {
            throw new BusinessRuleViolationException("WORKER_COUNT_REQUIRED");
        }
    }

    private void ensureAllChecked(InterWarehouseTransfer transfer) {
        // Duyệt nhập kho cuối chỉ chạy sau khi mọi dòng đã có số xác nhận, số đạt và số lỗi từ bước QC nhận.
        // Validate: không được duyệt cuối nếu còn dòng chưa qua kiểm tra/QC nhận.
        if (helper.items(transfer).stream().anyMatch(item -> item.getReceivedQty() == null
                || item.getQcPassedQty() == null || item.getQcFailedQty() == null)) {
            throw new BusinessRuleViolationException("RECEIVE_CHECK_REQUIRED");
        }
    }

    private void moveTransitToDestination(InterWarehouseTransfer transfer, InterWarehouseTransferFinalReceiveRequest request, User actor) {
        // Khi duyệt cuối: trừ hàng khỏi kho ảo đang vận chuyển, đưa hàng đạt vào vị trí thường,
        // đưa hàng lỗi vào khu cách ly, còn thiếu/thừa thì ghi vào hồ sơ chênh lệch.
        Warehouse transitWarehouse = helper.findTransitWarehouse();
        WarehouseLocation quarantineLocation = null;
        Warehouse targetWarehouse = transfer.isReturned() ? transfer.getSourceWarehouse() : transfer.getDestinationWarehouse();
        Map<Long, List<PutawayTarget>> putawayPlans = resolveFinalPutawayPlans(transfer, request);

        // Validate: kiểm tra sức chứa vị trí trước khi ghi tồn để tránh nhập được nửa chừng rồi lỗi quá tải.
        for (InterWarehouseTransferItem item : helper.items(transfer)) {
            BigDecimal passedQty = helper.zero(item.getQcPassedQty());
            BigDecimal failedQty = helper.zero(item.getQcFailedQty());
            if (passedQty.signum() > 0) {
                for (PutawayTarget putaway : putawayPlans.get(item.getId())) {
                    assertLocationCapacity(putaway.location(), item.getProduct(), putaway.quantity());
                }
            }
            if (failedQty.signum() > 0) {
                if (quarantineLocation == null) {
                    quarantineLocation = helper.findQuarantineLocation(transfer);
                }
                assertLocationCapacity(quarantineLocation, item.getProduct(), failedQty);
            }
        }

        for (InterWarehouseTransferItem item : helper.items(transfer)) {
            BigDecimal remainingPassed = putawayPlans.get(item.getId()).stream()
                    .map(PutawayTarget::quantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal remainingFailed = helper.zero(item.getQcFailedQty());
            Map<WarehouseLocation, BigDecimal> remainingPutaway = new LinkedHashMap<>();
            putawayPlans.get(item.getId()).forEach(line -> remainingPutaway.put(line.location(), line.quantity()));
            for (InterWarehouseTransferAllocation allocation : allocationRepository.findByTransferItemId(item.getId())) {
                Inventory transit = inventoryRepository.findByStockKeyForUpdate(transitWarehouse.getId(),
                                item.getProduct().getId(), allocation.getInventory().getBatch().getId(),
                                helper.firstTransitLocation(transitWarehouse).getId())
                        .orElseThrow(() -> new BusinessRuleViolationException("IN_TRANSIT_STOCK_NOT_FOUND"));
                BigDecimal qty = allocation.getAllocatedQty();
                transit.setTotalQty(transit.getTotalQty().subtract(qty));
                transit.setUpdatedAt(OffsetDateTime.now());
                inventoryRepository.save(transit);

                BigDecimal passQty = qty.min(remainingPassed);
                if (passQty.signum() > 0) {
                    distributePassedStock(targetWarehouse, item, transit, passQty, remainingPutaway);
                    remainingPassed = remainingPassed.subtract(passQty);
                }
                BigDecimal failQty = qty.subtract(passQty).min(remainingFailed);
                if (failQty.signum() > 0) {
                    if (quarantineLocation == null) {
                        quarantineLocation = helper.findQuarantineLocation(transfer);
                    }
                    applyLocationOccupancy(quarantineLocation, item.getProduct(), failQty);
                    helper.upsertInventory(targetWarehouse, item.getProduct(), transit.getBatch(),
                            quarantineLocation, failQty, transit.getCostPrice());
                    remainingFailed = remainingFailed.subtract(failQty);

                    // Lưu hồ sơ cách ly cho phần hàng điều chuyển bị lỗi QC.
                    QuarantineRecord qr = new QuarantineRecord();
                    qr.setWarehouse(targetWarehouse);
                    qr.setProduct(item.getProduct());
                    qr.setBatch(transit.getBatch());
                    qr.setLocation(quarantineLocation);
                    qr.setTransfer(transfer);
                    qr.setTransferItem(item);
                    qr.setOriginType("INTERNAL_TRANSFER");
                    qr.setQuantity(failQty);
                    qr.setRemainingQuantity(failQty);
                    qr.setReason(item.getQcFailureReason() != null ? item.getQcFailureReason() : "Hàng điều chuyển hỏng vật lý");
                    qr.setCreatedBy(actor);
                    qr.setCreatedAt(OffsetDateTime.now());
                    quarantineRecordRepository.save(qr);
                }
                BigDecimal shortageQty = qty.subtract(passQty).subtract(failQty);
                if (shortageQty.signum() > 0) {
                    DiscrepancyIncident incident = DiscrepancyIncident.builder()
                            .transfer(transfer)
                            .product(item.getProduct())
                            .incidentType("SHORTAGE")
                            .quantity(shortageQty)
                            .status("OPEN")
                            .resolutionNote(request.discrepancyReason())
                            .build();
                    discrepancyIncidentRepository.save(incident);

                    Adjustment adjustment = Adjustment.builder()
                            .adjustmentNumber(generateAdjustmentNumber())
                            .warehouse(targetWarehouse)
                            .product(item.getProduct())
                            .batch(transit.getBatch())
                            .location(item.getDestinationLocation() != null ? item.getDestinationLocation() : helper.findQuarantineLocation(transfer))
                            .quantityAdjustment(shortageQty.negate())
                            .type(AdjustmentType.TRANSFER_DISCREPANCY)
                            .referenceId(transfer.getId())
                            .referenceType("TRANSFER")
                            .reason(request.discrepancyReason())
                            .documentDate(transfer.getDocumentDate())
                            .accountingPeriod(transfer.getAccountingPeriod())
                            .createdBy(actor)
                            .createdAt(OffsetDateTime.now())
                            .build();
                    adjustmentRepository.save(adjustment);

                    auditUtil.logChange(actor, AuditAction.TRANSFER_DISCREPANCY_CREATE, "ADJUSTMENT",
                            adjustment.getId(), adjustment.getAdjustmentNumber(), Map.of(), Map.of());
                }
            }

            // Nếu nhận thừa so với hàng đang vận chuyển, đưa phần thừa vào danh sách tạm giữ của hồ sơ chênh lệch.
            BigDecimal overReceiptPassed = remainingPassed;
            BigDecimal overReceiptFailed = remainingFailed;
            BigDecimal totalOverReceipt = overReceiptPassed.add(overReceiptFailed);
            if (totalOverReceipt.signum() > 0) {
                DiscrepancyIncident incident = DiscrepancyIncident.builder()
                        .transfer(transfer)
                        .product(item.getProduct())
                        .incidentType("OVER_RECEIPT")
                        .quantity(totalOverReceipt)
                        .status("OPEN")
                        .resolutionNote("Over-receipt during transfer receiving")
                        .build();
                incident = discrepancyIncidentRepository.save(incident);

                Batch batch = item.getBatch();
                if (batch == null && !allocationRepository.findByTransferItemId(item.getId()).isEmpty()) {
                    batch = allocationRepository.findByTransferItemId(item.getId()).get(0).getInventory().getBatch();
                }

                if (overReceiptPassed.signum() > 0) {
                    distributeOverReceipt(targetWarehouse, item, batch, incident,
                            overReceiptPassed, remainingPutaway);
                }
                if (overReceiptFailed.signum() > 0) {
                    if (quarantineLocation == null) {
                        quarantineLocation = helper.findQuarantineLocation(transfer);
                    }
                    // Hàng thừa không được cộng vào tồn khả dụng/quarantine inventory ngay.
                    // Chỉ giữ trên hồ sơ chênh lệch để xử lý sau, tránh làm tổng tồn hệ thống tăng ảo.
                    discrepancyHoldEntryRepository.save(DiscrepancyHoldEntry.builder()
                            .incident(incident)
                            .warehouse(targetWarehouse)
                            .product(item.getProduct())
                            .batch(batch)
                            .holdQty(overReceiptFailed)
                            .holdLocation(quarantineLocation)
                            .build());

                    // Lưu hồ sơ cách ly cho phần hàng nhận thừa nhưng bị lỗi QC.
                    QuarantineRecord qr = new QuarantineRecord();
                    qr.setWarehouse(targetWarehouse);
                    qr.setProduct(item.getProduct());
                    qr.setBatch(batch);
                    qr.setLocation(quarantineLocation);
                    qr.setTransfer(transfer);
                    qr.setTransferItem(item);
                    qr.setOriginType("INTERNAL_TRANSFER");
                    qr.setQuantity(overReceiptFailed);
                    qr.setRemainingQuantity(overReceiptFailed);
                    qr.setReason(item.getQcFailureReason() != null ? item.getQcFailureReason() : "Over-receipt QC failed");
                    qr.setCreatedBy(actor);
                    qr.setCreatedAt(OffsetDateTime.now());
                    quarantineRecordRepository.save(qr);
                }
            }
        }
    }

    private Map<Long, List<PutawayTarget>> resolveFinalPutawayPlans(
            InterWarehouseTransfer transfer, InterWarehouseTransferFinalReceiveRequest request) {
        // Chuẩn hóa kế hoạch nhập vị trí theo từng dòng hàng và chống gửi trùng dòng trước khi ghi tồn.
        Map<Long, InterWarehouseTransferFinalPutawayItemRequest> requestedPlans = new java.util.HashMap<>();
        if (request.putawayItems() != null) {
            for (InterWarehouseTransferFinalPutawayItemRequest itemRequest : request.putawayItems()) {
                // Validate: mỗi dòng hàng chỉ có một kế hoạch nhập vị trí; nếu chia nhiều vị trí thì nằm trong danh sách vị trí của dòng đó.
                if (requestedPlans.put(itemRequest.transferItemId(), itemRequest) != null) {
                    throw new BusinessRuleViolationException("DUPLICATE_PUTAWAY_ITEM");
                }
            }
        }

        Map<Long, List<PutawayTarget>> plans = new java.util.HashMap<>();
        Long targetWarehouseId = transfer.isReturned()
                ? transfer.getSourceWarehouse().getId()
                : transfer.getDestinationWarehouse().getId();
        for (InterWarehouseTransferItem item : helper.items(transfer)) {
            plans.put(item.getId(), resolveItemPutawayPlan(item, requestedPlans.get(item.getId()), targetWarehouseId));
        }
        return plans;
    }

    private List<PutawayTarget> resolveItemPutawayPlan(
            InterWarehouseTransferItem item,
            InterWarehouseTransferFinalPutawayItemRequest requestedPlan,
            Long targetWarehouseId) {
        // Nếu không gửi kế hoạch mới thì dùng vị trí đã chọn ở bước QC nhận.
        BigDecimal passedQty = helper.zero(item.getQcPassedQty());
        if (passedQty.signum() == 0) return List.of();
        if (requestedPlan == null) {
            // Validate: nếu không gửi danh sách vị trí thì bước QC nhận phải đã chọn vị trí nhập.
            if (item.getDestinationLocation() == null) {
                throw new BusinessRuleViolationException("DESTINATION_LOCATION_REQUIRED");
            }
            return List.of(new PutawayTarget(item.getDestinationLocation(), passedQty));
        }

        BigDecimal allocatedQty = BigDecimal.ZERO;
        Set<Long> locationIds = new HashSet<>();
        java.util.ArrayList<PutawayTarget> targets = new java.util.ArrayList<>();
        for (InterWarehouseTransferPutawayAllocationRequest allocation : requestedPlan.allocations()) {
            ensureWholeQuantity(allocation.quantity());
            // Validate: không được lặp cùng một vị trí trong danh sách vị trí của một dòng hàng.
            if (!locationIds.add(allocation.locationId())) {
                throw new BusinessRuleViolationException("DUPLICATE_PUTAWAY_LOCATION");
            }
            validateDestinationLocation(allocation.locationId(), targetWarehouseId);
            targets.add(new PutawayTarget(
                    helper.reference(WarehouseLocation.class, allocation.locationId()), allocation.quantity()));
            allocatedQty = allocatedQty.add(allocation.quantity());
        }
        // Validate: kế hoạch không được nhập kho nhiều hơn số lượng QC đạt.
        if (allocatedQty.compareTo(passedQty) > 0) {
            throw new BusinessRuleViolationException("PUTAWAY_QUANTITY_MUST_MATCH_QC_PASSED");
        }
        return targets;
    }

    private record PutawayTarget(WarehouseLocation location, BigDecimal quantity) {}

    private void distributePassedStock(Warehouse warehouse,
                                       InterWarehouseTransferItem item,
                                       Inventory transit,
                                       BigDecimal quantity,
                                       Map<WarehouseLocation, BigDecimal> remainingPutaway) {
        // Phân phối hàng QC đạt vào các vị trí thường theo kế hoạch nhập vị trí.
        distributeToBins(quantity, remainingPutaway, (location, movedQty) -> {
            applyLocationOccupancy(location, item.getProduct(), movedQty);
            helper.upsertInventory(warehouse, item.getProduct(), transit.getBatch(),
                    location, movedQty, transit.getCostPrice());
        });
    }

    private void distributeOverReceipt(Warehouse warehouse,
                                       InterWarehouseTransferItem item,
                                       Batch batch,
                                       DiscrepancyIncident incident,
                                       BigDecimal quantity,
                                       Map<WarehouseLocation, BigDecimal> remainingPutaway) {
        // Hàng nhận thừa được đưa vào danh sách tạm giữ của hồ sơ chênh lệch,
        // chưa cộng tồn khả dụng cho tới khi hồ sơ chênh lệch được xử lý.
        distributeToBins(quantity, remainingPutaway, (location, movedQty) -> {
            discrepancyHoldEntryRepository.save(DiscrepancyHoldEntry.builder()
                    .incident(incident)
                    .warehouse(warehouse)
                    .product(item.getProduct())
                    .batch(batch)
                    .holdQty(movedQty)
                    .holdLocation(location)
                    .build());
        });
    }

    private void distributeToBins(BigDecimal quantity,
                                  Map<WarehouseLocation, BigDecimal> remainingPutaway,
                                  PutawayConsumer consumer) {
        // Dùng chung cho hàng đạt và hàng nhận thừa: đi lần lượt qua từng vị trí trong kế hoạch.
        // Nếu dùng hết kế hoạch mà vẫn còn số lượng cần nhập thì báo lỗi.
        BigDecimal remaining = quantity;
        for (Map.Entry<WarehouseLocation, BigDecimal> entry : remainingPutaway.entrySet()) {
            if (remaining.signum() <= 0) break;
            BigDecimal movedQty = remaining.min(entry.getValue());
            if (movedQty.signum() <= 0) continue;
            consumer.accept(entry.getKey(), movedQty);
            entry.setValue(entry.getValue().subtract(movedQty));
            remaining = remaining.subtract(movedQty);
        }
        // Validate: nếu tổng số trong kế hoạch không đủ để phân phối số lượng cần nhập thì báo lỗi.
        if (remaining.signum() > 0) {
            throw new BusinessRuleViolationException("PUTAWAY_PLAN_EXHAUSTED");
        }
    }

    @FunctionalInterface
    private interface PutawayConsumer {
        void accept(WarehouseLocation location, BigDecimal quantity);
    }

    private void moveTransitToQuarantine(InterWarehouseTransfer transfer, User actor) {
        // Khi từ chối toàn bộ: chuyển toàn bộ hàng từ kho ảo đang vận chuyển sang khu cách ly của kho nhận.
        Warehouse transitWarehouse = helper.findTransitWarehouse();
        WarehouseLocation quarantineLocation = helper.findQuarantineLocation(transfer);
        Warehouse targetWarehouse = transfer.isReturned() ? transfer.getSourceWarehouse() : transfer.getDestinationWarehouse();

        // Validate: kiểm tra sức chứa khu cách ly trước khi ghi tồn.
        BigDecimal totalQtyToQuarantine = BigDecimal.ZERO;
        for (InterWarehouseTransferItem item : helper.items(transfer)) {
            BigDecimal qty = item.getSentQty() != null ? item.getSentQty() : item.getPlannedQty();
            totalQtyToQuarantine = totalQtyToQuarantine.add(qty);
            assertLocationCapacity(quarantineLocation, item.getProduct(), qty);
        }

        for (InterWarehouseTransferItem item : helper.items(transfer)) {
            for (InterWarehouseTransferAllocation allocation : allocationRepository.findByTransferItemId(item.getId())) {
                Inventory transit = inventoryRepository.findByStockKeyForUpdate(transitWarehouse.getId(),
                                item.getProduct().getId(), allocation.getInventory().getBatch().getId(),
                                helper.firstTransitLocation(transitWarehouse).getId())
                        .orElseThrow(() -> new BusinessRuleViolationException("IN_TRANSIT_STOCK_NOT_FOUND"));
                BigDecimal qty = allocation.getAllocatedQty();
                transit.setTotalQty(transit.getTotalQty().subtract(qty));
                transit.setUpdatedAt(OffsetDateTime.now());
                inventoryRepository.save(transit);

                applyLocationOccupancy(quarantineLocation, item.getProduct(), qty);
                helper.upsertInventory(targetWarehouse, item.getProduct(), transit.getBatch(),
                        quarantineLocation, qty, transit.getCostPrice());

                // Lưu hồ sơ cách ly khi từ chối nhận toàn bộ hàng điều chuyển.
                QuarantineRecord qr = new QuarantineRecord();
                qr.setWarehouse(targetWarehouse);
                qr.setProduct(item.getProduct());
                qr.setBatch(transit.getBatch());
                qr.setLocation(quarantineLocation);
                qr.setTransfer(transfer);
                qr.setTransferItem(item);
                qr.setOriginType("INTERNAL_TRANSFER");
                qr.setQuantity(qty);
                qr.setRemainingQuantity(qty);
                qr.setReason(transfer.getRejectionReason() != null ? transfer.getRejectionReason() : "Từ chối và cách ly toàn bộ hàng điều chuyển");
                qr.setCreatedBy(actor);
                qr.setCreatedAt(OffsetDateTime.now());
                quarantineRecordRepository.save(qr);
            }
        }
    }

    @Transactional
    public InterWarehouseTransferResponse requestReturn(Long id, TransferReturnRequest request, User actor) {
        // HÀM CHÍNH: kho đích báo sai SKU và tạo yêu cầu quay đầu chờ quản lý duyệt.
        // Kho đích báo giao sai mã hàng trước khi nhận bàn giao hoặc đếm hàng.
        // Hệ thống tạo hồ sơ chờ quản lý kho đích quyết định có cho xe quay đầu hay không.
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        helper.requireStatus(transfer, InterWarehouseTransferStatus.IN_TRANSIT);
        helper.ensureWarehouseScope(actor, transfer.getDestinationWarehouse().getId());
        ensureReturnNotAlreadyInProgress(transfer);
        // Validate: chỉ được báo sai mã hàng/quay đầu sau khi tài xế đã đến kho đích.
        if (transfer.getDriverArrivedAt() == null) {
            throw new BusinessRuleViolationException("DRIVER_ARRIVE_REQUIRED");
        }
        // Validate: chỉ được báo sai mã hàng trước khi kho đích đã nhận bàn giao hoặc đếm hàng.
        if (transfer.getArrivalHandoverAt() != null) {
            throw new BusinessRuleViolationException("RETURN_REQUEST_ONLY_BEFORE_HANDOVER");
        }
        ensureNoReceiveCountOrCheck(transfer);
        // Validate: yêu cầu báo sai mã hàng phải chỉ rõ những dòng hàng nào bị sai.
        if (request.wrongSkuItems() == null || request.wrongSkuItems().isEmpty()) {
            throw new BusinessRuleViolationException("WRONG_SKU_ITEMS_REQUIRED");
        }
        validateWrongSkuItems(transfer, request.wrongSkuItems());

        Map<String, Object> before = helper.snapshot(transfer);
        transfer.setReturnRequested(true);
        transfer.setReturnReason(request.reason());
        transfer.setReturnRequestedBy(actor);
        transfer.setReturnRequestedAt(OffsetDateTime.now());
        transfer.setUpdatedAt(OffsetDateTime.now());

        WrongSkuReport report = WrongSkuReport.builder()
                .transfer(transfer)
                .status("PENDING")
                .reportedBy(actor)
                .reportedAt(OffsetDateTime.now())
                .build();
        report = wrongSkuReportRepository.save(report);
        saveWrongSkuItems(transfer, report, request.wrongSkuItems());

        InterWarehouseTransfer saved = transferRepository.save(transfer);
        helper.audit(saved, actor, AuditAction.UPDATE, before, helper.snapshot(saved));
        return helper.toResponse(saved);
    }

    @Transactional
    public InterWarehouseTransferResponse approveReturn(Long id, User actor) {
        // HÀM CHÍNH: quản lý kho đích duyệt yêu cầu quay đầu do sai SKU.
        // Quản lý duyệt yêu cầu quay đầu do sai mã hàng. Phiếu vẫn đang vận chuyển,
        // nhưng được đánh dấu là hàng quay về để các bước nhận tiếp theo diễn ra tại kho nguồn.
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        helper.requireStatus(transfer, InterWarehouseTransferStatus.IN_TRANSIT);
        // Validate: chỉ được duyệt khi đang có yêu cầu quay đầu chờ xử lý.
        if (!transfer.isReturnRequested()) {
            throw new BusinessRuleViolationException("NO_RETURN_REQUESTED");
        }
        ensureNoReceiveCountOrCheck(transfer);
        // Validate: nếu kho đích đã nhận bàn giao thì không còn được duyệt quay đầu theo nhánh sai mã hàng.
        if (transfer.getArrivalHandoverAt() != null) {
            throw new BusinessRuleViolationException("RETURN_REQUEST_ONLY_BEFORE_HANDOVER");
        }
        helper.ensureWarehouseScope(actor, transfer.getDestinationWarehouse().getId());
        // Validate: chỉ quản lý kho/CEO/Admin được duyệt yêu cầu quay đầu.
        if (actor.getRole() != UserRole.WAREHOUSE_MANAGER && actor.getRole() != UserRole.ADMIN && actor.getRole() != UserRole.CEO) {
            throw new BusinessRuleViolationException("WAREHOUSE_MANAGER_ROLE_REQUIRED");
        }

        Map<String, Object> before = helper.snapshot(transfer);
        transfer.setReturnApprovedBy(actor);
        transfer.setReturnApprovedAt(OffsetDateTime.now());
        transfer.setReturned(true);
        transfer.setReturnRequested(false);
        transfer.setUpdatedAt(OffsetDateTime.now());

        // Cập nhật các hồ sơ sai mã hàng đang chờ thành đã duyệt.
        List<WrongSkuReport> pendingReports = wrongSkuReportRepository.findByTransferId(transfer.getId());
        for (WrongSkuReport report : pendingReports) {
            if ("PENDING".equals(report.getStatus())) {
                report.setStatus("APPROVED");
                report.setManagerDecisionBy(actor);
                report.setManagerDecisionAt(OffsetDateTime.now());
                wrongSkuReportRepository.save(report);
            }
        }

        InterWarehouseTransfer saved = transferRepository.save(transfer);
        helper.audit(saved, actor, AuditAction.TRANSFER_RETURN_TO_SOURCE, before, helper.snapshot(saved));
        return helper.toResponse(saved);
    }

    @Transactional
    public InterWarehouseTransferResponse rejectReturn(Long id, TransferReturnRejectRequest request, User actor) {
        // HÀM CHÍNH: quản lý kho đích bác yêu cầu quay đầu để tiếp tục nhận hàng bình thường.
        // Quản lý bác yêu cầu quay đầu; phiếu tiếp tục luồng nhận hàng tại kho đích.
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        helper.requireStatus(transfer, InterWarehouseTransferStatus.IN_TRANSIT);
        // Validate: chỉ được bác khi đang có yêu cầu quay đầu chờ xử lý.
        if (!transfer.isReturnRequested()) {
            throw new BusinessRuleViolationException("NO_RETURN_REQUESTED");
        }
        ensureNoReceiveCountOrCheck(transfer);
        // Validate: nếu kho đích đã nhận bàn giao thì yêu cầu quay đầu không còn ở trạng thái có thể duyệt/bác.
        if (transfer.getArrivalHandoverAt() != null) {
            throw new BusinessRuleViolationException("RETURN_REQUEST_ONLY_BEFORE_HANDOVER");
        }
        helper.ensureWarehouseScope(actor, transfer.getDestinationWarehouse().getId());
        // Validate: chỉ quản lý kho/CEO/Admin được bác yêu cầu quay đầu.
        if (actor.getRole() != UserRole.WAREHOUSE_MANAGER && actor.getRole() != UserRole.ADMIN && actor.getRole() != UserRole.CEO) {
            throw new BusinessRuleViolationException("WAREHOUSE_MANAGER_ROLE_REQUIRED");
        }

        Map<String, Object> before = helper.snapshot(transfer);
        transfer.setReturnRejectedBy(actor);
        transfer.setReturnRejectedAt(OffsetDateTime.now());
        transfer.setReturnRejectionReason(request.reason());
        transfer.setReturnRequested(false);
        transfer.setUpdatedAt(OffsetDateTime.now());

        // Cập nhật các hồ sơ sai mã hàng đang chờ thành đã bị bác.
        List<WrongSkuReport> pendingReports = wrongSkuReportRepository.findByTransferId(transfer.getId());
        for (WrongSkuReport report : pendingReports) {
            if ("PENDING".equals(report.getStatus())) {
                report.setStatus("REJECTED");
                report.setManagerDecisionBy(actor);
                report.setManagerDecisionAt(OffsetDateTime.now());
                report.setManagerNote(request.reason());
                wrongSkuReportRepository.save(report);
            }
        }

        InterWarehouseTransfer saved = transferRepository.save(transfer);
        helper.audit(saved, actor, AuditAction.UPDATE, before, helper.snapshot(saved));
        return helper.toResponse(saved);
    }

    private void validateWrongSkuItems(InterWarehouseTransfer transfer, List<WrongSkuItemRequest> lines) {
        // Validate danh sách sai mã hàng trước khi lưu hồ sơ để không có mã thực tế hoặc số lượng không hợp lệ.
        Map<Long, InterWarehouseTransferItem> itemById = helper.itemMap(transfer);
        for (WrongSkuItemRequest line : lines) {
            InterWarehouseTransferItem item = helper.requireItem(itemById, line.transferItemId());
            validateWrongSkuItem(item, line);
            productRepository.findById(line.actualProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + line.actualProductId()));
        }
    }

    private String generateAdjustmentNumber() {
        return "ADJ-" + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
               + "-" + java.util.UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private void ensureReturnNotAlreadyInProgress(InterWarehouseTransfer transfer) {
        // Một phiếu chỉ được có một nhánh quay đầu hoặc một yêu cầu quay đầu đang chờ xử lý.
        // Validate: chặn tạo yêu cầu quay đầu lặp.
        if (Boolean.TRUE.equals(transfer.isReturned()) || Boolean.TRUE.equals(transfer.isReturnRequested())) {
            throw new BusinessRuleViolationException("RETURN_ALREADY_IN_PROGRESS");
        }
    }

    private void ensureNoReceiveCountOrCheck(InterWarehouseTransfer transfer) {
        // Nếu đã bắt đầu đếm hoặc QC nhận thì không được quay đầu nhanh theo nhánh sai mã hàng nữa.
        // Validate: lúc này phải xử lý tiếp bằng hồ sơ chênh lệch hoặc khu cách ly.
        if (helper.items(transfer).stream().anyMatch(item -> item.getWorkerReceivedQty() != null
                || item.getReceivedQty() != null
                || item.getQcPassedQty() != null
                || item.getQcFailedQty() != null)) {
            throw new BusinessRuleViolationException("RETURN_REQUEST_ONLY_BEFORE_COUNT");
        }
    }

    private void saveWrongSkuItems(InterWarehouseTransfer transfer, WrongSkuReport report, List<WrongSkuItemRequest> lines) {
        // Lưu từng dòng sai mã hàng kèm sản phẩm dự kiến, sản phẩm thực tế, số lượng ảnh hưởng và ảnh bằng chứng.
        Map<Long, InterWarehouseTransferItem> itemById = helper.itemMap(transfer);
        for (WrongSkuItemRequest line : lines) {
            InterWarehouseTransferItem item = helper.requireItem(itemById, line.transferItemId());
            validateWrongSkuItem(item, line);
            Product actual = productRepository.findById(line.actualProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + line.actualProductId()));

            WrongSkuReportItem reportItem = WrongSkuReportItem.builder()
                    .report(report)
                    .transferItem(item)
                    .expectedProduct(item.getProduct())
                    .actualProduct(actual)
                    .affectedQty(line.affectedQty())
                    .reason(line.reason())
                    .photoRef(line.photoRef())
                    .build();
            wrongSkuReportItemRepository.save(reportItem);
        }
    }

    private void validateWrongSkuItem(InterWarehouseTransferItem item, WrongSkuItemRequest line) {
        // Số lượng bị ảnh hưởng không được vượt số đã gửi; sản phẩm thực tế phải khác sản phẩm dự kiến.
        // Validate: dữ liệu gửi lên phải khớp sản phẩm dự kiến của dòng hàng để tránh báo nhầm dòng.
        if (!Objects.equals(item.getProduct().getId(), line.expectedProductId())) {
            throw new BusinessRuleViolationException("EXPECTED_PRODUCT_MISMATCH");
        }
        // Validate: sản phẩm thực tế phải khác sản phẩm dự kiến thì mới được coi là sai mã hàng.
        if (Objects.equals(line.expectedProductId(), line.actualProductId())) {
            throw new BusinessRuleViolationException("ACTUAL_PRODUCT_MUST_DIFFER");
        }
        // Validate: số lượng bị ảnh hưởng phải dương.
        if (line.affectedQty() == null || line.affectedQty().signum() <= 0) {
            throw new BusinessRuleViolationException("AFFECTED_QTY_MUST_BE_POSITIVE");
        }
        BigDecimal maxQty = item.getSentQty() != null ? item.getSentQty() : item.getPlannedQty();
        // Validate: số lượng sai mã hàng không được lớn hơn số lượng đã gửi hoặc dự kiến gửi của dòng đó.
        if (line.affectedQty().compareTo(maxQty) > 0) {
            throw new BusinessRuleViolationException("AFFECTED_QTY_EXCEEDS_SENT_QTY");
        }
    }

    private void assertLocationCapacity(WarehouseLocation location, Product product, BigDecimal qty) {
        // Kiểm tra thử sức chứa trước khi ghi tồn để tránh nhập nửa chừng rồi mới phát hiện vị trí quá tải.
        if (location == null || qty == null || qty.signum() <= 0) {
            return;
        }
        BigDecimal addedVolume = product.getVolumeM3() != null ? product.getVolumeM3().multiply(qty) : BigDecimal.ZERO;
        BigDecimal addedWeight = product.getWeightKg() != null ? product.getWeightKg().multiply(qty) : BigDecimal.ZERO;

        BigDecimal currentVolume = location.getCurrentVolumeM3() != null ? location.getCurrentVolumeM3() : BigDecimal.ZERO;
        BigDecimal currentWeight = location.getCurrentWeightKg() != null ? location.getCurrentWeightKg() : BigDecimal.ZERO;

        // Validate: không cho vượt sức chứa thể tích của vị trí nếu vị trí có cấu hình giới hạn thể tích.
        if (location.getCapacityM3() != null && currentVolume.add(addedVolume).compareTo(location.getCapacityM3()) > 0) {
            throw new BusinessRuleViolationException("BIN_CAPACITY_EXCEEDED: Volume exceeds location capacity for " + location.getCode());
        }
        // Validate: không cho vượt sức chứa trọng lượng của vị trí nếu vị trí có cấu hình giới hạn trọng lượng.
        if (location.getCapacityKg() != null && currentWeight.add(addedWeight).compareTo(location.getCapacityKg()) > 0) {
            throw new BusinessRuleViolationException("BIN_CAPACITY_EXCEEDED: Weight exceeds location capacity for " + location.getCode());
        }
    }

    private void ensureWholeQuantity(BigDecimal quantity) {
        // Số lượng điều chuyển/nhận/QC đều là số nguyên theo domain hàng gia dụng.
        // Validate: không nhận số lẻ/thập phân vì hệ thống không quản lý tách lẻ từng sản phẩm.
        if (quantity.stripTrailingZeros().scale() > 0) {
            throw new BusinessRuleViolationException("TRANSFER_QTY_MUST_BE_WHOLE_NUMBER");
        }
    }

    private void applyLocationOccupancy(WarehouseLocation location, Product product, BigDecimal qty) {
        // Cập nhật mức đã sử dụng của vị trí sau khi tồn thực sự được nhập vào vị trí thường hoặc khu cách ly.
        if (location == null || qty == null || qty.signum() <= 0) {
            return;
        }
        BigDecimal addedVolume = product.getVolumeM3() != null ? product.getVolumeM3().multiply(qty) : BigDecimal.ZERO;
        BigDecimal addedWeight = product.getWeightKg() != null ? product.getWeightKg().multiply(qty) : BigDecimal.ZERO;

        BigDecimal currentVolume = location.getCurrentVolumeM3() != null ? location.getCurrentVolumeM3() : BigDecimal.ZERO;
        BigDecimal currentWeight = location.getCurrentWeightKg() != null ? location.getCurrentWeightKg() : BigDecimal.ZERO;

        location.setCurrentVolumeM3(currentVolume.add(addedVolume));
        location.setCurrentWeightKg(currentWeight.add(addedWeight));
        location.setUpdatedAt(OffsetDateTime.now());
        locationRepository.save(location);
    }
}
