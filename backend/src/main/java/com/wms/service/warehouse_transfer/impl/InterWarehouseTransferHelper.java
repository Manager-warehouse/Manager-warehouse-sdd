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
import com.wms.enums.warehouse_location.LocationType;
import com.wms.enums.warehouse_location.WarehouseType;
import com.wms.enums.warehouse_transfer.InterWarehouseTransferStatus;
import com.wms.enums.audit_trail.AuditAction;
import com.wms.enums.access_control.UserRole;
import com.wms.enums.order_fulfillment.TripStatus;
import com.wms.exception.BusinessRuleViolationException;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.*;
import com.wms.mapper.InterWarehouseTransferMapper;
import com.wms.util.PartnerAuditUtil;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Bộ hàm dùng chung cho toàn bộ luồng điều chuyển nội bộ.
 * Gom các quy tắc nền tảng: quyền theo kho, kiểm trạng thái phiếu, giữ/trả hàng,
 * cập nhật tồn, ghi lịch sử và tạo dữ liệu trả về cho giao diện.
 */
@Component
@RequiredArgsConstructor
public class InterWarehouseTransferHelper {

    /*
     * Helper giữ các quy tắc dùng chung cho toàn bộ luồng điều chuyển:
     * quyền theo kho, kiểm trạng thái phiếu, giữ/trả hàng, cập nhật tồn, ghi lịch sử và chuyển dữ liệu trả về.
     * Các service con gọi helper để tránh mỗi giai đoạn tự viết lại rule nền tảng.
     */
    public static final String ENTITY = "TRANSFER";
    public static final String IN_TRANSIT_WAREHOUSE_CODE = "IN_TRANSIT";
    public static final List<InterWarehouseTransferStatus> DUPLICATE_IGNORED_STATUSES =
            List.of(InterWarehouseTransferStatus.REJECTED, InterWarehouseTransferStatus.CANCELLED);
    public static final List<TripStatus> RESOURCE_BLOCKING_TRIP_STATUSES =
            List.of(TripStatus.PLANNED, TripStatus.IN_TRANSIT);
    public static final int TRANSFER_MIN_NOTICE_DAYS = 7;
    public static final int TRANSFER_WARNING_WINDOW_DAYS = 3;

    private final InterWarehouseTransferRepository transferRepository;
    private final InterWarehouseTransferItemRepository transferItemRepository;
    private final InterWarehouseTransferAllocationRepository allocationRepository;
    private final InventoryRepository inventoryRepository;
    private final WarehouseLocationRepository locationRepository;
    private final WarehouseRepository warehouseRepository;
    private final UserWarehouseAssignmentRepository assignmentRepository;
    private final TripRepository tripRepository;
    private final InterWarehouseTransferMapper transferMapper;
    private final PartnerAuditUtil auditUtil;
    private final EntityManager entityManager;

    public WarehouseLocation findQuarantineLocation(InterWarehouseTransfer transfer) {
        // Kho cần tìm khu cách ly là kho đích trong luồng thường, hoặc kho nguồn nếu xe đang quay đầu về.
        Long targetWarehouseId = transfer.isReturned() ? transfer.getSourceWarehouse().getId() : transfer.getDestinationWarehouse().getId();
        return locationRepository.findByWarehouseIdAndIsQuarantineTrueAndIsActiveTrue(targetWarehouseId)
                .stream().findFirst()
                .orElseThrow(() -> new BusinessRuleViolationException("QUARANTINE_LOCATION_NOT_CONFIGURED"));
    }

    public Warehouse findTransitWarehouse() {
        // Kho trung chuyển được nhận diện bằng loại IN_TRANSIT để admin có thể đặt mã kho thực tế như TR-01.
        return warehouseRepository.findFirstByTypeAndIsActiveTrue(WarehouseType.IN_TRANSIT)
                .orElseThrow(() -> new BusinessRuleViolationException("IN_TRANSIT_WAREHOUSE_NOT_CONFIGURED"));
    }

    public WarehouseLocation firstTransitLocation(Warehouse transitWarehouse) {
        // Kho ảo "đang vận chuyển" cần một vị trí đang hoạt động để giữ hàng đang trên đường.
        return locationRepository.findByWarehouseIdAndTypeAndIsActiveTrue(transitWarehouse.getId(), LocationType.BIN)
                .stream().findFirst()
                .orElseThrow(() -> new BusinessRuleViolationException("IN_TRANSIT_LOCATION_NOT_CONFIGURED"));
    }

    public void upsertInventory(Warehouse warehouse, Product product, Batch batch, WarehouseLocation location,
                                 BigDecimal qty, BigDecimal costPrice) {
        // Ghi tăng tồn theo đúng kho, sản phẩm, lô hàng và vị trí; khóa dòng tồn để tránh hai thao tác ghi đè nhau.
        Inventory inventory = inventoryRepository.findByStockKeyForUpdate(
                        warehouse.getId(), product.getId(), batch.getId(), location.getId())
                .orElseGet(() -> Inventory.builder()
                        .warehouse(warehouse)
                        .product(product)
                        .batch(batch)
                        .location(location)
                        .totalQty(BigDecimal.ZERO)
                        .reservedQty(BigDecimal.ZERO)
                        .costPrice(costPrice)
                        .version(0)
                        .updatedAt(OffsetDateTime.now())
                        .build());
        inventory.setTotalQty(inventory.getTotalQty().add(qty));
        inventory.setUpdatedAt(OffsetDateTime.now());
        inventoryRepository.save(inventory);
    }

    public void allocateReservations(InterWarehouseTransfer transfer) {
        // Khi trưởng kho duyệt phiếu, giữ phần hàng khả dụng ở kho nguồn theo nguyên tắc xuất trước.
        allocationRepository.deleteByTransferItemTransferId(transfer.getId());
        for (InterWarehouseTransferItem item : items(transfer)) {
            List<Inventory> candidates = inventoryRepository.findReservableForUpdate(
                    transfer.getSourceWarehouse().getId(), item.getProduct().getId());
            BigDecimal availableTotal = candidates.stream()
                    .map(inventory -> inventory.getTotalQty().subtract(inventory.getReservedQty()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (availableTotal.compareTo(item.getPlannedQty()) < 0) {
                throw new BusinessRuleViolationException(
                        "INSUFFICIENT_AVAILABLE_STOCK: "
                                + item.getProduct().getSku()
                                + " required " + item.getPlannedQty()
                                + ", available " + availableTotal
                                + " in " + transfer.getSourceWarehouse().getCode());
            }

            BigDecimal remaining = item.getPlannedQty();
            for (Inventory inventory : candidates) {
                if (remaining.signum() <= 0) {
                    break;
                }
                BigDecimal available = inventory.getTotalQty().subtract(inventory.getReservedQty());
                if (available.signum() <= 0) {
                    continue;
                }
                BigDecimal allocated = available.min(remaining);
                inventory.setReservedQty(inventory.getReservedQty().add(allocated));
                inventory.setUpdatedAt(OffsetDateTime.now());
                inventoryRepository.save(inventory);
                allocationRepository.save(InterWarehouseTransferAllocation.builder()
                        .transferItem(item)
                        .inventory(inventory)
                        .allocatedQty(allocated)
                        .build());
                remaining = remaining.subtract(allocated);
            }
        }
    }

    public void releaseReservations(InterWarehouseTransfer transfer) {
        // Khi hủy phiếu đã duyệt nhưng xe chưa rời kho, trả lại số lượng đang giữ chỗ cho tồn kho nguồn.
        for (InterWarehouseTransferAllocation allocation : allocationRepository.findByTransferItemTransferId(transfer.getId())) {
            Inventory inventory = inventoryRepository.findByIdForUpdate(allocation.getInventory().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory not found: " + allocation.getInventory().getId()));
            BigDecimal newReserved = inventory.getReservedQty().subtract(allocation.getAllocatedQty());
            if (newReserved.compareTo(java.math.BigDecimal.ZERO) < 0) {
                newReserved = java.math.BigDecimal.ZERO;
            }
            inventory.setReservedQty(newReserved);
            inventory.setUpdatedAt(OffsetDateTime.now());
            inventoryRepository.save(inventory);
        }
        allocationRepository.deleteByTransferItemTransferId(transfer.getId());
    }

    public InterWarehouseTransfer findTransfer(Long id) {
        // Lấy phiếu kèm dữ liệu chi tiết cần cho nghiệp vụ; không có thì báo không tìm thấy.
        return transferRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer not found: " + id));
    }

    public List<InterWarehouseTransferItem> items(InterWarehouseTransfer transfer) {
        // Dòng hàng luôn đọc từ repository để tránh collection lazy chưa tải đủ dữ liệu.
        return transferItemRepository.findByTransferIdOrderById(transfer.getId());
    }

    public Map<Long, InterWarehouseTransferItem> itemMap(InterWarehouseTransfer transfer) {
        // Gom dòng hàng theo id để kiểm tra dữ liệu gửi lên đủ dòng, không trùng, không lạc sang phiếu khác.
        return items(transfer).stream().collect(Collectors.toMap(InterWarehouseTransferItem::getId, Function.identity()));
    }

    public InterWarehouseTransferItem requireItem(Map<Long, InterWarehouseTransferItem> items, Long id) {
        // Dùng cho từng dòng dữ liệu gửi lên; id không thuộc phiếu hiện tại thì chặn ngay.
        InterWarehouseTransferItem item = items.get(id);
        if (item == null) {
            throw new ResourceNotFoundException("Transfer item not found: " + id);
        }
        return item;
    }

    public void requireStatus(InterWarehouseTransfer transfer, InterWarehouseTransferStatus expected) {
        // Kiểm trạng thái chung: mỗi thao tác chỉ được chạy khi phiếu đang ở đúng trạng thái nghiệp vụ.
        if (transfer.getStatus() != expected) {
            throw new BusinessRuleViolationException("INVALID_TRANSFER_STATUS");
        }
    }

    public LocalDate requiredArrivalDate(InterWarehouseTransfer transfer) {
        // Deadline cứng ưu tiên lấy từ yêu cầu điều chuyển gốc; phiếu thủ công dùng plannedDate làm ngày phải có hàng.
        if (transfer.getTransferRequest() != null && transfer.getTransferRequest().getNeededByDate() != null) {
            return transfer.getTransferRequest().getNeededByDate();
        }
        return transfer.getPlannedDate();
    }

    public LocalDateTime requiredArrivalEndAt(InterWarehouseTransfer transfer) {
        LocalDate requiredDate = requiredArrivalDate(transfer);
        return requiredDate == null ? null : requiredDate.plusDays(1).atStartOfDay();
    }

    public boolean isPastRequiredArrivalDate(InterWarehouseTransfer transfer) {
        LocalDateTime deadlineExclusive = requiredArrivalEndAt(transfer);
        return deadlineExclusive != null && !LocalDateTime.now().isBefore(deadlineExclusive);
    }

    public void ensureDeadlineOpenForPlanning(InterWarehouseTransfer transfer) {
        // Validate: quá ngày cần hàng thì không cho lập chuyến/xuất kho nữa; phiếu chưa đi phải hủy để trả tồn giữ chỗ.
        if (isPastRequiredArrivalDate(transfer)) {
            throw new BusinessRuleViolationException("TRANSFER_REQUIRED_DATE_EXPIRED");
        }
    }

    public void ensureTripArrivesWithinRequiredDate(InterWarehouseTransfer transfer, LocalDateTime plannedEndAt) {
        LocalDateTime deadlineExclusive = requiredArrivalEndAt(transfer);
        // Validate: dispatcher không được lập chuyến có giờ kết thúc sau cuối ngày cần hàng.
        if (deadlineExclusive != null && !plannedEndAt.isBefore(deadlineExclusive)) {
            throw new BusinessRuleViolationException("TRIP_END_MUST_NOT_BE_AFTER_REQUIRED_DATE");
        }
    }

    public String requiredReason(InterWarehouseTransferReasonRequest request, String code) {
        // Các thao tác đóng/từ chối/hủy phải có lý do để lịch sử thao tác đọc được.
        if (request == null || isBlank(request.reason())) {
            throw new BusinessRuleViolationException(code);
        }
        return request.reason();
    }

    public <T> T reference(Class<T> type, Long id) {
        // Lấy tham chiếu JPA khi chỉ cần gắn khóa ngoại, tránh query thừa ở luồng tạo/sửa.
        return entityManager.getReference(type, id);
    }

    public void ensureWarehouseScope(User actor, Long warehouseId) {
        // ADMIN/CEO được xem toàn hệ thống; vai trò vận hành phải được phân công vào kho liên quan.
        if (actor.getRole() == UserRole.ADMIN || actor.getRole() == UserRole.CEO) {
            return;
        }
        if (!assignmentRepository.findWarehouseIdsByUserId(actor.getId()).contains(warehouseId)) {
            throw new BusinessRuleViolationException("WAREHOUSE_SCOPE_REQUIRED");
        }
    }

    /**
     * Tải danh sách kho phụ trách một lần cho mỗi lần xử lý để tránh đọc database lặp khi lọc danh sách.
     * ADMIN và CEO không bị giới hạn theo kho nên trả danh sách rỗng làm dấu hiệu đặc biệt.
     */
    public List<Long> loadWarehouseIds(User actor) {
        // ADMIN/CEO trả danh sách rỗng làm dấu hiệu đặc biệt vì hàm kiểm quyền đã xử lý quyền toàn hệ thống trước.
        if (actor.getRole() == UserRole.ADMIN || actor.getRole() == UserRole.CEO) {
            return List.of();
        }
        return assignmentRepository.findWarehouseIdsByUserId(actor.getId());
    }

    /**
     * Bản dùng danh sách kho đã tải sẵn để tránh query lặp khi lọc danh sách.
     */
    public boolean canViewTransfer(User actor, List<Long> warehouseIds, InterWarehouseTransfer transfer) {
        // Quyền xem danh sách/chi tiết: tài xế chỉ thấy chuyến được gán; nhân sự kho thấy phiếu có kho nguồn/kho đích thuộc phạm vi mình.
        if (actor.getRole() == UserRole.ADMIN || actor.getRole() == UserRole.CEO) {
            return true;
        }

        Long sourceWarehouseId = transfer.getSourceWarehouse().getId();
        Long destinationWarehouseId = transfer.getDestinationWarehouse().getId();

        if (actor.getRole() == UserRole.DRIVER) {
            boolean belongsToWarehouse = warehouseIds.contains(sourceWarehouseId) || warehouseIds.contains(destinationWarehouseId);
            return belongsToWarehouse && transfer.getTrip() != null
                    && transfer.getTrip().getDriver() != null
                    && transfer.getTrip().getDriver().getUser() != null
                    && Objects.equals(transfer.getTrip().getDriver().getUser().getId(), actor.getId());
        }

        return switch (actor.getRole()) {
            case DISPATCHER -> warehouseIds.contains(sourceWarehouseId);
            case PLANNER, WAREHOUSE_STAFF, STOREKEEPER, WAREHOUSE_MANAGER ->
                    warehouseIds.contains(sourceWarehouseId) || warehouseIds.contains(destinationWarehouseId);
            default -> false;
        };
    }

    public boolean canViewTransfer(User actor, InterWarehouseTransfer transfer) {
        return canViewTransfer(actor, loadWarehouseIds(actor), transfer);
    }

    /**
     * Chuyển phiếu sang dữ liệu trả về bằng danh sách dòng hàng đã được tải sẵn.
     * Cách này tránh đọc database lại khi danh sách đã có đủ dòng hàng.
     */
    public InterWarehouseTransferResponse toResponseEager(InterWarehouseTransfer transfer) {
        // Dùng cho danh sách đã tải sẵn dòng hàng để giảm query lặp.
        TransferTripAlert alert = summarizeTripAlert(transfer);
        return transferMapper.toResponse(transfer, transfer.getItems(), alert.warningActive(), alert.overdue(), alert.message());
    }

    public InterWarehouseTransferResponse toResponse(InterWarehouseTransfer transfer) {
        // Dùng cho chi tiết hoặc kết quả sau thao tác: đọc dòng hàng mới nhất rồi chuyển sang dữ liệu trả về cho giao diện.
        TransferTripAlert alert = summarizeTripAlert(transfer);
        return transferMapper.toResponse(transfer, items(transfer), alert.warningActive(), alert.overdue(), alert.message());
    }

    public void audit(InterWarehouseTransfer transfer, User actor, AuditAction action,
                      Map<String, Object> before, Map<String, Object> after) {
        // Ghi lịch sử chuẩn cho thao tác sửa phiếu: ai làm, làm gì, phiếu nào, trước/sau ra sao.
        auditUtil.logChange(actor, action, ENTITY, transfer.getId(), transfer.getTransferNumber(), before, after);
    }

    public Map<String, Object> snapshot(InterWarehouseTransfer transfer) {
        // Bản ghi lịch sử chỉ lấy các trường trạng thái/chứng từ chính để dễ đọc, tránh ghi cả object lớn.
        return PartnerAuditUtil.values(
                "transferNumber", transfer.getTransferNumber(),
                "externalInstructionCode", transfer.getExternalInstructionCode(),
                "sourceWarehouseId", transfer.getSourceWarehouse() == null ? null : transfer.getSourceWarehouse().getId(),
                "destinationWarehouseId", transfer.getDestinationWarehouse() == null ? null : transfer.getDestinationWarehouse().getId(),
                "status", transfer.getStatus(),
                "tripId", transfer.getTrip() == null ? null : transfer.getTrip().getId(),
                "tripPlannedStartAt", transfer.getTrip() == null ? null : transfer.getTrip().getPlannedStartAt(),
                "tripPlannedEndAt", transfer.getTrip() == null ? null : transfer.getTrip().getPlannedEndAt(),
                "documentDate", transfer.getDocumentDate(),
                "plannedDate", transfer.getPlannedDate(),
                "discrepancyReason", transfer.getDiscrepancyReason(),
                "rejectionReason", transfer.getRejectionReason(),
                "notes", transfer.getNotes());
    }

    public TransferTripAlert summarizeTripAlert(InterWarehouseTransfer transfer) {
        // Tạo cảnh báo trễ hạn cho UI dựa vào hạn kết thúc dự kiến và trạng thái đã đóng/chưa đóng.
        Trip trip = transfer.getTrip();
        if (trip == null || trip.getPlannedEndAt() == null || isTerminalTransferStatus(transfer.getStatus())) {
            return new TransferTripAlert(false, false, null);
        }
        if (isTripOverdue(transfer)) {
            return new TransferTripAlert(true, true, "Chuyến đã quá hạn hoàn thành.");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime warningStart = trip.getPlannedEndAt().minusDays(TRANSFER_WARNING_WINDOW_DAYS);
        if (!now.isBefore(warningStart)) {
            return new TransferTripAlert(true, false, "Chuyến đang ở 3 ngày cuối trước hạn giao.");
        }
        return new TransferTripAlert(false, false, null);
    }

    public boolean isTripOverdue(InterWarehouseTransfer transfer) {
        // Quá hạn chỉ áp dụng cho phiếu chưa đóng và đã quá hạn kết thúc dự kiến.
        Trip trip = transfer.getTrip();
        return trip != null
                && trip.getPlannedEndAt() != null
                && !isTerminalTransferStatus(transfer.getStatus())
                && LocalDateTime.now().isAfter(trip.getPlannedEndAt());
    }

    public boolean isTerminalTransferStatus(InterWarehouseTransferStatus status) {
        // Các trạng thái đã đóng không còn tính cảnh báo chuyến.
        return status == InterWarehouseTransferStatus.COMPLETED
                || status == InterWarehouseTransferStatus.COMPLETED_WITH_DISCREPANCY
                || status == InterWarehouseTransferStatus.CANCELLED
                || status == InterWarehouseTransferStatus.REJECTED;
    }

    public String generateTransferNumber() {
        // Sinh mã phiếu theo ngày và đảm bảo không trùng trong cơ sở dữ liệu.
        String prefix = "TRF-" + OffsetDateTime.now().toLocalDate().toString().replace("-", "");
        int sequence = 1;
        String candidate;
        do {
            candidate = prefix + "-" + String.format("%04d", sequence++);
        } while (transferRepository.existsByTransferNumber(candidate));
        return candidate;
    }

    public String generateTripNumber() {
        // Chuyến điều chuyển dùng tiền tố riêng TTR để phân biệt với chuyến giao hàng bán.
        String prefix = "TTR-" + OffsetDateTime.now().toLocalDate().toString().replace("-", "");
        int sequence = 1;
        String candidate;
        do {
            candidate = prefix + "-" + String.format("%04d", sequence++);
        } while (tripRepository.existsByTripNumber(candidate));
        return candidate;
    }

    public BigDecimal zero(BigDecimal value) {
        // Chuẩn hóa số lượng null thành 0 cho các phép tính chênh lệch/QC.
        return value == null ? BigDecimal.ZERO : value;
    }

    public boolean isBlank(String value) {
        // Hàm nhỏ dùng chung để kiểm tra lý do, ghi chú hoặc đường dẫn ảnh có bị trống không.
        return value == null || value.isBlank();
    }

    public record TransferTripAlert(boolean warningActive, boolean overdue, String message) {
    }
}
