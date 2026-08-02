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

import com.wms.dto.request.InterWarehouseTransferTripAssignRequest;
import com.wms.dto.request.OutboundQcRequest;
import com.wms.dto.request.LoadHandoverRequest;
import com.wms.dto.request.SourceLoadReportRequest;
import com.wms.dto.response.InterWarehouseTransferResponse;
import com.wms.exception.BusinessRuleViolationException;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.*;
import com.wms.repository.driver_management.DriverRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phụ trách giai đoạn xuất kho và vận chuyển.
 * Gán xe/tài xế, báo cáo xếp hàng, QC xuất, bàn giao ảnh, xe rời kho, xe đến nơi và các mốc xe quay đầu.
 */
@Service
@RequiredArgsConstructor
public class InterWarehouseTransferShippingService {

    /*
     * LUỒNG XUẤT KHO NGUỒN VÀ VẬN CHUYỂN:
     * - Các hàm public là hành động chính trên giao diện: lập chuyến, báo xếp hàng, QC xuất, bàn giao, xe đi/đến/quay đầu.
     * - Các hàm private là hàm hỗ trợ: validate lịch xe/tài xế, kiểm deadline, kiểm điều kiện xuất và chuyển tồn sang kho đang vận chuyển.
     *
     * Giai đoạn xuất kho xử lý phiếu từ "đã duyệt" đến "đang vận chuyển" và các mốc xe di chuyển.
     * Luồng chuẩn: gán chuyến xe -> báo cáo xếp hàng -> QC xuất -> chốt gửi -> bàn giao tải hàng -> xe rời kho.
     * Khi tài xế bấm rời kho, hệ thống mới chuyển tồn từ kho nguồn sang kho ảo "đang vận chuyển".
     */
    private final InterWarehouseTransferRepository transferRepository;
    private final InterWarehouseTransferItemRepository transferItemRepository;
    private final InterWarehouseTransferAllocationRepository allocationRepository;
    private final InventoryRepository inventoryRepository;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseLocationRepository locationRepository;
    private final UserWarehouseAssignmentRepository assignmentRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final TripRepository tripRepository;
    private final InterWarehouseTransferHelper helper;

    @Transactional
    public InterWarehouseTransferResponse assignTrip(Long id, InterWarehouseTransferTripAssignRequest request, User actor) {
        // HÀM CHÍNH: Dispatcher lập hoặc đổi chuyến xe cho phiếu đã duyệt.
        // Điều phối viên gán một chuyến xe riêng cho phiếu điều chuyển; kiểm lịch, bằng lái, kho nguồn và tải trọng.
        // Bước 1: lấy phiếu và chỉ cho gán xe khi phiếu đã được duyệt, tức là kho nguồn đã giữ hàng cho phiếu này.
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        helper.requireStatus(transfer, InterWarehouseTransferStatus.APPROVED);
        if (autoCancelIfDeadlineExpiredBeforeDeparture(transfer, actor)) {
            return helper.toResponse(transfer);
        }

        // Bước 2: nếu phiếu đã có trip cũ thì chỉ cho đổi khi trip chưa rời kho.
        Trip oldTrip = transfer.getTrip();
        if (oldTrip != null) {
            // Validate: nếu trip cũ đã rời kho thì không được đổi xe/tài xế nữa.
            if (oldTrip.getStatus() != TripStatus.PLANNED) {
                throw new BusinessRuleViolationException("TRIP_ALREADY_DEPARTED");
            }
        }

        // Bước 3: lấy xe/tài xế theo dữ liệu người dùng gửi và kiểm tra điều phối viên thuộc đúng kho nguồn.
        validateTripSchedule(request);
        Vehicle vehicle = vehicleRepository.findById(request.vehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + request.vehicleId()));
        Driver driver = driverRepository.findById(request.driverId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found: " + request.driverId()));
        helper.ensureWarehouseScope(actor, transfer.getSourceWarehouse().getId());

        // Bước 4: kiểm tra tài nguyên vận tải có được dùng cho khung giờ này không.
        helper.ensureTripArrivesWithinRequiredDate(transfer, request.plannedEndAt());
        Long excludedTripId = oldTrip != null ? oldTrip.getId() : null;
        ensureVehicleAndDriverSchedulable(vehicle, driver, request.plannedStartAt(), request.plannedEndAt(), excludedTripId);
        ensureVehicleBelongsToSourceWarehouse(transfer, vehicle);
        ensureDriverBelongsToSourceWarehouse(transfer, driver);

        // Bước 5: tính tổng cân nặng chuyến = số lượng dự kiến chuyển * cân nặng mỗi sản phẩm.
        // Hệ thống hiện chỉ dùng cân nặng để kiểm xe có chở nổi hay không; thể tích để 0 theo model Trip hiện tại.
        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal totalVolume = BigDecimal.ZERO;
        for (InterWarehouseTransferItem item : helper.items(transfer)) {
            BigDecimal qty = item.getPlannedQty() != null ? item.getPlannedQty() : BigDecimal.ZERO;
            BigDecimal weight = item.getProduct().getWeightKg() != null ? item.getProduct().getWeightKg() : BigDecimal.ZERO;
            totalWeight = totalWeight.add(qty.multiply(weight));
        }

        // T033: Reject TRIP_CAPACITY_EXCEEDED when weight exceeds capacity
        // Validate: tổng trọng lượng hàng điều chuyển không được vượt tải trọng tối đa của xe.
        if (vehicle.getMaxWeightKg() != null && totalWeight.compareTo(vehicle.getMaxWeightKg()) > 0) {
            throw new BusinessRuleViolationException("TRIP_CAPACITY_EXCEEDED");
        }

        Map<String, Object> before = helper.snapshot(transfer);
        Trip trip;
        if (oldTrip != null) {
            // Bước 6a: nếu trip cũ còn dùng bởi phiếu khác thì tách phiếu này sang trip mới.
            long siblingTransfers = transferRepository.countByTripIdAndIdNot(oldTrip.getId(), transfer.getId());
            if (siblingTransfers > 0) {
                // Trip đang shared nên không sửa trực tiếp, tránh làm đổi xe/tài xế của phiếu khác.
                trip = new Trip();
                trip.setTripNumber(helper.generateTripNumber());
                trip.setCreatedAt(OffsetDateTime.now());
            } else {
                // Chuyến xe chỉ phục vụ phiếu này nên cập nhật lại chuyến cũ để giữ lịch sử mã chuyến.
                trip = oldTrip;
            }
        } else {
            // Bước 6b: phiếu chưa có trip thì tạo trip điều chuyển mới.
            trip = new Trip();
            trip.setTripNumber(helper.generateTripNumber());
            trip.setCreatedAt(OffsetDateTime.now());
        }

        // Bước 7: ghi thông tin điều phối vào Trip và gắn Trip vào phiếu.
        trip.setVehicle(vehicle);
        trip.setDriver(driver);
        trip.setDispatcher(actor);
        trip.setPlannedDate(request.plannedStartAt().toLocalDate());
        trip.setPlannedStartAt(request.plannedStartAt());
        trip.setPlannedEndAt(request.plannedEndAt());
        trip.setTripType(TripType.TRANSFER);
        trip.setStatus(TripStatus.PLANNED);
        trip.setWarehouse(transfer.getSourceWarehouse());
        trip.setTotalWeightKg(totalWeight);
        trip.setTotalVolumeM3(totalVolume);
        trip.setCalculatedWeightKg(totalWeight);
        trip.setCalculatedVolumeM3(totalVolume);
        trip.setUpdatedAt(OffsetDateTime.now());

        // Bước 8: lưu chuyến xe trước, gắn chuyến xe vào phiếu, rồi ghi lịch sử thao tác điều phối.
        transfer.setTrip(tripRepository.save(trip));
        transfer.setUpdatedAt(OffsetDateTime.now());
        InterWarehouseTransfer saved = transferRepository.save(transfer);
        helper.audit(saved, actor, AuditAction.TRANSFER_TRIP_ASSIGN, before, helper.snapshot(saved));
        return helper.toResponse(saved);
    }

    @Transactional
    public InterWarehouseTransferResponse recordSourceLoadReport(Long id, SourceLoadReportRequest request, User actor) {
        // HÀM CHÍNH: công nhân kho nguồn báo số lượng thực tế đã xếp lên xe.
        // Công nhân kho nguồn nhập số lượng thực tế đã xếp lên xe. Nếu lệch số lượng dự kiến thì bắt xếp lại/giải trình.
        // Bước 1: phiếu phải APPROVED và đã có trip điều chuyển trước khi công nhân báo số lượng xếp.
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        helper.requireStatus(transfer, InterWarehouseTransferStatus.APPROVED);
        if (autoCancelIfDeadlineExpiredBeforeDeparture(transfer, actor)) {
            return helper.toResponse(transfer);
        }
        helper.ensureWarehouseScope(actor, transfer.getSourceWarehouse().getId());
        ensureSingleTransferTrip(transfer);

        // Bước 2: lập danh sách dòng hàng của phiếu để so với dữ liệu người dùng gửi, tránh thiếu/thừa dòng.
        Map<Long, InterWarehouseTransferItem> itemsById = helper.items(transfer).stream()
                .collect(Collectors.toMap(InterWarehouseTransferItem::getId, Function.identity()));
        // Validate: báo cáo xếp hàng phải gửi đủ mọi dòng hàng để không bỏ sót hàng trước khi QC xuất.
        if (request.items().size() != itemsById.size()) {
            throw new BusinessRuleViolationException("SOURCE_LOAD_ITEMS_REQUIRED");
        }

        boolean wasReworkRequired = transfer.isSourceLoadReworkRequired();
        Map<String, Object> before = helper.snapshot(transfer);
        OffsetDateTime now = OffsetDateTime.now();
        // Bước 3: ghi số lượng đã xếp cho từng dòng; công nhân phải nhập đúng kế hoạch được giao.
        for (var row : request.items()) {
            // Validate: số lượng xếp lên xe phải là số nguyên.
            if (row.loadedQty().stripTrailingZeros().scale() > 0) {
                throw new BusinessRuleViolationException("TRANSFER_QTY_MUST_BE_WHOLE_NUMBER");
            }
            InterWarehouseTransferItem item = itemsById.get(row.transferItemId());
            // Validate: dữ liệu gửi lên không được chứa mã dòng hàng không thuộc phiếu.
            if (item == null) {
                throw new BusinessRuleViolationException("TRANSFER_ITEM_NOT_FOUND");
            }
            if (row.loadedQty().compareTo(item.getPlannedQty()) != 0) {
                throw new BusinessRuleViolationException("SOURCE_LOAD_QTY_MUST_MATCH_PLAN");
            }
            // Nếu báo cáo lại sau khi xếp lại, số lượng đã chốt gửi cũ bị xóa để thủ kho QC/chốt lại từ đầu.
            item.setLoadedQty(row.loadedQty());
            item.setLoadedReportedBy(actor);
            item.setLoadedReportedAt(now);
            item.setSentQty(null);
            transferItemRepository.save(item);
        }
        // Bước 4: cập nhật trạng thái cần xếp lại của phiếu và xóa toàn bộ kết quả QC/bàn giao cũ.
        // Lý do: khi số lượng xếp thay đổi, ảnh QC và ảnh bàn giao cũ không còn đại diện cho hàng hiện tại.
        transfer.setSourceLoadedReportedBy(actor);
        transfer.setSourceLoadedReportedAt(now);
        transfer.setSourceLoadReworkRequired(false);
        transfer.setSourceLoadReworkReason(null);
        transfer.setOutboundQcPassed(null);
        transfer.setOutboundQcNote(null);
        transfer.setOutboundQcPhotoRef(null);
        transfer.setOutboundQcBy(null);
        transfer.setOutboundQcAt(null);
        transfer.setLoadHandoverPhotoRef(null);
        transfer.setLoadHandoverBy(null);
        transfer.setLoadHandoverAt(null);
        transfer.setUpdatedAt(now);

        InterWarehouseTransfer saved = transferRepository.save(transfer);
        // Bước 5: ghi lịch sử là báo cáo xếp hàng lần đầu hoặc báo cáo sau khi xếp lại.
        helper.audit(saved, actor, wasReworkRequired ? AuditAction.TRANSFER_SOURCE_LOAD_REWORK : AuditAction.TRANSFER_SOURCE_LOAD_REPORT,
                before, helper.snapshot(saved));
        return helper.toResponse(saved);
    }

    @Transactional
    public InterWarehouseTransferResponse shipTransfer(Long id, User actor) {
        // HÀM CHÍNH: thủ kho nguồn chốt số lượng gửi sau khi QC xuất đạt.
        // Thủ kho nguồn chốt số lượng gửi bằng đúng số lượng đã xếp sau khi QC xuất đạt.
        // Bước 1: chỉ chốt gửi khi phiếu đã duyệt, đúng kho nguồn, đã có chuyến xe và đã báo cáo xếp đủ.
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        helper.requireStatus(transfer, InterWarehouseTransferStatus.APPROVED);
        if (autoCancelIfDeadlineExpiredBeforeDeparture(transfer, actor)) {
            return helper.toResponse(transfer);
        }
        helper.ensureWarehouseScope(actor, transfer.getSourceWarehouse().getId());
        ensureSingleTransferTrip(transfer);
        ensureSourceLoadReadyForQc(transfer);
        ensureNoSourceLoadRework(transfer);
        // Validate: chỉ được chốt số lượng gửi sau khi QC xuất đạt.
        if (transfer.getOutboundQcPassed() == null || !transfer.getOutboundQcPassed()) {
            throw new BusinessRuleViolationException("OUTBOUND_QC_NOT_PASSED");
        }
        Map<String, Object> before = helper.snapshot(transfer);
        // Bước 2: số lượng gửi chính thức lấy từ số lượng đã xếp và đã QC đạt; lúc rời kho sẽ dựa vào số này.
        for (InterWarehouseTransferItem item : helper.items(transfer)) {
            item.setSentQty(item.getLoadedQty());
            transferItemRepository.save(item);
        }
        transfer.setUpdatedAt(OffsetDateTime.now());
        InterWarehouseTransfer saved = transferRepository.save(transfer);
        helper.audit(saved, actor, AuditAction.TRANSFER_SHIP, before, helper.snapshot(saved));
        return helper.toResponse(saved);
    }

    @Transactional
    public InterWarehouseTransferResponse unshipTransfer(Long id, User actor) {
        // Gỡ số lượng đã chốt gửi khi cần quay lại trước bước xe rời kho.
        // Bước 1: chỉ gỡ được khi xe chưa rời kho, vì sau khi rời kho hàng đã sang kho ảo "đang vận chuyển".
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        helper.requireStatus(transfer, InterWarehouseTransferStatus.APPROVED);
        helper.ensureWarehouseScope(actor, transfer.getSourceWarehouse().getId());
        Map<String, Object> before = helper.snapshot(transfer);
        // Bước 2: xóa số lượng đã chốt gửi ở mọi dòng, giữ lại số đã xếp/QC để người vận hành xử lý tiếp.
        for (InterWarehouseTransferItem item : helper.items(transfer)) {
            item.setSentQty(null);
            transferItemRepository.save(item);
        }
        transfer.setUpdatedAt(OffsetDateTime.now());
        InterWarehouseTransfer saved = transferRepository.save(transfer);
        helper.audit(saved, actor, AuditAction.TRANSFER_UNSHIP, before, helper.snapshot(saved));
        return helper.toResponse(saved);
    }

    @Transactional
    public InterWarehouseTransferResponse recordOutboundQc(Long id, OutboundQcRequest request, User actor) {
        // HÀM CHÍNH: thủ kho/QL kho nguồn ghi kết quả QC xuất.
        // QC xuất kho nguồn: nếu không đạt thì phải ghi lý do và bắt kho nguồn xếp/kiểm lại trước khi bàn giao.
        // Bước 1: QC xuất chỉ chạy sau khi đã báo cáo xếp đủ và phiếu đang ở trạng thái đã duyệt.
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        helper.requireStatus(transfer, InterWarehouseTransferStatus.APPROVED);
        if (autoCancelIfDeadlineExpiredBeforeDeparture(transfer, actor)) {
            return helper.toResponse(transfer);
        }
        helper.ensureWarehouseScope(actor, transfer.getSourceWarehouse().getId());
        ensureSourceLoadReadyForQc(transfer);
        // Validate: QC không đạt phải có ghi chú để công nhân biết lý do cần xếp/kiểm lại.
        if (!Boolean.TRUE.equals(request.passed()) && helper.isBlank(request.note())) {
            throw new BusinessRuleViolationException("OUTBOUND_QC_FAILURE_REASON_REQUIRED");
        }

        Map<String, Object> before = helper.snapshot(transfer);
        // Bước 2: lưu kết quả QC, ảnh QC và người kiểm. Nếu không đạt thì bật trạng thái cần xếp/kiểm lại.
        transfer.setOutboundQcPassed(request.passed());
        transfer.setOutboundQcNote(request.note());
        transfer.setOutboundQcPhotoRef(request.photoRef());
        transfer.setOutboundQcBy(actor);
        transfer.setOutboundQcAt(OffsetDateTime.now());
        transfer.setSourceLoadReworkRequired(!Boolean.TRUE.equals(request.passed()));
        transfer.setSourceLoadReworkReason(Boolean.TRUE.equals(request.passed()) ? null : request.note());
        transfer.setUpdatedAt(OffsetDateTime.now());

        InterWarehouseTransfer saved = transferRepository.save(transfer);
        helper.audit(saved, actor, AuditAction.TRANSFER_OUTBOUND_QC, before, helper.snapshot(saved));
        return helper.toResponse(saved);
    }

    @Transactional
    public InterWarehouseTransferResponse loadHandover(Long id, LoadHandoverRequest request, User actor) {
        // Chụp ảnh bàn giao tải hàng cho tài xế; depart bắt buộc có ảnh này.
        // Bước 1: chỉ được bàn giao khi đã xếp đủ, không còn yêu cầu xếp lại và QC xuất đã đạt.
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        helper.requireStatus(transfer, InterWarehouseTransferStatus.APPROVED);
        if (autoCancelIfDeadlineExpiredBeforeDeparture(transfer, actor)) {
            return helper.toResponse(transfer);
        }
        helper.ensureWarehouseScope(actor, transfer.getSourceWarehouse().getId());

        ensureSourceLoadReadyForQc(transfer);
        ensureNoSourceLoadRework(transfer);
        // Validate: chỉ được ghi bàn giao sau khi QC xuất đã đạt.
        if (transfer.getOutboundQcPassed() == null || !transfer.getOutboundQcPassed()) {
            throw new BusinessRuleViolationException("OUTBOUND_QC_REQUIRED");
        }

        Map<String, Object> before = helper.snapshot(transfer);
        // Bước 2: lưu ảnh bàn giao, người thực hiện và thời điểm; đây là bằng chứng trước khi tài xế rời kho.
        transfer.setLoadHandoverPhotoRef(request.photoRef());
        transfer.setLoadHandoverBy(actor);
        transfer.setLoadHandoverAt(OffsetDateTime.now());
        transfer.setUpdatedAt(OffsetDateTime.now());

        InterWarehouseTransfer saved = transferRepository.save(transfer);
        helper.audit(saved, actor, AuditAction.TRANSFER_LOAD_HANDOVER, before, helper.snapshot(saved));
        return helper.toResponse(saved);
    }

    @Transactional
    public InterWarehouseTransferResponse departTransfer(Long id, User actor) {
        // HÀM CHÍNH: tài xế xác nhận xe rời kho nguồn, lúc này tồn chuyển sang kho đang vận chuyển.
        // Tài xế rời kho: trừ tồn/giữ hàng ở kho nguồn và cộng hàng sang kho ảo "đang vận chuyển".
        // Bước 1: chỉ tài xế được gán mới được bấm rời kho và mọi bước xuất kho phải hoàn tất.
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        helper.requireStatus(transfer, InterWarehouseTransferStatus.APPROVED);
        if (autoCancelIfDeadlineExpiredBeforeDeparture(transfer, actor)) {
            return helper.toResponse(transfer);
        }
        ensureAssignedDriver(transfer, actor);
        ensureSourceLoadReadyForQc(transfer);
        ensureNoSourceLoadRework(transfer);
        ensureAllSent(transfer);

        // Validate: tài xế không được rời kho nếu QC xuất chưa đạt.
        if (transfer.getOutboundQcPassed() == null || !transfer.getOutboundQcPassed()) {
            throw new BusinessRuleViolationException("OUTBOUND_QC_NOT_PASSED");
        }
        // Validate: depart bắt buộc đã có ảnh bàn giao tải hàng.
        if (transfer.getLoadHandoverPhotoRef() == null) {
            throw new BusinessRuleViolationException("LOAD_HANDOVER_REQUIRED");
        }

        Map<String, Object> before = helper.snapshot(transfer);
        // Ensure UOM snapshot attributes are locked for all transfer items before departure
        for (InterWarehouseTransferItem item : helper.items(transfer)) {
            item.snapshotProductAttributes(item.getProduct());
            transferItemRepository.save(item);
        }
        // Bước 2: đây là thời điểm hàng chính thức rời kho nguồn và xuất hiện trong kho ảo "đang vận chuyển".
        moveSourceToTransit(transfer);
        // Bước 3: chuyển tài xế, xe, chuyến xe và phiếu sang trạng thái đang vận chuyển.
        transfer.getTrip().getDriver().setStatus(DriverStatus.ON_TRIP);
        transfer.getTrip().getVehicle().setStatus(VehicleStatus.ON_TRIP);
        transfer.getTrip().setStatus(TripStatus.IN_TRANSIT);
        transfer.getTrip().setUpdatedAt(OffsetDateTime.now());
        transfer.setStatus(InterWarehouseTransferStatus.IN_TRANSIT);
        transfer.setUpdatedAt(OffsetDateTime.now());
        InterWarehouseTransfer saved = transferRepository.save(transfer);
        helper.audit(saved, actor, AuditAction.TRANSFER_DEPART, before, helper.snapshot(saved));
        return helper.toResponse(saved);
    }

    private boolean autoCancelIfDeadlineExpiredBeforeDeparture(InterWarehouseTransfer transfer, User actor) {
        // HÀM HỖ TRỢ: tự hủy phiếu nếu quá ngày cần hàng trước khi xe rời kho.
        // Ngày cần hàng là deadline cứng: quá deadline mà xe chưa rời kho thì phiếu bị hủy và trả lại hàng đang giữ chỗ.
        if (!helper.isPastRequiredArrivalDate(transfer)) {
            return false;
        }
        Map<String, Object> before = helper.snapshot(transfer);
        helper.releaseReservations(transfer);
        for (InterWarehouseTransferItem item : helper.items(transfer)) {
            item.setSentQty(null);
            transferItemRepository.save(item);
        }
        transfer.setStatus(InterWarehouseTransferStatus.CANCELLED);
        transfer.setRejectionReason("TRANSFER_REQUIRED_DATE_EXPIRED");
        transfer.setUpdatedAt(OffsetDateTime.now());
        InterWarehouseTransfer saved = transferRepository.save(transfer);
        helper.audit(saved, actor, AuditAction.TRANSFER_CANCEL, before, helper.snapshot(saved));
        return true;
    }

    private boolean autoForceReturnIfDeadlineMissedInTransit(InterWarehouseTransfer transfer, User actor) {
        // HÀM HỖ TRỢ: quá hạn khi đang vận chuyển thì chuyển phiếu sang nhánh quay đầu.
        // Khi hàng đã lên xe thì không được cancel mất dấu hàng; quá deadline bắt buộc chuyển sang nhánh quay đầu về kho nguồn.
        if (transfer.isReturned() || !helper.isPastRequiredArrivalDate(transfer)) {
            return false;
        }
        Map<String, Object> before = helper.snapshot(transfer);
        transfer.setReturned(true);
        transfer.setReturnRequested(false);
        transfer.setReturnReason("TRANSFER_REQUIRED_DATE_EXPIRED");
        transfer.setUpdatedAt(OffsetDateTime.now());
        InterWarehouseTransfer saved = transferRepository.save(transfer);
        helper.audit(saved, actor, AuditAction.TRANSFER_RETURN_TO_SOURCE, before, helper.snapshot(saved));
        return true;
    }

    private void validateTripSchedule(InterWarehouseTransferTripAssignRequest request) {
        // HÀM HỖ TRỢ: validate thời gian bắt đầu/kết thúc chuyến.
        // Lịch chuyến phải đi tới tương lai hợp lệ, tránh tạo chuyến đã hết hạn ngay lúc assign.
        // Hàm này chỉ kiểm tra tính hợp lệ của thời gian người dùng gửi lên, chưa đụng xe/tài xế.
        // Validate: thời điểm kết thúc phải sau thời điểm bắt đầu.
        if (!request.plannedEndAt().isAfter(request.plannedStartAt())) {
            throw new BusinessRuleViolationException("TRIP_SCHEDULE_INVALID");
        }
        LocalDateTime now = LocalDateTime.now();
        // Validate: không tạo chuyến bắt đầu quá khứ ngoài biên dung sai 15 phút.
        if (request.plannedStartAt().isBefore(now.minusMinutes(15))) {
            throw new BusinessRuleViolationException("TRIP_START_IN_PAST");
        }
        // Validate: không tạo chuyến có plannedEndAt đã qua.
        if (request.plannedEndAt().isBefore(now)) {
            throw new BusinessRuleViolationException("TRIP_END_IN_PAST");
        }
    }

    private void ensureVehicleAndDriverSchedulable(Vehicle vehicle,
                                                   Driver driver,
                                                   LocalDateTime plannedStartAt,
                                                   LocalDateTime plannedEndAt,
                                                   Long excludedTripId) {
        // Xe/tài xế phải đang hoạt động, không bận hoặc bảo dưỡng, bằng lái còn hạn và không trùng lịch.
        // Hàm này gom toàn bộ rule "tài nguyên vận tải có thể nhận chuyến mới không".
        // Validate: xe đã bị tắt hoặc đang bảo dưỡng thì không được gán chuyến.
        if (Boolean.FALSE.equals(vehicle.getIsActive()) || vehicle.getStatus() == VehicleStatus.MAINTENANCE) {
            throw new BusinessRuleViolationException("VEHICLE_NOT_AVAILABLE");
        }
        // Validate: tài xế đã bị tắt hoặc đang nghỉ/không khả dụng thì không được gán chuyến.
        if (Boolean.FALSE.equals(driver.getIsActive()) || driver.getStatus() == DriverStatus.UNAVAILABLE) {
            throw new BusinessRuleViolationException("DRIVER_NOT_AVAILABLE");
        }
        // Validate: tài xế chưa có hạn GPLX hoặc GPLX hết hạn thì không được gán chuyến.
        if (driver.getLicenseExpiry() == null || driver.getLicenseExpiry().isBefore(java.time.LocalDate.now())) {
            throw new BusinessRuleViolationException("DRIVER_LICENSE_EXPIRED");
        }
        // Validate: xe không được có chuyến đã lên kế hoạch hoặc đang chạy khác trùng lịch.
        if (tripRepository.existsVehicleScheduleOverlapExcludingTrip(vehicle.getId(), plannedStartAt, plannedEndAt, InterWarehouseTransferHelper.RESOURCE_BLOCKING_TRIP_STATUSES, excludedTripId)) {
            throw new BusinessRuleViolationException("VEHICLE_SCHEDULE_OVERLAP");
        }
        // Validate: tài xế không được có chuyến đã lên kế hoạch hoặc đang chạy khác trùng lịch.
        if (tripRepository.existsDriverScheduleOverlapExcludingTrip(driver.getId(), plannedStartAt, plannedEndAt, InterWarehouseTransferHelper.RESOURCE_BLOCKING_TRIP_STATUSES, excludedTripId)) {
            throw new BusinessRuleViolationException("DRIVER_SCHEDULE_OVERLAP");
        }
    }

    private void ensureVehicleBelongsToSourceWarehouse(InterWarehouseTransfer transfer, Vehicle vehicle) {
        // Xe nội bộ phải thuộc kho nguồn của phiếu để tránh điều phối chéo kho ngoài phạm vi phụ trách.
        // Dùng warehouse của Vehicle, khác với tài xế dùng UserWarehouseAssignment.
        if (vehicle.getWarehouse() == null || !Objects.equals(vehicle.getWarehouse().getId(), transfer.getSourceWarehouse().getId())) {
            throw new BusinessRuleViolationException("VEHICLE_SOURCE_WAREHOUSE_REQUIRED");
        }
    }

    private void ensureDriverBelongsToSourceWarehouse(InterWarehouseTransfer transfer, Driver driver) {
        // Tài xế phải có warehouse assignment tại kho nguồn.
        // Driver entity nối sang User; quyền kho của tài xế nằm ở assignment theo userId.
        Long sourceWarehouseId = transfer.getSourceWarehouse().getId();
        Long driverUserId = driver.getUser().getId();
        if (!assignmentRepository.findWarehouseIdsByUserId(driverUserId).contains(sourceWarehouseId)) {
            throw new BusinessRuleViolationException("DRIVER_SOURCE_WAREHOUSE_REQUIRED");
        }
    }

    private void ensureSingleTransferTrip(InterWarehouseTransfer transfer) {
        // Điều chuyển nội bộ chỉ đi với chuyến xe loại "điều chuyển kho", không dùng chung chuyến giao hàng bán.
        // Các bước xếp hàng/chốt gửi/rời kho đều gọi hàm này để chắc phiếu đã được điều phối đúng loại chuyến.
        if (transfer.getTrip() == null || transfer.getTrip().getTripType() != TripType.TRANSFER) {
            throw new BusinessRuleViolationException("TRANSFER_TRIP_REQUIRED");
        }
    }

    private void ensureAssignedDriver(InterWarehouseTransfer transfer, User actor) {
        // Chỉ đúng user tài xế được gán trong trip mới được bấm depart/arrive.
        // So sánh tài khoản đang đăng nhập với tài khoản gắn trên hồ sơ tài xế, không so với mã hồ sơ tài xế.
        ensureSingleTransferTrip(transfer);
        Long driverUserId = transfer.getTrip().getDriver().getUser().getId();
        if (!Objects.equals(driverUserId, actor.getId())) {
            throw new BusinessRuleViolationException("ASSIGNED_DRIVER_REQUIRED");
        }
    }

    private void ensureAllSent(InterWarehouseTransfer transfer) {
        // Xe rời kho chỉ cho phép khi toàn bộ dòng đã được thủ kho chốt gửi đủ số lượng dự kiến.
        // Nếu thiếu số lượng chốt gửi hoặc chốt khác số lượng dự kiến thì hàng chưa sẵn sàng rời kho.
        if (helper.items(transfer).stream().anyMatch(item -> item.getSentQty() == null
                || item.getSentQty().compareTo(item.getPlannedQty()) != 0)) {
            throw new BusinessRuleViolationException("SENT_QTY_REQUIRED");
        }
    }

    private void ensureSourceLoadReadyForQc(InterWarehouseTransfer transfer) {
        // QC/bàn giao/rời kho cần đủ số lượng đã xếp và không còn lệch số lượng dự kiến.
        // Hàm này là chốt kiểm chung sau bước công nhân báo số lượng xếp.
        if (helper.items(transfer).stream().anyMatch(item -> item.getLoadedQty() == null)) {
            throw new BusinessRuleViolationException("SOURCE_LOAD_REPORT_REQUIRED");
        }
        if (helper.items(transfer).stream()
                .anyMatch(item -> item.getLoadedQty().compareTo(item.getPlannedQty()) != 0)) {
            throw new BusinessRuleViolationException("SENT_QTY_MISMATCH");
        }
    }

    private void ensureNoSourceLoadRework(InterWarehouseTransfer transfer) {
        // Nếu xếp hàng hoặc QC không đạt thì bắt buộc công nhân xử lý lại và báo lại trước khi tiếp tục.
        // Cờ "cần xếp/kiểm lại" được bật khi số lượng đã xếp lệch dự kiến hoặc QC xuất không đạt.
        if (transfer.isSourceLoadReworkRequired()) {
            throw new BusinessRuleViolationException("SOURCE_LOAD_REWORK_REQUIRED");
        }
    }

    private void moveSourceToTransit(InterWarehouseTransfer transfer) {
        // HÀM HỖ TRỢ: chuyển tồn thật từ kho nguồn sang kho ảo đang vận chuyển.
        // Ghi nhận tồn khi xe rời kho: giảm tồn kho nguồn và cộng đúng lô hàng sang kho ảo "đang vận chuyển".
        // Bước 1: tìm kho ảo "đang vận chuyển" và vị trí đang hoạt động để giữ hàng trên đường.
        Warehouse transitWarehouse = helper.findTransitWarehouse();
        WarehouseLocation transitLocation = helper.firstTransitLocation(transitWarehouse);
        // Bước 2: đi theo từng dòng tồn đã được giữ chỗ khi trưởng kho duyệt phiếu.
        // Làm vậy để chuyển đúng lô hàng, đúng thứ tự xuất trước và đúng giá vốn đã giữ từ đầu.
        for (InterWarehouseTransferAllocation allocation : allocationRepository.findByTransferItemTransferId(transfer.getId())) {
            // Bước 2.1: khóa dòng tồn kho nguồn trước khi trừ số lượng tồn và số lượng đang giữ chỗ.
            Inventory source = inventoryRepository.findByIdForUpdate(allocation.getInventory().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory not found: " + allocation.getInventory().getId()));
            source.setTotalQty(source.getTotalQty().subtract(allocation.getAllocatedQty()));
            BigDecimal newReserved = source.getReservedQty().subtract(allocation.getAllocatedQty());
            // Validate: số lượng đang giữ chỗ không được âm sau khi trừ phần hàng đã rời kho.
            if (newReserved.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessRuleViolationException("INVENTORY_INVARIANT_VIOLATED: Reserved quantity cannot be negative");
            }
            source.setReservedQty(newReserved);
            source.setUpdatedAt(OffsetDateTime.now());
            inventoryRepository.save(source);
            // Bước 2.2: cộng đúng lô hàng, giá vốn và số lượng vừa trừ vào kho ảo "đang vận chuyển".
            helper.upsertInventory(transitWarehouse, allocation.getTransferItem().getProduct(), source.getBatch(),
                    transitLocation, allocation.getAllocatedQty(), source.getCostPrice());
        }
    }

    @Transactional
    public InterWarehouseTransferResponse driverArrive(Long id, User actor) {
        // HÀM CHÍNH: tài xế xác nhận đã đến kho đích hoặc điểm quay đầu.
        // Tài xế đến điểm nhận; kho nhận chưa được nhập số lượng nếu chưa có ảnh/bản ghi bàn giao khi xe đến.
        // Bước 1: chỉ tài xế được gán của phiếu đang vận chuyển được ghi mốc đến nơi.
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        helper.requireStatus(transfer, InterWarehouseTransferStatus.IN_TRANSIT);
        ensureAssignedDriver(transfer, actor);
        if (autoForceReturnIfDeadlineMissedInTransit(transfer, actor)) {
            return helper.toResponse(transfer);
        }

        Map<String, Object> before = helper.snapshot(transfer);
        // Bước 2: chỉ ghi thời điểm xe đến, chưa thay đổi tồn kho hoặc trạng thái phiếu.
        transfer.setDriverArrivedAt(OffsetDateTime.now());
        transfer.setUpdatedAt(OffsetDateTime.now());

        InterWarehouseTransfer saved = transferRepository.save(transfer);
        helper.audit(saved, actor, AuditAction.TRANSFER_ARRIVE, before, helper.snapshot(saved));
        return helper.toResponse(saved);
    }

    @Transactional
    public InterWarehouseTransferResponse receivingHandover(Long id, LoadHandoverRequest request, User actor) {
        // HÀM CHÍNH: kho nhận ghi bằng chứng bàn giao khi xe đến.
        // Kho nhận xác nhận bàn giao với ảnh; đây là bước bắt buộc trước khi nhập số lượng nhận.
        // Bước 1: xác định kho được phép bàn giao. Nếu xe quay đầu thì kho nhận lại chính là kho nguồn.
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        helper.requireStatus(transfer, InterWarehouseTransferStatus.IN_TRANSIT);
        if (autoForceReturnIfDeadlineMissedInTransit(transfer, actor)) {
            return helper.toResponse(transfer);
        }

        Long targetWarehouseId = transfer.isReturned() ? transfer.getSourceWarehouse().getId() : transfer.getDestinationWarehouse().getId();
        helper.ensureWarehouseScope(actor, targetWarehouseId);

        // Validate: phải có mốc tài xế đến trước khi kho ghi nhận bàn giao.
        if (transfer.getDriverArrivedAt() == null) {
            throw new BusinessRuleViolationException("DRIVER_ARRIVE_REQUIRED");
        }
        // Validate: nếu đang chờ duyệt yêu cầu xe quay đầu thì không cho bàn giao nhận hàng bình thường.
        if (!Boolean.TRUE.equals(transfer.isReturned()) && Boolean.TRUE.equals(transfer.isReturnRequested())) {
            throw new BusinessRuleViolationException("RETURN_REQUEST_PENDING");
        }

        Map<String, Object> before = helper.snapshot(transfer);
        // Bước 2: lưu ảnh bàn giao khi xe đến nơi; bước nhập số lượng nhận sẽ kiểm mốc này.
        transfer.setArrivalHandoverAt(OffsetDateTime.now());
        transfer.setArrivalHandoverPhotoRef(request.photoRef());
        transfer.setArrivalHandoverBy(actor);
        transfer.setUpdatedAt(OffsetDateTime.now());

        InterWarehouseTransfer saved = transferRepository.save(transfer);
        helper.audit(saved, actor, AuditAction.TRANSFER_ARRIVAL_HANDOVER, before, helper.snapshot(saved));
        return helper.toResponse(saved);
    }

    @Transactional
    public InterWarehouseTransferResponse returnDepart(Long id, User actor) {
        // HÀM CHÍNH: tài xế xác nhận xe bắt đầu quay đầu từ kho đích về kho nguồn.
        // Chuyến quay đầu: tài xế rời kho đích để chở hàng quay về kho nguồn.
        // Bước 1: chỉ cho chạy nếu phiếu đã được đánh dấu là xe quay đầu về kho nguồn.
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        helper.requireStatus(transfer, InterWarehouseTransferStatus.IN_TRANSIT);
        // Validate: nếu phiếu chưa được duyệt cho xe quay đầu thì không được ghi mốc xe rời kho đích.
        if (!Boolean.TRUE.equals(transfer.isReturned())) {
            throw new BusinessRuleViolationException("TRANSFER_NOT_RETURNED_LEG");
        }
        ensureAssignedDriver(transfer, actor);

        Map<String, Object> before = helper.snapshot(transfer);
        // Bước 2: ghi thời điểm xe rời kho đích để quay về, hàng vẫn đang nằm ở kho ảo đang vận chuyển.
        transfer.setReturnDepartedAt(OffsetDateTime.now());
        transfer.setUpdatedAt(OffsetDateTime.now());

        InterWarehouseTransfer saved = transferRepository.save(transfer);
        helper.audit(saved, actor, AuditAction.TRANSFER_RETURN_DEPART, before, helper.snapshot(saved));
        return helper.toResponse(saved);
    }

    @Transactional
    public InterWarehouseTransferResponse returnArrive(Long id, User actor) {
        // HÀM CHÍNH: tài xế xác nhận xe quay đầu đã về tới kho nguồn.
        // Chuyến quay đầu: tài xế đã chở hàng quay về tới kho nguồn.
        // Bước 1: chỉ được ghi xe về tới kho nguồn sau khi đã ghi mốc xe rời kho đích.
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        helper.requireStatus(transfer, InterWarehouseTransferStatus.IN_TRANSIT);
        // Validate: nếu phiếu chưa được duyệt cho xe quay đầu thì không được ghi mốc xe về kho nguồn.
        if (!Boolean.TRUE.equals(transfer.isReturned())) {
            throw new BusinessRuleViolationException("TRANSFER_NOT_RETURNED_LEG");
        }
        ensureAssignedDriver(transfer, actor);
        // Validate: phải có mốc xe rời kho đích trước, rồi mới được ghi mốc xe về tới kho nguồn.
        if (transfer.getReturnDepartedAt() == null) {
            throw new BusinessRuleViolationException("RETURN_DEPART_REQUIRED");
        }

        Map<String, Object> before = helper.snapshot(transfer);
        // Bước 2: ghi thời điểm xe về tới kho nguồn, lúc này hàng vẫn chưa nhập lại kho nguồn.
        transfer.setReturnArrivedAt(OffsetDateTime.now());
        transfer.setUpdatedAt(OffsetDateTime.now());

        InterWarehouseTransfer saved = transferRepository.save(transfer);
        helper.audit(saved, actor, AuditAction.TRANSFER_RETURN_ARRIVE, before, helper.snapshot(saved));
        return helper.toResponse(saved);
    }

    @Transactional
    public InterWarehouseTransferResponse returnHandover(Long id, LoadHandoverRequest request, User actor) {
        // HÀM CHÍNH: kho nguồn nhận bàn giao hàng quay đầu.
        // Kho nguồn nhận bàn giao ảnh khi xe quay đầu; service nhận hàng sẽ kiểm mốc này trước khi đếm và QC.
        // Bước 1: chỉ thủ kho/trưởng kho/admin/CEO thuộc kho nguồn được xác nhận bàn giao hàng quay về.
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        helper.requireStatus(transfer, InterWarehouseTransferStatus.IN_TRANSIT);
        // Validate: nếu phiếu chưa được duyệt cho xe quay đầu thì không được xác nhận bàn giao hàng quay về.
        if (!Boolean.TRUE.equals(transfer.isReturned())) {
            throw new BusinessRuleViolationException("TRANSFER_NOT_RETURNED_LEG");
        }
        // Validate: role phải là người có quyền nhận bàn giao tại kho.
        if (actor.getRole() != UserRole.STOREKEEPER
                && actor.getRole() != UserRole.WAREHOUSE_MANAGER
                && actor.getRole() != UserRole.ADMIN
                && actor.getRole() != UserRole.CEO) {
            throw new BusinessRuleViolationException("RETURN_HANDOVER_STOREKEEPER_REQUIRED");
        }
        helper.ensureWarehouseScope(actor, transfer.getSourceWarehouse().getId());
        // Validate: phải có mốc xe quay về kho nguồn trước khi nhận bàn giao.
        if (transfer.getReturnArrivedAt() == null) {
            throw new BusinessRuleViolationException("RETURN_ARRIVE_REQUIRED");
        }

        Map<String, Object> before = helper.snapshot(transfer);
        // Bước 2: lưu ảnh bàn giao hàng quay về; sau đó kho nguồn mới được nhập số lượng nhận.
        transfer.setReturnArrivalHandoverAt(OffsetDateTime.now());
        transfer.setReturnArrivalHandoverBy(actor);
        transfer.setReturnPhotoRef(request.photoRef());
        transfer.setUpdatedAt(OffsetDateTime.now());

        InterWarehouseTransfer saved = transferRepository.save(transfer);
        helper.audit(saved, actor, AuditAction.TRANSFER_RETURN_HANDOVER, before, helper.snapshot(saved));
        return helper.toResponse(saved);
    }
}
