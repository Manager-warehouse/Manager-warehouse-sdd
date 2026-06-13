package com.wms.service;

import com.wms.dto.request.CreateReceiptItemRequest;
import com.wms.dto.request.CreateReceiptRequest;
import com.wms.dto.response.ReceiptResponse;
import com.wms.entity.DocumentSequence;
import com.wms.entity.Product;
import com.wms.entity.Receipt;
import com.wms.entity.ReceiptItem;
import com.wms.entity.Supplier;
import com.wms.entity.User;
import com.wms.entity.Warehouse;
import com.wms.enums.AuditAction;
import com.wms.enums.ReceiptStatus;
import com.wms.enums.ReceiptType;
import com.wms.enums.UserRole;
import com.wms.exception.DuplicateResourceException;
import com.wms.exception.ResourceNotFoundException;
import com.wms.exception.UnprocessableEntityException;
import com.wms.mapper.ReceiptMapper;
import com.wms.repository.DocumentSequenceRepository;
import com.wms.repository.ProductRepository;
import com.wms.repository.ReceiptItemRepository;
import com.wms.repository.ReceiptRepository;
import com.wms.repository.SupplierRepository;
import com.wms.repository.UserWarehouseAssignmentRepository;
import com.wms.repository.WarehouseRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    public ReceiptService(DocumentSequenceRepository sequenceRepository,
                          ReceiptRepository receiptRepository,
                          ReceiptItemRepository receiptItemRepository,
                          SupplierRepository supplierRepository,
                          WarehouseRepository warehouseRepository,
                          ProductRepository productRepository,
                          UserWarehouseAssignmentRepository assignmentRepository,
                          AuditLogService auditLogService,
                          ReceiptMapper receiptMapper) {
        this.sequenceRepository = sequenceRepository;
        this.receiptRepository = receiptRepository;
        this.receiptItemRepository = receiptItemRepository;
        this.supplierRepository = supplierRepository;
        this.warehouseRepository = warehouseRepository;
        this.productRepository = productRepository;
        this.assignmentRepository = assignmentRepository;
        this.auditLogService = auditLogService;
        this.receiptMapper = receiptMapper;
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

    private void requirePlanner(User actor) {
        if (actor == null || actor.getRole() != UserRole.PLANNER) {
            throw new AccessDeniedException("Planner role is required");
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
                        ReceiptStatus.REJECTED);
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
        Receipt receipt = new Receipt();
        receipt.setReceiptNumber(generateReceiptNumber());
        receipt.setSourceOrderCode(request.getSourceReference());
        receipt.setType(ReceiptType.PURCHASE);
        receipt.setWarehouse(warehouse);
        receipt.setSupplier(supplier);
        receipt.setContactPerson(request.getContactPerson());
        receipt.setSourceChannel(request.getSourceChannel().name());
        receipt.setStatus(ReceiptStatus.PENDING_RECEIPT);
        receipt.setDocumentDate(LocalDate.now());
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
        item.setOverReceivedQty(BigDecimal.ZERO);
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
                .orElseThrow(() -> new IllegalStateException("Receipt sequence is not configured"));
        long value = sequence.getNextValue();
        sequence.setNextValue(value + 1);
        sequence.setUpdatedAt(OffsetDateTime.now());
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
}
