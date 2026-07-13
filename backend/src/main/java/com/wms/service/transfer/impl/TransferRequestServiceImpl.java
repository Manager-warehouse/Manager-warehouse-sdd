package com.wms.service.transfer.impl;

import com.wms.dto.request.*;
import com.wms.dto.response.*;
import com.wms.entity.*;
import com.wms.enums.*;
import com.wms.exception.BusinessRuleViolationException;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.*;
import com.wms.service.transfer.InterWarehouseTransferService;
import com.wms.service.transfer.TransferRequestService;
import com.wms.util.PartnerAuditUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransferRequestServiceImpl implements TransferRequestService {

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
        List<Long> assignedWarehouseIds = loadWarehouseIds(actor);
        return requestRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(req -> canViewRequest(actor, assignedWarehouseIds, req))
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TransferRequestResponse getRequestById(Long id, User actor) {
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
        ensureRequesterRole(actor);
        ensureWarehouseScope(actor, request.destinationWarehouseId());
        if (Objects.equals(request.sourceWarehouseId(), request.destinationWarehouseId())) {
            throw new BusinessRuleViolationException("SOURCE_DESTINATION_MUST_DIFFER");
        }

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
    public TransferRequestResponse submitRequest(Long id, User actor) {
        TransferRequest req = findRequest(id);
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
        if (actor.getRole() != UserRole.CEO && actor.getRole() != UserRole.ADMIN) {
            throw new BusinessRuleViolationException("CEO_ROLE_REQUIRED");
        }

        TransferRequest req = findRequest(id);
        if (req.getStatus() != TransferRequestStatus.SUBMITTED) {
            throw new BusinessRuleViolationException("ONLY_SUBMITTED_CAN_BE_APPROVED");
        }
        validateSourceAvailability(req);

        Map<String, Object> before = snapshot(req);
        req.setStatus(TransferRequestStatus.APPROVED);
        req.setApprovedBy(actor);
        req.setApprovedAt(OffsetDateTime.now());
        req.setUpdatedAt(OffsetDateTime.now());

        TransferRequest saved = requestRepository.save(req);

        auditUtil.logChange(actor, AuditAction.TRANSFER_REQUEST_CEO_APPROVE, "TRANSFER_REQUEST",
                saved.getId(), saved.getRequestNumber(), before, snapshot(saved));

        return toResponse(saved);
    }

    @Override
    @Transactional
    public TransferRequestResponse rejectRequest(Long id, TransferRequestRejectRequest request, User actor) {
        if (actor.getRole() != UserRole.CEO && actor.getRole() != UserRole.ADMIN) {
            throw new BusinessRuleViolationException("CEO_ROLE_REQUIRED");
        }

        TransferRequest req = findRequest(id);
        if (req.getStatus() != TransferRequestStatus.SUBMITTED) {
            throw new BusinessRuleViolationException("ONLY_SUBMITTED_CAN_BE_REJECTED");
        }

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

        auditUtil.logChange(actor, AuditAction.TRANSFER_REQUEST_CEO_REJECT, "TRANSFER_REQUEST",
                saved.getId(), saved.getRequestNumber(), before, snapshot(saved));

        return toResponse(saved);
    }

    @Override
    @Transactional
    public TransferRequestResponse convertToTransfer(Long id, User actor) {
        if (actor.getRole() != UserRole.PLANNER && actor.getRole() != UserRole.ADMIN) {
            throw new BusinessRuleViolationException("PLANNER_ROLE_REQUIRED");
        }

        TransferRequest req = findRequest(id);
        if (req.getStatus() != TransferRequestStatus.APPROVED) {
            throw new BusinessRuleViolationException("ONLY_APPROVED_CAN_BE_CONVERTED");
        }
        if (req.getConvertedTransfer() != null) {
            throw new BusinessRuleViolationException("TRANSFER_REQUEST_ALREADY_CONVERTED");
        }
        // Unique guard for active transfer conversion (T028)
        if (interWarehouseTransferRepository.existsByTransferRequestIdAndStatusNotIn(req.getId(),
                List.of(InterWarehouseTransferStatus.CANCELLED, InterWarehouseTransferStatus.REJECTED))) {
            throw new BusinessRuleViolationException("TRANSFER_REQUEST_ALREADY_CONVERTED");
        }

        // Convert to InterWarehouseTransfer using transferService
        List<InterWarehouseTransferItemRequest> itemRequests = req.getItems().stream()
                .map(item -> new InterWarehouseTransferItemRequest(
                        item.getProduct().getId(),
                        null, // source location selected later by shipper
                        null, // destination location
                        item.getRequestedQty()
                ))
                .toList();

        // T027: plannedDate is taken from neededByDate instead of now()+2
        LocalDate plannedDate = req.getNeededByDate() != null ? req.getNeededByDate() : LocalDate.now().plusDays(2);

        InterWarehouseTransferCreateRequest createRequest = new InterWarehouseTransferCreateRequest(
                req.getRequestNumber(), // Use request number as external instruction code
                req.getSourceWarehouse().getId(),
                req.getDestinationWarehouse().getId(),
                LocalDate.now(),
                plannedDate,
                req.getNotes(),
                itemRequests
        );

        // Call the executable transfer creation service
        InterWarehouseTransferResponse transferResponse = transferService.createTransfer(createRequest, actor);

        Map<String, Object> before = snapshot(req);
        req.setStatus(TransferRequestStatus.CONVERTED);
        InterWarehouseTransfer transfer = interWarehouseTransferRepository.findById(transferResponse.id())
                .orElseThrow(() -> new ResourceNotFoundException("Transfer not found: " + transferResponse.id()));
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

    // --- Helpers ---

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
        // Manager can view requests where their assigned warehouse is source or destination
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
        if (actor.getRole() != UserRole.WAREHOUSE_MANAGER && actor.getRole() != UserRole.ADMIN) {
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
        for (TransferRequestItemRequest line : items) {
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
        // Simple direct entity fetch
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
