package com.wms.service.stock_receiving;


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
import com.wms.dto.request.CreateReceiptItemRequest;
import com.wms.dto.request.CreateReceiptRequest;
import com.wms.dto.request.ReceiveReceiptItemRequest;
import com.wms.dto.request.ReceiveReceiptRequest;
import com.wms.dto.response.ReceiptResponse;
import com.wms.entity.document_numbering.DocumentSequence;
import com.wms.entity.product_catalog.Product;
import com.wms.entity.stock_receiving.Receipt;
import com.wms.entity.stock_receiving.ReceiptItem;
import com.wms.entity.supplier_management.Supplier;
import com.wms.entity.access_control.User;
import com.wms.entity.warehouse_location.Warehouse;
import com.wms.enums.audit_trail.AuditAction;
import com.wms.enums.stock_receiving.ReceiptStatus;
import com.wms.enums.stock_receiving.ReceiptType;
import com.wms.enums.access_control.UserRole;
import com.wms.exception.DuplicateResourceException;
import com.wms.exception.ReceiptCountException;
import com.wms.exception.ResourceNotFoundException;
import com.wms.exception.UnprocessableEntityException;
import com.wms.mapper.ReceiptMapper;
import com.wms.repository.DocumentSequenceRepository;
import com.wms.repository.CreditNoteRepository;
import com.wms.repository.product_catalog.ProductRepository;
import com.wms.repository.ReceiptItemRepository;
import com.wms.repository.ReceiptRepository;
import com.wms.repository.supplier_management.SupplierRepository;
import com.wms.repository.UserWarehouseAssignmentRepository;
import com.wms.repository.WarehouseRepository;
import com.wms.service.audit_trail.AuditLogService;
import com.wms.service.billing_payment.AccountingPeriodService;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReceiptService {

    private static final DateTimeFormatter RECEIPT_NUMBER_DATE =
            DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String RECEIPT_SEQUENCE_KEY = "RECEIPT";

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
                          CreditNoteRepository creditNoteRepository) {
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
            response.setCreditNoteGenerated(creditNoteRepository.existsByReceiptId(receipt.getId()));
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
        validateDuplicateSource(request);

        Receipt receipt = buildReceipt(request, actor, supplier, warehouse);
        Receipt savedReceipt = saveReceipt(receipt);
        List<ReceiptItem> items = buildItems(request.getItems(), savedReceipt);
        List<ReceiptItem> savedItems = receiptItemRepository.saveAll(items);

        auditLogService.log(actor, AuditAction.CREATE, "RECEIPT",
                savedReceipt.getId(), savedReceipt.getReceiptNumber(),
                warehouse.getId(), null, snapshot(savedReceipt, savedItems));
        return receiptMapper.toResponse(savedReceipt, savedItems);
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

        List<ReceiptItem> items = receiptItemRepository.findByReceiptIdOrderByIdAsc(receiptId);
        Map<String, Object> before = receiveSnapshot(receipt, items);
        Map<Long, ReceiveReceiptItemRequest> counts = validateCountCoverage(request, items);

        for (ReceiptItem item : items) {
            applyCount(item, counts.get(item.getId()).getCountedQty());
        }
        if (hasQcData(items)) {
            clearQcData(items);
        }
        receipt.setStatus(ReceiptStatus.DRAFT);
        receipt.setUpdatedAt(OffsetDateTime.now());

        List<ReceiptItem> savedItems = receiptItemRepository.saveAll(items);
        Receipt savedReceipt = receiptRepository.save(receipt);
        auditLogService.log(actor, AuditAction.UPDATE, "RECEIPT",
                savedReceipt.getId(), savedReceipt.getReceiptNumber(),
                savedReceipt.getWarehouse().getId(), before,
                receiveSnapshot(savedReceipt, savedItems));
        return receiptMapper.toResponse(savedReceipt, savedItems);
    }

    private void requirePlanner(User actor) {
        if (actor == null || actor.getRole() != UserRole.PLANNER) {
            throw new AccessDeniedException("Planner role is required");
        }
    }

    private void requireWarehouseStaff(User actor) {
        if (actor == null || (actor.getRole() != UserRole.WAREHOUSE_STAFF
                && actor.getRole() != UserRole.STOREKEEPER
                && actor.getRole() != UserRole.WAREHOUSE_MANAGER
                && actor.getRole() != UserRole.ADMIN)) {
            throw new AccessDeniedException("Warehouse Staff, Storekeeper, Warehouse Manager, or Admin role is required");
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

    private void validateDuplicateSource(CreateReceiptRequest request) {
        boolean duplicate = receiptRepository
                .existsBySupplierIdAndWarehouseIdAndSourceOrderCodeAndTypeAndStatusNot(
                        request.getSupplierId(), request.getWarehouseId(),
                        request.getSourceReference(), ReceiptType.PURCHASE,
                        ReceiptStatus.RETURNED_TO_SUPPLIER);
        if (duplicate) {
            throw new DuplicateResourceException(
                    "Receipt source reference already exists for supplier and warehouse");
        }
    }

    private void validateRequest(CreateReceiptRequest request) {
        if (request == null) {
            throw new UnprocessableEntityException("Receipt request is required");
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
        LocalDate documentDate = LocalDate.now();
        Receipt receipt = new Receipt();
        receipt.setReceiptNumber(generateReceiptNumber());
        receipt.setSourceOrderCode(request.getSourceReference());
        receipt.setType(ReceiptType.PURCHASE);
        receipt.setWarehouse(warehouse);
        receipt.setSupplier(supplier);
        receipt.setContactPerson(request.getContactPerson());
        receipt.setSourceChannel(request.getSourceChannel().name());
        receipt.setStatus(ReceiptStatus.PENDING_RECEIPT);
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
                    "Receipt source reference already exists for supplier and warehouse");
        }
    }

    private List<ReceiptItem> buildItems(List<CreateReceiptItemRequest> itemRequests,
                                         Receipt receipt) {
        return itemRequests.stream()
                .map(itemRequest -> buildItem(itemRequest, receipt))
                .toList();
    }

    private ReceiptItem buildItem(CreateReceiptItemRequest request, Receipt receipt) {
        Product product = findActiveProduct(request.getProductId());
        ReceiptItem item = new ReceiptItem();
        item.setReceipt(receipt);
        item.setProduct(product);
        item.setExpectedQty(request.getExpectedQty());
        item.setUnitCost(request.getUnitCost());
        item.setOverReceivedQty(0);
        return item;
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

    private String generateReceiptNumber() {
        String date = LocalDate.now().format(RECEIPT_NUMBER_DATE);
        DocumentSequence sequence = sequenceRepository
                .findBySequenceKeyForUpdate(RECEIPT_SEQUENCE_KEY)
                .orElseGet(() -> {
                    DocumentSequence newSeq = new DocumentSequence();
                    newSeq.setSequenceKey(RECEIPT_SEQUENCE_KEY);
                    newSeq.setNextValue(1L);
                    newSeq.setUpdatedAt(OffsetDateTime.now());
                    return sequenceRepository.save(newSeq);
                });
        long value = sequence.getNextValue();
        sequence.setNextValue(value + 1);
        sequence.setUpdatedAt(OffsetDateTime.now());
        sequenceRepository.save(sequence);
        return "RN-" + date + "-" + String.format("%06d", value);
    }

    private Map<String, Object> snapshot(Receipt receipt, List<ReceiptItem> items) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("receiptNumber", receipt.getReceiptNumber());
        values.put("type", receipt.getType().name());
        values.put("status", receipt.getStatus().name());
        values.put("supplierId", receipt.getSupplier().getId());
        values.put("warehouseId", receipt.getWarehouse().getId());
        values.put("sourceReference", receipt.getSourceOrderCode());
        values.put("sourceChannel", receipt.getSourceChannel());
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

    private void validateReceivableStatus(Receipt receipt) {
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

    private void applyCount(ReceiptItem item, Integer countedQty) {
        if (countedQty <= item.getExpectedQty()) {
            item.setActualQty(countedQty);
            item.setOverReceivedQty(0);
            return;
        }
        item.setActualQty(item.getExpectedQty());
        item.setOverReceivedQty(countedQty - item.getExpectedQty());
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
            item.setQcSamplingMethod(null);
            item.setQcFailureReason(null);
            item.setQcBy(null);
        }
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

    private ReceiptCountException invalidReceiptCount() {
        return receiptCountError("INVALID_RECEIPT_COUNT",
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Receipt count contains invalid item or quantity");
    }

    private ReceiptCountException receiptCountError(String code,
                                                    HttpStatus status,
                                                    String message) {
        return new ReceiptCountException(code, status, message);
    }
}
