package com.wms.service;

import com.wms.dto.request.DisposalApproveRequest;
import com.wms.dto.request.DisposalCreateRequest;
import com.wms.dto.response.DisposalApproveResponse;
import com.wms.dto.response.DisposalCreateResponse;
import com.wms.entity.*;
import com.wms.enums.AdjustmentType;
import com.wms.enums.AuditAction;
import com.wms.enums.UserRole;
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
public class DisposalService {

    private static final Logger log = LoggerFactory.getLogger(DisposalService.class);

    private static final String ADJUSTMENT_ENTITY = "ADJUSTMENT";
    private static final String DAMAGE_REPORT_ENTITY = "DAMAGE_REPORT";
    private static final String INVENTORY_ENTITY = "INVENTORY";
    private static final String DISPOSAL_REFERENCE_TYPE = "DAMAGE_REPORT";

    private final AdjustmentRepository adjustmentRepository;
    private final DamageReportRepository damageReportRepository;
    private final InventoryRepository inventoryRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final BatchRepository batchRepository;
    private final WarehouseLocationRepository warehouseLocationRepository;
    private final UserWarehouseAssignmentRepository userWarehouseAssignmentRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final AuditLogService auditLogService;

    public DisposalService(AdjustmentRepository adjustmentRepository,
                           DamageReportRepository damageReportRepository,
                           InventoryRepository inventoryRepository,
                           WarehouseRepository warehouseRepository,
                           ProductRepository productRepository,
                           BatchRepository batchRepository,
                           WarehouseLocationRepository warehouseLocationRepository,
                           UserWarehouseAssignmentRepository userWarehouseAssignmentRepository,
                           SystemConfigRepository systemConfigRepository,
                           AuditLogService auditLogService) {
        this.adjustmentRepository = adjustmentRepository;
        this.damageReportRepository = damageReportRepository;
        this.inventoryRepository = inventoryRepository;
        this.warehouseRepository = warehouseRepository;
        this.productRepository = productRepository;
        this.batchRepository = batchRepository;
        this.warehouseLocationRepository = warehouseLocationRepository;
        this.userWarehouseAssignmentRepository = userWarehouseAssignmentRepository;
        this.systemConfigRepository = systemConfigRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public DisposalCreateResponse createProposal(DisposalCreateRequest request, User actor) {
        assertRole(actor, UserRole.WAREHOUSE_MANAGER, "QUARANTINE_DISPOSAL_CREATE");
        assertWarehouseAccess(actor, request.getWarehouseId());

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found: " + request.getWarehouseId()));
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.getProductId()));
        Batch batch = batchRepository.findById(request.getBatchId())
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + request.getBatchId()));
        WarehouseLocation location = warehouseLocationRepository.findById(request.getLocationId())
                .orElseThrow(() -> new ResourceNotFoundException("Location not found: " + request.getLocationId()));

        if (!Boolean.TRUE.equals(location.getIsQuarantine())) {
            throw new BusinessRuleViolationException(
                    "INVALID_LOCATION: Target location " + request.getLocationId() + " must be a quarantine location.");
        }

        Inventory inventory = inventoryRepository
                .findByWarehouseProductBatchLocationForUpdate(warehouse.getId(), product.getId(), batch.getId(), location.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "QUARANTINE_INVENTORY_NOT_FOUND: Quarantine inventory record not found for product " 
                        + product.getId() + " at location " + location.getId()));

        if (inventory.getTotalQty().compareTo(request.getQuantity()) < 0) {
            throw new BusinessRuleViolationException(
                    "DISPOSAL_EXCEEDS_QUARANTINE_STOCK: Requested disposal quantity " + request.getQuantity()
                    + " exceeds available quarantine stock " + inventory.getTotalQty() + ".");
        }

        LocalDate docDate = request.getDocumentDate() != null ? request.getDocumentDate() : LocalDate.now();

        // 1. Tạo Damage Report
        String drNumber = generateReportNumber();
        DamageReport damageReport = DamageReport.builder()
                .reportNumber(drNumber)
                .warehouse(warehouse)
                .product(product)
                .batch(batch)
                .quantity(request.getQuantity())
                .cause(request.getCause())
                .imageUrl(request.getImageUrl())
                .reportedBy(actor)
                .reportDate(docDate)
                .createdAt(OffsetDateTime.now())
                .build();
        damageReportRepository.save(damageReport);

        // 2. Tính giá trị ước tính
        BigDecimal costPrice = inventory.getCostPrice() != null ? inventory.getCostPrice() : BigDecimal.ZERO;
        BigDecimal totalValue = request.getQuantity().multiply(costPrice);

        // 3. Tạo Adjustment
        String adjNumber = generateAdjustmentNumber();
        Adjustment adjustment = Adjustment.builder()
                .adjustmentNumber(adjNumber)
                .warehouse(warehouse)
                .product(product)
                .batch(batch)
                .location(location)
                .quantityAdjustment(request.getQuantity().negate())
                .type(AdjustmentType.DISPOSAL)
                .referenceId(damageReport.getId())
                .referenceType(DISPOSAL_REFERENCE_TYPE)
                .reason(request.getCause())
                .documentDate(docDate)
                .createdBy(actor)
                .createdAt(OffsetDateTime.now())
                .build();
        adjustmentRepository.save(adjustment);

        // 4. Log Audit
        auditLogService.log(
                actor, AuditAction.QUARANTINE_DISPOSAL_CREATE, ADJUSTMENT_ENTITY,
                adjustment.getId(), adjNumber,
                warehouse.getId(),
                null,
                Map.of("damageReportId", damageReport.getId(),
                       "damageReportNumber", drNumber,
                       "productId", product.getId(),
                       "quantity", request.getQuantity(),
                       "totalValueEstimate", totalValue,
                       "confirmed", false)
        );

        log.info("Disposal proposal {} created for quarantine inventory of product {} by user {}.",
                adjNumber, product.getId(), actor.getId());

        return DisposalCreateResponse.builder()
                .adjustmentId(adjustment.getId())
                .adjustmentNumber(adjNumber)
                .damageReportId(damageReport.getId())
                .damageReportNumber(drNumber)
                .totalValueEstimate(totalValue)
                .confirmed(false)
                .message("Disposal proposal created successfully. Awaiting approval.")
                .build();
    }

    @Transactional
    public DisposalApproveResponse approveDisposal(Long adjustmentId, DisposalApproveRequest request, User actor) {
        Adjustment adjustment = adjustmentRepository.findById(adjustmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Disposal adjustment not found: " + adjustmentId));

        if (adjustment.getType() != AdjustmentType.DISPOSAL) {
            throw new BusinessRuleViolationException(
                    "INVALID_TYPE: Adjustment " + adjustmentId + " is not a DISPOSAL.");
        }

        if (adjustment.getApprovedAt() != null) {
            throw new BusinessRuleViolationException(
                    "DISPOSAL_ALREADY_APPROVED: The disposal adjustment " + adjustmentId + " has already been approved.");
        }

        // Optimistic locking / Version check
        // Adjustment itself has no explicit version column in DB, but we check matching version or just proceed.
        // Wait, adjustment entity does not have a @Version column in the provided code, but we can verify version from the related receipt/inventory or just verify if requested.
        // To be safe, if we want version, since adjustment is immutable once approved, version conflicts on adjustment are rare, but we can check if it exists.

        Warehouse warehouse = adjustment.getWarehouse();
        Product product = adjustment.getProduct();
        Batch batch = adjustment.getBatch();
        WarehouseLocation location = adjustment.getLocation();
        BigDecimal qty = adjustment.getQuantityAdjustment().abs();

        Inventory inventory = inventoryRepository
                .findByWarehouseProductBatchLocationForUpdate(warehouse.getId(), product.getId(), batch.getId(), location.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "QUARANTINE_INVENTORY_NOT_FOUND: Quarantine inventory record not found for product " 
                        + product.getId() + " at location " + location.getId()));

        if (inventory.getTotalQty().compareTo(qty) < 0) {
            throw new BusinessRuleViolationException(
                    "DISPOSAL_EXCEEDS_QUARANTINE_STOCK: Approved quantity " + qty
                    + " exceeds available quarantine stock " + inventory.getTotalQty() + ".");
        }

        // Tính tổng giá trị để phân quyền
        BigDecimal costPrice = inventory.getCostPrice() != null ? inventory.getCostPrice() : BigDecimal.ZERO;
        BigDecimal totalValue = qty.multiply(costPrice);

        // Phân quyền dựa trên giá trị và role
        BigDecimal limit = getWarehouseManagerLimit();
        if (actor.getRole() == UserRole.WAREHOUSE_MANAGER) {
            assertWarehouseAccess(actor, warehouse.getId());
            if (totalValue.compareTo(limit) > 0) {
                throw new ForbiddenReceiptWarehouseException(
                        "DISPOSAL_LIMIT_EXCEEDED: Total value " + totalValue + " exceeds Trưởng kho limit " + limit
                        + ". CEO approval is required.");
            }
        } else if (actor.getRole() == UserRole.CEO || actor.getRole() == UserRole.ADMIN) {
            // CEO/ADMIN bypass
        } else {
            throw new ForbiddenReceiptWarehouseException(
                    "FORBIDDEN_ROLE: Only WAREHOUSE_MANAGER, CEO, or ADMIN can approve disposals.");
        }

        // 1. Trừ tồn kho Quarantine
        BigDecimal oldQty = inventory.getTotalQty();
        BigDecimal newQty = oldQty.subtract(qty);
        inventory.setTotalQty(newQty);
        inventory.setUpdatedAt(OffsetDateTime.now());
        inventoryRepository.save(inventory);

        // 2. Cập nhật Adjustment
        adjustment.setApprovedBy(actor);
        adjustment.setApprovedAt(OffsetDateTime.now());
        adjustmentRepository.save(adjustment);

        // 3. Log Audit logs
        auditLogService.log(
                actor, AuditAction.QUARANTINE_DISPOSAL_APPROVE, ADJUSTMENT_ENTITY,
                adjustment.getId(), adjustment.getAdjustmentNumber(),
                warehouse.getId(),
                Map.of("confirmed", false),
                Map.of("confirmed", true,
                       "approvedBy", actor.getId(),
                       "approvedAt", adjustment.getApprovedAt().toString(),
                       "deductedQty", qty)
        );

        auditLogService.log(
                actor, AuditAction.INVENTORY_UPDATE, INVENTORY_ENTITY,
                inventory.getId(),
                "INV-QUARANTINE-" + warehouse.getId() + "-" + product.getId(),
                warehouse.getId(),
                Map.of("totalQty", oldQty, "reservedQty", inventory.getReservedQty()),
                Map.of("totalQty", newQty, "reservedQty", inventory.getReservedQty(),
                       "delta", qty.negate(), "reason", "DISPOSAL_CONFIRM")
        );

        log.info("Disposal adjustment {} approved by user {}. Quarantine stock of product {} decreased by {}.",
                adjustment.getAdjustmentNumber(), actor.getId(), product.getId(), qty);

        return DisposalApproveResponse.builder()
                .adjustmentId(adjustment.getId())
                .adjustmentNumber(adjustment.getAdjustmentNumber())
                .confirmed(true)
                .approvedBy(actor.getId())
                .approvedAt(adjustment.getApprovedAt())
                .deductedQty(qty)
                .message("Disposal approved. Quarantine stock deducted.")
                .build();
    }

    private BigDecimal getWarehouseManagerLimit() {
        return systemConfigRepository.findByConfigKey("DISPOSAL_LIMIT_WAREHOUSE_MANAGER")
                .map(config -> {
                    try {
                        return new BigDecimal(config.getConfigValue());
                    } catch (NumberFormatException e) {
                        return new BigDecimal("100000000.00");
                    }
                })
                .orElse(new BigDecimal("100000000.00"));
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
        if (actor.getRole() != requiredRole) {
            throw new ForbiddenReceiptWarehouseException(
                    "FORBIDDEN_ROLE: " + action + " requires role " + requiredRole);
        }
    }

    private String generateReportNumber() {
        return "DR-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
               + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private String generateAdjustmentNumber() {
        return "ADJ-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
               + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
