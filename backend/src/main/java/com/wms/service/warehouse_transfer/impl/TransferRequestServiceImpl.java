package com.wms.service.warehouse_transfer.impl;
import com.wms.dto.request.InterWarehouseTransferCreateRequest;
import com.wms.dto.request.InterWarehouseTransferItemRequest;
import com.wms.dto.request.TransferRequestCreateRequest;
import com.wms.dto.request.TransferRequestItemRequest;
import com.wms.dto.request.TransferRequestRejectRequest;
import com.wms.dto.request.TransferRequestUpdateRequest;
import com.wms.dto.response.InterWarehouseTransferResponse;
import com.wms.dto.response.TransferRequestItemResponse;
import com.wms.dto.response.TransferRequestResponse;
import com.wms.dto.response.WarehouseStockLookupResponse;
import com.wms.entity.access_control.User;
import com.wms.entity.product_catalog.Product;
import com.wms.entity.warehouse_location.Warehouse;
import com.wms.entity.warehouse_transfer.InterWarehouseTransfer;
import com.wms.entity.warehouse_transfer.TransferRequest;
import com.wms.entity.warehouse_transfer.TransferRequestItem;
import com.wms.enums.access_control.UserRole;
import com.wms.enums.audit_trail.AuditAction;
import com.wms.enums.warehouse_location.WarehouseType;
import com.wms.enums.warehouse_transfer.InterWarehouseTransferStatus;
import com.wms.enums.warehouse_transfer.TransferRequestStatus;
import com.wms.exception.BusinessRuleViolationException;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.InterWarehouseTransferRepository;
import com.wms.repository.InventoryRepository;
import com.wms.repository.TransferRequestItemRepository;
import com.wms.repository.TransferRequestRepository;
import com.wms.repository.UserWarehouseAssignmentRepository;
import com.wms.repository.WarehouseRepository;
import com.wms.repository.product_catalog.ProductRepository;
import com.wms.service.warehouse_transfer.InterWarehouseTransferService;
import com.wms.service.warehouse_transfer.TransferRequestService;
import com.wms.util.PartnerAuditUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransferRequestServiceImpl implements TransferRequestService {

    private static final List<TransferRequestStatus> OPEN_DUPLICATE_STATUSES = List.of(
            TransferRequestStatus.DRAFT,
            TransferRequestStatus.SUBMITTED,
            TransferRequestStatus.APPROVED
    );

    /*
     * LUỒNG YÊU CẦU ĐIỀU CHUYỂN:
     * - Các hàm public là hành động chính của người dùng: tạo nháp, gửi duyệt, nguồn duyệt/từ chối, Planner chuyển thành phiếu TRF.
     * - Các hàm private cuối file là hàm hỗ trợ: tìm request, kiểm quyền kho, kiểm tồn, tự hủy quá hạn, sinh mã và map response.
     */
    private final TransferRequestRepository requestRepository;
    private final TransferRequestItemRepository requestItemRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final InterWarehouseTransferRepository interWarehouseTransferRepository;
    private final UserWarehouseAssignmentRepository userWarehouseAssignmentRepository;
    private final InterWarehouseTransferService transferService;
    private final PartnerAuditUtil auditUtil;

    @Override
    @Transactional(readOnly = true)
    public List<TransferRequestResponse> getAllRequests(User actor) {
        // HÀM CHÍNH: liệt kê yêu cầu điều chuyển người dùng được phép xem.
        List<Long> assignedWarehouseIds = loadWarehouseIds(actor);
        return requestRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(req -> canViewRequest(actor, assignedWarehouseIds, req))
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TransferRequestResponse getRequestById(Long id, User actor) {
        // HÀM CHÍNH: xem chi tiết một yêu cầu điều chuyển sau khi kiểm quyền theo kho.
        TransferRequest req = findRequest(id);
        List<Long> assignedWarehouseIds = loadWarehouseIds(actor);
        if (!canViewRequest(actor, assignedWarehouseIds, req)) {
            throw new BusinessRuleViolationException("WAREHOUSE_SCOPE_REQUIRED");
        }
        return toResponse(req);
    }

    @Override
    @Transactional
    public TransferRequestResponse createRequest(TransferRequestCreateRequest request, User actor) {
        // HÀM CHÍNH: quản lý kho tạo yêu cầu xin điều chuyển hàng về kho mình.
        ensureRequesterRole(actor);
        ensureWarehouseScope(actor, request.destinationWarehouseId());
        if (Objects.equals(request.sourceWarehouseId(), request.destinationWarehouseId())) {
            throw new BusinessRuleViolationException("SOURCE_DESTINATION_MUST_DIFFER");
        }
        ensureNeededByDateIsNotPast(request.neededByDate());
        ensurePhysicalWarehouses(request.sourceWarehouseId(), request.destinationWarehouseId());
        ensureNoOpenDuplicateRequest(request.sourceWarehouseId(), request.destinationWarehouseId(),
                request.neededByDate(), null);

        OffsetDateTime now = OffsetDateTime.now();
        TransferRequest req = new TransferRequest();
        req.setRequestNumber(generateRequestNumber());
        req.setSourceWarehouse(reference(Warehouse.class, request.sourceWarehouseId()));
        req.setDestinationWarehouse(reference(Warehouse.class, request.destinationWarehouseId()));
        req.setStatus(TransferRequestStatus.DRAFT);
        req.setNeededByDate(request.neededByDate());
        req.setBusinessReason(request.businessReason());
        req.setNotes(request.notes());
        req.setCreatedBy(actor);
        req.setCreatedAt(now);
        req.setUpdatedAt(now);

        TransferRequest saved = requestRepository.save(req);
        saveItems(saved, request.items());
        
        auditUtil.logChange(actor, AuditAction.TRANSFER_REQUEST_CREATE, "TRANSFER_REQUEST",
                saved.getId(), saved.getRequestNumber(), Map.of(), snapshot(saved));

        return toResponse(saved);
    }

    @Override
    @Transactional
    public TransferRequestResponse updateRequest(Long id, TransferRequestUpdateRequest request, User actor) {
        // HÀM CHÍNH: sửa yêu cầu khi còn ở trạng thái nháp.
        TransferRequest req = findRequest(id);
        if (req.getStatus() != TransferRequestStatus.DRAFT) {
            throw new BusinessRuleViolationException("ONLY_DRAFT_CAN_BE_UPDATED");
        }
        ensureRequesterRole(actor);
        ensureWarehouseScope(actor, req.getDestinationWarehouse().getId());
        ensureWarehouseScope(actor, request.destinationWarehouseId());
        
        if (Objects.equals(request.sourceWarehouseId(), request.destinationWarehouseId())) {
            throw new BusinessRuleViolationException("SOURCE_DESTINATION_MUST_DIFFER");
        }
        ensureNeededByDateIsNotPast(request.neededByDate());
        ensurePhysicalWarehouses(request.sourceWarehouseId(), request.destinationWarehouseId());
        ensureNoOpenDuplicateRequest(request.sourceWarehouseId(), request.destinationWarehouseId(),
                request.neededByDate(), id);

        Map<String, Object> before = snapshot(req);

        req.setSourceWarehouse(reference(Warehouse.class, request.sourceWarehouseId()));
        req.setDestinationWarehouse(reference(Warehouse.class, request.destinationWarehouseId()));
        req.setNeededByDate(request.neededByDate());
        req.setBusinessReason(request.businessReason());
        req.setNotes(request.notes());
        req.setUpdatedAt(OffsetDateTime.now());

        TransferRequest saved = requestRepository.save(req);
        saveItems(saved, request.items());

        auditUtil.logChange(actor, AuditAction.TRANSFER_REQUEST_UPDATE, "TRANSFER_REQUEST",
                saved.getId(), saved.getRequestNumber(), before, snapshot(saved));

        return toResponse(saved);
    }

    @Override
    @Transactional
    public TransferRequestResponse cancelRequest(Long id, User actor) {
        // HÀM CHÍNH: hủy yêu cầu trước khi thành phiếu điều chuyển chính thức.
        TransferRequest req = findRequest(id);
        if (req.getStatus() != TransferRequestStatus.DRAFT) {
            throw new BusinessRuleViolationException("ONLY_DRAFT_CAN_BE_CANCELLED");
        }
        ensureRequesterRole(actor);
        ensureWarehouseScope(actor, req.getDestinationWarehouse().getId());

        Map<String, Object> before = snapshot(req);
        req.setStatus(TransferRequestStatus.CANCELLED);
        req.setUpdatedAt(OffsetDateTime.now());

        TransferRequest saved = requestRepository.save(req);
        auditUtil.logChange(actor, AuditAction.CANCEL, "TRANSFER_REQUEST",
                saved.getId(), saved.getRequestNumber(), before, snapshot(saved));

        return toResponse(saved);
    }

    @Override
    @Transactional
    public TransferRequestResponse submitRequest(Long id, User actor) {
        // HÀM CHÍNH: gửi yêu cầu từ nháp sang chờ Quản lý kho nguồn/Admin duyệt.
        TransferRequest req = findRequest(id);
        if (autoCancelExpiredRequest(req, actor)) {
            return toResponse(req);
        }
        if (req.getStatus() != TransferRequestStatus.DRAFT) {
            throw new BusinessRuleViolationException("ONLY_DRAFT_CAN_BE_SUBMITTED");
        }
        ensureRequesterRole(actor);
        ensureWarehouseScope(actor, req.getDestinationWarehouse().getId());
        validateSourceAvailability(req);

        Map<String, Object> before = snapshot(req);
        req.setStatus(TransferRequestStatus.SUBMITTED);
        req.setSubmittedBy(actor);
        req.setSubmittedAt(OffsetDateTime.now());
        req.setUpdatedAt(OffsetDateTime.now());

        TransferRequest saved = requestRepository.save(req);

        auditUtil.logChange(actor, AuditAction.TRANSFER_REQUEST_SUBMIT, "TRANSFER_REQUEST",
                saved.getId(), saved.getRequestNumber(), before, snapshot(saved));

        return toResponse(saved);
    }

    @Override
    @Transactional
    public TransferRequestResponse approveRequest(Long id, User actor) {
        // HÀM CHÍNH: Quản lý kho nguồn/Admin duyệt yêu cầu và giữ hàng nguồn ngay.
        if (actor.getRole() != UserRole.WAREHOUSE_MANAGER && actor.getRole() != UserRole.ADMIN) {
            throw new BusinessRuleViolationException("SOURCE_MANAGER_ROLE_REQUIRED");
        }

        TransferRequest req = findRequest(id);
        if (autoCancelExpiredRequest(req, actor)) {
            return toResponse(req);
        }
        if (req.getStatus() != TransferRequestStatus.SUBMITTED) {
            throw new BusinessRuleViolationException("ONLY_SUBMITTED_CAN_BE_APPROVED");
        }
        ensureWarehouseScope(actor, req.getSourceWarehouse().getId());
        validateSourceAvailability(req);

        Map<String, Object> before = snapshot(req);
        InterWarehouseTransfer preparedTransfer = prepareReservedTransfer(req, actor);
        req.setStatus(TransferRequestStatus.APPROVED);
        req.setConvertedTransfer(preparedTransfer);
        req.setApprovedBy(actor);
        req.setApprovedAt(OffsetDateTime.now());
        req.setUpdatedAt(OffsetDateTime.now());

        TransferRequest saved = requestRepository.save(req);

        auditUtil.logChange(actor, AuditAction.TRANSFER_REQUEST_SOURCE_APPROVE, "TRANSFER_REQUEST",
                saved.getId(), saved.getRequestNumber(), before, snapshot(saved));

        return toResponse(saved);
    }

    @Override
    @Transactional
    public TransferRequestResponse rejectRequest(Long id, TransferRequestRejectRequest request, User actor) {
        // HÀM CHÍNH: Quản lý kho nguồn/Admin từ chối yêu cầu và lưu lý do.
        if (actor.getRole() != UserRole.WAREHOUSE_MANAGER && actor.getRole() != UserRole.ADMIN) {
            throw new BusinessRuleViolationException("SOURCE_MANAGER_ROLE_REQUIRED");
        }

        TransferRequest req = findRequest(id);
        if (req.getStatus() != TransferRequestStatus.SUBMITTED) {
            throw new BusinessRuleViolationException("ONLY_SUBMITTED_CAN_BE_REJECTED");
        }
        ensureWarehouseScope(actor, req.getSourceWarehouse().getId());

        if (request.reason() == null || request.reason().trim().isEmpty()) {
            throw new BusinessRuleViolationException("REJECTION_REASON_REQUIRED");
        }

        Map<String, Object> before = snapshot(req);
        req.setStatus(TransferRequestStatus.REJECTED);
        req.setRejectedBy(actor);
        req.setRejectedAt(OffsetDateTime.now());
        req.setRejectionReason(request.reason());
        req.setUpdatedAt(OffsetDateTime.now());

        TransferRequest saved = requestRepository.save(req);

        auditUtil.logChange(actor, AuditAction.TRANSFER_REQUEST_SOURCE_REJECT, "TRANSFER_REQUEST",
                saved.getId(), saved.getRequestNumber(), before, snapshot(saved));

        return toResponse(saved);
    }

    @Override
    @Transactional
    public TransferRequestResponse convertToTransfer(Long id, User actor) {
        // HÀM CHÍNH: Planner chuyển yêu cầu đã duyệt thành phiếu điều chuyển TRF.
        if (actor.getRole() != UserRole.PLANNER && actor.getRole() != UserRole.ADMIN) {
            throw new BusinessRuleViolationException("PLANNER_ROLE_REQUIRED");
        }

        TransferRequest req = findRequest(id);
        if (autoCancelExpiredRequest(req, actor)) {
            return toResponse(req);
        }
        if (req.getStatus() != TransferRequestStatus.APPROVED) {
            throw new BusinessRuleViolationException("ONLY_APPROVED_CAN_BE_CONVERTED");
        }
        Map<String, Object> before = snapshot(req);
        InterWarehouseTransfer transfer = req.getConvertedTransfer();
        if (transfer == null) {
            // Tương thích dữ liệu cũ: nếu request đã APPROVED trước luồng mới, convert vẫn phải tạo TRF đã giữ hàng.
            transfer = createTransferForRequest(req, actor);
            transfer.setTransferRequest(req);
            transfer.setUpdatedAt(OffsetDateTime.now());
            interWarehouseTransferRepository.save(transfer);
            InterWarehouseTransferResponse approved = transferService.approveTransfer(transfer.getId(), actor);
            transfer = interWarehouseTransferRepository.findById(approved.id())
                    .orElseThrow(() -> new ResourceNotFoundException("Transfer not found: " + approved.id()));
        }
        req.setStatus(TransferRequestStatus.CONVERTED);
        transfer.setTransferRequest(req);
        transfer.setUpdatedAt(OffsetDateTime.now());
        interWarehouseTransferRepository.save(transfer);
        req.setConvertedTransfer(transfer);
        req.setConvertedBy(actor);
        req.setConvertedAt(OffsetDateTime.now());
        req.setUpdatedAt(OffsetDateTime.now());
        TransferRequest saved = requestRepository.save(req);

        auditUtil.logChange(actor, AuditAction.TRANSFER_REQUEST_CONVERT, "TRANSFER_REQUEST",
                saved.getId(), saved.getRequestNumber(), before, snapshot(saved));

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseStockLookupResponse> stockLookup(Long productId, User actor) {
        // HÀM CHÍNH: tra cứu tồn khả dụng ở các kho vật lý để chọn kho nguồn.
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        List<Warehouse> warehouses = warehouseRepository.findAll().stream()
                .filter(w -> w.getType() != WarehouseType.IN_TRANSIT)
                .toList();

        List<WarehouseStockLookupResponse> lookup = new ArrayList<>();
        for (Warehouse w : warehouses) {
            BigDecimal available = inventoryRepository.sumValidAvailableQty(w.getId(), product.getId());
            lookup.add(new WarehouseStockLookupResponse(w.getId(), w.getName(), available != null ? available : BigDecimal.ZERO));
        }
        return lookup;
    }

    // --- HÀM HỖ TRỢ: tìm dữ liệu, kiểm quyền, validate, tự hủy quá hạn, sinh mã và map response ---

    private TransferRequest findRequest(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer request not found: " + id));
    }

    private List<Long> loadWarehouseIds(User actor) {
        return userWarehouseAssignmentRepository.findWarehouseIdsByUserId(actor.getId());
    }

    private boolean canViewRequest(User actor, List<Long> assignedWarehouseIds, TransferRequest req) {
        if (actor.getRole() == UserRole.ADMIN || actor.getRole() == UserRole.CEO || actor.getRole() == UserRole.PLANNER) {
            return true;
        }
        // Quản lý kho được xem yêu cầu nếu kho mình phụ trách là kho nguồn hoặc kho đích.
        return assignedWarehouseIds.contains(req.getSourceWarehouse().getId())
                || assignedWarehouseIds.contains(req.getDestinationWarehouse().getId());
    }

    private void ensureWarehouseScope(User actor, Long warehouseId) {
        if (actor.getRole() == UserRole.ADMIN || actor.getRole() == UserRole.CEO || actor.getRole() == UserRole.PLANNER) {
            return;
        }
        List<Long> assigned = loadWarehouseIds(actor);
        if (!assigned.contains(warehouseId)) {
            throw new BusinessRuleViolationException("WAREHOUSE_SCOPE_REQUIRED");
        }
    }

    private void ensureRequesterRole(User actor) {
        if (actor.getRole() != UserRole.WAREHOUSE_MANAGER) {
            throw new BusinessRuleViolationException("WAREHOUSE_MANAGER_ROLE_REQUIRED");
        }
    }

    private void validateSourceAvailability(TransferRequest req) {
        Map<Long, BigDecimal> requestedByProduct = new HashMap<>();
        for (TransferRequestItem item : req.getItems()) {
            Long productId = item.getProduct().getId();
            requestedByProduct.merge(productId, item.getRequestedQty(), BigDecimal::add);
        }
        for (Map.Entry<Long, BigDecimal> entry : requestedByProduct.entrySet()) {
            BigDecimal available = inventoryRepository.sumValidAvailableQty(req.getSourceWarehouse().getId(), entry.getKey());
            BigDecimal safeAvailable = available == null ? BigDecimal.ZERO : available;
            if (safeAvailable.compareTo(entry.getValue()) < 0) {
                throw new BusinessRuleViolationException("TRANSFER_REQUEST_QTY_EXCEEDS_SOURCE_AVAILABLE");
            }
        }
    }

    private InterWarehouseTransfer prepareReservedTransfer(TransferRequest req, User actor) {
        InterWarehouseTransfer existing = req.getConvertedTransfer();
        if (existing != null) {
            return existing;
        }
        if (interWarehouseTransferRepository.existsByTransferRequestIdAndStatusNotIn(req.getId(),
                List.of(InterWarehouseTransferStatus.CANCELLED, InterWarehouseTransferStatus.REJECTED))) {
            throw new BusinessRuleViolationException("TRANSFER_REQUEST_ALREADY_CONVERTED");
        }
        InterWarehouseTransfer transfer = createTransferForRequest(req, actor);
        transfer.setTransferRequest(req);
        transfer.setUpdatedAt(OffsetDateTime.now());
        interWarehouseTransferRepository.save(transfer);
        InterWarehouseTransferResponse approved = transferService.approveTransfer(transfer.getId(), actor);
        return interWarehouseTransferRepository.findById(approved.id())
                .orElseThrow(() -> new ResourceNotFoundException("Transfer not found: " + approved.id()));
    }

    private InterWarehouseTransfer createTransferForRequest(TransferRequest req, User actor) {
        List<InterWarehouseTransferItemRequest> itemRequests = req.getItems().stream()
                .map(item -> new InterWarehouseTransferItemRequest(
                        item.getProduct().getId(),
                        null,
                        null,
                        item.getRequestedQty()
                ))
                .toList();

        LocalDate plannedDate = req.getNeededByDate() != null ? req.getNeededByDate() : LocalDate.now().plusDays(2);
        InterWarehouseTransferCreateRequest createRequest = new InterWarehouseTransferCreateRequest(
                req.getRequestNumber(),
                req.getSourceWarehouse().getId(),
                req.getDestinationWarehouse().getId(),
                LocalDate.now(),
                plannedDate,
                req.getNotes(),
                itemRequests
        );

        InterWarehouseTransferResponse transferResponse = transferService.createTransferFromApprovedRequest(createRequest,
                actor);
        return interWarehouseTransferRepository.findById(transferResponse.id())
                .orElseThrow(() -> new ResourceNotFoundException("Transfer not found: " + transferResponse.id()));
    }

    private void ensureNeededByDateIsNotPast(LocalDate neededByDate) {
        if (neededByDate != null && neededByDate.isBefore(LocalDate.now())) {
            throw new BusinessRuleViolationException("NEEDED_BY_DATE_MUST_NOT_BE_PAST");
        }
    }

    private boolean autoCancelExpiredRequest(TransferRequest req, User actor) {
        // Nếu quá ngày cần hàng mà yêu cầu chưa thành phiếu điều chuyển, hệ thống hủy luôn để Planner không thể chuyển đơn trễ.
        if (req.getNeededByDate() != null
                && req.getNeededByDate().isBefore(LocalDate.now())
                && req.getStatus() != TransferRequestStatus.CANCELLED
                && req.getStatus() != TransferRequestStatus.CONVERTED
                && req.getStatus() != TransferRequestStatus.REJECTED) {
            Map<String, Object> before = snapshot(req);
            req.setStatus(TransferRequestStatus.CANCELLED);
            req.setUpdatedAt(OffsetDateTime.now());
            TransferRequest saved = requestRepository.save(req);
            auditUtil.logChange(actor, AuditAction.CANCEL, "TRANSFER_REQUEST",
                    saved.getId(), saved.getRequestNumber(), before, snapshot(saved));
            return true;
        }
        return false;
    }

    private void ensureNoOpenDuplicateRequest(Long sourceWarehouseId,
                                              Long destinationWarehouseId,
                                              LocalDate neededByDate,
                                              Long currentRequestId) {
        // HÀM HỖ TRỢ: không cho tạo hai yêu cầu đang mở cho cùng tuyến kho và cùng ngày cần hàng.
        boolean exists = currentRequestId == null
                ? requestRepository.existsBySourceWarehouseIdAndDestinationWarehouseIdAndNeededByDateAndStatusIn(
                        sourceWarehouseId, destinationWarehouseId, neededByDate, OPEN_DUPLICATE_STATUSES)
                : requestRepository.existsBySourceWarehouseIdAndDestinationWarehouseIdAndNeededByDateAndStatusInAndIdNot(
                        sourceWarehouseId, destinationWarehouseId, neededByDate, OPEN_DUPLICATE_STATUSES,
                        currentRequestId);
        if (exists) {
            throw new BusinessRuleViolationException("DUPLICATE_OPEN_TRANSFER_REQUEST");
        }
    }

    private void ensurePhysicalWarehouses(Long sourceWarehouseId, Long destinationWarehouseId) {
        Warehouse source = reference(Warehouse.class, sourceWarehouseId);
        Warehouse destination = reference(Warehouse.class, destinationWarehouseId);
        if (source.getType() == WarehouseType.IN_TRANSIT) {
            throw new BusinessRuleViolationException("SOURCE_WAREHOUSE_MUST_BE_PHYSICAL");
        }
        if (destination.getType() == WarehouseType.IN_TRANSIT) {
            throw new BusinessRuleViolationException("DESTINATION_WAREHOUSE_MUST_BE_PHYSICAL");
        }
    }

    private void saveItems(TransferRequest req, List<TransferRequestItemRequest> items) {
        List<TransferRequestItem> currentItems = req.getItems();
        if (currentItems == null) {
            currentItems = new ArrayList<>();
            req.setItems(currentItems);
        } else {
            try {
                currentItems.clear();
            } catch (UnsupportedOperationException e) {
                currentItems = new ArrayList<>();
                req.setItems(currentItems);
            }
        }
        Set<Long> productIds = new HashSet<>();
        for (TransferRequestItemRequest line : items) {
            if (!productIds.add(line.productId())) {
                throw new BusinessRuleViolationException("DUPLICATE_PRODUCT_IN_TRANSFER");
            }
            if (line.requestedQty().stripTrailingZeros().scale() > 0) {
                throw new BusinessRuleViolationException("TRANSFER_QTY_MUST_BE_WHOLE_NUMBER");
            }
            Product p = productRepository.findById(line.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + line.productId()));
            TransferRequestItem item = TransferRequestItem.builder()
                    .transferRequest(req)
                    .product(p)
                    .requestedQty(line.requestedQty())
                    .build();
            currentItems.add(item);
        }
    }

    private String generateRequestNumber() {
        String todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "TRQ-" + todayStr + "-" + random;
    }

    private <T> T reference(Class<T> clazz, Long id) {
        // Lấy trực tiếp entity cần tham chiếu để báo lỗi rõ ràng nếu id không tồn tại.
        if (clazz == Warehouse.class) {
            return clazz.cast(warehouseRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found: " + id)));
        }
        if (clazz == Product.class) {
            return clazz.cast(productRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id)));
        }
        return null;
    }

    private TransferRequestResponse toResponse(TransferRequest req) {
        List<TransferRequestItemResponse> itemResponses = req.getItems().stream()
                .map(item -> new TransferRequestItemResponse(
                        item.getId(),
                        item.getProduct().getId(),
                        item.getProduct().getSku(),
                        item.getProduct().getName(),
                        item.getProduct().getUnit(),
                        item.getRequestedQty()
                ))
                .toList();

        return new TransferRequestResponse(
                req.getId(),
                req.getRequestNumber(),
                req.getSourceWarehouse().getId(),
                req.getSourceWarehouse().getName(),
                req.getDestinationWarehouse().getId(),
                req.getDestinationWarehouse().getName(),
                req.getStatus(),
                req.getCreatedBy().getId(),
                req.getCreatedBy().getFullName(),
                req.getSubmittedBy() != null ? req.getSubmittedBy().getId() : null,
                req.getSubmittedBy() != null ? req.getSubmittedBy().getFullName() : null,
                req.getSubmittedAt(),
                req.getApprovedBy() != null ? req.getApprovedBy().getId() : null,
                req.getApprovedBy() != null ? req.getApprovedBy().getFullName() : null,
                req.getApprovedAt(),
                req.getRejectedBy() != null ? req.getRejectedBy().getId() : null,
                req.getRejectedBy() != null ? req.getRejectedBy().getFullName() : null,
                req.getRejectedAt(),
                req.getRejectionReason(),
                req.getNeededByDate(),
                req.getBusinessReason(),
                req.getNotes(),
                req.getConvertedTransfer() != null ? req.getConvertedTransfer().getId() : null,
                req.getConvertedTransfer() != null ? req.getConvertedTransfer().getTransferNumber() : null,
                req.getConvertedBy() != null ? req.getConvertedBy().getId() : null,
                req.getConvertedBy() != null ? req.getConvertedBy().getFullName() : null,
                req.getConvertedAt(),
                req.getCreatedAt(),
                req.getUpdatedAt(),
                itemResponses
        );
    }

    private Map<String, Object> snapshot(TransferRequest req) {
        Map<String, Object> snap = new HashMap<>();
        snap.put("status", req.getStatus());
        snap.put("notes", req.getNotes());
        snap.put("neededByDate", req.getNeededByDate());
        snap.put("businessReason", req.getBusinessReason());
        snap.put("convertedTransferId", req.getConvertedTransfer() != null ? req.getConvertedTransfer().getId() : null);
        snap.put("sourceWarehouseId", req.getSourceWarehouse().getId());
        snap.put("destinationWarehouseId", req.getDestinationWarehouse().getId());
        return snap;
    }
}
