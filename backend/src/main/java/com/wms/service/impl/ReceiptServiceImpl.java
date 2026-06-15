package com.wms.service.impl;

import com.wms.dto.request.*;
import com.wms.dto.response.ReceiptDetailResponse;
import com.wms.dto.response.ReceiptItemResponse;
import com.wms.dto.response.ReceiptResponse;
import com.wms.entity.*;
import com.wms.enums.*;
import com.wms.exception.BusinessRuleViolationException;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.*;
import com.wms.service.AuditLogService;
import com.wms.service.ReceiptService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReceiptServiceImpl implements ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final ReceiptItemRepository receiptItemRepository;
    private final SupplierRepository supplierRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final AccountingPeriodRepository accountingPeriodRepository;
    private final AdjustmentRepository adjustmentRepository;
    private final DebitNoteRepository debitNoteRepository;
    private final ReceiptInventoryHelper inventoryHelper;
    private final AuditLogService auditLogService;

    public ReceiptServiceImpl(ReceiptRepository receiptRepository,
                              ReceiptItemRepository receiptItemRepository,
                              SupplierRepository supplierRepository,
                              WarehouseRepository warehouseRepository,
                              ProductRepository productRepository,
                              AccountingPeriodRepository accountingPeriodRepository,
                              AdjustmentRepository adjustmentRepository,
                              DebitNoteRepository debitNoteRepository,
                              ReceiptInventoryHelper inventoryHelper,
                              AuditLogService auditLogService) {
        this.receiptRepository = receiptRepository;
        this.receiptItemRepository = receiptItemRepository;
        this.supplierRepository = supplierRepository;
        this.warehouseRepository = warehouseRepository;
        this.productRepository = productRepository;
        this.accountingPeriodRepository = accountingPeriodRepository;
        this.adjustmentRepository = adjustmentRepository;
        this.debitNoteRepository = debitNoteRepository;
        this.inventoryHelper = inventoryHelper;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional
    public ReceiptDetailResponse create(ReceiptCreateRequest req, User actor) {
        Supplier supplier = findSupplier(req.getSupplierId());
        Warehouse warehouse = findWarehouse(req.getWarehouseId());

        if (receiptRepository.existsBySourceOrderCodeAndSupplierIdAndWarehouseId(
                req.getSourceOrderCode(), supplier.getId(), warehouse.getId())) {
            throw new BusinessRuleViolationException("Duplicate source reference for this supplier and warehouse");
        }

        OffsetDateTime now = OffsetDateTime.now();
        Receipt receipt = Receipt.builder()
                .type(ReceiptType.PURCHASE)
                .supplier(supplier)
                .warehouse(warehouse)
                .contactPerson(req.getContactPerson())
                .sourceOrderCode(req.getSourceOrderCode())
                .sourceChannel(req.getSourceChannel())
                .status(ReceiptStatus.PENDING_RECEIPT)
                .documentDate(req.getDocumentDate())
                .accountingPeriod(findAccountingPeriod(req.getDocumentDate()))
                .createdBy(actor)
                .notes(req.getNotes())
                .createdAt(now)
                .updatedAt(now)
                .receiptNumber("TEMP")
                .build();

        Receipt saved = receiptRepository.save(receipt);
        saved.setReceiptNumber(generateReceiptNumber(saved.getId(), req.getDocumentDate()));
        saved = receiptRepository.save(saved);

        List<ReceiptItem> items = buildItems(req.getItems(), saved);
        receiptItemRepository.saveAll(items);

        auditLogService.log(actor, AuditAction.RECEIPT_CREATE, "RECEIPT",
                saved.getId(), saved.getReceiptNumber(), warehouse.getId(),
                null, Map.of("status", ReceiptStatus.PENDING_RECEIPT.name()));

        return toDetail(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ReceiptDetailResponse get(Long id) {
        return toDetail(findReceipt(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReceiptResponse> listByWarehouse(Long warehouseId) {
        return receiptRepository.findByWarehouseIdAndTypeOrderByDocumentDateDescCreatedAtDesc(
                warehouseId, ReceiptType.PURCHASE).stream().map(ReceiptResponse::from).toList();
    }

    @Override
    @Transactional
    public ReceiptDetailResponse receive(Long id, ReceiptReceiveRequest req, User actor) {
        Receipt receipt = findReceipt(id);
        requireStatus(receipt, ReceiptStatus.PENDING_RECEIPT);

        long approvedCount = receiptRepository.countBySupplierIdAndStatus(
                receipt.getSupplier().getId(), ReceiptStatus.APPROVED);
        QcSamplingMethod method = approvedCount >= 5
                ? QcSamplingMethod.RANDOM_SAMPLE : QcSamplingMethod.FULL_INSPECTION;

        List<ReceiptItem> items = receiptItemRepository.findByReceiptId(id);
        req.getItems().forEach(r -> items.stream()
                .filter(i -> i.getId().equals(r.getReceiptItemId())).findFirst()
                .ifPresent(i -> {
                    i.setActualQty(r.getActualQty());
                    i.setQcSamplingMethod(method);
                    i.setQcResult(QcResult.PENDING);
                }));
        receiptItemRepository.saveAll(items);

        receipt.setStatus(ReceiptStatus.DRAFT);
        receipt.setUpdatedAt(OffsetDateTime.now());
        Receipt saved = receiptRepository.save(receipt);

        auditLogService.log(actor, AuditAction.RECEIPT_RECEIVE, "RECEIPT",
                saved.getId(), saved.getReceiptNumber(), saved.getWarehouse().getId(),
                Map.of("status", ReceiptStatus.PENDING_RECEIPT.name()),
                Map.of("status", ReceiptStatus.DRAFT.name()));
        return toDetail(saved);
    }

    @Override
    @Transactional
    public ReceiptDetailResponse qc(Long id, ReceiptQcRequest req, User actor) {
        return "SUBMIT".equals(req.getAction())
                ? doQcSubmit(id, req, actor)
                : doQcConfirm(id, req, actor);
    }

    @Override
    @Transactional
    public ReceiptDetailResponse approve(Long id, ReceiptApproveRequest req, User actor) {
        Receipt receipt = findReceipt(id);
        requireStatus(receipt, ReceiptStatus.QC_COMPLETED);

        List<ReceiptItem> items = receiptItemRepository.findByReceiptId(id);
        req.getItems().forEach(r -> items.stream()
                .filter(i -> i.getId().equals(r.getReceiptItemId())).findFirst()
                .ifPresent(i -> processApproveItem(i, r, receipt, actor)));
        receiptItemRepository.saveAll(items);

        OffsetDateTime now = OffsetDateTime.now();
        receipt.setStatus(ReceiptStatus.APPROVED);
        receipt.setApprovedBy(actor);
        receipt.setApprovedAt(now);
        receipt.setUpdatedAt(now);
        Receipt saved = receiptRepository.save(receipt);

        auditLogService.log(actor, AuditAction.RECEIPT_APPROVE, "RECEIPT",
                saved.getId(), saved.getReceiptNumber(), saved.getWarehouse().getId(),
                Map.of("status", ReceiptStatus.QC_COMPLETED.name()),
                Map.of("status", ReceiptStatus.APPROVED.name()));
        return toDetail(saved);
    }

    @Override
    @Transactional
    public ReceiptDetailResponse reject(Long id, ReceiptRejectRequest req, User actor) {
        Receipt receipt = findReceipt(id);
        requireStatus(receipt, ReceiptStatus.QC_COMPLETED);

        receipt.setStatus(ReceiptStatus.REJECTED);
        receipt.setRejectionReason(req.getRejectionReason());
        receipt.setUpdatedAt(OffsetDateTime.now());
        Receipt saved = receiptRepository.save(receipt);

        auditLogService.log(actor, AuditAction.RECEIPT_REJECT, "RECEIPT",
                saved.getId(), saved.getReceiptNumber(), saved.getWarehouse().getId(),
                Map.of("status", ReceiptStatus.QC_COMPLETED.name()),
                Map.of("status", ReceiptStatus.REJECTED.name(), "reason", req.getRejectionReason()));
        return toDetail(saved);
    }

    @Override
    @Transactional
    public ReceiptDetailResponse rtv(Long id, ReceiptRtvRequest req, User actor) {
        Receipt receipt = findReceipt(id);
        requireStatus(receipt, ReceiptStatus.QC_FAILED);

        List<ReceiptItem> items = receiptItemRepository.findByReceiptId(id);
        WarehouseLocation quarantineLoc = inventoryHelper.requireQuarantineLocation(
                receipt.getWarehouse().getId());
        AccountingPeriod period = findAccountingPeriod(req.getDocumentDate());
        OffsetDateTime now = OffsetDateTime.now();

        items.forEach(item -> {
            if (item.getActualQty() == null || item.getActualQty().compareTo(BigDecimal.ZERO) == 0) return;
            inventoryHelper.deductQuarantineInventory(
                    receipt.getWarehouse(), item.getProduct(), quarantineLoc, item.getActualQty());
            adjustmentRepository.save(Adjustment.builder()
                    .adjustmentNumber("ADJ-" + receipt.getReceiptNumber() + "-" + item.getId())
                    .warehouse(receipt.getWarehouse())
                    .product(item.getProduct())
                    .location(quarantineLoc)
                    .quantityAdjustment(item.getActualQty().negate())
                    .type(AdjustmentType.RETURN_TO_VENDOR)
                    .referenceId(receipt.getId())
                    .referenceType("RECEIPT")
                    .reason(req.getReason())
                    .approvedBy(actor)
                    .approvedAt(now)
                    .documentDate(req.getDocumentDate())
                    .accountingPeriod(period)
                    .createdBy(actor)
                    .createdAt(now)
                    .build());
        });

        BigDecimal totalFailedQty = items.stream()
                .map(i -> i.getActualQty() != null ? i.getActualQty() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        debitNoteRepository.save(DebitNote.builder()
                .debitNoteNumber("DN-" + receipt.getReceiptNumber())
                .supplier(receipt.getSupplier())
                .receipt(receipt)
                .failedQty(totalFailedQty)
                .amount(req.getAmount())
                .reason(req.getReason())
                .createdBy(actor)
                .documentDate(req.getDocumentDate())
                .accountingPeriod(period)
                .createdAt(now)
                .build());

        auditLogService.log(actor, AuditAction.QUARANTINE_RTV_CONFIRM, "RECEIPT",
                receipt.getId(), receipt.getReceiptNumber(), receipt.getWarehouse().getId(),
                Map.of("status", ReceiptStatus.QC_FAILED.name()),
                Map.of("action", "RTV_COMPLETED", "debitNote", "DN-" + receipt.getReceiptNumber()));
        return toDetail(receipt);
    }

    @Override
    @Transactional
    public ReceiptDetailResponse putaway(Long id, ReceiptPutawayRequest req, User actor) {
        Receipt receipt = findReceipt(id);
        requireStatus(receipt, ReceiptStatus.APPROVED);

        List<ReceiptItem> items = receiptItemRepository.findByReceiptId(id);
        req.getItems().forEach(r -> items.stream()
                .filter(i -> i.getId().equals(r.getReceiptItemId())).findFirst()
                .ifPresent(i -> relocateItem(i, r.getLocationId())));
        receiptItemRepository.saveAll(items);

        auditLogService.log(actor, AuditAction.RECEIPT_PUTAWAY_COMPLETE, "RECEIPT",
                receipt.getId(), receipt.getReceiptNumber(), receipt.getWarehouse().getId(),
                null, Map.of("action", "PUTAWAY_COMPLETE"));
        return toDetail(receipt);
    }

    private ReceiptDetailResponse doQcSubmit(Long id, ReceiptQcRequest req, User actor) {
        Receipt receipt = findReceipt(id);
        requireStatus(receipt, ReceiptStatus.DRAFT);

        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new BusinessRuleViolationException("QC items are required for SUBMIT action");
        }

        List<ReceiptItem> items = receiptItemRepository.findByReceiptId(id);
        req.getItems().forEach(r -> {
            if (r.getSamplePassedQty().add(r.getSampleFailedQty()).compareTo(r.getSampleQty()) != 0) {
                throw new BusinessRuleViolationException("QC_SAMPLE_MISMATCH: passed + failed must equal sample_qty");
            }
            items.stream().filter(i -> i.getId().equals(r.getReceiptItemId())).findFirst()
                    .ifPresent(i -> {
                        i.setSampleQty(r.getSampleQty());
                        i.setSamplePassedQty(r.getSamplePassedQty());
                        i.setSampleFailedQty(r.getSampleFailedQty());
                        i.setQcFailureReason(r.getQcFailureReason());
                        i.setQcResult(inventoryHelper.computeItemQcResult(
                                r.getSampleQty(), r.getSamplePassedQty(), r.getSampleFailedQty()));
                        i.setQcBy(actor);
                    });
        });
        receiptItemRepository.saveAll(items);

        auditLogService.log(actor, AuditAction.RECEIPT_QC_SUBMIT, "RECEIPT",
                receipt.getId(), receipt.getReceiptNumber(), receipt.getWarehouse().getId(),
                null, Map.of("action", "QC_SUBMITTED"));
        return toDetail(receipt);
    }

    private ReceiptDetailResponse doQcConfirm(Long id, ReceiptQcRequest req, User actor) {
        if (req.getDecision() == null) {
            throw new BusinessRuleViolationException("decision is required for CONFIRM action");
        }
        Receipt receipt = findReceipt(id);
        requireStatus(receipt, ReceiptStatus.DRAFT);

        boolean passed = "PASSED".equals(req.getDecision());
        receipt.setStatus(passed ? ReceiptStatus.QC_COMPLETED : ReceiptStatus.QC_FAILED);
        receipt.setUpdatedAt(OffsetDateTime.now());

        if (!passed) {
            WarehouseLocation quarantineLoc = inventoryHelper.requireQuarantineLocation(
                    receipt.getWarehouse().getId());
            receiptItemRepository.findByReceiptId(id).forEach(item -> {
                if (item.getActualQty() != null && item.getActualQty().compareTo(BigDecimal.ZERO) > 0) {
                    inventoryHelper.upsertQuarantineInventory(
                            receipt.getWarehouse(), item.getProduct(),
                            quarantineLoc, item.getActualQty(), item.getUnitCost());
                }
            });
        }

        Receipt saved = receiptRepository.save(receipt);
        auditLogService.log(actor, AuditAction.RECEIPT_QC_CONFIRM, "RECEIPT",
                saved.getId(), saved.getReceiptNumber(), saved.getWarehouse().getId(),
                Map.of("status", ReceiptStatus.DRAFT.name()),
                Map.of("status", saved.getStatus().name()));
        return toDetail(saved);
    }

    private void processApproveItem(ReceiptItem item, ReceiptApproveItemRequest r, Receipt receipt, User actor) {
        WarehouseLocation location = inventoryHelper.requireLocation(r.getLocationId());
        if (location.getIsQuarantine()) {
            throw new BusinessRuleViolationException("Cannot approve into quarantine location");
        }
        BigDecimal unitCost = r.getUnitCost() != null ? r.getUnitCost() : item.getUnitCost();
        if (unitCost != null) item.setUnitCost(unitCost);

        String batchNumber = receipt.getReceiptNumber() + "-" + item.getProduct().getSku();
        BigDecimal qty = item.getActualQty() != null ? item.getActualQty() : item.getExpectedQty();
        Batch batch = inventoryHelper.createOrUpdateBatch(batchNumber, item.getProduct(),
                receipt.getWarehouse(), receipt.getDocumentDate(), r.getGrade(), qty);

        item.setBatch(batch);
        item.setLocation(location);
        inventoryHelper.upsertInventory(receipt.getWarehouse(), item.getProduct(),
                batch, location, qty, item.getUnitCost());

        auditLogService.log(actor, AuditAction.INVENTORY_UPDATE, "INVENTORY",
                batch.getId(), batchNumber, receipt.getWarehouse().getId(),
                null, Map.of("productSku", item.getProduct().getSku(), "qty", qty));
    }

    private void relocateItem(ReceiptItem item, Long newLocationId) {
        WarehouseLocation newLoc = inventoryHelper.requireLocation(newLocationId);
        if (item.getBatch() == null || item.getLocation() == null) return;

        BigDecimal qty = item.getActualQty() != null ? item.getActualQty() : BigDecimal.ZERO;
        Product p = item.getProduct();

        if (newLoc.getCapacityM3() != null && p.getVolumeM3() != null) {
            BigDecimal incoming = p.getVolumeM3().multiply(qty);
            if (newLoc.getCurrentVolumeM3().add(incoming).compareTo(newLoc.getCapacityM3()) > 0) {
                throw new BusinessRuleViolationException("Location capacity (volume) exceeded: " + newLoc.getCode());
            }
        }
        if (newLoc.getCapacityKg() != null && p.getWeightKg() != null) {
            BigDecimal incoming = p.getWeightKg().multiply(qty);
            if (newLoc.getCurrentWeightKg().add(incoming).compareTo(newLoc.getCapacityKg()) > 0) {
                throw new BusinessRuleViolationException("Location capacity (weight) exceeded: " + newLoc.getCode());
            }
        }
        item.setLocation(newLoc);
    }

    private List<ReceiptItem> buildItems(List<ReceiptCreateItemRequest> reqs, Receipt receipt) {
        return reqs.stream().map(r -> {
            Product product = productRepository.findByIdAndIsActiveTrue(r.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found or inactive: " + r.getProductId()));
            return ReceiptItem.builder()
                    .receipt(receipt)
                    .product(product)
                    .expectedQty(r.getExpectedQty())
                    .unitCost(r.getUnitCost())
                    .qcResult(QcResult.PENDING)
                    .build();
        }).toList();
    }

    private ReceiptDetailResponse toDetail(Receipt receipt) {
        List<ReceiptItemResponse> items = receiptItemRepository.findByReceiptId(receipt.getId())
                .stream().map(ReceiptItemResponse::from).toList();
        return ReceiptDetailResponse.from(receipt, items);
    }

    private AccountingPeriod findAccountingPeriod(LocalDate date) {
        return accountingPeriodRepository.findOpenPeriodForDate(AccountingPeriodStatus.OPEN, date)
                .orElse(null);
    }

    private Receipt findReceipt(Long id) {
        return receiptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found: " + id));
    }

    private Supplier findSupplier(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + id));
    }

    private Warehouse findWarehouse(Long id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found: " + id));
    }

    private void requireStatus(Receipt receipt, ReceiptStatus expected) {
        if (receipt.getStatus() != expected) {
            throw new BusinessRuleViolationException(
                    "Receipt must be in status " + expected + " but was " + receipt.getStatus());
        }
    }

    private String generateReceiptNumber(Long id, LocalDate date) {
        return String.format("REC-%s-%05d", date.format(DateTimeFormatter.ofPattern("yyyyMM")), id);
    }
}
