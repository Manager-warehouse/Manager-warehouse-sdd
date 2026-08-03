package com.wms.service.stock_receiving;

import com.wms.dto.request.CreateReceiptItemRequest;
import com.wms.dto.request.CreateReceiptRequest;
import com.wms.dto.request.PreReceiveApprovalRequest;
import com.wms.dto.request.ReceiptCancelRequest;
import com.wms.dto.request.ReceiptReopenRequest;
import com.wms.dto.request.ReceiveQcReceiptItemRequest;
import com.wms.dto.request.ReceiveQcReceiptRequest;
import com.wms.dto.request.ReceiveReceiptItemRequest;
import com.wms.dto.request.ReceiveReceiptRequest;
import com.wms.dto.request.ReviseReceiptItemRequest;
import com.wms.dto.request.ReviseReceiptRequest;
import com.wms.dto.request.StorekeeperReviewRequest;
import com.wms.dto.response.ReceiptResponse;
import com.wms.entity.access_control.User;
import com.wms.entity.document_numbering.DocumentSequence;
import com.wms.entity.price_management.PriceHistory;
import com.wms.entity.product_catalog.Product;
import com.wms.entity.stock_receiving.Receipt;
import com.wms.entity.stock_receiving.ReceiptItem;
import com.wms.entity.supplier_management.Supplier;
import com.wms.entity.warehouse_location.Warehouse;
import com.wms.enums.access_control.UserRole;
import com.wms.enums.audit_trail.AuditAction;
import com.wms.enums.stock_receiving.QcResult;
import com.wms.enums.stock_receiving.ReceiptStatus;
import com.wms.enums.stock_receiving.ReceiptType;
import com.wms.enums.stock_receiving.StorekeeperReviewDecision;
import com.wms.exception.DuplicateResourceException;
import com.wms.exception.ReceiptCountException;
import com.wms.exception.ResourceNotFoundException;
import com.wms.exception.UnprocessableEntityException;
import com.wms.mapper.ReceiptMapper;
import com.wms.repository.CreditNoteRepository;
import com.wms.repository.DocumentSequenceRepository;
import com.wms.repository.UserWarehouseAssignmentRepository;
import com.wms.repository.WarehouseRepository;
import com.wms.repository.product_catalog.ProductRepository;
import com.wms.repository.stock_receiving.ReceiptItemRepository;
import com.wms.repository.stock_receiving.ReceiptRepository;
import com.wms.repository.supplier_management.SupplierRepository;
import com.wms.service.audit_trail.AuditLogService;
import com.wms.service.billing_payment.AccountingPeriodService;
import com.wms.service.price_management.PriceHistoryService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReceiptService {

    private static final DateTimeFormatter RECEIPT_NUMBER_DATE =
            DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String RECEIPT_SEQUENCE_KEY_PREFIX = "RECEIPT";

    private final DocumentSequenceRepository sequenceRepository;
    private final ReceiptRepository receiptRepository;
    private final ReceiptItemRepository receiptItemRepository;
    private final SupplierRepository supplierRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final UserWarehouseAssignmentRepository assignmentRepository;
    private final AuditLogService auditLogService;
    private final ReceiptMapper receiptMapper;
    private final AccountingPeriodService accountingPeriodService;
    private final CreditNoteRepository creditNoteRepository;
    private final PriceHistoryService priceHistoryService;

    public ReceiptService(DocumentSequenceRepository sequenceRepository,
                          ReceiptRepository receiptRepository,
                          ReceiptItemRepository receiptItemRepository,
                          SupplierRepository supplierRepository,
                          WarehouseRepository warehouseRepository,
                          ProductRepository productRepository,
                          UserWarehouseAssignmentRepository assignmentRepository,
                          AuditLogService auditLogService,
                          ReceiptMapper receiptMapper,
                          AccountingPeriodService accountingPeriodService,
                          CreditNoteRepository creditNoteRepository,
                          PriceHistoryService priceHistoryService) {
        this.sequenceRepository = sequenceRepository;
        this.receiptRepository = receiptRepository;
        this.receiptItemRepository = receiptItemRepository;
        this.supplierRepository = supplierRepository;
        this.warehouseRepository = warehouseRepository;
        this.productRepository = productRepository;
        this.assignmentRepository = assignmentRepository;
        this.auditLogService = auditLogService;
        this.receiptMapper = receiptMapper;
        this.accountingPeriodService = accountingPeriodService;
        this.creditNoteRepository = creditNoteRepository;
        this.priceHistoryService = priceHistoryService;
    }

    @Transactional(readOnly = true)
    public List<ReceiptResponse> getReceiptsByWarehouse(Long warehouseId, User actor) {
        return getReceiptsByWarehouseAndType(warehouseId, null, actor);
    }

    @Transactional(readOnly = true)
    public List<ReceiptResponse> getReceiptsByWarehouseAndType(Long warehouseId, ReceiptType type, User actor) {
        requireWarehouseAccess(actor, warehouseId);
        List<Receipt> receipts = type != null 
                ? receiptRepository.findByWarehouseIdAndTypeOrderByDocumentDateDescCreatedAtDesc(warehouseId, type)
                : receiptRepository.findByWarehouseIdOrderByDocumentDateDescCreatedAtDesc(warehouseId);
        return receipts.stream()
                .map(r -> enrichReceiptResponse(r, receiptMapper.toResponse(r,
                        receiptItemRepository.findByReceiptIdOrderByIdAsc(r.getId()))))
                .toList();
    }

    @Transactional(readOnly = true)
    public ReceiptResponse getReceiptById(Long id, User actor) {
        Receipt receipt = receiptRepository.findByIdWithSupplierAndWarehouse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found with id: " + id));
        requireWarehouseAccess(actor, receipt.getWarehouse().getId());
        List<ReceiptItem> items = receiptItemRepository.findByReceiptIdOrderByIdAsc(id);
        return enrichReceiptResponse(receipt, receiptMapper.toResponse(receipt, items));
    }

    private ReceiptResponse enrichReceiptResponse(Receipt receipt, ReceiptResponse response) {
        if (receipt.getType() == ReceiptType.RETURN) {
            // creditNoteId lets the UI target a specific Credit Note for a Correction
            // Voucher (US-WMS-29) without a second lookup call.
            creditNoteRepository.findByReceiptId(receipt.getId()).ifPresentOrElse(
                    creditNote -> {
                        response.setCreditNoteGenerated(true);
                        response.setCreditNoteId(creditNote.getId());
                    },
                    () -> response.setCreditNoteGenerated(false));
        }
        return response;
    }

    @Transactional
    public ReceiptResponse createPurchaseReceipt(CreateReceiptRequest request, User actor) {
        requirePlanner(actor);
        validateRequest(request);
        Supplier supplier = findActiveSupplier(request.getSupplierId());
        Warehouse warehouse = findActiveWarehouse(request.getWarehouseId());
        requireWarehouseAccess(actor, warehouse.getId());
        validateItems(request.getItems());

        Receipt receipt = buildReceipt(request, actor, supplier, warehouse);
        Receipt savedReceipt = saveReceipt(receipt);
        List<ReceiptItem> items = buildItems(request.getItems(), savedReceipt, request.getDocumentDate());
        List<ReceiptItem> savedItems = receiptItemRepository.saveAll(items);

        auditLogService.log(actor, AuditAction.RECEIPT_CREATE, "RECEIPT",
                savedReceipt.getId(), savedReceipt.getReceiptNumber(),
                warehouse.getId(), null, snapshot(savedReceipt, savedItems));
        return receiptMapper.toResponse(savedReceipt, savedItems);
    }

    @Transactional
    public ReceiptResponse decidePreReceiveApproval(Long receiptId,
                                                    PreReceiveApprovalRequest request,
                                                    User actor) {
        requireWarehouseManager(actor);
        Receipt receipt = receiptRepository.findByIdWithWarehouse(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found with id: " + receiptId));
        requireWarehouseAccess(actor, receipt.getWarehouse().getId());
        assertVersion(receipt, request.getExpectedVersion());
        List<ReceiptItem> items = receiptItemRepository.findByReceiptIdOrderByIdAsc(receiptId);
        boolean legacyPendingWithoutCount = receipt.getStatus() == ReceiptStatus.PENDING_RECEIPT
                && receipt.getType() == ReceiptType.PURCHASE
                && receipt.getPreReceiveApprovedAt() == null
                && items.stream().allMatch(item -> item.getActualQty() == null);
        if (receipt.getStatus() != ReceiptStatus.PENDING_MANAGER_APPROVAL
                && !legacyPendingWithoutCount) {
            throw new UnprocessableEntityException("PRE_RECEIVE_APPROVAL_INVALID_STATUS: " + receipt.getStatus());
        }

        Map<String, Object> before = snapshot(receipt, items);
        OffsetDateTime now = OffsetDateTime.now();
        if ("APPROVE".equals(request.getDecision())) {
            receipt.setStatus(ReceiptStatus.PENDING_RECEIPT);
            receipt.setPreReceiveApprovedBy(actor);
            receipt.setPreReceiveApprovedAt(now);
            receipt.setPreReceiveRejectionReason(null);
            receipt.setUpdatedAt(now);
            Receipt saved = receiptRepository.save(receipt);
            auditLogService.log(actor, AuditAction.RECEIPT_PRE_RECEIVE_APPROVE, "RECEIPT",
                    saved.getId(), saved.getReceiptNumber(), saved.getWarehouse().getId(), before,
                    preReceiveSnapshot(saved, items, request.getDecision(), null));
            return receiptMapper.toResponse(saved, items);
        }

        if (!"REJECT".equals(request.getDecision())) {
            throw new UnprocessableEntityException("PRE_RECEIVE_DECISION_INVALID");
        }
        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new UnprocessableEntityException("PRE_RECEIVE_REJECTION_REASON_REQUIRED");
        }
        receipt.setStatus(ReceiptStatus.REVISION_REQUIRED);
        receipt.setPreReceiveApprovedBy(null);
        receipt.setPreReceiveApprovedAt(null);
        receipt.setPreReceiveRejectionReason(request.getReason());
        receipt.setUpdatedAt(now);
        Receipt saved = receiptRepository.save(receipt);
        auditLogService.log(actor, AuditAction.RECEIPT_PRE_RECEIVE_REJECT, "RECEIPT",
                saved.getId(), saved.getReceiptNumber(), saved.getWarehouse().getId(), before,
                preReceiveSnapshot(saved, items, request.getDecision(), request.getReason()));
        return receiptMapper.toResponse(saved, items);
    }

    @Transactional
    public ReceiptResponse reviseReceipt(Long receiptId, ReviseReceiptRequest request, User actor) {
        requirePlannerOnly(actor);
        validateRevisionRequest(request);
        Receipt receipt = receiptRepository.findByIdWithWarehouse(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found with id: " + receiptId));
        requireWarehouseAccess(actor, receipt.getWarehouse().getId());
        assertVersion(receipt, request.getExpectedVersion());
        if (receipt.getStatus() != ReceiptStatus.REVISION_REQUIRED) {
            throw new UnprocessableEntityException("RECEIPT_REVISION_INVALID_STATUS: " + receipt.getStatus());
        }

        List<ReceiptItem> items = receiptItemRepository.findByReceiptIdOrderByIdAsc(receiptId);
        Map<String, Object> before = snapshot(receipt, items);
        applyRevisionItems(request.getItems(), items, request.getDocumentDate());
        receipt.setDocumentDate(request.getDocumentDate());
        receipt.setAccountingPeriod(accountingPeriodService.resolveOpenPeriod(request.getDocumentDate()));
        receipt.setNotes(request.getNotes());
        receipt.setStatus(ReceiptStatus.PENDING_MANAGER_APPROVAL);
        receipt.setPreReceiveRejectionReason(null);
        receipt.setUpdatedAt(OffsetDateTime.now());

        List<ReceiptItem> savedItems = receiptItemRepository.saveAll(items);
        Receipt saved = receiptRepository.save(receipt);
        auditLogService.log(actor, AuditAction.RECEIPT_PRE_RECEIVE_RESUBMIT, "RECEIPT",
                saved.getId(), saved.getReceiptNumber(), saved.getWarehouse().getId(), before,
                snapshot(saved, savedItems));
        return receiptMapper.toResponse(saved, savedItems);
    }

    @Transactional
    public ReceiptResponse receiveReceiptCounts(Long receiptId,
                                                ReceiveReceiptRequest request,
                                                User actor) {
        requireWarehouseStaff(actor);
        validateReceiveRequest(request);
        Receipt receipt = receiptRepository.findByIdWithWarehouse(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Receipt not found with id: " + receiptId));
        requireWarehouseAccess(actor, receipt.getWarehouse().getId());
        validateReceivableStatus(receipt);

        if (request.getExpectedVersion() == null) {
            throw receiptCountError("EXPECTED_VERSION_REQUIRED", HttpStatus.BAD_REQUEST,
                    "expectedVersion is required");
        }
        if (receipt.getVersion() != null && !request.getExpectedVersion().equals(receipt.getVersion())) {
            throw receiptCountError("INVENTORY_VERSION_CONFLICT",
                    HttpStatus.CONFLICT,
                    "Receipt version mismatch (expected: " + request.getExpectedVersion() + ", actual: " + receipt.getVersion() + ")");
        }

        List<ReceiptItem> items = receiptItemRepository.findByReceiptIdOrderByIdAsc(receiptId);
        Map<String, Object> before = receiveSnapshot(receipt, items);
        Map<Long, ReceiveReceiptItemRequest> counts = validateCountCoverage(request, items);

        for (ReceiptItem item : items) {
            applyCount(item, counts.get(item.getId()).getCountedQty());
        }
        boolean hadQc = hasQcData(items);
        if (hadQc) {
            clearQcData(items);
        }

        receipt.setStatus(ReceiptStatus.DRAFT);
        receipt.setRejectionReason(null);
        receipt.setUpdatedAt(OffsetDateTime.now());

        List<ReceiptItem> savedItems = receiptItemRepository.saveAll(items);
        Receipt savedReceipt = receiptRepository.save(receipt);
        AuditAction auditAction = hadQc ? AuditAction.RECEIPT_CORRECTION : AuditAction.RECEIPT_RECEIVE;
        auditLogService.log(actor, auditAction, "RECEIPT",
                savedReceipt.getId(), savedReceipt.getReceiptNumber(),
                savedReceipt.getWarehouse().getId(), before,
                receiveSnapshot(savedReceipt, savedItems));
        return receiptMapper.toResponse(savedReceipt, savedItems);
    }

    @Transactional
    public ReceiptResponse receiveAndQcReceipt(Long receiptId,
                                               ReceiveQcReceiptRequest request,
                                               User actor) {
        requireReceiveQcRole(actor);
        validateReceiveQcRequest(request);
        Receipt receipt = receiptRepository.findByIdWithWarehouse(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Receipt not found with id: " + receiptId));
        requireWarehouseAccess(actor, receipt.getWarehouse().getId());
        validateReceiveQcStatus(receipt);
        assertVersion(receipt, request.getExpectedVersion());

        List<ReceiptItem> items = receiptItemRepository.findByReceiptIdOrderByIdAsc(receiptId);
        ensureReceiveQcEditable(receipt, items);
        Map<String, Object> before = receiveSnapshot(receipt, items);
        Map<Long, ReceiveQcReceiptItemRequest> counts = validateReceiveQcCoverage(request, items);

        for (ReceiptItem item : items) {
            ReceiveQcReceiptItemRequest count = counts.get(item.getId());
            clearReceiptItemQcData(item);
            applyReceiveQc(item, count, actor);
        }

        receipt.setStatus(ReceiptStatus.PENDING_STOREKEEPER_REVIEW);
        receipt.setRejectionReason(null);
        receipt.setRecountReason(null);
        receipt.setStorekeeperReviewedBy(null);
        receipt.setStorekeeperReviewedAt(null);
        receipt.setUpdatedAt(OffsetDateTime.now());

        List<ReceiptItem> savedItems = receiptItemRepository.saveAll(items);
        Receipt savedReceipt = receiptRepository.save(receipt);
        auditLogService.log(actor, AuditAction.RECEIPT_RECEIVE_QC, "RECEIPT",
                savedReceipt.getId(), savedReceipt.getReceiptNumber(),
                savedReceipt.getWarehouse().getId(), before,
                receiveSnapshot(savedReceipt, savedItems));
        return receiptMapper.toResponse(savedReceipt, savedItems);
    }

    @Transactional
    public ReceiptResponse reviewStorekeeperCountQc(Long receiptId,
                                                    StorekeeperReviewRequest request,
                                                    User actor) {
        requireStorekeeperReviewRole(actor);
        validateStorekeeperReviewRequest(request);
        Receipt receipt = receiptRepository.findByIdWithWarehouse(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Receipt not found with id: " + receiptId));
        requireWarehouseAccess(actor, receipt.getWarehouse().getId());
        validateStorekeeperReviewStatus(receipt);
        assertVersion(receipt, request.getExpectedVersion());

        List<ReceiptItem> items = receiptItemRepository.findByReceiptIdOrderByIdAsc(receiptId);
        Map<String, Object> before = receiveSnapshot(receipt, items);
        OffsetDateTime now = OffsetDateTime.now();
        receipt.setStorekeeperReviewedBy(actor);
        receipt.setStorekeeperReviewedAt(now);
        receipt.setUpdatedAt(now);

        if (request.getDecision() == StorekeeperReviewDecision.REQUEST_RECOUNT) {
            receipt.setStatus(ReceiptStatus.RECOUNT_REQUIRED);
            receipt.setRecountReason(request.getReason().trim());
            Receipt savedReceipt = receiptRepository.save(receipt);
            auditLogService.log(actor, AuditAction.RECEIPT_STOREKEEPER_RECOUNT_REQUEST, "RECEIPT",
                    savedReceipt.getId(), savedReceipt.getReceiptNumber(),
                    savedReceipt.getWarehouse().getId(), before,
                    storekeeperReviewSnapshot(savedReceipt, items, request.getDecision()));
            return receiptMapper.toResponse(savedReceipt, items);
        }

        boolean hasFailed = false;
        for (ReceiptItem item : items) {
            int failedQty = item.getQualityFailedQty() == null ? 0 : item.getQualityFailedQty();
            item.setQuarantineReadyQty(failedQty);
            hasFailed = hasFailed || failedQty > 0;
        }
        receipt.setStatus(hasFailed ? ReceiptStatus.QC_FAILED : ReceiptStatus.QC_COMPLETED);
        receipt.setRecountReason(null);

        List<ReceiptItem> savedItems = receiptItemRepository.saveAll(items);
        Receipt savedReceipt = receiptRepository.save(receipt);
        auditLogService.log(actor, AuditAction.RECEIPT_STOREKEEPER_REVIEW_APPROVE, "RECEIPT",
                savedReceipt.getId(), savedReceipt.getReceiptNumber(),
                savedReceipt.getWarehouse().getId(), before,
                storekeeperReviewSnapshot(savedReceipt, savedItems, request.getDecision()));
        return receiptMapper.toResponse(savedReceipt, savedItems);
    }

    @Transactional
    public ReceiptResponse cancelReceipt(Long receiptId, ReceiptCancelRequest request, User actor) {
        requireCancelRole(actor);
        Receipt receipt = receiptRepository.findByIdWithWarehouse(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found with id: " + receiptId));
        requireWarehouseAccess(actor, receipt.getWarehouse().getId());
        assertVersion(receipt, request.getExpectedVersion());
        if (receipt.getStatus() == ReceiptStatus.PUTAWAY_COMPLETED
                || receipt.getStatus() == ReceiptStatus.RETURNED_TO_SUPPLIER
                || receipt.getStatus() == ReceiptStatus.CANCELLED) {
            throw new UnprocessableEntityException("Cannot cancel finalized receipt in status: " + receipt.getStatus());
        }
        Map<String, Object> before = Map.of("status", receipt.getStatus().name());
        receipt.setStatus(ReceiptStatus.CANCELLED);
        receipt.setRejectionReason(request.getReason());
        receipt.setUpdatedAt(OffsetDateTime.now());
        Receipt saved = receiptRepository.save(receipt);
        List<ReceiptItem> items = receiptItemRepository.findByReceiptIdOrderByIdAsc(receiptId);
        auditLogService.log(actor, AuditAction.RECEIPT_CANCEL, "RECEIPT",
                saved.getId(), saved.getReceiptNumber(),
                saved.getWarehouse().getId(), before, Map.of("status", "CANCELLED", "reason", request.getReason()));
        return receiptMapper.toResponse(saved, items);
    }

    @Transactional
    public ReceiptResponse reopenReceipt(Long receiptId, ReceiptReopenRequest request, User actor) {
        requireManager(actor);
        Receipt receipt = receiptRepository.findByIdWithWarehouse(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found with id: " + receiptId));
        requireWarehouseAccess(actor, receipt.getWarehouse().getId());
        assertVersion(receipt, request.getExpectedVersion());

        if (receipt.getStatus() == ReceiptStatus.PUTAWAY_COMPLETED
                || receipt.getStatus() == ReceiptStatus.RETURNED_TO_SUPPLIER
                || receipt.getStatus() == ReceiptStatus.CANCELLED) {
            throw new UnprocessableEntityException("Cannot reopen receipt in status: " + receipt.getStatus());
        }
        if (receipt.getStatus() == ReceiptStatus.APPROVED && receipt.getPutawayCompletedAt() != null) {
            throw new UnprocessableEntityException("Cannot reopen receipt after putaway completion");
        }

        Map<String, Object> before = Map.of("status", receipt.getStatus().name());
        List<ReceiptItem> items = receiptItemRepository.findByReceiptIdOrderByIdAsc(receiptId);
        clearQcData(items);
        receiptItemRepository.saveAll(items);

        receipt.setStatus(ReceiptStatus.DRAFT);
        receipt.setRejectionReason(null);
        receipt.setApprovedBy(null);
        receipt.setApprovedAt(null);
        receipt.setUpdatedAt(OffsetDateTime.now());
        Receipt saved = receiptRepository.save(receipt);

        auditLogService.log(actor, AuditAction.RECEIPT_REOPEN, "RECEIPT",
                saved.getId(), saved.getReceiptNumber(),
                saved.getWarehouse().getId(), before, Map.of("status", "DRAFT", "reason", request.getReason()));
        return receiptMapper.toResponse(saved, items);
    }

    private void requireCancelRole(User actor) {
        if (actor == null || (actor.getRole() != UserRole.PLANNER
                && actor.getRole() != UserRole.WAREHOUSE_MANAGER
                && actor.getRole() != UserRole.ADMIN)) {
            throw new AccessDeniedException("Planner, Warehouse Manager, or Admin role is required");
        }
    }

    private void assertVersion(Receipt receipt, Integer expectedVersion) {
        if (expectedVersion == null) {
            throw new IllegalArgumentException("EXPECTED_VERSION_REQUIRED");
        }
        if (receipt.getVersion() != null && !receipt.getVersion().equals(expectedVersion)) {
            throw receiptCountError("INVENTORY_VERSION_CONFLICT", HttpStatus.CONFLICT,
                    "Receipt version mismatch (expected: " + expectedVersion + ", actual: " + receipt.getVersion() + ")");
        }
    }

    private void requireManager(User actor) {
        if (actor == null || (actor.getRole() != UserRole.WAREHOUSE_MANAGER
                && actor.getRole() != UserRole.STOREKEEPER
                && actor.getRole() != UserRole.ADMIN)) {
            throw new AccessDeniedException("Warehouse Manager, Storekeeper, or Admin role is required");
        }
    }

    private void requirePlanner(User actor) {
        if (actor == null || (actor.getRole() != UserRole.PLANNER
                && actor.getRole() != UserRole.STOREKEEPER
                && actor.getRole() != UserRole.WAREHOUSE_MANAGER
                && actor.getRole() != UserRole.ADMIN)) {
            throw new AccessDeniedException("Planner, Storekeeper, Warehouse Manager, or Admin role is required");
        }
    }

    private void requireWarehouseStaff(User actor) {
        if (actor == null || (actor.getRole() != UserRole.WAREHOUSE_STAFF
                && actor.getRole() != UserRole.ADMIN)) {
            throw new AccessDeniedException("Warehouse Staff or Admin role is required");
        }
    }

    private void requireReceiveQcRole(User actor) {
        if (actor == null || (actor.getRole() != UserRole.WAREHOUSE_STAFF
                && actor.getRole() != UserRole.ADMIN)) {
            throw new AccessDeniedException("Warehouse Staff or Admin role is required");
        }
    }

    private void requireStorekeeperReviewRole(User actor) {
        if (actor == null || (actor.getRole() != UserRole.STOREKEEPER
                && actor.getRole() != UserRole.ADMIN)) {
            throw new AccessDeniedException("Storekeeper or Admin role is required");
        }
    }

    private Supplier findActiveSupplier(Long supplierId) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Supplier not found with id: " + supplierId));
        if (!Boolean.TRUE.equals(supplier.getIsActive())) {
            throw new UnprocessableEntityException("Supplier is inactive: " + supplierId);
        }
        return supplier;
    }

    private Warehouse findActiveWarehouse(Long warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Warehouse not found with id: " + warehouseId));
        if (!Boolean.TRUE.equals(warehouse.getIsActive())) {
            throw new UnprocessableEntityException("Warehouse is inactive: " + warehouseId);
        }
        return warehouse;
    }

    private void requireWarehouseAccess(User actor, Long warehouseId) {
        if (actor.getRole() == com.wms.enums.access_control.UserRole.ADMIN || actor.getRole() == com.wms.enums.access_control.UserRole.CEO
                || actor.getRole() == com.wms.enums.access_control.UserRole.ACCOUNTANT
                || actor.getRole() == com.wms.enums.access_control.UserRole.ACCOUNTANT_MANAGER) {
            return;
        }
        boolean assigned = assignmentRepository.findWarehouseIdsByUserId(actor.getId())
                .contains(warehouseId);
        if (!assigned) {
            throw new AccessDeniedException("User is not assigned to warehouse: " + warehouseId);
        }
    }

    private void validateRequest(CreateReceiptRequest request) {
        if (request == null) {
            throw new UnprocessableEntityException("Receipt request is required");
        }
        if (request.getDocumentDate() == null) {
            throw new UnprocessableEntityException("Receipt document date is required");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new UnprocessableEntityException("Receipt items are required");
        }
    }

    private void validateItems(List<CreateReceiptItemRequest> items) {
        Set<Long> productIds = new HashSet<>();
        for (CreateReceiptItemRequest item : items) {
            if (item == null || item.getProductId() == null) {
                throw new UnprocessableEntityException("Product id is required");
            }
            if (item.getExpectedQty() == null || item.getExpectedQty() <= 0) {
                throw new UnprocessableEntityException("Expected quantity must be a positive integer");
            }
            if (!productIds.add(item.getProductId())) {
                throw new UnprocessableEntityException(
                        "Duplicate product line is not allowed: " + item.getProductId());
            }
        }
    }

    private Receipt buildReceipt(CreateReceiptRequest request,
                                 User actor,
                                 Supplier supplier,
                                 Warehouse warehouse) {
        OffsetDateTime now = OffsetDateTime.now();
        LocalDate documentDate = request.getDocumentDate();
        Receipt receipt = new Receipt();
        receipt.setReceiptNumber(generateReceiptNumber(documentDate));
        receipt.setType(ReceiptType.PURCHASE);
        receipt.setWarehouse(warehouse);
        receipt.setSupplier(supplier);
        receipt.setStatus(ReceiptStatus.PENDING_MANAGER_APPROVAL);
        receipt.setDocumentDate(documentDate);
        receipt.setAccountingPeriod(accountingPeriodService.resolveOpenPeriod(documentDate));
        receipt.setCreatedBy(actor);
        receipt.setNotes(request.getNotes());
        receipt.setCreatedAt(now);
        receipt.setUpdatedAt(now);
        return receipt;
    }

    private Receipt saveReceipt(Receipt receipt) {
        try {
            return receiptRepository.saveAndFlush(receipt);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateResourceException(
                    "Receipt number already exists; please retry receipt creation");
        }
    }

    private List<ReceiptItem> buildItems(List<CreateReceiptItemRequest> itemRequests,
                                          Receipt receipt,
                                          LocalDate documentDate) {
        return itemRequests.stream()
                .map(itemRequest -> buildItem(itemRequest, receipt, documentDate))
                .toList();
    }

    private ReceiptItem buildItem(CreateReceiptItemRequest request, Receipt receipt, LocalDate documentDate) {
        Product product = findActiveProduct(request.getProductId());
        ReceiptItem item = new ReceiptItem();
        item.setReceipt(receipt);
        item.setProduct(product);
        item.setExpectedQty(request.getExpectedQty());
        item.setUnitCost(resolveApprovedCost(product.getId(), receipt.getWarehouse().getId(), documentDate));
        item.setOverReceivedQty(0);
        return item;
    }

    private BigDecimal resolveApprovedCost(Long productId, Long warehouseId, LocalDate documentDate) {
        return priceHistoryService.lookupApproved(productId, warehouseId, documentDate)
                .map(PriceHistory::getCostPrice)
                .orElseThrow(() -> new UnprocessableEntityException(
                        "APPROVED_PRICE_REQUIRED: Product " + productId
                                + " has no approved cost price for warehouse " + warehouseId
                                + " at " + documentDate));
    }

    private Product findActiveProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + productId));
        if (!Boolean.TRUE.equals(product.getIsActive())) {
            throw new UnprocessableEntityException("Product is inactive: " + productId);
        }
        return product;
    }

    private String generateReceiptNumber(LocalDate documentDate) {
        String date = documentDate.format(RECEIPT_NUMBER_DATE);
        String sequenceKey = RECEIPT_SEQUENCE_KEY_PREFIX + "-" + date;
        DocumentSequence sequence = sequenceRepository
                .findBySequenceKeyForUpdate(sequenceKey)
                .orElseGet(() -> {
                    DocumentSequence newSeq = new DocumentSequence();
                    newSeq.setSequenceKey(sequenceKey);
                    newSeq.setNextValue(1L);
                    newSeq.setUpdatedAt(OffsetDateTime.now());
                    return sequenceRepository.save(newSeq);
                });
        long value = sequence.getNextValue();
        sequence.setNextValue(value + 1);
        sequence.setUpdatedAt(OffsetDateTime.now());
        sequenceRepository.save(sequence);
        return "PO-" + date + "-" + String.format("%04d", value);
    }

    private Map<String, Object> snapshot(Receipt receipt, List<ReceiptItem> items) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("receiptNumber", receipt.getReceiptNumber());
        values.put("type", receipt.getType().name());
        values.put("status", receipt.getStatus().name());
        values.put("supplierId", receipt.getSupplier().getId());
        values.put("warehouseId", receipt.getWarehouse().getId());
        values.put("documentDate", receipt.getDocumentDate());
        values.put("itemCount", items.size());
        values.put("items", items.stream().map(this::itemSnapshot).toList());
        return values;
    }

    private Map<String, Object> itemSnapshot(ReceiptItem item) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("productId", item.getProduct().getId());
        values.put("expectedQty", item.getExpectedQty());
        return values;
    }

    private void validateReceiveRequest(ReceiveReceiptRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw receiptCountError("RECEIPT_COUNT_INCOMPLETE",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Receipt count request must include every receipt item");
        }
    }

    private void validateReceiveQcRequest(ReceiveQcReceiptRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw receiptCountError("RECEIPT_RECEIVE_QC_INCOMPLETE",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Receive-QC request must include every receipt item");
        }
    }

    private void validateStorekeeperReviewRequest(StorekeeperReviewRequest request) {
        if (request == null || request.getDecision() == null) {
            throw receiptCountError("STOREKEEPER_REVIEW_DECISION_REQUIRED",
                    HttpStatus.BAD_REQUEST,
                    "Storekeeper review decision is required");
        }
        if (request.getDecision() == StorekeeperReviewDecision.REQUEST_RECOUNT
                && (request.getReason() == null || request.getReason().isBlank())) {
            throw receiptCountError("RECOUNT_REASON_REQUIRED",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Recount reason is required when requesting a recount");
        }
    }

    private void validateReceivableStatus(Receipt receipt) {
        if (receipt.getStatus() == ReceiptStatus.PENDING_MANAGER_APPROVAL
                || receipt.getStatus() == ReceiptStatus.REVISION_REQUIRED) {
            throw receiptCountError("RECEIPT_PENDING_MANAGER_APPROVAL",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Receipt requires manager approval before receiving");
        }
        if (receipt.getStatus() == ReceiptStatus.APPROVED
                || receipt.getStatus() == ReceiptStatus.RETURN_TO_SUPPLIER_PENDING
                || receipt.getStatus() == ReceiptStatus.RETURNED_TO_SUPPLIER) {
            throw receiptCountError("RECEIPT_ALREADY_FINALIZED",
                    HttpStatus.CONFLICT,
                    "Receipt is already finalized");
        }
        if (receipt.getStatus() != ReceiptStatus.PENDING_RECEIPT
                && receipt.getStatus() != ReceiptStatus.DRAFT
                && receipt.getStatus() != ReceiptStatus.QC_COMPLETED
                && receipt.getStatus() != ReceiptStatus.QC_FAILED) {
            throw receiptCountError("INVALID_RECEIPT_STATUS",
                    HttpStatus.CONFLICT,
                    "Receipt status does not allow receiving");
        }
    }

    private Map<Long, ReceiveReceiptItemRequest> validateCountCoverage(
            ReceiveReceiptRequest request,
            List<ReceiptItem> items) {
        if (items.isEmpty()) {
            throw receiptCountError("RECEIPT_COUNT_INCOMPLETE",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Receipt has no items to count");
        }
        Map<Long, ReceiptItem> itemById = items.stream()
                .collect(Collectors.toMap(ReceiptItem::getId, Function.identity()));
        Map<Long, ReceiveReceiptItemRequest> countByItemId = new LinkedHashMap<>();
        for (ReceiveReceiptItemRequest count : request.getItems()) {
            validateCountLine(count, itemById, countByItemId);
            countByItemId.put(count.getReceiptItemId(), count);
        }
        if (countByItemId.size() != itemById.size()) {
            throw receiptCountError("RECEIPT_COUNT_INCOMPLETE",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Receipt count request must include every receipt item");
        }
        return countByItemId;
    }

    private void validateCountLine(ReceiveReceiptItemRequest count,
                                   Map<Long, ReceiptItem> itemById,
                                   Map<Long, ReceiveReceiptItemRequest> countByItemId) {
        if (count == null || count.getReceiptItemId() == null
                || count.getCountedQty() == null || count.getCountedQty() < 0) {
            throw invalidReceiptCount();
        }
        if (countByItemId.containsKey(count.getReceiptItemId())
                || !itemById.containsKey(count.getReceiptItemId())) {
            throw invalidReceiptCount();
        }
    }

    private Map<Long, ReceiveQcReceiptItemRequest> validateReceiveQcCoverage(
            ReceiveQcReceiptRequest request,
            List<ReceiptItem> items) {
        if (items.isEmpty()) {
            throw receiptCountError("RECEIPT_RECEIVE_QC_INCOMPLETE",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Receipt has no items to receive and QC");
        }
        Map<Long, ReceiptItem> itemById = items.stream()
                .collect(Collectors.toMap(ReceiptItem::getId, Function.identity()));
        Map<Long, ReceiveQcReceiptItemRequest> countByItemId = new LinkedHashMap<>();
        for (ReceiveQcReceiptItemRequest count : request.getItems()) {
            validateReceiveQcLine(count, itemById, countByItemId);
            countByItemId.put(count.getReceiptItemId(), count);
        }
        if (countByItemId.size() != itemById.size()) {
            throw receiptCountError("RECEIPT_RECEIVE_QC_INCOMPLETE",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Receive-QC request must include every receipt item");
        }
        return countByItemId;
    }

    private void validateReceiveQcLine(ReceiveQcReceiptItemRequest count,
                                       Map<Long, ReceiptItem> itemById,
                                       Map<Long, ReceiveQcReceiptItemRequest> countByItemId) {
        if (count == null || count.getReceiptItemId() == null
                || count.getActualQty() == null || count.getActualQty() < 0
                || count.getQualityPassedQty() == null || count.getQualityPassedQty() < 0
                || count.getQualityFailedQty() == null || count.getQualityFailedQty() < 0) {
            throw invalidReceiveQc();
        }
        if (countByItemId.containsKey(count.getReceiptItemId())
                || !itemById.containsKey(count.getReceiptItemId())) {
            throw invalidReceiveQc();
        }
        if (count.getQualityPassedQty() + count.getQualityFailedQty() != count.getActualQty()) {
            throw receiptCountError("RECEIVE_QC_TOTAL_MISMATCH",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "qualityPassedQty + qualityFailedQty must equal actualQty");
        }
        if (count.getQualityFailedQty() > 0
                && (count.getQcFailureReason() == null || count.getQcFailureReason().isBlank())) {
            throw receiptCountError("QC_FAILURE_REASON_REQUIRED",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "QC failure reason is required when failed quantity is greater than zero");
        }
    }

    private void applyCount(ReceiptItem item, Integer countedQty) {
        if (countedQty <= item.getExpectedQty()) {
            item.setActualQty(countedQty);
            item.setOverReceivedQty(0);
            return;
        }
        item.setActualQty(item.getExpectedQty());
        item.setOverReceivedQty(countedQty - item.getExpectedQty());
    }

    private void applyReceiveQc(ReceiptItem item,
                                ReceiveQcReceiptItemRequest count,
                                User actor) {
        item.setActualQty(count.getActualQty());
        item.setOverReceivedQty(Math.max(0, count.getActualQty() - item.getExpectedQty()));
        item.setQualityPassedQty(count.getQualityPassedQty());
        item.setQualityFailedQty(count.getQualityFailedQty());
        item.setQuarantineReadyQty(0);
        item.setQcFailureReason(count.getQualityFailedQty() > 0
                ? count.getQcFailureReason().trim()
                : null);
        item.setQcResult(count.getQualityFailedQty() > 0 ? QcResult.FAILED : QcResult.PASSED);
        item.setQcBy(actor);
    }

    private boolean hasQcData(List<ReceiptItem> items) {
        return items.stream().anyMatch(item ->
                item.getQcResult() != null
                        || item.getSampleQty() != null
                        || item.getSamplePassedQty() != null
                        || item.getSampleFailedQty() != null
                        || item.getQcSamplingMethod() != null
                        || item.getQcFailureReason() != null);
    }

    private void clearQcData(List<ReceiptItem> items) {
        for (ReceiptItem item : items) {
            item.setQcResult(null);
            item.setSampleQty(null);
            item.setSamplePassedQty(null);
            item.setSampleFailedQty(null);
            item.setQualityPassedQty(0);
            item.setQualityFailedQty(0);
            item.setApprovedQty(0);
            item.setQuarantineReadyQty(0);
            item.setQuarantineQty(0);
            item.setResolvedQuarantineQty(0);
            item.setQcSamplingMethod(null);
            item.setQcFailureReason(null);
            item.setQcBy(null);
        }
    }

    private boolean positive(Integer value) {
        return value != null && value > 0;
    }

    private Map<String, Object> receiveSnapshot(Receipt receipt, List<ReceiptItem> items) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("receiptNumber", receipt.getReceiptNumber());
        values.put("status", receipt.getStatus().name());
        values.put("warehouseId", receipt.getWarehouse().getId());
        values.put("items", items.stream().map(this::receiveItemSnapshot).toList());
        return values;
    }

    private Map<String, Object> receiveItemSnapshot(ReceiptItem item) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("receiptItemId", item.getId());
        values.put("productId", item.getProduct().getId());
        values.put("expectedQty", item.getExpectedQty());
        values.put("actualQty", item.getActualQty());
        values.put("overReceivedQty", item.getOverReceivedQty());
        values.put("qualityPassedQty", item.getQualityPassedQty());
        values.put("qualityFailedQty", item.getQualityFailedQty());
        values.put("approvedQty", item.getApprovedQty());
        values.put("quarantineReadyQty", item.getQuarantineReadyQty());
        values.put("quarantineQty", item.getQuarantineQty());
        values.put("resolvedQuarantineQty", item.getResolvedQuarantineQty());
        values.put("qcResult", item.getQcResult() == null ? null : item.getQcResult().name());
        values.put("sampleQty", item.getSampleQty());
        values.put("samplePassedQty", item.getSamplePassedQty());
        values.put("sampleFailedQty", item.getSampleFailedQty());
        values.put("qcSamplingMethod",
                item.getQcSamplingMethod() == null ? null : item.getQcSamplingMethod().name());
        values.put("qcFailureReason", item.getQcFailureReason());
        values.put("qcBy", item.getQcBy() == null ? null : item.getQcBy().getId());
        return values;
    }

    private Map<String, Object> storekeeperReviewSnapshot(Receipt receipt,
                                                          List<ReceiptItem> items,
                                                          StorekeeperReviewDecision decision) {
        Map<String, Object> values = receiveSnapshot(receipt, items);
        values.put("decision", decision.name());
        values.put("storekeeperReviewedBy",
                receipt.getStorekeeperReviewedBy() == null ? null : receipt.getStorekeeperReviewedBy().getId());
        values.put("storekeeperReviewedAt", receipt.getStorekeeperReviewedAt());
        values.put("recountReason", receipt.getRecountReason());
        return values;
    }

    private ReceiptCountException invalidReceiptCount() {
        return receiptCountError("INVALID_RECEIPT_COUNT",
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Receipt count contains invalid item or quantity");
    }

    private ReceiptCountException invalidReceiveQc() {
        return receiptCountError("INVALID_RECEIVE_QC",
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Receive-QC contains invalid item or quantity");
    }

    private ReceiptCountException receiptCountError(String code,
                                                    HttpStatus status,
                                                    String message) {
        return new ReceiptCountException(code, status, message);
    }

    private void requireWarehouseManager(User actor) {
        if (actor == null || actor.getRole() != UserRole.WAREHOUSE_MANAGER) {
            throw new AccessDeniedException("Warehouse Manager role is required");
        }
    }

    private void validateReceiveQcStatus(Receipt receipt) {
        if (receipt.getStatus() == ReceiptStatus.PENDING_MANAGER_APPROVAL
                || receipt.getStatus() == ReceiptStatus.REVISION_REQUIRED) {
            throw receiptCountError("RECEIPT_PENDING_MANAGER_APPROVAL",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Receipt requires manager approval before receiving and QC");
        }
        if (receipt.getStatus() != ReceiptStatus.PENDING_RECEIPT
                && receipt.getStatus() != ReceiptStatus.DRAFT
                && receipt.getStatus() != ReceiptStatus.RECOUNT_REQUIRED) {
            throw receiptCountError("INVALID_RECEIPT_STATUS",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Receipt status does not allow receive-QC");
        }
    }

    private void validateStorekeeperReviewStatus(Receipt receipt) {
        if (receipt.getStatus() != ReceiptStatus.PENDING_STOREKEEPER_REVIEW) {
            throw receiptCountError("STOREKEEPER_REVIEW_INVALID_STATUS",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Storekeeper review requires PENDING_STOREKEEPER_REVIEW status");
        }
    }

    private void ensureReceiveQcEditable(Receipt receipt, List<ReceiptItem> items) {
        if (receipt != null && (receipt.getStatus() == ReceiptStatus.DRAFT
                || receipt.getStatus() == ReceiptStatus.PENDING_RECEIPT
                || receipt.getStatus() == ReceiptStatus.RECOUNT_REQUIRED)) {
            return;
        }
        boolean managerDecisionStarted = items.stream().anyMatch(item ->
                positive(item.getApprovedQty())
                        || positive(item.getQuarantineQty())
                        || positive(item.getResolvedQuarantineQty()));
        if (managerDecisionStarted) {
            throw receiptCountError("RECEIPT_QC_ALREADY_DECIDED",
                    HttpStatus.CONFLICT,
                    "Receive-QC cannot be edited after manager decision has started");
        }
    }

    private void requirePlannerOnly(User actor) {
        if (actor == null || (actor.getRole() != UserRole.PLANNER
                && actor.getRole() != UserRole.ADMIN)) {
            throw new AccessDeniedException("Planner or Admin role is required");
        }
    }

    private void validateRevisionRequest(ReviseReceiptRequest request) {
        if (request == null || request.getDocumentDate() == null) {
            throw new UnprocessableEntityException("DOCUMENT_DATE_REQUIRED");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new UnprocessableEntityException("RECEIPT_ITEMS_REQUIRED");
        }
    }

    private void applyRevisionItems(List<ReviseReceiptItemRequest> revisions,
                                    List<ReceiptItem> items,
                                    LocalDate documentDate) {
        validateRevisionItems(revisions, items);
        Map<Long, ReceiptItem> itemById = items.stream()
                .collect(Collectors.toMap(ReceiptItem::getId, Function.identity()));
        for (ReviseReceiptItemRequest revision : revisions) {
            ReceiptItem item = itemById.get(revision.getReceiptItemId());
            Product product = findActiveProduct(revision.getProductId());
            item.setProduct(product);
            item.setExpectedQty(revision.getExpectedQty());
            item.setUnitCost(resolveApprovedCost(product.getId(),
                    item.getReceipt().getWarehouse().getId(),
                    documentDate));
            item.setActualQty(null);
            item.setOverReceivedQty(0);
            clearReceiptItemQcData(item);
        }
    }

    private void validateRevisionItems(List<ReviseReceiptItemRequest> revisions,
                                       List<ReceiptItem> items) {
        if (revisions.size() != items.size()) {
            throw new UnprocessableEntityException("RECEIPT_REVISION_ITEMS_MISMATCH");
        }
        Set<Long> existingIds = items.stream().map(ReceiptItem::getId).collect(Collectors.toSet());
        Set<Long> seenItemIds = new HashSet<>();
        Set<Long> seenProductIds = new HashSet<>();
        for (ReviseReceiptItemRequest revision : revisions) {
            if (revision == null || revision.getReceiptItemId() == null
                    || !existingIds.contains(revision.getReceiptItemId())
                    || !seenItemIds.add(revision.getReceiptItemId())) {
                throw new UnprocessableEntityException("RECEIPT_REVISION_ITEMS_MISMATCH");
            }
            if (revision.getProductId() == null || !seenProductIds.add(revision.getProductId())) {
                throw new UnprocessableEntityException("Duplicate product line is not allowed: "
                        + revision.getProductId());
            }
            if (revision.getExpectedQty() == null || revision.getExpectedQty() <= 0) {
                throw new UnprocessableEntityException("Expected quantity must be a positive integer");
            }
        }
    }

    private void clearReceiptItemQcData(ReceiptItem item) {
        item.setQcResult(null);
        item.setSampleQty(null);
        item.setSamplePassedQty(null);
        item.setSampleFailedQty(null);
        item.setQualityPassedQty(0);
        item.setQualityFailedQty(0);
        item.setApprovedQty(0);
        item.setQuarantineReadyQty(0);
        item.setQuarantineQty(0);
        item.setResolvedQuarantineQty(0);
        item.setQcSamplingMethod(null);
        item.setQcFailureReason(null);
        item.setQcBy(null);
    }

    private Map<String, Object> preReceiveSnapshot(Receipt receipt,
                                                   List<ReceiptItem> items,
                                                   String decision,
                                                   String reason) {
        Map<String, Object> values = snapshot(receipt, items);
        values.put("decision", decision);
        values.put("reason", reason);
        values.put("preReceiveApprovedBy", receipt.getPreReceiveApprovedBy() == null
                ? null : receipt.getPreReceiveApprovedBy().getId());
        values.put("preReceiveApprovedAt", receipt.getPreReceiveApprovedAt());
        return values;
    }
}
