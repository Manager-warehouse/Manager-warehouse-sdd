package com.wms.service;

import com.wms.dto.request.*;
import com.wms.dto.response.ReturnCreditNoteResponse;
import com.wms.entity.*;
import com.wms.enums.*;
import com.wms.exception.BusinessRuleViolationException;
import com.wms.exception.ForbiddenReceiptWarehouseException;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ReturnService {

    private static final Logger log = LoggerFactory.getLogger(ReturnService.class);

    private static final String RECEIPT_ENTITY = "RECEIPT";
    private static final String CREDIT_NOTE_ENTITY = "CREDIT_NOTE";
    private static final String INVENTORY_ENTITY = "INVENTORY";

    private final ReceiptRepository receiptRepository;
    private final ReceiptItemRepository receiptItemRepository;
    private final DeliveryOrderRepository deliveryOrderRepository;
    private final DealerRepository dealerRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final BatchRepository batchRepository;
    private final WarehouseLocationRepository warehouseLocationRepository;
    private final InventoryRepository inventoryRepository;
    private final CreditNoteRepository creditNoteRepository;
    private final DeliveryOrderItemRepository deliveryOrderItemRepository;
    private final UserWarehouseAssignmentRepository userWarehouseAssignmentRepository;
    private final AccountingPeriodRepository accountingPeriodRepository;
    private final AuditLogService auditLogService;

    public ReturnService(ReceiptRepository receiptRepository,
                         ReceiptItemRepository receiptItemRepository,
                         DeliveryOrderRepository deliveryOrderRepository,
                         DealerRepository dealerRepository,
                         WarehouseRepository warehouseRepository,
                         ProductRepository productRepository,
                         BatchRepository batchRepository,
                         WarehouseLocationRepository warehouseLocationRepository,
                         InventoryRepository inventoryRepository,
                         CreditNoteRepository creditNoteRepository,
                         UserWarehouseAssignmentRepository userWarehouseAssignmentRepository,
                         AccountingPeriodRepository accountingPeriodRepository,
                         DeliveryOrderItemRepository deliveryOrderItemRepository,
                         AuditLogService auditLogService) {
        this.receiptRepository = receiptRepository;
        this.receiptItemRepository = receiptItemRepository;
        this.deliveryOrderRepository = deliveryOrderRepository;
        this.dealerRepository = dealerRepository;
        this.warehouseRepository = warehouseRepository;
        this.productRepository = productRepository;
        this.batchRepository = batchRepository;
        this.warehouseLocationRepository = warehouseLocationRepository;
        this.inventoryRepository = inventoryRepository;
        this.creditNoteRepository = creditNoteRepository;
        this.userWarehouseAssignmentRepository = userWarehouseAssignmentRepository;
        this.accountingPeriodRepository = accountingPeriodRepository;
        this.deliveryOrderItemRepository = deliveryOrderItemRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public Receipt createReturnReceipt(ReturnCreateRequest request, User actor) {
        assertRole(actor, UserRole.STOREKEEPER, "RETURN_CREATE");
        assertWarehouseAccess(actor, request.getWarehouseId());

        LocalDate docDate = request.getDocumentDate() != null ? request.getDocumentDate() : LocalDate.now();

        // 1. Kiểm tra kỳ kế toán
        AccountingPeriod period = accountingPeriodRepository.findPeriodByDateAndStatus(docDate, AccountingPeriodStatus.OPEN)
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "PERIOD_CLOSED: No open accounting period found for date: " + docDate));
        if (period.getStatus() != AccountingPeriodStatus.OPEN) {
            throw new BusinessRuleViolationException("PERIOD_CLOSED: Accounting period is closed.");
        }

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found: " + request.getWarehouseId()));
        Dealer dealer = dealerRepository.findById(request.getDealerId())
                .orElseThrow(() -> new ResourceNotFoundException("Dealer not found: " + request.getDealerId()));
        DeliveryOrder deliveryOrder = deliveryOrderRepository.findById(request.getDeliveryOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Delivery Order not found: " + request.getDeliveryOrderId()));

        if (deliveryOrder.getStatus() != DeliveryOrderStatus.DELIVERED) {
            throw new BusinessRuleViolationException(
                    "INVALID_DO_STATUS: Can only return items from DELIVERED delivery orders. DO "
                    + deliveryOrder.getId() + " is: " + deliveryOrder.getStatus());
        }

        // 2. Tạo Receipt
        String receiptNumber = generateReceiptNumber();
        Receipt receipt = Receipt.builder()
                .receiptNumber(receiptNumber)
                .sourceOrderCode(deliveryOrder.getDoNumber())
                .deliveryOrder(deliveryOrder)
                .type(ReceiptType.RETURN)
                .warehouse(warehouse)
                .dealer(dealer)
                .contactPerson(request.getContactPerson())
                .status(ReceiptStatus.DRAFT)
                .documentDate(docDate)
                .accountingPeriod(period)
                .createdBy(actor)
                .notes(request.getNotes())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .version(0)
                .build();
        receiptRepository.save(receipt);

        // 3. Tạo items
        for (ReturnItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + itemReq.getProductId()));

            ReceiptItem item = ReceiptItem.builder()
                    .receipt(receipt)
                    .product(product)
                    .expectedQty(itemReq.getExpectedQty().intValue())
                    .overReceivedQty(0)
                    .build();
            receiptItemRepository.save(item);
        }

        auditLogService.log(
                actor, AuditAction.CREATE, RECEIPT_ENTITY,
                receipt.getId(), receiptNumber,
                warehouse.getId(),
                null,
                Map.of("type", ReceiptType.RETURN.name(), "status", ReceiptStatus.DRAFT.name())
        );

        log.info("Return receipt {} drafted by storekeeper {} for DO {}.",
                receiptNumber, actor.getId(), deliveryOrder.getDoNumber());

        return receipt;
    }

    @Transactional
    public Receipt submitQc(Long receiptId, ReturnQcRequest request, User actor) {
        assertRole(actor, UserRole.STOREKEEPER, "RETURN_QC_SUBMIT");
        assertWarehouseAccess(actor, loadReceiptWarehouseId(receiptId));

        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Return receipt not found: " + receiptId));

        if (!receipt.getVersion().equals(request.getExpectedVersion())) {
            throw new BusinessRuleViolationException(
                    "INVENTORY_VERSION_CONFLICT: Receipt " + receiptId + " has been modified.");
        }

        if (receipt.getStatus() != ReceiptStatus.DRAFT && receipt.getStatus() != ReceiptStatus.PENDING_RECEIPT) {
            throw new BusinessRuleViolationException(
                    "INVALID_STATUS: QC can only be submitted for DRAFT or PENDING_RECEIPT returns.");
        }

        DeliveryOrder doOrder = receipt.getDeliveryOrder();
        List<ReceiptItem> dbItems = receiptItemRepository.findByReceiptId(receiptId);

        for (ReturnQcItemRequest itemReq : request.getItems()) {
            ReceiptItem dbItem = dbItems.stream()
                    .filter(i -> i.getId().equals(itemReq.getReceiptItemId()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Receipt item not found: " + itemReq.getReceiptItemId()));

            BigDecimal actual = itemReq.getActualQty();
            BigDecimal passed = itemReq.getQcPassedQty();
            BigDecimal failed = itemReq.getQcFailedQty();

            if (passed.add(failed).compareTo(actual) != 0) {
                throw new BusinessRuleViolationException(
                        "INVALID_QC_QUANTITIES: Passed + Failed quantities must equal Actual quantity.");
            }

            // Kiểm tra giới hạn số lượng trả từ DO gốc
            BigDecimal originalIssued = deliveryOrderItemRepository.findByDeliveryOrderId(doOrder.getId()).stream()
                    .filter(doi -> doi.getProduct().getId().equals(dbItem.getProduct().getId()))
                    .map(doi -> doi.getIssuedQty())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (actual.compareTo(originalIssued) > 0) {
                throw new BusinessRuleViolationException(
                        "RETURN_EXCEEDS_ORIGINAL_SALE: Returned quantity " + actual
                        + " exceeds original issued quantity " + originalIssued + " in DO " + doOrder.getDoNumber() + ".");
            }

            dbItem.setActualQty(actual.intValue());
            dbItem.setSamplePassedQty(passed.intValue());
            dbItem.setSampleFailedQty(failed.intValue());
            dbItem.setQcFailureReason(itemReq.getQcFailureReason());
            dbItem.setQcBy(actor);

            if (failed.compareTo(BigDecimal.ZERO) == 0) {
                dbItem.setQcResult(QcResult.PASSED);
            } else if (passed.compareTo(BigDecimal.ZERO) == 0) {
                dbItem.setQcResult(QcResult.FAILED);
            } else {
                dbItem.setQcResult(QcResult.PARTIAL);
            }

            receiptItemRepository.save(dbItem);
        }

        receipt.setStatus(ReceiptStatus.QC_COMPLETED);
        receipt.setUpdatedAt(OffsetDateTime.now());
        receiptRepository.save(receipt);

        auditLogService.log(
                actor, AuditAction.RECEIPT_QC_CONFIRM, RECEIPT_ENTITY,
                receipt.getId(), receipt.getReceiptNumber(),
                receipt.getWarehouse().getId(),
                Map.of("status", ReceiptStatus.DRAFT.name()),
                Map.of("status", ReceiptStatus.QC_COMPLETED.name())
        );

        log.info("Return receipt {} QC counts submitted by storekeeper {}.", receipt.getReceiptNumber(), actor.getId());
        return receipt;
    }

    @Transactional
    public Receipt approveReturn(Long receiptId, ReceiptDecisionRequest request, User actor) {
        assertRole(actor, UserRole.WAREHOUSE_MANAGER, "RETURN_APPROVE");
        assertWarehouseAccess(actor, loadReceiptWarehouseId(receiptId));

        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Return receipt not found: " + receiptId));

        if (!receipt.getVersion().equals(request.getExpectedVersion())) {
            throw new BusinessRuleViolationException("INVENTORY_VERSION_CONFLICT");
        }

        if (receipt.getStatus() != ReceiptStatus.QC_COMPLETED) {
            throw new BusinessRuleViolationException(
                    "INVALID_STATUS: Return must be QC_COMPLETED before approval.");
        }

        List<ReceiptItem> items = receiptItemRepository.findByReceiptId(receiptId);
        for (ReceiptItem item : items) {
            Batch batch = resolveOrCreateReturnBatch(item, receipt);
            item.setBatch(batch);
            receiptItemRepository.save(item);
        }

        receipt.setStatus(ReceiptStatus.APPROVED);
        receipt.setApprovedBy(actor);
        receipt.setApprovedAt(OffsetDateTime.now());
        receipt.setUpdatedAt(OffsetDateTime.now());
        receiptRepository.save(receipt);

        auditLogService.log(
                actor, AuditAction.RECEIPT_APPROVE, RECEIPT_ENTITY,
                receipt.getId(), receipt.getReceiptNumber(),
                receipt.getWarehouse().getId(),
                Map.of("status", ReceiptStatus.QC_COMPLETED.name()),
                Map.of("status", ReceiptStatus.APPROVED.name())
        );

        log.info("Return receipt {} approved by warehouse manager {}.", receipt.getReceiptNumber(), actor.getId());
        return receipt;
    }

    @Transactional
    public Receipt completePutaway(Long receiptId, ReturnPutawayRequest request, User actor) {
        assertRole(actor, UserRole.STOREKEEPER, "RETURN_PUTAWAY_COMPLETE");
        assertWarehouseAccess(actor, loadReceiptWarehouseId(receiptId));

        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Return receipt not found: " + receiptId));

        if (!receipt.getVersion().equals(request.getExpectedVersion())) {
            throw new BusinessRuleViolationException("INVENTORY_VERSION_CONFLICT");
        }

        if (receipt.getStatus() != ReceiptStatus.APPROVED) {
            throw new BusinessRuleViolationException(
                    "INVALID_STATUS: Return must be APPROVED before putaway.");
        }

        List<ReceiptItem> items = receiptItemRepository.findByReceiptId(receiptId);

        for (ReturnPutawayItemRequest putawayReq : request.getPutawayItems()) {
            ReceiptItem item = items.stream()
                    .filter(i -> i.getId().equals(putawayReq.getReceiptItemId()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Item not found: " + putawayReq.getReceiptItemId()));

            WarehouseLocation passedLoc = warehouseLocationRepository.findById(putawayReq.getPassedLocationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Location not found: " + putawayReq.getPassedLocationId()));
            WarehouseLocation failedLoc = warehouseLocationRepository.findById(putawayReq.getFailedLocationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Location not found: " + putawayReq.getFailedLocationId()));

            if (Boolean.TRUE.equals(passedLoc.getIsQuarantine())) {
                throw new BusinessRuleViolationException("INVALID_LOCATION: Passed target must be a regular Bin.");
            }
            if (!Boolean.TRUE.equals(failedLoc.getIsQuarantine())) {
                throw new BusinessRuleViolationException("INVALID_LOCATION: Failed target must be a quarantine Bin.");
            }

            // Tăng tồn kho Đạt (Regular)
            if (item.getSamplePassedQty() > 0) {
                increaseInventory(receipt, item, passedLoc, BigDecimal.valueOf(item.getSamplePassedQty()), actor, "RETURN_PASSED");
            }

            // Tăng tồn kho Lỗi (Quarantine)
            if (item.getSampleFailedQty() > 0) {
                increaseInventory(receipt, item, failedLoc, BigDecimal.valueOf(item.getSampleFailedQty()), actor, "RETURN_FAILED");
            }

            item.setLocation(passedLoc); // Set primary location to passedLoc
            receiptItemRepository.save(item);
        }

        receipt.setUpdatedAt(OffsetDateTime.now());
        receiptRepository.save(receipt);

        auditLogService.log(
                actor, AuditAction.RECEIPT_PUTAWAY_COMPLETE, RECEIPT_ENTITY,
                receipt.getId(), receipt.getReceiptNumber(),
                receipt.getWarehouse().getId(),
                Map.of("status", ReceiptStatus.APPROVED.name()),
                Map.of("putawayCompleted", true)
        );

        log.info("Return receipt {} putaway completed by storekeeper {}.", receipt.getReceiptNumber(), actor.getId());
        return receipt;
    }

    @Transactional
    public ReturnCreditNoteResponse createCreditNote(Long receiptId, ReturnCreditNoteRequest request, User actor) {
        assertRole(actor, UserRole.ACCOUNTANT, "CREDIT_NOTE_CREATE");

        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Return receipt not found: " + receiptId));

        if (!receipt.getVersion().equals(request.getExpectedVersion())) {
            throw new BusinessRuleViolationException("INVENTORY_VERSION_CONFLICT");
        }

        if (receipt.getStatus() != ReceiptStatus.APPROVED) {
            throw new BusinessRuleViolationException("INVALID_STATUS: Return receipt must be APPROVED.");
        }

        if (creditNoteRepository.existsByReceiptId(receiptId)) {
            throw new BusinessRuleViolationException(
                    "CREDIT_NOTE_ALREADY_EXISTS: A credit note already exists for return receipt " + receiptId + ".");
        }

        LocalDate docDate = request.getDocumentDate() != null ? request.getDocumentDate() : LocalDate.now();

        // Kiểm tra kỳ kế toán
        AccountingPeriod period = accountingPeriodRepository.findPeriodByDateAndStatus(docDate, AccountingPeriodStatus.OPEN)
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "PERIOD_CLOSED: No open accounting period found for date: " + docDate));
        if (period.getStatus() != AccountingPeriodStatus.OPEN) {
            throw new BusinessRuleViolationException("PERIOD_CLOSED: Accounting period is closed.");
        }

        // Tính tổng tiền hoàn (dựa trên actualQty * unitPrice của DO)
        BigDecimal totalAmount = BigDecimal.ZERO;
        DeliveryOrder doOrder = receipt.getDeliveryOrder();
        List<ReceiptItem> items = receiptItemRepository.findByReceiptId(receiptId);

        for (ReceiptItem item : items) {
            BigDecimal qty = item.getActualQty() != null ? BigDecimal.valueOf(item.getActualQty()) : BigDecimal.ZERO;
            BigDecimal unitPrice = deliveryOrderItemRepository.findByDeliveryOrderId(doOrder.getId()).stream()
                    .filter(doi -> doi.getProduct().getId().equals(item.getProduct().getId()))
                    .map(doi -> doi.getUnitPrice())
                    .findFirst()
                    .orElse(BigDecimal.ZERO);

            totalAmount = totalAmount.add(qty.multiply(unitPrice));
        }

        // Tạo Credit Note
        String cnNumber = generateCreditNoteNumber();
        CreditNote creditNote = CreditNote.builder()
                .creditNoteNumber(cnNumber)
                .dealer(receipt.getDealer())
                .receipt(receipt)
                .amount(totalAmount)
                .reason(request.getReason())
                .createdBy(actor)
                .documentDate(docDate)
                .accountingPeriod(period)
                .createdAt(OffsetDateTime.now())
                .build();
        creditNoteRepository.save(creditNote);

        // Cập nhật công nợ đại lý: Giảm dư nợ
        Dealer dealer = receipt.getDealer();
        BigDecimal oldBalance = dealer.getCurrentBalance();
        dealer.setCurrentBalance(oldBalance.subtract(totalAmount));
        dealerRepository.save(dealer);

        auditLogService.log(
                actor, AuditAction.CREDIT_NOTE_CREATE, CREDIT_NOTE_ENTITY,
                creditNote.getId(), cnNumber,
                receipt.getWarehouse().getId(),
                Map.of("oldBalance", oldBalance),
                Map.of("newBalance", dealer.getCurrentBalance(),
                       "amount", totalAmount,
                       "receiptId", receiptId)
        );

        log.info("Credit Note {} created for return {} cấn trừ công nợ đại lý {} by {}.",
                cnNumber, receipt.getReceiptNumber(), dealer.getCode(), actor.getId());

        return ReturnCreditNoteResponse.builder()
                .creditNoteId(creditNote.getId())
                .creditNoteNumber(cnNumber)
                .amount(totalAmount)
                .dealerId(dealer.getId())
                .message("Credit Note generated successfully. Dealer balance reduced.")
                .build();
    }

    private void increaseInventory(Receipt receipt, ReceiptItem item, WarehouseLocation location,
                                   BigDecimal qty, User actor, String reason) {
        if (qty.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        Long warehouseId = receipt.getWarehouse().getId();
        Long productId = item.getProduct().getId();
        Long batchId = item.getBatch().getId();
        Long locationId = location.getId();

        Inventory inventory = inventoryRepository
                .findByWarehouseProductBatchLocationForUpdate(warehouseId, productId, batchId, locationId)
                .orElseGet(() -> Inventory.builder()
                        .warehouse(receipt.getWarehouse())
                        .product(item.getProduct())
                        .batch(item.getBatch())
                        .location(location)
                        .totalQty(BigDecimal.ZERO)
                        .reservedQty(BigDecimal.ZERO)
                        .costPrice(item.getUnitCost() != null ? item.getUnitCost() : BigDecimal.ZERO)
                        .updatedAt(OffsetDateTime.now())
                        .build());

        BigDecimal oldQty = inventory.getTotalQty();
        inventory.setTotalQty(oldQty.add(qty));
        inventory.setUpdatedAt(OffsetDateTime.now());
        inventoryRepository.save(inventory);

        auditLogService.log(
                actor, AuditAction.INVENTORY_UPDATE, INVENTORY_ENTITY,
                inventory.getId(),
                "INV-" + warehouseId + "-" + productId,
                warehouseId,
                Map.of("totalQty", oldQty, "reservedQty", inventory.getReservedQty()),
                Map.of("totalQty", inventory.getTotalQty(), "reservedQty", inventory.getReservedQty(),
                       "locationId", locationId, "delta", qty, "reason", reason)
        );
    }

    private Batch resolveOrCreateReturnBatch(ReceiptItem item, Receipt receipt) {
        Long productId = item.getProduct().getId();
        Long warehouseId = receipt.getWarehouse().getId();
        LocalDate receivedDate = receipt.getDocumentDate();

        return batchRepository
                .findByProductWarehouseAndReceivedDate(productId, warehouseId, receivedDate)
                .orElseGet(() -> {
                    String batchNumber = String.format("BCH-RET-%d-%s-%s",
                            productId,
                            receipt.getReceiptNumber(),
                            receivedDate.toString());
                    Batch newBatch = Batch.builder()
                            .batchNumber(batchNumber)
                            .product(item.getProduct())
                            .warehouse(receipt.getWarehouse())
                            .receivedDate(receivedDate)
                            .quantity(BigDecimal.valueOf(item.getActualQty()))
                            .createdAt(OffsetDateTime.now())
                            .build();
                    return batchRepository.save(newBatch);
                });
    }

    private Long loadReceiptWarehouseId(Long receiptId) {
        return receiptRepository.findById(receiptId)
                .map(r -> r.getWarehouse().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found: " + receiptId));
    }

    private void assertWarehouseAccess(User actor, Long warehouseId) {
        if (actor.getRole() == UserRole.ADMIN || actor.getRole() == UserRole.CEO) {
            return;
        }
        List<Long> assignedWarehouseIds = userWarehouseAssignmentRepository
                .findWarehouseIdsByUserId(actor.getId());
        if (!assignedWarehouseIds.contains(warehouseId)) {
            throw new ForbiddenReceiptWarehouseException(
                    "FORBIDDEN_WAREHOUSE_ACCESS: User " + actor.getId()
                    + " is not assigned to warehouse " + warehouseId);
        }
    }

    private void assertRole(User actor, UserRole requiredRole, String action) {
        if (actor == null) {
            throw new ForbiddenReceiptWarehouseException("FORBIDDEN_ROLE: actor is null");
        }
        if (actor.getRole() == UserRole.ADMIN || actor.getRole() == UserRole.CEO) {
            return;
        }

        boolean isAuthorized = false;
        if (requiredRole == UserRole.STOREKEEPER) {
            isAuthorized = actor.getRole() == UserRole.STOREKEEPER
                    || actor.getRole() == UserRole.WAREHOUSE_MANAGER;
        } else if (requiredRole == UserRole.WAREHOUSE_MANAGER) {
            isAuthorized = actor.getRole() == UserRole.WAREHOUSE_MANAGER;
        } else {
            isAuthorized = actor.getRole() == requiredRole;
        }

        if (!isAuthorized) {
            throw new ForbiddenReceiptWarehouseException(
                    "FORBIDDEN_ROLE: " + action + " requires role " + requiredRole);
        }
    }

    private String generateReceiptNumber() {
        return "RET-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
               + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private String generateCreditNoteNumber() {
        return "CN-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
               + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
