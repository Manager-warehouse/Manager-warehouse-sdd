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

import com.wms.dto.request.InterWarehouseTransferCreateRequest;
import com.wms.dto.request.InterWarehouseTransferItemRequest;
import com.wms.dto.request.InterWarehouseTransferReasonRequest;
import com.wms.dto.request.InterWarehouseTransferUpdateRequest;
import com.wms.dto.response.InterWarehouseTransferResponse;
import com.wms.enums.warehouse_transfer.InterWarehouseTransferStatus;
import com.wms.enums.warehouse_location.WarehouseType;
import com.wms.enums.audit_trail.AuditAction;
import com.wms.enums.access_control.UserRole;
import com.wms.exception.BusinessRuleViolationException;
import com.wms.repository.InterWarehouseTransferItemRepository;
import com.wms.repository.InterWarehouseTransferRepository;
import com.wms.service.billing_payment.AccountingPeriodService;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phụ trách giai đoạn lập kế hoạch phiếu điều chuyển.
 * Tạo/sửa/hủy phiếu mới hoặc phiếu đã duyệt nhưng chưa xếp hàng; kiểm thông tin phiếu, kho, ngày, dòng hàng và vị trí.
 */
@Service
@RequiredArgsConstructor
public class InterWarehouseTransferPlanningService {

    /*
     * LUỒNG LẬP PHIẾU:
     * - Các hàm public là hành động chính của Planner: tạo phiếu, tạo từ yêu cầu đã duyệt, sửa phiếu, hủy phiếu.
     * - Các hàm private là hàm hỗ trợ: kiểm quyền, validate kho/ngày/dòng hàng/vị trí, sinh dữ liệu entity.
     *
     * Giai đoạn lập phiếu tạo/sửa/hủy phiếu điều chuyển trước khi kho nguồn duyệt.
     * Các kiểm tra ở đây bảo đảm phiếu hợp lệ về kho phụ trách, ngày chứng từ,
     * kho vật lý, dòng hàng không trùng, số lượng nguyên và vị trí đúng kho.
     */
    private final InterWarehouseTransferRepository transferRepository;
    private final InterWarehouseTransferItemRepository transferItemRepository;
    private final InterWarehouseTransferHelper helper;
    private final AccountingPeriodService accountingPeriodService;

    @Transactional
    public InterWarehouseTransferResponse createTransfer(InterWarehouseTransferCreateRequest request, User actor) {
        // HÀM CHÍNH: Planner tạo phiếu điều chuyển thủ công.
        // Luồng tạo thủ công: người lập phiếu phải thuộc kho nguồn.
        return createTransfer(request, actor, false);
    }

    @Transactional
    public InterWarehouseTransferResponse createTransferFromApprovedRequest(InterWarehouseTransferCreateRequest request,
            User actor) {
        // HÀM CHÍNH: tạo phiếu điều chuyển từ yêu cầu đã được CEO/Admin duyệt.
        // Luồng sinh từ yêu cầu điều chuyển đã duyệt: người lập phiếu có thể thuộc kho nguồn hoặc kho đích liên quan.
        return createTransfer(request, actor, true);
    }

    private InterWarehouseTransferResponse createTransfer(InterWarehouseTransferCreateRequest request, User actor,
            boolean allowDestinationScopedPlanner) {
        // Thông tin chính và danh sách hàng được kiểm tra trước, rồi mới lưu phiếu, lưu dòng hàng và ghi lịch sử tạo.
        ensureCreateScope(actor, request, allowDestinationScopedPlanner);
        ensureDifferentWarehouses(request.sourceWarehouseId(), request.destinationWarehouseId());
        validateTransferDates(request.documentDate(), request.plannedDate());
        ensurePhysicalWarehouses(request.sourceWarehouseId(), request.destinationWarehouseId());
        validateTransferItems(request.items(), request.sourceWarehouseId(), request.destinationWarehouseId());
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

    private void ensureCreateScope(User actor, InterWarehouseTransferCreateRequest request,
            boolean allowDestinationScopedPlanner) {
        // Quyền tạo phiếu bảo vệ kho nguồn; riêng phiếu sinh từ yêu cầu đã duyệt thì chấp nhận cả kho đích liên quan.
        if (!allowDestinationScopedPlanner) {
            helper.ensureWarehouseScope(actor, request.sourceWarehouseId());
            return;
        }
        if (actor.getRole() == UserRole.ADMIN || actor.getRole() == UserRole.CEO) {
            return;
        }
        List<Long> assignedWarehouseIds = helper.loadWarehouseIds(actor);
        // Validate: người tạo phiếu từ yêu cầu đã duyệt phải thuộc ít nhất một trong hai kho liên quan.
        if (!assignedWarehouseIds.contains(request.sourceWarehouseId())
                && !assignedWarehouseIds.contains(request.destinationWarehouseId())) {
            throw new BusinessRuleViolationException("WAREHOUSE_SCOPE_REQUIRED");
        }
    }

    @Transactional
    public InterWarehouseTransferResponse updateTransfer(Long id, InterWarehouseTransferUpdateRequest request, User actor) {
        // HÀM CHÍNH: sửa phiếu khi còn NEW, trước khi kho nguồn duyệt/giữ hàng.
        // Chỉ phiếu mới được sửa; nếu đã duyệt thì phải hủy hoặc gỡ số lượng đã chốt gửi thay vì sửa trực tiếp.
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        helper.requireStatus(transfer, InterWarehouseTransferStatus.NEW);
        helper.ensureWarehouseScope(actor, transfer.getSourceWarehouse().getId());
        helper.ensureWarehouseScope(actor, request.sourceWarehouseId());
        ensureDifferentWarehouses(request.sourceWarehouseId(), request.destinationWarehouseId());
        validateTransferDates(request.documentDate(), request.plannedDate());
        ensurePhysicalWarehouses(request.sourceWarehouseId(), request.destinationWarehouseId());
        validateTransferItems(request.items(), request.sourceWarehouseId(), request.destinationWarehouseId());
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
        // HÀM CHÍNH: hủy phiếu NEW hoặc APPROVED nhưng chưa xếp/chốt gửi.
        // Hủy phiếu mới trực tiếp; phiếu đã duyệt chỉ được hủy khi chưa chốt số lượng gửi và phải trả lại hàng đang giữ chỗ.
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        helper.ensureWarehouseScope(actor, transfer.getSourceWarehouse().getId());
        Map<String, Object> before = helper.snapshot(transfer);
        if (transfer.getStatus() == InterWarehouseTransferStatus.APPROVED) {
            ensureNotLoaded(transfer);
            helper.releaseReservations(transfer);
        } else if (transfer.getStatus() != InterWarehouseTransferStatus.NEW) {
            // Validate: chỉ phiếu mới hoặc phiếu đã duyệt nhưng chưa xếp/chốt gửi mới được hủy.
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
        // Phiếu điều chuyển nội bộ không được tự chuyển trong cùng một kho.
        // Validate: kho nguồn và kho đích phải khác nhau.
        if (Objects.equals(sourceWarehouseId, destinationWarehouseId)) {
            throw new BusinessRuleViolationException("SOURCE_DESTINATION_MUST_DIFFER");
        }
    }

    private void ensurePhysicalWarehouses(Long sourceWarehouseId, Long destinationWarehouseId) {
        // Kho ảo "đang vận chuyển" là kho kỹ thuật, người dùng không được chọn làm kho nguồn/kho đích.
        Warehouse source = helper.reference(Warehouse.class, sourceWarehouseId);
        Warehouse destination = helper.reference(Warehouse.class, destinationWarehouseId);
        // Validate: kho nguồn không được là kho ảo "đang vận chuyển".
        if (source.getType() == WarehouseType.IN_TRANSIT) {
            throw new BusinessRuleViolationException("SOURCE_WAREHOUSE_MUST_BE_PHYSICAL");
        }
        // Validate: kho đích không được là kho ảo "đang vận chuyển".
        if (destination.getType() == WarehouseType.IN_TRANSIT) {
            throw new BusinessRuleViolationException("DESTINATION_WAREHOUSE_MUST_BE_PHYSICAL");
        }
    }

    private void validateTransferDates(LocalDate documentDate, LocalDate plannedDate) {
        // Không tạo phiếu lùi ngày để tránh lệch kỳ kế toán và lịch vận hành.
        LocalDate today = LocalDate.now();
        // Validate: ngày chứng từ không được ở quá khứ.
        if (documentDate.isBefore(today)) {
            throw new BusinessRuleViolationException("DOCUMENT_DATE_MUST_NOT_BE_PAST");
        }
        // Validate: ngày dự kiến điều chuyển không được ở quá khứ.
        if (plannedDate.isBefore(today)) {
            throw new BusinessRuleViolationException("PLANNED_DATE_MUST_NOT_BE_PAST");
        }
        // Validate: ngày dự kiến không được trước ngày chứng từ.
        if (plannedDate.isBefore(documentDate)) {
            throw new BusinessRuleViolationException("PLANNED_DATE_MUST_NOT_BE_BEFORE_DOCUMENT_DATE");
        }
    }

    private void validateTransferItems(List<InterWarehouseTransferItemRequest> requests,
                                       Long sourceWarehouseId,
                                       Long destinationWarehouseId) {
        // Mỗi sản phẩm chỉ có một dòng; vị trí có thể để trống, nhưng nếu nhập thì phải đúng kho và không phải khu cách ly.
        Set<Long> productIds = new HashSet<>();
        for (InterWarehouseTransferItemRequest request : requests) {
            // Validate: không cho trùng sản phẩm trong cùng phiếu để tránh giữ hàng/nhận hàng bị tách khó kiểm soát.
            if (!productIds.add(request.productId())) {
                throw new BusinessRuleViolationException("DUPLICATE_PRODUCT_IN_TRANSFER");
            }
            ensureWholeQuantity(request.plannedQty());
            if (request.sourceLocationId() != null) {
                validateLocation(request.sourceLocationId(), sourceWarehouseId, "INVALID_SOURCE_LOCATION");
            }
            if (request.destinationLocationId() != null) {
                validateLocation(request.destinationLocationId(), destinationWarehouseId, "INVALID_DESTINATION_LOCATION");
            }
        }
    }

    private void ensureWholeQuantity(BigDecimal quantity) {
        // Hàng gia dụng không quản lý từng số serial hoặc đơn vị lẻ, nên số lượng điều chuyển phải là số nguyên.
        if (quantity.stripTrailingZeros().scale() > 0) {
            throw new BusinessRuleViolationException("TRANSFER_QTY_MUST_BE_WHOLE_NUMBER");
        }
    }

    private void validateLocation(Long locationId, Long warehouseId, String errorCode) {
        // Vị trí người dùng nhập phải là vị trí thường, đang hoạt động và thuộc đúng kho nguồn/kho đích.
        WarehouseLocation location = helper.reference(WarehouseLocation.class, locationId);
        // Validate: vị trí phải thuộc đúng kho, đang hoạt động và không phải khu cách ly.
        if (!Objects.equals(location.getWarehouse().getId(), warehouseId)
                || Boolean.FALSE.equals(location.getIsActive())
                || Boolean.TRUE.equals(location.getIsQuarantine())) {
            throw new BusinessRuleViolationException(errorCode);
        }
    }

    private void ensureUniqueExternalInstruction(String code, Long sourceWarehouseId, Long destinationWarehouseId,
                                                 java.time.LocalDate documentDate, Long currentId) {
        // Mã lệnh ngoài dùng để tránh tạo trùng phiếu theo kho nguồn, kho đích và ngày chứng từ; bỏ qua phiếu đã hủy/từ chối.
        boolean exists = currentId == null
                ? transferRepository.existsByExternalInstructionCodeAndSourceWarehouseIdAndDestinationWarehouseIdAndDocumentDateAndStatusNotIn(
                    code.trim(), sourceWarehouseId, destinationWarehouseId, documentDate, InterWarehouseTransferHelper.DUPLICATE_IGNORED_STATUSES)
                : transferRepository.existsByExternalInstructionCodeAndSourceWarehouseIdAndDestinationWarehouseIdAndDocumentDateAndStatusNotInAndIdNot(
                    code.trim(), sourceWarehouseId, destinationWarehouseId, documentDate, InterWarehouseTransferHelper.DUPLICATE_IGNORED_STATUSES, currentId);
        // Validate: không tạo hai phiếu còn hiệu lực cho cùng mã lệnh ngoài, kho nguồn, kho đích và ngày chứng từ.
        if (exists) {
            throw new BusinessRuleViolationException("DUPLICATE_EXTERNAL_INSTRUCTION");
        }
    }

    private void applyTransferFields(InterWarehouseTransfer transfer, String externalInstructionCode, Long sourceWarehouseId,
                                     Long destinationWarehouseId, java.time.LocalDate documentDate,
                                     java.time.LocalDate plannedDate, String notes) {
        // Xác định kỳ kế toán tại lúc tạo/sửa để phiếu nằm trong kỳ đang mở.
        transfer.setExternalInstructionCode(externalInstructionCode.trim());
        transfer.setSourceWarehouse(helper.reference(Warehouse.class, sourceWarehouseId));
        transfer.setDestinationWarehouse(helper.reference(Warehouse.class, destinationWarehouseId));
        transfer.setDocumentDate(documentDate);
        transfer.setAccountingPeriod(accountingPeriodService.resolveOpenPeriod(documentDate));
        transfer.setPlannedDate(plannedDate);
        transfer.setNotes(notes);
    }

    private void replaceItems(InterWarehouseTransfer transfer, List<InterWarehouseTransferItemRequest> requests) {
        // Khi sửa, thay toàn bộ danh sách dòng hàng vì chỉ cho phép sửa khi phiếu còn mới.
        transferItemRepository.deleteByTransferId(transfer.getId());
        for (InterWarehouseTransferItemRequest request : requests) {
            Product product = helper.reference(Product.class, request.productId());
            InterWarehouseTransferItem item = new InterWarehouseTransferItem();
            item.setTransfer(transfer);
            item.setProduct(product);
            item.setSourceLocation(request.sourceLocationId() == null ? null : helper.reference(WarehouseLocation.class, request.sourceLocationId()));
            item.setDestinationLocation(request.destinationLocationId() == null ? null : helper.reference(WarehouseLocation.class, request.destinationLocationId()));
            item.setPlannedQty(request.plannedQty());
            item.snapshotProductAttributes(product);
            transferItemRepository.save(item);
        }
    }


    private void ensureNotLoaded(InterWarehouseTransfer transfer) {
        // Khi đã có số lượng chốt gửi nghĩa là kho nguồn đã chốt xuất, phải gỡ số lượng đó trước khi hủy.
        // Validate: không hủy trực tiếp phiếu đã có số lượng chốt gửi vì có thể đã qua QC hoặc bàn giao.
        if (helper.items(transfer).stream().anyMatch(item -> item.getSentQty() != null)) {
            throw new BusinessRuleViolationException("UNSHIP_REQUIRED_BEFORE_CANCEL");
        }
    }
}
