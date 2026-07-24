package com.wms.service.return_disposal;
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

import com.wms.dto.request.*;
import com.wms.dto.response.CreditNoteResponse;
import com.wms.dto.response.ReceiptActionResponse;
import com.wms.exception.BusinessRuleViolationException;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.*;
import com.wms.repository.dealer_management.DealerRepository;
import com.wms.service.audit_trail.AuditLogService;
import com.wms.service.billing_payment.AccountingPeriodService;
import com.wms.service.stock_receiving.ReceiptValidationService;
import com.wms.service.user_configuration.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReturnsService {

    private static final Logger log = LoggerFactory.getLogger(ReturnsService.class);

    private static final String RECEIPT_ENTITY = "RECEIPT";
    private static final String INVENTORY_ENTITY = "INVENTORY";
    private static final String DEALER_ENTITY = "DEALER";
    private static final String CREDIT_NOTE_ENTITY = "CREDIT_NOTE";

    private final ReceiptRepository receiptRepository;
    private final ReceiptItemRepository receiptItemRepository;
    private final DeliveryOrderRepository deliveryOrderRepository;
    private final DeliveryOrderItemRepository deliveryOrderItemRepository;
    private final DealerRepository dealerRepository;
    private final WarehouseLocationRepository warehouseLocationRepository;
    private final InventoryRepository inventoryRepository;
    private final CreditNoteRepository creditNoteRepository;
    private final ReceiptValidationService receiptValidationService;
    private final AuditLogService auditLogService;
    private final AccountingPeriodService accountingPeriodService;
    private final SystemConfigService systemConfigService;

    public ReturnsService(ReceiptRepository receiptRepository,
                          ReceiptItemRepository receiptItemRepository,
                          DeliveryOrderRepository deliveryOrderRepository,
                          DeliveryOrderItemRepository deliveryOrderItemRepository,
                          DealerRepository dealerRepository,
                          WarehouseLocationRepository warehouseLocationRepository,
                          InventoryRepository inventoryRepository,
                          CreditNoteRepository creditNoteRepository,
                          ReceiptValidationService receiptValidationService,
                          AuditLogService auditLogService,
                          AccountingPeriodService accountingPeriodService,
                          SystemConfigService systemConfigService) {
        this.receiptRepository = receiptRepository;
        this.receiptItemRepository = receiptItemRepository;
        this.deliveryOrderRepository = deliveryOrderRepository;
        this.deliveryOrderItemRepository = deliveryOrderItemRepository;
        this.dealerRepository = dealerRepository;
        this.warehouseLocationRepository = warehouseLocationRepository;
        this.inventoryRepository = inventoryRepository;
        this.creditNoteRepository = creditNoteRepository;
        this.receiptValidationService = receiptValidationService;
        this.auditLogService = auditLogService;
        this.accountingPeriodService = accountingPeriodService;
        this.systemConfigService = systemConfigService;
    }

    @Transactional
    public ReceiptActionResponse createReturnReceipt(CreateReturnRequest request, User actor) {
        receiptValidationService.assertRole(actor, UserRole.WAREHOUSE_STAFF, "RETURN_RECEIPT_CREATE");
        receiptValidationService.assertWarehouseAccess(actor, request.getWarehouseId());

        DeliveryOrder doOrder = deliveryOrderRepository.findById(request.getDeliveryOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Delivery Order not found: " + request.getDeliveryOrderId()));

        if (!doOrder.getDealer().getId().equals(request.getDealerId())) {
            throw new BusinessRuleViolationException(
                    "DEALER_MISMATCH: Returned dealer does not match original Delivery Order dealer");
        }

        if (doOrder.getStatus() != DeliveryOrderStatus.DELIVERED && doOrder.getStatus() != DeliveryOrderStatus.COMPLETED) {
            throw new BusinessRuleViolationException(
                    "INVALID_DO_STATUS: Returns are only allowed for DELIVERED or COMPLETED Delivery Orders. Current status: " + doOrder.getStatus());
        }

        List<DeliveryOrderItem> doItems = deliveryOrderItemRepository.findByDeliveryOrderId(doOrder.getId());
        List<Receipt> existingReturns = receiptRepository.findByDeliveryOrderIdAndType(doOrder.getId(), ReceiptType.RETURN);

        List<ReceiptItem> receiptItemsToSave = new ArrayList<>();

        // Validate each returned product against DO sales limits
        for (CreateReturnItemRequest itemReq : request.getItems()) {
            final Long reqProductId = itemReq.getProductId();
            DeliveryOrderItem matchingDoItem = doItems.stream()
                    .filter(doi -> doi.getProduct().getId().equals(reqProductId))
                    .findFirst()
                    .orElseThrow(() -> new BusinessRuleViolationException(
                            "PRODUCT_NOT_IN_DO: Product ID " + reqProductId + " is not in the original Delivery Order"));

            BigDecimal previouslyReturnedQty = BigDecimal.ZERO;
            for (Receipt returnReceipt : existingReturns) {
                List<ReceiptItem> items = receiptItemRepository.findByReceiptId(returnReceipt.getId());
                BigDecimal sum = items.stream()
                        .filter(ri -> ri.getProduct().getId().equals(reqProductId))
                        .map(ri -> BigDecimal.valueOf(ri.getExpectedQty()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                previouslyReturnedQty = previouslyReturnedQty.add(sum);
            }

            BigDecimal currentReturnQty = BigDecimal.valueOf(itemReq.getExpectedQty());
            BigDecimal totalReturned = previouslyReturnedQty.add(currentReturnQty);
            BigDecimal issuedQty = matchingDoItem.getIssuedQty();

            if (totalReturned.compareTo(issuedQty) > 0) {
                throw new BusinessRuleViolationException(
                        "RETURN_EXCEEDS_ORIGINAL_SALE: Total returned quantity (" + totalReturned 
                        + ") exceeds original sales quantity (" + issuedQty + ") for product ID " + reqProductId);
            }

            ReceiptItem receiptItem = ReceiptItem.builder()
                    .product(matchingDoItem.getProduct())
                    .batch(matchingDoItem.getBatch()) // Retain original batch for FIFO tracking
                    .expectedQty(itemReq.getExpectedQty())
                    .unitCost(matchingDoItem.getUnitPrice()) // Use original unit price for credit calculation
                    .qcResult(QcResult.PENDING)
                    .overReceivedQty(0)
                    .build();

            receiptItemsToSave.add(receiptItem);
        }

        // Generate Return Receipt Number
        String receiptNumber = "REC-RET-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Receipt receipt = Receipt.builder()
                .receiptNumber(receiptNumber)
                .sourceOrderCode(doOrder.getDoNumber())
                .type(ReceiptType.RETURN)
                .warehouse(doOrder.getWarehouse())
                .dealer(doOrder.getDealer())
                .deliveryOrder(doOrder)
                .status(ReceiptStatus.DRAFT)
                .documentDate(LocalDate.now())
                .accountingPeriod(doOrder.getAccountingPeriod())
                .createdBy(actor)
                .notes(request.getNotes())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .version(1)
                .build();

        receipt = receiptRepository.save(receipt);

        for (ReceiptItem item : receiptItemsToSave) {
            item.setReceipt(receipt);
            receiptItemRepository.save(item);
        }

        auditLogService.log(
                actor, AuditAction.RECEIPT_CREATE, RECEIPT_ENTITY,
                receipt.getId(), receipt.getReceiptNumber(),
                receipt.getWarehouse().getId(),
                Map.of(),
                Map.of("type", ReceiptType.RETURN.name(), "status", ReceiptStatus.DRAFT.name())
        );

        return ReceiptActionResponse.builder()
                .id(receipt.getId())
                .receiptNumber(receipt.getReceiptNumber())
                .status(receipt.getStatus())
                .version(receipt.getVersion())
                .updatedAt(receipt.getUpdatedAt())
                .message("Tạo phiếu trả hàng thành công")
                .build();
    }

    @Transactional
    public ReceiptActionResponse processReturnQc(Long receiptId, ReturnQcRequest request, User actor) {
        receiptValidationService.assertRole(actor, UserRole.WAREHOUSE_STAFF, "RETURN_QC");
        
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found: " + receiptId));

        if (receipt.getType() != ReceiptType.RETURN) {
            throw new BusinessRuleViolationException("INVALID_RECEIPT_TYPE: Receipt is not a RETURN receipt");
        }

        if (receipt.getStatus() != ReceiptStatus.DRAFT) {
            throw new BusinessRuleViolationException(
                    "INVALID_STATE: QC processing can only be executed in DRAFT state. Current status: " + receipt.getStatus());
        }

        receiptValidationService.assertWarehouseAccess(actor, receipt.getWarehouse().getId());
        receiptValidationService.assertVersionMatch(receipt, request.getExpectedVersion());

        List<DeliveryOrderItem> doItems = deliveryOrderItemRepository.findByDeliveryOrderId(receipt.getDeliveryOrder().getId());

        for (ReturnQcItemRequest itemReq : request.getItems()) {
            final ReceiptItem item = receiptItemRepository.findById(itemReq.getReceiptItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Receipt item not found: " + itemReq.getReceiptItemId()));

            if (!item.getReceipt().getId().equals(receiptId)) {
                throw new BusinessRuleViolationException("ITEM_RECEIPT_MISMATCH: Item does not belong to receipt " + receiptId);
            }

            if (itemReq.getPassedQty() + itemReq.getFailedQty() != itemReq.getActualQty()) {
                throw new BusinessRuleViolationException("QC_SAMPLE_SUM_MISMATCH: passedQty + failedQty must equal actualQty");
            }

            final Long productId = item.getProduct().getId();
            final DeliveryOrderItem matchingDoItem = doItems.stream()
                    .filter(doi -> doi.getProduct().getId().equals(productId))
                    .findFirst()
                    .orElseThrow(() -> new BusinessRuleViolationException("PRODUCT_NOT_IN_DO"));

            // 1. Process Passed Qty (Increase Regular Inventory)
            if (itemReq.getPassedQty() > 0) {
                WarehouseLocation passedLoc = warehouseLocationRepository.findById(itemReq.getPassedLocationId())
                        .orElseThrow(() -> new ResourceNotFoundException("Location not found: " + itemReq.getPassedLocationId()));

                if (Boolean.TRUE.equals(passedLoc.getIsQuarantine())) {
                    throw new BusinessRuleViolationException(
                            "INVALID_LOCATION: Passed target must be a regular (non-quarantine) Bin. Location: " + itemReq.getPassedLocationId());
                }

                // Assert capacity
                BigDecimal addedVol = BigDecimal.valueOf(itemReq.getPassedQty()).multiply(
                        item.getProduct().getVolumeM3() != null ? item.getProduct().getVolumeM3() : BigDecimal.ZERO);
                BigDecimal addedWt = BigDecimal.valueOf(itemReq.getPassedQty()).multiply(
                        item.getProduct().getWeightKg() != null ? item.getProduct().getWeightKg() : BigDecimal.ZERO);

                BigDecimal curVol = passedLoc.getCurrentVolumeM3() != null ? passedLoc.getCurrentVolumeM3() : BigDecimal.ZERO;
                BigDecimal curWt = passedLoc.getCurrentWeightKg() != null ? passedLoc.getCurrentWeightKg() : BigDecimal.ZERO;

                if (passedLoc.getCapacityM3() != null && curVol.add(addedVol).compareTo(passedLoc.getCapacityM3()) > 0) {
                    throw new BusinessRuleViolationException("BIN_CAPACITY_EXCEEDED: Passed volume exceeds location capacity");
                }
                if (passedLoc.getCapacityKg() != null && curWt.add(addedWt).compareTo(passedLoc.getCapacityKg()) > 0) {
                    throw new BusinessRuleViolationException("BIN_CAPACITY_EXCEEDED: Passed weight exceeds location capacity");
                }

                final Warehouse finalWarehouse = receipt.getWarehouse();
                final Product finalProduct = item.getProduct();
                final Batch finalBatch = item.getBatch();
                final WarehouseLocation finalPassedLoc = passedLoc;
                final BigDecimal finalUnitCost = matchingDoItem.getUnitCost();

                // Update inventory
                Inventory inventory = inventoryRepository
                        .findByWarehouseProductBatchLocationForUpdate(
                                finalWarehouse.getId(),
                                finalProduct.getId(),
                                finalBatch.getId(),
                                finalPassedLoc.getId())
                        .orElseGet(() -> Inventory.builder()
                                .warehouse(finalWarehouse)
                                .product(finalProduct)
                                .batch(finalBatch)
                                .location(finalPassedLoc)
                                .totalQty(BigDecimal.ZERO)
                                .reservedQty(BigDecimal.ZERO)
                                .costPrice(finalUnitCost != null ? finalUnitCost : BigDecimal.ZERO) // keep original cost price
                                .updatedAt(OffsetDateTime.now())
                                .build());

                BigDecimal oldQty = inventory.getTotalQty();
                inventory.setTotalQty(oldQty.add(BigDecimal.valueOf(itemReq.getPassedQty())));
                inventory.setUpdatedAt(OffsetDateTime.now());
                inventoryRepository.save(inventory);

                // Update location occupancy
                passedLoc.setCurrentVolumeM3(curVol.add(addedVol));
                passedLoc.setCurrentWeightKg(curWt.add(addedWt));
                passedLoc.setUpdatedAt(OffsetDateTime.now());
                warehouseLocationRepository.save(passedLoc);

                // Audit Log
                auditLogService.log(
                        actor, AuditAction.INVENTORY_UPDATE, INVENTORY_ENTITY,
                        inventory.getId(), "INV-" + receipt.getWarehouse().getId() + "-" + item.getProduct().getId(),
                        receipt.getWarehouse().getId(),
                        Map.of("totalQty", oldQty),
                        Map.of("totalQty", inventory.getTotalQty(), "locationId", passedLoc.getId(), "delta", itemReq.getPassedQty())
                );
            }

            // 2. Process Failed Qty (Increase Quarantine Inventory)
            if (itemReq.getFailedQty() > 0) {
                WarehouseLocation quarantineLoc = warehouseLocationRepository.findById(itemReq.getQuarantineLocationId())
                        .orElseThrow(() -> new ResourceNotFoundException("Location not found: " + itemReq.getQuarantineLocationId()));

                if (!Boolean.TRUE.equals(quarantineLoc.getIsQuarantine())) {
                    throw new BusinessRuleViolationException(
                            "INVALID_LOCATION: Failed target must be a quarantine location. Location: " + itemReq.getQuarantineLocationId());
                }

                // Assert capacity
                BigDecimal addedVol = BigDecimal.valueOf(itemReq.getFailedQty()).multiply(
                        item.getProduct().getVolumeM3() != null ? item.getProduct().getVolumeM3() : BigDecimal.ZERO);
                BigDecimal addedWt = BigDecimal.valueOf(itemReq.getFailedQty()).multiply(
                        item.getProduct().getWeightKg() != null ? item.getProduct().getWeightKg() : BigDecimal.ZERO);

                BigDecimal curVol = quarantineLoc.getCurrentVolumeM3() != null ? quarantineLoc.getCurrentVolumeM3() : BigDecimal.ZERO;
                BigDecimal curWt = quarantineLoc.getCurrentWeightKg() != null ? quarantineLoc.getCurrentWeightKg() : BigDecimal.ZERO;

                if (quarantineLoc.getCapacityM3() != null && curVol.add(addedVol).compareTo(quarantineLoc.getCapacityM3()) > 0) {
                    throw new BusinessRuleViolationException("BIN_CAPACITY_EXCEEDED: Quarantine volume exceeds location capacity");
                }
                if (quarantineLoc.getCapacityKg() != null && curWt.add(addedWt).compareTo(quarantineLoc.getCapacityKg()) > 0) {
                    throw new BusinessRuleViolationException("BIN_CAPACITY_EXCEEDED: Quarantine weight exceeds location capacity");
                }

                final Warehouse finalWarehouse = receipt.getWarehouse();
                final Product finalProduct = item.getProduct();
                final Batch finalBatch = item.getBatch();
                final WarehouseLocation finalQuarantineLoc = quarantineLoc;
                final BigDecimal finalUnitCost = matchingDoItem.getUnitCost();

                // Update inventory
                Inventory inventory = inventoryRepository
                        .findByWarehouseProductBatchLocationForUpdate(
                                finalWarehouse.getId(),
                                finalProduct.getId(),
                                finalBatch.getId(),
                                finalQuarantineLoc.getId())
                        .orElseGet(() -> Inventory.builder()
                                .warehouse(finalWarehouse)
                                .product(finalProduct)
                                .batch(finalBatch)
                                .location(finalQuarantineLoc)
                                .totalQty(BigDecimal.ZERO)
                                .reservedQty(BigDecimal.ZERO)
                                .costPrice(finalUnitCost != null ? finalUnitCost : BigDecimal.ZERO) // keep original cost price
                                .updatedAt(OffsetDateTime.now())
                                .build());

                BigDecimal oldQty = inventory.getTotalQty();
                inventory.setTotalQty(oldQty.add(BigDecimal.valueOf(itemReq.getFailedQty())));
                inventory.setUpdatedAt(OffsetDateTime.now());
                inventoryRepository.save(inventory);

                // Update location occupancy
                quarantineLoc.setCurrentVolumeM3(curVol.add(addedVol));
                quarantineLoc.setCurrentWeightKg(curWt.add(addedWt));
                quarantineLoc.setUpdatedAt(OffsetDateTime.now());
                warehouseLocationRepository.save(quarantineLoc);

                // Audit Log
                auditLogService.log(
                        actor, AuditAction.INVENTORY_UPDATE, INVENTORY_ENTITY,
                        inventory.getId(), "INV-" + receipt.getWarehouse().getId() + "-" + item.getProduct().getId(),
                        receipt.getWarehouse().getId(),
                        Map.of("totalQty", oldQty),
                        Map.of("totalQty", inventory.getTotalQty(), "locationId", quarantineLoc.getId(), "delta", itemReq.getFailedQty())
                );
            }

            // 3. Update ReceiptItem QC status
            item.setActualQty(itemReq.getActualQty());
            item.setSampleQty(itemReq.getActualQty());
            item.setSamplePassedQty(itemReq.getPassedQty());
            item.setSampleFailedQty(itemReq.getFailedQty());
            item.setQcResult(itemReq.getFailedQty() > 0 ? QcResult.FAILED : QcResult.PASSED);
            item.setLocation(warehouseLocationRepository.findById(itemReq.getPassedLocationId()).orElse(null));
            item.setQcBy(actor);
            receiptItemRepository.save(item);
        }

        // Set receipt status directly to APPROVED
        receipt.setStatus(ReceiptStatus.APPROVED);
        receipt.setApprovedBy(actor);
        receipt.setApprovedAt(OffsetDateTime.now());
        receipt.setUpdatedAt(OffsetDateTime.now());
        receipt = receiptRepository.save(receipt);

        auditLogService.log(
                actor, AuditAction.RECEIPT_APPROVE, RECEIPT_ENTITY,
                receipt.getId(), receipt.getReceiptNumber(),
                receipt.getWarehouse().getId(),
                Map.of("status", ReceiptStatus.DRAFT.name()),
                Map.of("status", ReceiptStatus.APPROVED.name(), "approvedBy", actor.getId(), "approvedAt", receipt.getApprovedAt().toString())
        );

        return ReceiptActionResponse.builder()
                .id(receipt.getId())
                .receiptNumber(receipt.getReceiptNumber())
                .status(receipt.getStatus())
                .version(receipt.getVersion())
                .updatedAt(receipt.getUpdatedAt())
                .message("Đã hoàn tất phân tách QC và duyệt nhập kho hàng trả")
                .build();
    }

    @Transactional
    public CreditNoteResponse createCreditNote(Long receiptId, CreateCreditNoteRequest request, User actor) {
        // Assert role WAREHOUSE_MANAGER, ACCOUNTANT (includes ACCOUNTANT_MANAGER, CEO, ADMIN)
        if (actor.getRole() != UserRole.ACCOUNTANT && actor.getRole() != UserRole.ACCOUNTANT_MANAGER 
                && actor.getRole() != UserRole.WAREHOUSE_MANAGER && actor.getRole() != UserRole.CEO && actor.getRole() != UserRole.ADMIN) {
            throw new BusinessRuleViolationException("FORBIDDEN_CREDIT_NOTE_ROLE: User is not authorized to generate credit notes");
        }

        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found: " + receiptId));

        if (receipt.getType() != ReceiptType.RETURN) {
            throw new BusinessRuleViolationException("INVALID_RECEIPT_TYPE: Credit note can only be created for RETURN receipts");
        }

        if (receipt.getStatus() != ReceiptStatus.APPROVED) {
            throw new BusinessRuleViolationException("INVALID_STATE: Credit note requires APPROVED return receipt status");
        }

        // Check if credit note already generated
        if (creditNoteRepository.findByReceiptId(receiptId).isPresent()) {
            throw new BusinessRuleViolationException("CREDIT_NOTE_ALREADY_EXISTS: Credit note has already been generated for this receipt");
        }

        receiptValidationService.assertWarehouseAccess(actor, receipt.getWarehouse().getId());

        List<ReceiptItem> items = receiptItemRepository.findByReceiptId(receiptId);
        BigDecimal totalRefundAmount = BigDecimal.ZERO;

        for (ReceiptItem item : items) {
            BigDecimal qty = item.getActualQty() != null ? BigDecimal.valueOf(item.getActualQty()) : BigDecimal.ZERO;
            BigDecimal returnPrice = item.getUnitCost() != null ? item.getUnitCost() : BigDecimal.ZERO; // unitCost contains the original DO unitPrice
            totalRefundAmount = totalRefundAmount.add(qty.multiply(returnPrice));
        }

        // Locked for the duration of this transaction so a concurrent invoice/payment for
        // the same dealer can't race on current_balance.
        Dealer dealer = dealerRepository.findByIdForUpdate(receipt.getDealer().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Dealer not found: " + receipt.getDealer().getId()));
        BigDecimal oldBalance = dealer.getCurrentBalance();
        BigDecimal newBalance = oldBalance.subtract(totalRefundAmount); // Refund reduces outstanding balance dealer owes us

        dealer.setCurrentBalance(newBalance);

        // A return can bring a CREDIT_HOLD dealer back under the unlock threshold just
        // like a cash payment does (US-WMS-15) - without this, a dealer whose balance
        // only ever drops via returns (no cash payment) would stay incorrectly locked.
        if (dealer.getCreditStatus() == CreditStatus.CREDIT_HOLD) {
            BigDecimal creditLimit = dealer.getCreditLimit() != null ? dealer.getCreditLimit() : BigDecimal.ZERO;
            BigDecimal bufferPct = systemConfigService.getDecimalValue("CREDIT_UNLOCK_BUFFER_PCT", new BigDecimal("0.8"));
            if (newBalance.compareTo(creditLimit.multiply(bufferPct)) < 0) {
                dealer.setCreditStatus(CreditStatus.ACTIVE);
            }
        }

        dealerRepository.save(dealer);

        // Generate Credit Note Number
        String creditNoteNumber = "CN-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        // Stamp the period for the credit note's own document date (today), not the
        // (possibly older, possibly since-closed) return receipt's period — matches the
        // Invoice/PaymentReceipt pattern and rejects backdating into a closed period.
        LocalDate documentDate = LocalDate.now();
        AccountingPeriod accountingPeriod = accountingPeriodService.resolveOpenPeriod(documentDate);

        CreditNote creditNote = CreditNote.builder()
                .creditNoteNumber(creditNoteNumber)
                .dealer(dealer)
                .receipt(receipt)
                .amount(totalRefundAmount)
                .reason(request.getReason())
                .createdBy(actor)
                .documentDate(documentDate)
                .accountingPeriod(accountingPeriod)
                .createdAt(OffsetDateTime.now())
                .build();

        creditNote = creditNoteRepository.save(creditNote);

        auditLogService.log(
                actor, AuditAction.CREDIT_NOTE_CREATE, CREDIT_NOTE_ENTITY,
                creditNote.getId(), creditNoteNumber,
                receipt.getWarehouse().getId(),
                Map.of(),
                Map.of("amount", totalRefundAmount, "dealerId", dealer.getId())
        );

        auditLogService.log(
                actor, AuditAction.UPDATE, DEALER_ENTITY,
                dealer.getId(), dealer.getCode(),
                receipt.getWarehouse().getId(),
                Map.of("balance", oldBalance),
                Map.of("balance", newBalance, "delta", totalRefundAmount.negate(), "creditNoteId", creditNote.getId())
        );

        return CreditNoteResponse.builder()
                .creditNoteId(creditNote.getId())
                .creditNoteNumber(creditNoteNumber)
                .dealerId(dealer.getId())
                .dealerName(dealer.getName())
                .amount(totalRefundAmount)
                .currentBalance(newBalance)
                .reason(request.getReason())
                .documentDate(LocalDate.now())
                .createdAt(OffsetDateTime.now())
                .build();
    }
}
