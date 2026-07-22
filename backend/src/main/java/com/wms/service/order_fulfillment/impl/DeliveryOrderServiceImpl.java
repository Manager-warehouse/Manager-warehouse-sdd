package com.wms.service.order_fulfillment.impl;


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
import com.wms.dto.request.DeliveryOrderAllocationRequest;
import com.wms.dto.request.DeliveryOrderCancelRequest;
import com.wms.dto.request.DeliveryOrderCreateRequest;
import com.wms.dto.request.DeliveryOrderItemCreateRequest;
import com.wms.dto.request.DeliveryOrderPickQcResultRequest;
import com.wms.dto.request.DeliveryOrderPickQcRowRequest;
import com.wms.dto.request.DeliveryOrderPickingPlanRequest;
import com.wms.dto.request.DeliveryOrderQualityApprovalRequest;
import com.wms.dto.request.DeliveryOrderReplacementAllocationRequest;
import com.wms.dto.request.DeliveryOrderReplacementPlanRequest;
import com.wms.dto.request.DeliveryOrderReturnToBinRequest;
import com.wms.dto.request.DeliveryOrderUpdateRequest;
import com.wms.dto.request.DeliveryOrderWarehouseApprovalRequest;
import com.wms.dto.request.DeliveryOrderWarehouseRejectRequest;
import com.wms.dto.request.DeliveryOrderWarehouseRejectReturnRequest;
import com.wms.dto.response.DeliveryOrderResponse;
import com.wms.dto.response.PickingCandidateResponse;
import com.wms.entity.stock_control.Adjustment;
import com.wms.entity.stock_control.Batch;
import com.wms.entity.dealer_management.Dealer;
import com.wms.entity.order_fulfillment.DeliveryOrder;
import com.wms.entity.order_fulfillment.DeliveryOrderItem;
import com.wms.entity.order_fulfillment.DeliveryOrderItemAllocation;
import com.wms.entity.order_fulfillment.DeliveryOrderItemReplacement;
import com.wms.entity.order_fulfillment.DeliveryOrderItemReturnToBinRecord;
import com.wms.entity.order_fulfillment.DeliveryOrderWarehouseApproval;
import com.wms.entity.stock_control.Inventory;
import com.wms.entity.order_fulfillment.OutboundQcRecord;
import com.wms.entity.price_management.PriceHistory;
import com.wms.entity.product_catalog.Product;
import com.wms.entity.stock_receiving.QuarantineRecord;
import com.wms.entity.access_control.User;
import com.wms.entity.warehouse_location.Warehouse;
import com.wms.entity.warehouse_location.WarehouseLocation;
import com.wms.entity.stock_control.WarehouseProductReservation;
import com.wms.enums.audit_trail.AuditAction;
import com.wms.enums.stock_control.AdjustmentType;
import com.wms.enums.stock_control.AllocationStatus;
import com.wms.enums.order_fulfillment.ApprovalResult;
import com.wms.enums.dealer_management.CreditStatus;
import com.wms.enums.order_fulfillment.DeliveryOrderStatus;
import com.wms.enums.billing_payment.InvoiceStatus;
import com.wms.enums.warehouse_location.LocationType;
import com.wms.enums.access_control.UserRole;
import com.wms.enums.warehouse_location.WarehouseType;
import com.wms.exception.OutboundDeliveryException;
import com.wms.exception.PriceHistoryException;
import com.wms.exception.ResourceNotFoundException;
import com.wms.mapper.DeliveryOrderMapper;
import com.wms.repository.dealer_management.DealerRepository;
import com.wms.repository.AdjustmentRepository;
import com.wms.repository.DeliveryOrderItemAllocationRepository;
import com.wms.repository.DeliveryOrderItemReplacementRepository;
import com.wms.repository.DeliveryOrderItemRepository;
import com.wms.repository.DeliveryOrderItemReturnToBinRecordRepository;
import com.wms.repository.DeliveryOrderRepository;
import com.wms.repository.DeliveryOrderWarehouseApprovalRepository;
import com.wms.repository.InventoryRepository;
import com.wms.repository.InvoiceRepository;
import com.wms.repository.OutboundQcRecordRepository;
import com.wms.repository.PriceHistoryRepository;
import com.wms.repository.product_catalog.ProductRepository;
import com.wms.repository.QuarantineRecordRepository;
import com.wms.repository.UserWarehouseAssignmentRepository;
import com.wms.repository.WarehouseProductReservationRepository;
import com.wms.repository.WarehouseRepository;
import com.wms.entity.billing_payment.AccountingPeriod;
import com.wms.service.billing_payment.AccountingPeriodService;
import com.wms.service.order_fulfillment.DeliveryOrderService;
import com.wms.service.dealer_management.PartnerEligibilityService;
import com.wms.service.price_management.PriceHistoryService;
import com.wms.service.user_configuration.SystemConfigService;
import com.wms.util.PartnerAuditUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliveryOrderServiceImpl implements DeliveryOrderService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
    private static final int DEFAULT_CREDIT_HOLD_OVERDUE_DAYS = 30;

    private static final Set<DeliveryOrderStatus> CANCELLABLE_STATUSES = EnumSet.of(
            DeliveryOrderStatus.NEW,
            DeliveryOrderStatus.WAITING_PICKING,
            DeliveryOrderStatus.QC_PENDING_APPROVAL,
            DeliveryOrderStatus.QC_COMPLETED);

    private final DeliveryOrderRepository deliveryOrderRepository;
    private final DeliveryOrderItemRepository deliveryOrderItemRepository;
    private final DeliveryOrderItemAllocationRepository allocationRepository;
    private final DeliveryOrderItemReturnToBinRecordRepository returnToBinRecordRepository;
    private final DeliveryOrderItemReplacementRepository replacementRepository;
    private final DeliveryOrderWarehouseApprovalRepository deliveryOrderWarehouseApprovalRepository;
    private final DealerRepository dealerRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final InvoiceRepository invoiceRepository;
    private final OutboundQcRecordRepository outboundQcRecordRepository;
    private final QuarantineRecordRepository quarantineRecordRepository;
    private final AdjustmentRepository adjustmentRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final WarehouseProductReservationRepository reservationRepository;
    private final UserWarehouseAssignmentRepository assignmentRepository;
    private final PartnerEligibilityService partnerEligibilityService;
    private final DeliveryOrderMapper deliveryOrderMapper;
    private final PartnerAuditUtil auditUtil;
    private final EntityManager entityManager;
    private final PriceHistoryService priceHistoryService;
    private final AccountingPeriodService accountingPeriodService;
    private final SystemConfigService systemConfigService;

    public DeliveryOrderServiceImpl(DeliveryOrderRepository deliveryOrderRepository,
            DeliveryOrderItemRepository deliveryOrderItemRepository,
            DeliveryOrderItemAllocationRepository allocationRepository,
            DeliveryOrderItemReturnToBinRecordRepository returnToBinRecordRepository,
            DeliveryOrderItemReplacementRepository replacementRepository,
            DeliveryOrderWarehouseApprovalRepository deliveryOrderWarehouseApprovalRepository,
            DealerRepository dealerRepository,
            WarehouseRepository warehouseRepository,
            ProductRepository productRepository,
            InventoryRepository inventoryRepository,
            InvoiceRepository invoiceRepository,
            OutboundQcRecordRepository outboundQcRecordRepository,
            QuarantineRecordRepository quarantineRecordRepository,
            AdjustmentRepository adjustmentRepository,
            PriceHistoryRepository priceHistoryRepository,
            WarehouseProductReservationRepository reservationRepository,
            UserWarehouseAssignmentRepository assignmentRepository,
            PartnerEligibilityService partnerEligibilityService,
            DeliveryOrderMapper deliveryOrderMapper,
            PartnerAuditUtil auditUtil,
            EntityManager entityManager,
            PriceHistoryService priceHistoryService,
            AccountingPeriodService accountingPeriodService,
            SystemConfigService systemConfigService) {
        this.deliveryOrderRepository = deliveryOrderRepository;
        this.deliveryOrderItemRepository = deliveryOrderItemRepository;
        this.allocationRepository = allocationRepository;
        this.returnToBinRecordRepository = returnToBinRecordRepository;
        this.replacementRepository = replacementRepository;
        this.deliveryOrderWarehouseApprovalRepository = deliveryOrderWarehouseApprovalRepository;
        this.dealerRepository = dealerRepository;
        this.warehouseRepository = warehouseRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.invoiceRepository = invoiceRepository;
        this.outboundQcRecordRepository = outboundQcRecordRepository;
        this.quarantineRecordRepository = quarantineRecordRepository;
        this.adjustmentRepository = adjustmentRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.reservationRepository = reservationRepository;
        this.assignmentRepository = assignmentRepository;
        this.partnerEligibilityService = partnerEligibilityService;
        this.deliveryOrderMapper = deliveryOrderMapper;
        this.auditUtil = auditUtil;
        this.entityManager = entityManager;
        this.priceHistoryService = priceHistoryService;
        this.accountingPeriodService = accountingPeriodService;
        this.systemConfigService = systemConfigService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryOrderResponse> getAllDeliveryOrders(User actor) {
        List<DeliveryOrder> orders;
        if (isWarehouseScopedReadRole(actor)) {
            List<Long> warehouseIds = assignmentRepository.findWarehouseIdsByUserId(actor.getId());
            if (warehouseIds.isEmpty()) {
                return List.of();
            }
            orders = deliveryOrderRepository.findDetailedByWarehouseIdIn(warehouseIds);
        } else {
            orders = deliveryOrderRepository.findAllDetailedOrderByUpdatedAtDesc();
        }
        return orders.stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryOrderResponse getDeliveryOrderById(Long id, User actor) {
        DeliveryOrder order = findOrder(id);
        if (isWarehouseScopedReadRole(actor)) {
            requireWarehouseScope(actor, order.getWarehouse().getId());
        }
        return toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, List<PickingCandidateResponse>> getPickingCandidates(Long doId, User actor) {
        requireRole(actor, UserRole.STOREKEEPER, "Only Storekeeper can view picking candidates");
        DeliveryOrder order = findOrder(doId);
        requireWarehouseScope(actor, order.getWarehouse().getId());

        if (order.getStatus() != DeliveryOrderStatus.NEW
                && order.getStatus() != DeliveryOrderStatus.WAITING_PICKING) {
            // Outside picking window — return empty map, no candidates needed
            return Map.of();
        }

        List<DeliveryOrderItem> orderItems = items(order.getId());
        Long warehouseId = order.getWarehouse().getId();

        Map<Long, List<PickingCandidateResponse>> result = new LinkedHashMap<>();
        for (DeliveryOrderItem item : orderItems) {
            Long productId = item.getProduct().getId();
            List<Inventory> candidates = inventoryRepository.findValidFifoCandidates(warehouseId, productId);
            List<PickingCandidateResponse> candidateResponses = candidates.stream()
                    .map(inv -> toPickingCandidate(inv, item))
                    .sorted(Comparator.comparing(PickingCandidateResponse::getReceivedDate,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();
            result.put(item.getId(), candidateResponses);
        }
        return result;
    }

    /**
     * Maps an Inventory row + the parent zone (location.parent) to a
     * PickingCandidateResponse.
     */
    private PickingCandidateResponse toPickingCandidate(Inventory inv, DeliveryOrderItem item) {
        WarehouseLocation bin = inv.getLocation();
        // Parent of a BIN is the ZONE; parent of a ZONE is null
        WarehouseLocation zone = (bin != null && bin.getParent() != null) ? bin.getParent() : bin;
        Batch batch = inv.getBatch();
        BigDecimal available = value(inv.getTotalQty()).subtract(value(inv.getReservedQty()));
        return PickingCandidateResponse.builder()
                .inventoryId(inv.getId())
                .batchId(batch != null ? batch.getId() : null)
                .batchCode(batch != null ? batch.getBatchNumber() : null)
                .locationId(bin != null ? bin.getId() : null)
                .locationCode(bin != null ? bin.getCode() : null)
                .zoneId(zone != null ? zone.getId() : null)
                .zoneCode(zone != null ? zone.getCode() : null)
                .availableQty(available.compareTo(ZERO) < 0 ? ZERO : available)
                .receivedDate(batch != null ? batch.getReceivedDate() : null)
                .build();
    }

    @Override
    @Transactional
    public DeliveryOrderResponse createDeliveryOrder(DeliveryOrderCreateRequest request, User actor) {
        requireRole(actor, UserRole.PLANNER, "Only Planner can create Delivery Orders");
        Warehouse warehouse = activeWarehouse(request.getWarehouseId());
        requireWarehouseScope(actor, warehouse.getId());
        partnerEligibilityService.ensureDealerActive(request.getDealerId());
        // Locked for the duration of this transaction so a concurrent order for the same
        // dealer can't read a stale balance and both pass the credit check.
        Dealer dealer = dealerRepository.findByIdForUpdate(request.getDealerId())
                .orElseThrow(() -> new ResourceNotFoundException("Dealer not found with id: " + request.getDealerId()));

        // Validate every line has an approved price before building any plan, in one pass,
        // so a DO missing prices for several products reports all of them together instead
        // of failing on just the first one buildItemPlans() would hit.
        List<Long> missingPrice = request.getItems().stream()
                .map(DeliveryOrderItemCreateRequest::getProductId)
                .distinct()
                .filter(pid -> priceHistoryService
                        .lookupApproved(pid, request.getWarehouseId(), request.getDocumentDate()).isEmpty())
                .toList();
        if (!missingPrice.isEmpty()) {
            throw PriceHistoryException.missingPrice(missingPrice.toString());
        }

        List<ItemPlan> itemPlans = buildItemPlans(request);
        BigDecimal orderValue = itemPlans.stream()
                .map(ItemPlan::lineAmount)
                .reduce(ZERO, BigDecimal::add);
        validateCredit(dealer, orderValue);

        AccountingPeriod accountingPeriod = accountingPeriodService.resolveOpenPeriod(request.getDocumentDate());

        Map<Long, BigDecimal> requestedByProduct = itemPlans.stream()
                .collect(Collectors.toMap(plan -> plan.product().getId(), ItemPlan::requestedQty, BigDecimal::add));
        validateAvailability(warehouse, requestedByProduct);

        OffsetDateTime now = OffsetDateTime.now();
        DeliveryOrder order = new DeliveryOrder();
        order.setDoNumber(generateDoNumber());
        order.setDealer(dealer);
        order.setWarehouse(warehouse);
        order.setType(request.getType());
        order.setExpectedDeliveryDate(request.getExpectedDeliveryDate());
        order.setStatus(DeliveryOrderStatus.NEW);
        order.setCreatedBy(actor);
        order.setDocumentDate(request.getDocumentDate());
        order.setAccountingPeriod(accountingPeriod);
        order.setNotes(request.getNotes());
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        DeliveryOrder saved = deliveryOrderRepository.save(order);
        List<Map<String, Object>> reservationDeltas = reserveWarehouseProducts(warehouse, requestedByProduct, now);
        List<DeliveryOrderItem> savedItems = itemPlans.stream()
                .map(plan -> toEntity(plan, saved))
                .map(deliveryOrderItemRepository::save)
                .toList();

        auditUtil.logChange(actor, AuditAction.CREATE, "DELIVERY_ORDER", saved.getId(), saved.getDoNumber(),
                Map.of(), snapshot(saved, orderValue, reservationDeltas, savedItems, List.of()));
        return deliveryOrderMapper.toResponse(saved, savedItems, List.of());
    }

    @Override
    @Transactional
    public DeliveryOrderResponse updateDeliveryOrder(Long id, DeliveryOrderUpdateRequest request, User actor) {
        DeliveryOrder order = findOrder(id);
        Map<String, Object> before = snapshot(order);
        if (request.getExpectedDeliveryDate() != null) {
            order.setExpectedDeliveryDate(request.getExpectedDeliveryDate());
        }
        if (request.getNotes() != null) {
            order.setNotes(request.getNotes());
        }
        if (request.getCancelReason() != null) {
            order.setCancelReason(request.getCancelReason());
        }
        order.setUpdatedAt(OffsetDateTime.now());
        DeliveryOrder saved = deliveryOrderRepository.save(order);
        auditUtil.logChange(actor, AuditAction.UPDATE, "DELIVERY_ORDER", saved.getId(), saved.getDoNumber(),
                before, snapshot(saved));
        return toResponse(saved);
    }

    @Override
    @Transactional
    public DeliveryOrderResponse cancelDeliveryOrder(Long id, DeliveryOrderCancelRequest request, User actor) {
        requireRole(actor, UserRole.WAREHOUSE_MANAGER, "Only Warehouse Manager can cancel Delivery Orders");
        DeliveryOrder order = findOrder(id);
        requireWarehouseScope(actor, order.getWarehouse().getId());
        if (!CANCELLABLE_STATUSES.contains(order.getStatus())) {
            throw new OutboundDeliveryException("DELIVERY_ORDER_CANCEL_FORBIDDEN",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Delivery Order cannot be cancelled after warehouse approval");
        }

        List<DeliveryOrderItem> orderItems = items(order.getId());
        List<DeliveryOrderItemAllocation> orderAllocations = allocations(order.getId());
        if (orderAllocations.stream().anyMatch(this::hasQcOrPickedRecord)) {
            throw new OutboundDeliveryException("PICKED_GOODS_RETURN_REQUIRED",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Picked or QC-processed goods must complete the warehouse return flow before cancellation");
        }
        Map<String, Object> before = snapshot(order, null, List.of(), orderItems, orderAllocations);
        OffsetDateTime now = OffsetDateTime.now();
        List<Map<String, Object>> releasedDeltas = releaseReservations(order, orderItems, orderAllocations, now);

        order.setStatus(DeliveryOrderStatus.CANCELLED);
        order.setCancelReason(request.getCancelReason());
        order.setUpdatedAt(now);
        DeliveryOrder saved = deliveryOrderRepository.save(order);

        auditUtil.logChange(actor, AuditAction.CANCEL, "DELIVERY_ORDER", saved.getId(), saved.getDoNumber(),
                before, snapshot(saved, null, releasedDeltas, orderItems, List.of()));
        return deliveryOrderMapper.toResponse(saved, orderItems, List.of());
    }

    @Override
    @Transactional
    public DeliveryOrderResponse saveDeliveryOrderPickingPlan(Long id,
            DeliveryOrderPickingPlanRequest request,
            User actor) {
        requireRole(actor, UserRole.STOREKEEPER, "Only Storekeeper can save picking plans");
        DeliveryOrder order = findOrder(id);
        entityManager.lock(order, LockModeType.PESSIMISTIC_WRITE);
        requireWarehouseScope(actor, order.getWarehouse().getId());
        if (order.getStatus() != DeliveryOrderStatus.NEW && order.getStatus() != DeliveryOrderStatus.WAITING_PICKING) {
            throw new OutboundDeliveryException("DELIVERY_ORDER_STATUS_INVALID",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Picking plan can only be saved from NEW or WAITING_PICKING status");
        }

        List<DeliveryOrderItem> orderItems = items(order.getId());
        List<DeliveryOrderItemAllocation> existingAllocations = allocations(order.getId());
        Map<String, Object> before = snapshot(order, null, List.of(), orderItems, existingAllocations);

        List<DeliveryOrderAllocationRequest> allocationRequests = preparePickingPlanRequests(order, orderItems,
                request.getAllocations());
        List<ResolvedAllocationSelection> requestedSelections = resolvePickingSelections(order, orderItems,
                existingAllocations, allocationRequests, actor);
        validateFifoSelections(order, orderItems, existingAllocations, requestedSelections);
        validateRequestedItemTotals(orderItems, requestedSelections);

        OffsetDateTime now = OffsetDateTime.now();
        List<Map<String, Object>> reservationDeltas;
        List<DeliveryOrderItemAllocation> finalAllocations;
        if (order.getStatus() == DeliveryOrderStatus.NEW) {
            reservationDeltas = transferPlannerReservations(order, requestedSelections, now);
            finalAllocations = createInitialAllocations(requestedSelections, now, actor);
        } else {
            reservationDeltas = List.of();
            finalAllocations = reviseAllocations(order, existingAllocations, requestedSelections,
                    request.getReturnToBinRecords(), now, actor);
        }

        refreshItemSummaries(orderItems, finalAllocations);
        deliveryOrderItemRepository.saveAll(orderItems);

        order.setStatus(DeliveryOrderStatus.WAITING_PICKING);
        order.setUpdatedAt(now);
        DeliveryOrder saved = deliveryOrderRepository.save(order);

        auditUtil.logChange(actor, AuditAction.PICKING_PLAN_SAVE, "DELIVERY_ORDER", saved.getId(), saved.getDoNumber(),
                before, snapshot(saved, null, reservationDeltas, orderItems, finalAllocations));
        return deliveryOrderMapper.toResponse(saved, orderItems, finalAllocations);
    }

    private List<DeliveryOrderAllocationRequest> preparePickingPlanRequests(DeliveryOrder order,
            List<DeliveryOrderItem> orderItems,
            List<DeliveryOrderAllocationRequest> requests) {
        List<DeliveryOrderAllocationRequest> safeRequests = Optional.ofNullable(requests).orElse(List.of());
        if (!safeRequests.isEmpty()) {
            return safeRequests;
        }
        if (order.getStatus() == DeliveryOrderStatus.NEW) {
            return buildAutoFifoPickingPlanRequests(order, orderItems);
        }
        return safeRequests;
    }

    @Override
    @Transactional
    public DeliveryOrderResponse saveDeliveryOrderPickQcResult(Long id,
            DeliveryOrderPickQcResultRequest request,
            User actor) {
        requireRole(actor, UserRole.WAREHOUSE_STAFF, "Only Warehouse Staff can save pick/QC results");
        DeliveryOrder order = findOrder(id);
        requireWarehouseScope(actor, order.getWarehouse().getId());
        String requestHash = buildRequestHash(request);
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            List<OutboundQcRecord> replayRows = outboundQcRecordRepository.findByDeliveryOrderIdAndIdempotencyKey(
                    order.getId(), request.getIdempotencyKey());
            if (!replayRows.isEmpty()) {
                String existingHash = replayRows.get(0).getRequestHash();
                if (!Objects.equals(existingHash, requestHash)) {
                    throw new OutboundDeliveryException("IDEMPOTENCY_KEY_CONFLICT",
                            HttpStatus.CONFLICT,
                            "Idempotency key was already used with a different payload");
                }
                return toResponse(order);
            }
        }
        if (order.getStatus() != DeliveryOrderStatus.WAITING_PICKING) {
            throw new OutboundDeliveryException("DELIVERY_ORDER_STATUS_INVALID",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Pick/QC result can only be saved from WAITING_PICKING status");
        }

        List<DeliveryOrderItem> orderItems = items(order.getId());
        List<DeliveryOrderItemAllocation> orderAllocations = allocations(order.getId());
        Map<String, Object> before = snapshot(order, null, List.of(), orderItems, orderAllocations);
        Map<Long, DeliveryOrderItemAllocation> allocationsById = orderAllocations.stream()
                .collect(Collectors.toMap(DeliveryOrderItemAllocation::getId, Function.identity()));
        List<OutboundQcRecord> existingRows = outboundQcRecordRepository.findByAllocationIdIn(
                orderAllocations.stream().map(DeliveryOrderItemAllocation::getId).toList());
        Map<Long, OutboundQcRecord> qcByAllocationId = existingRows.stream()
                .collect(Collectors.toMap(row -> row.getAllocation().getId(), Function.identity(),
                        (left, right) -> left));

        List<DeliveryOrderItemAllocation> activeAllocations = orderAllocations.stream()
                .filter(allocation -> !qcByAllocationId.containsKey(allocation.getId()))
                .toList();
        if (activeAllocations.isEmpty()) {
            throw new OutboundDeliveryException("QC_RESULT_ALREADY_RECORDED",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Pick/QC results have already been recorded for all active allocations");
        }

        Map<Long, DeliveryOrderItemAllocation> activeAllocationById = activeAllocations.stream()
                .collect(Collectors.toMap(DeliveryOrderItemAllocation::getId, Function.identity()));
        validatePickQcRequest(order, orderItems, request.getResults(), activeAllocations, allocationsById,
                qcByAllocationId);

        OffsetDateTime now = OffsetDateTime.now();
        for (DeliveryOrderPickQcRowRequest row : request.getResults()) {
            DeliveryOrderItemAllocation allocation = activeAllocationById.get(row.getAllocationId());
            DeliveryOrderItem item = allocation.getDeliveryOrderItem();
            Inventory sourceInventory = allocation.getInventory();

            sourceInventory.setTotalQty(subtractOrThrow(value(sourceInventory.getTotalQty()), row.getPickedQty(),
                    "INVENTORY_ROW_INVALID", "Source inventory does not have enough quantity"));
            sourceInventory.setReservedQty(subtractOrThrow(value(sourceInventory.getReservedQty()), row.getPickedQty(),
                    "INVENTORY_ROW_INVALID", "Source inventory does not have enough reserved quantity"));
            sourceInventory.setUpdatedAt(now);
            saveInventoryWithConflictHandling(sourceInventory);

            WarehouseLocation stagingLocation = resolveWarehouseLocation(order, row.getStagingLocationId(), false,
                    "staging");
            Inventory stagingInventory = null;
            if (value(row.getQcPassQty()).compareTo(ZERO) > 0) {
                stagingInventory = loadOrCreateInventoryRow(order, item.getProduct(), allocation.getBatch(),
                        stagingLocation, sourceInventory, false, now);
                stagingInventory.setTotalQty(value(stagingInventory.getTotalQty()).add(row.getQcPassQty()));
                stagingInventory.setReservedQty(value(stagingInventory.getReservedQty()).add(row.getQcPassQty()));
                stagingInventory.setUpdatedAt(now);
                saveInventoryWithConflictHandling(stagingInventory);
            }

            WarehouseLocation quarantineLocation = null;
            QuarantineRecord quarantineRecord = null;
            Adjustment adjustment = null;
            if (value(row.getQcFailQty()).compareTo(ZERO) > 0) {
                quarantineLocation = resolveWarehouseLocation(order, row.getQuarantineLocationId(), true, "quarantine");
                Inventory quarantineInventory = loadOrCreateInventoryRow(order, item.getProduct(),
                        allocation.getBatch(),
                        quarantineLocation, sourceInventory, true, now);
                quarantineInventory.setTotalQty(value(quarantineInventory.getTotalQty()).add(row.getQcFailQty()));
                quarantineInventory.setUpdatedAt(now);
                saveInventoryWithConflictHandling(quarantineInventory);

                quarantineRecord = new QuarantineRecord();
                quarantineRecord.setWarehouse(order.getWarehouse());
                quarantineRecord.setProduct(item.getProduct());
                quarantineRecord.setBatch(allocation.getBatch());
                quarantineRecord.setLocation(quarantineLocation);
                quarantineRecord.setDeliveryOrder(order);
                quarantineRecord.setDeliveryOrderItem(item);
                quarantineRecord.setAllocation(allocation);
                quarantineRecord.setQuantity(row.getQcFailQty());
                quarantineRecord.setReason(row.getQcFailReason());
                quarantineRecord.setCreatedBy(actor);
                quarantineRecord.setCreatedAt(now);
                quarantineRecord = quarantineRecordRepository.save(quarantineRecord);

                adjustment = new Adjustment();
                adjustment.setAdjustmentNumber(generateAdjustmentNumber());
                adjustment.setWarehouse(order.getWarehouse());
                adjustment.setProduct(item.getProduct());
                adjustment.setBatch(allocation.getBatch());
                adjustment.setLocation(allocation.getLocation());
                adjustment.setDeliveryOrder(order);
                adjustment.setDeliveryOrderItem(item);
                adjustment.setAllocation(allocation);
                adjustment.setQuantityAdjustment(row.getQcFailQty().negate());
                adjustment.setType(AdjustmentType.QC_FAIL_OUTBOUND);
                adjustment.setReferenceId(quarantineRecord.getId());
                adjustment.setReferenceType("OUTBOUND_QC_FAIL");
                adjustment.setQuarantineRecord(quarantineRecord);
                adjustment.setReason(row.getQcFailReason());
                adjustment.setApprovedBy(actor);
                adjustment.setApprovedAt(now);
                adjustment.setDocumentDate(LocalDate.now());
                adjustment.setAccountingPeriod(accountingPeriodService.resolveOpenPeriod(LocalDate.now()));
                adjustment.setCreatedBy(actor);
                adjustment.setCreatedAt(now);

                auditUtil.logChange(actor, AuditAction.OUTBOUND_QC_FAIL_QUARANTINE, "DELIVERY_ORDER", order.getId(),
                        order.getDoNumber(), Map.of(), PartnerAuditUtil.values(
                                "allocationId", allocation.getId(),
                                "productId", item.getProduct().getId(),
                                "failedQty", row.getQcFailQty(),
                                "quarantineLocationId", quarantineLocation.getId(),
                                "reason", row.getQcFailReason()));
            }

            OutboundQcRecord qcRecord = new OutboundQcRecord();
            qcRecord.setDeliveryOrder(order);
            qcRecord.setDeliveryOrderItem(item);
            qcRecord.setAllocation(allocation);
            qcRecord.setBatch(allocation.getBatch());
            qcRecord.setLocation(allocation.getLocation());
            qcRecord.setZone(allocation.getZone());
            qcRecord.setStagingLocation(stagingLocation);
            qcRecord.setQuarantineLocation(quarantineLocation);
            qcRecord.setQuarantineRecord(quarantineRecord);
            qcRecord.setPickedQty(row.getPickedQty());
            qcRecord.setQcPassQty(row.getQcPassQty());
            qcRecord.setQcFailQty(row.getQcFailQty());
            qcRecord.setQcFailReason(row.getQcFailReason());
            qcRecord.setIdempotencyKey(blankToNull(request.getIdempotencyKey()));
            qcRecord.setRequestHash(requestHash);
            qcRecord.setNotes(row.getNotes());
            qcRecord.setCreatedBy(actor);
            qcRecord.setCreatedAt(now);
            qcRecord = outboundQcRecordRepository.save(qcRecord);

            if (quarantineRecord != null) {
                quarantineRecord.setOutboundQcRecord(qcRecord);
                quarantineRecordRepository.save(quarantineRecord);
            }
            if (adjustment != null) {
                adjustment.setOutboundQcRecord(qcRecord);
                adjustmentRepository.save(adjustment);
            }

            allocation.setPickedQty(value(allocation.getPickedQty()).add(row.getPickedQty()));
            allocation.setUpdatedAt(now);
            allocationRepository.save(allocation);
            item.setPickedQty(value(item.getPickedQty()).add(row.getPickedQty()));
            item.setQcPassQty(value(item.getQcPassQty()).add(row.getQcPassQty()));
            item.setQcFailQty(value(item.getQcFailQty()).add(row.getQcFailQty()));
        }

        deliveryOrderItemRepository.saveAll(orderItems);
        order.setStatus(DeliveryOrderStatus.QC_PENDING_APPROVAL);
        order.setUpdatedAt(now);
        DeliveryOrder saved = deliveryOrderRepository.save(order);

        auditUtil.logChange(actor, AuditAction.DELIVERY_ORDER_PICK_COMPLETE, "DELIVERY_ORDER",
                saved.getId(), saved.getDoNumber(), before,
                snapshot(saved, null, List.of(), orderItems, orderAllocations));
        return deliveryOrderMapper.toResponse(saved, orderItems, allocations(saved.getId()));
    }

    @Override
    @Transactional
    public DeliveryOrderResponse saveDeliveryOrderReplacementPlan(Long id,
            DeliveryOrderReplacementPlanRequest request,
            User actor) {
        requireRole(actor, UserRole.STOREKEEPER, "Only Storekeeper can save replacement plans");
        DeliveryOrder order = findOrder(id);
        requireWarehouseScope(actor, order.getWarehouse().getId());
        if (order.getStatus() != DeliveryOrderStatus.QC_PENDING_APPROVAL) {
            throw new OutboundDeliveryException("DELIVERY_ORDER_STATUS_INVALID",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Replacement plan can only be saved from QC_PENDING_APPROVAL status");
        }

        List<DeliveryOrderItem> orderItems = items(order.getId());
        List<DeliveryOrderItemAllocation> orderAllocations = allocations(order.getId());
        Map<String, DeliveryOrderItemAllocation> allocationsByFailedInventoryKey = orderAllocations.stream()
                .collect(Collectors.toMap(
                        allocation -> allocationKey(allocation.getDeliveryOrderItem().getId(),
                                allocation.getInventory().getId()),
                        Function.identity(),
                        (left, right) -> left));
        Map<Long, DeliveryOrderItem> itemById = orderItems.stream()
                .collect(Collectors.toMap(DeliveryOrderItem::getId, Function.identity()));
        Map<String, Object> before = snapshot(order, null, List.of(), orderItems, orderAllocations);

        List<Inventory> replacementInventories = inventoryRepository
                .findByIdInWithLock(request.getReplacements().stream()
                        .map(DeliveryOrderReplacementAllocationRequest::getReplacementInventoryId)
                        .distinct()
                        .toList());
        Map<Long, Inventory> replacementInventoryById = replacementInventories.stream()
                .collect(Collectors.toMap(Inventory::getId, Function.identity()));
        List<DeliveryOrderItemReplacement> replacements = new ArrayList<>();
        List<DeliveryOrderItemAllocation> newAllocations = new ArrayList<>(orderAllocations);
        OffsetDateTime now = OffsetDateTime.now();

        for (DeliveryOrderReplacementAllocationRequest replacementRequest : request.getReplacements()) {
            DeliveryOrderItem item = itemById.get(replacementRequest.getDoItemId());
            if (item == null) {
                throw new OutboundDeliveryException("DELIVERY_ORDER_ITEM_NOT_FOUND",
                        HttpStatus.NOT_FOUND,
                        "Delivery Order item not found: " + replacementRequest.getDoItemId());
            }
            Inventory replacementInventory = replacementInventoryById
                    .get(replacementRequest.getReplacementInventoryId());
            if (replacementInventory == null) {
                throw new ResourceNotFoundException("Replacement inventory not found with id: "
                        + replacementRequest.getReplacementInventoryId());
            }
            validateReplacementInventory(order, item, replacementInventory, replacementRequest);
            BigDecimal replacedQty = replacementRepository.sumReplacementQtyByDeliveryOrderItemId(item.getId());
            BigDecimal unresolvedQty = value(item.getQcFailQty()).subtract(value(replacedQty));
            if (unresolvedQty.compareTo(replacementRequest.getQuantity()) < 0) {
                throw new OutboundDeliveryException("QC_REPLACEMENT_REQUIRED",
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "Replacement quantity exceeds unresolved QC-failed quantity");
            }

            DeliveryOrderItemAllocation failedAllocation = allocationsByFailedInventoryKey.get(
                    allocationKey(item.getId(), replacementRequest.getFailedInventoryId()));
            BigDecimal newReservedQty = value(replacementInventory.getReservedQty())
                    .add(replacementRequest.getQuantity());
            if (value(replacementInventory.getTotalQty()).compareTo(newReservedQty) < 0) {
                throw new OutboundDeliveryException("INVENTORY_ROW_INVALID",
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "Replacement inventory does not have enough available quantity");
            }
            replacementInventory.setReservedQty(newReservedQty);
            replacementInventory.setUpdatedAt(now);
            saveInventoryWithConflictHandling(replacementInventory);

            DeliveryOrderItemReplacement replacement = DeliveryOrderItemReplacement.builder()
                    .deliveryOrderItem(item)
                    .failedInventory(reference(Inventory.class, replacementRequest.getFailedInventoryId()))
                    .replacementInventory(replacementInventory)
                    .failedBatch(reference(Batch.class, replacementRequest.getFailedBatchId()))
                    .failedLocation(reference(WarehouseLocation.class, replacementRequest.getFailedLocationId()))
                    .replacementBatch(reference(Batch.class, replacementRequest.getReplacementBatchId()))
                    .replacementLocation(
                            reference(WarehouseLocation.class, replacementRequest.getReplacementLocationId()))
                    .quantity(replacementRequest.getQuantity())
                    .reason(replacementRequest.getReason())
                    .createdBy(actor)
                    .createdAt(now)
                    .build();
            replacements.add(replacementRepository.save(replacement));

            DeliveryOrderItemAllocation allocation = DeliveryOrderItemAllocation.builder()
                    .deliveryOrderItem(item)
                    .inventory(replacementInventory)
                    .batch(reference(Batch.class, replacementRequest.getReplacementBatchId()))
                    .location(reference(WarehouseLocation.class, replacementRequest.getReplacementLocationId()))
                    .zone(reference(WarehouseLocation.class, replacementRequest.getReplacementZoneId()))
                    .plannedQty(replacementRequest.getQuantity())
                    .pickedQty(ZERO)
                    .replacement(true)
                    .status(AllocationStatus.ACTIVE)
                    .replacedAllocation(failedAllocation)
                    .createdBy(actor)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            newAllocations.add(allocationRepository.save(allocation));
        }

        refreshItemSummaries(orderItems, newAllocations);
        deliveryOrderItemRepository.saveAll(orderItems);
        order.setStatus(DeliveryOrderStatus.WAITING_PICKING);
        order.setUpdatedAt(now);
        DeliveryOrder saved = deliveryOrderRepository.save(order);

        auditUtil.logChange(actor, AuditAction.PICKING_REPLACEMENT_SAVE, "DELIVERY_ORDER", saved.getId(),
                saved.getDoNumber(), before, snapshot(saved, null, List.of(), orderItems, newAllocations));
        return deliveryOrderMapper.toResponse(saved, orderItems, newAllocations);
    }

    @Override
    @Transactional
    public DeliveryOrderResponse approveDeliveryOrderQuality(Long id,
            DeliveryOrderQualityApprovalRequest request,
            User actor) {
        requireRole(actor, UserRole.STOREKEEPER, "Only Storekeeper can approve outbound quality");
        DeliveryOrder order = findOrder(id);
        requireWarehouseScope(actor, order.getWarehouse().getId());
        if (order.getStatus() != DeliveryOrderStatus.QC_PENDING_APPROVAL) {
            throw new OutboundDeliveryException("DELIVERY_ORDER_STATUS_INVALID",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Quality approval is only allowed from QC_PENDING_APPROVAL status");
        }

        List<DeliveryOrderItem> orderItems = items(order.getId());
        List<DeliveryOrderItemAllocation> orderAllocations = allocations(order.getId());
        for (DeliveryOrderItem item : orderItems) {
            if (value(item.getQcPassQty()).compareTo(value(item.getRequestedQty())) < 0) {
                throw new OutboundDeliveryException("QC_REPLACEMENT_REQUIRED",
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "All requested quantity must pass QC before quality approval");
            }
        }

        Map<String, Object> before = snapshot(order, null, List.of(), orderItems, orderAllocations);
        order.setStatus(DeliveryOrderStatus.QC_COMPLETED);
        order.setUpdatedAt(OffsetDateTime.now());
        DeliveryOrder saved = deliveryOrderRepository.save(order);
        auditUtil.logChange(actor, AuditAction.DELIVERY_ORDER_QC_APPROVE, "DELIVERY_ORDER",
                saved.getId(), saved.getDoNumber(), before,
                snapshot(saved, null, List.of(), orderItems, orderAllocations));
        return deliveryOrderMapper.toResponse(saved, orderItems, orderAllocations);
    }

    @Override
    @Transactional
    public DeliveryOrderResponse approveDeliveryOrderWarehouseRelease(Long id,
            DeliveryOrderWarehouseApprovalRequest request,
            User actor) {
        requireRole(actor, UserRole.WAREHOUSE_MANAGER, "Only Warehouse Manager can approve outbound release");
        DeliveryOrder order = findOrder(id);
        requireWarehouseScope(actor, order.getWarehouse().getId());
        if (order.getStatus() != DeliveryOrderStatus.QC_COMPLETED) {
            throw new OutboundDeliveryException("DELIVERY_ORDER_STATUS_INVALID",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Warehouse approval is only allowed from QC_COMPLETED status");
        }

        List<DeliveryOrderItem> orderItems = items(order.getId());
        List<DeliveryOrderItemAllocation> orderAllocations = allocations(order.getId());
        Map<String, Object> before = snapshot(order, null, List.of(), orderItems, orderAllocations);
        OffsetDateTime now = OffsetDateTime.now();

        DeliveryOrderWarehouseApproval approval = DeliveryOrderWarehouseApproval.builder()
                .deliveryOrder(order)
                .approver(actor)
                .result(ApprovalResult.APPROVED)
                .notes(request.getNotes())
                .approvedAt(now)
                .build();
        deliveryOrderWarehouseApprovalRepository.save(approval);

        order.setStatus(DeliveryOrderStatus.WAREHOUSE_APPROVED);
        order.setUpdatedAt(now);
        DeliveryOrder saved = deliveryOrderRepository.save(order);
        auditUtil.logChange(actor, AuditAction.DELIVERY_ORDER_WAREHOUSE_APPROVE, "DELIVERY_ORDER",
                saved.getId(), saved.getDoNumber(), before,
                snapshot(saved, null, List.of(), orderItems, orderAllocations));
        return deliveryOrderMapper.toResponse(saved, orderItems, orderAllocations);
    }

    @Override
    @Transactional
    public DeliveryOrderResponse rejectDeliveryOrderWarehouseRelease(Long id,
            DeliveryOrderWarehouseRejectRequest request,
            User actor) {
        requireRole(actor, UserRole.WAREHOUSE_MANAGER, "Only Warehouse Manager can reject outbound release");
        DeliveryOrder order = findOrder(id);
        requireWarehouseScope(actor, order.getWarehouse().getId());
        if (order.getStatus() != DeliveryOrderStatus.QC_COMPLETED) {
            throw new OutboundDeliveryException("DELIVERY_ORDER_STATUS_INVALID",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Warehouse reject is only allowed from QC_COMPLETED status");
        }

        List<DeliveryOrderItem> orderItems = items(order.getId());
        List<DeliveryOrderItemAllocation> orderAllocations = allocations(order.getId());
        List<OutboundQcRecord> qcRows = outboundQcRecordRepository.findByAllocationIdIn(
                orderAllocations.stream().map(DeliveryOrderItemAllocation::getId).toList());
        Map<Long, BigDecimal> stagedPassByAllocationId = qcRows.stream()
                .collect(Collectors.groupingBy(row -> row.getAllocation().getId(),
                        Collectors.mapping(OutboundQcRecord::getQcPassQty, Collectors.reducing(ZERO, this::valueAdd))));
        BigDecimal totalStagedPass = stagedPassByAllocationId.values().stream().reduce(ZERO, this::valueAdd);
        if (totalStagedPass.compareTo(ZERO) > 0
                && (request.getReturnToBinRecords() == null || request.getReturnToBinRecords().isEmpty())) {
            throw new OutboundDeliveryException("PICKED_GOODS_RETURN_REQUIRED",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Return-to-bin records are required when staged pass quantity exists");
        }

        Map<Long, DeliveryOrderItemAllocation> allocationsById = orderAllocations.stream()
                .collect(Collectors.toMap(DeliveryOrderItemAllocation::getId, Function.identity()));
        BigDecimal totalReturned = ZERO;
        OffsetDateTime now = OffsetDateTime.now();
        for (DeliveryOrderWarehouseRejectReturnRequest row : Optional.ofNullable(request.getReturnToBinRecords())
                .orElse(List.of())) {
            DeliveryOrderItemAllocation allocation = allocationsById.get(row.getAllocationId());
            if (allocation == null || !allocation.getDeliveryOrderItem().getId().equals(row.getDoItemId())) {
                throw new OutboundDeliveryException("INVENTORY_ROW_INVALID",
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "Warehouse reject row does not match an existing delivery order allocation");
            }
            BigDecimal expectedReturned = value(stagedPassByAllocationId.get(allocation.getId()));
            if (expectedReturned.compareTo(row.getReturnedQty()) != 0) {
                throw new OutboundDeliveryException("PICKED_GOODS_RETURN_REQUIRED",
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "Returned quantity must equal the staged pass quantity for each allocation");
            }
            if (!allocation.getBatch().getId().equals(row.getBatchId())
                    || !allocation.getLocation().getId().equals(row.getOriginalLocationId())
                    || !allocation.getZone().getId().equals(row.getOriginalZoneId())) {
                throw new OutboundDeliveryException("INVENTORY_ROW_INVALID",
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "Warehouse reject row does not match original batch/bin/zone");
            }

            WarehouseLocation stagingLocation = resolveWarehouseLocation(order, row.getSourceLocationId(), false,
                    "staging");
            Inventory stagingInventory = inventoryRepository.findConcreteReservationRowForUpdate(
                    order.getWarehouse().getId(),
                    allocation.getDeliveryOrderItem().getProduct().getId(),
                    allocation.getBatch().getId(),
                    stagingLocation.getId())
                    .orElseThrow(() -> new OutboundDeliveryException("INVENTORY_ROW_INVALID",
                            HttpStatus.UNPROCESSABLE_ENTITY,
                            "Staging inventory row not found for warehouse reject"));
            stagingInventory.setTotalQty(subtractOrThrow(value(stagingInventory.getTotalQty()), row.getReturnedQty(),
                    "INVENTORY_ROW_INVALID", "Staging inventory does not have enough quantity"));
            stagingInventory
                    .setReservedQty(subtractOrThrow(value(stagingInventory.getReservedQty()), row.getReturnedQty(),
                            "INVENTORY_ROW_INVALID", "Staging inventory does not have enough reserved quantity"));
            stagingInventory.setUpdatedAt(now);
            saveInventoryWithConflictHandling(stagingInventory);

            Inventory originalInventory = allocation.getInventory();
            originalInventory.setTotalQty(value(originalInventory.getTotalQty()).add(row.getReturnedQty()));
            originalInventory.setUpdatedAt(now);
            saveInventoryWithConflictHandling(originalInventory);

            totalReturned = totalReturned.add(row.getReturnedQty());
            auditUtil.logChange(actor, AuditAction.PICKED_GOODS_RETURN_TO_BIN, "DELIVERY_ORDER",
                    order.getId(), order.getDoNumber(), Map.of(), PartnerAuditUtil.values(
                            "allocationId", allocation.getId(),
                            "returnedQty", row.getReturnedQty(),
                            "sourceLocationId", row.getSourceLocationId(),
                            "originalLocationId", row.getOriginalLocationId(),
                            "originalZoneId", row.getOriginalZoneId(),
                            "reason", row.getReason()));
        }
        if (totalReturned.compareTo(totalStagedPass) != 0) {
            throw new OutboundDeliveryException("PICKED_GOODS_RETURN_REQUIRED",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Returned quantity must cover all staged pass quantity on the delivery order");
        }

        Map<String, Object> before = snapshot(order, null, List.of(), orderItems, orderAllocations);
        for (DeliveryOrderItem item : orderItems) {
            item.setReservedQty(ZERO);
        }
        deliveryOrderItemRepository.saveAll(orderItems);
        DeliveryOrderWarehouseApproval rejectApproval = DeliveryOrderWarehouseApproval.builder()
                .deliveryOrder(order)
                .approver(actor)
                .result(ApprovalResult.REJECTED)
                .notes(request.getReason())
                .approvedAt(now)
                .build();
        deliveryOrderWarehouseApprovalRepository.save(rejectApproval);

        order.setRejectionReason(request.getReason());
        order.setStatus(DeliveryOrderStatus.REJECTED);
        order.setUpdatedAt(now);
        DeliveryOrder saved = deliveryOrderRepository.save(order);
        auditUtil.logChange(actor, AuditAction.DELIVERY_ORDER_WAREHOUSE_REJECT, "DELIVERY_ORDER",
                saved.getId(), saved.getDoNumber(), before,
                snapshot(saved, null, List.of(), orderItems, orderAllocations));
        return deliveryOrderMapper.toResponse(saved, orderItems, orderAllocations);
    }

    private DeliveryOrderResponse toResponse(DeliveryOrder order) {
        List<DeliveryOrderItem> orderItems = items(order.getId());
        List<DeliveryOrderItemAllocation> orderAllocations = allocations(order.getId());
        return deliveryOrderMapper.toResponse(order, orderItems, orderAllocations);
    }

    private DeliveryOrder findOrder(Long id) {
        return deliveryOrderRepository.findWithDealerAndWarehouseById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery order not found with id: " + id));
    }

    private List<DeliveryOrderItem> items(Long orderId) {
        return deliveryOrderItemRepository.findByDeliveryOrderId(orderId);
    }

    private List<DeliveryOrderItemAllocation> allocations(Long orderId) {
        return allocationRepository.findByDeliveryOrderItemDeliveryOrderId(orderId).stream()
                .filter(this::isActiveAllocation)
                .toList();
    }

    private boolean isActiveAllocation(DeliveryOrderItemAllocation allocation) {
        return allocation.getStatus() == null || allocation.getStatus() == AllocationStatus.ACTIVE;
    }

    private List<ItemPlan> buildItemPlans(DeliveryOrderCreateRequest request) {
        Set<Long> duplicateProductIds = request.getItems().stream()
                .collect(Collectors.groupingBy(DeliveryOrderItemCreateRequest::getProductId, Collectors.counting()))
                .entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        if (!duplicateProductIds.isEmpty()) {
            throw new OutboundDeliveryException("DUPLICATE_PRODUCT_ITEM",
                    HttpStatus.BAD_REQUEST,
                    "Duplicate product rows are not allowed in one Delivery Order",
                    Map.of("productIds", duplicateProductIds));
        }

        List<ItemPlan> plans = new ArrayList<>();
        for (DeliveryOrderItemCreateRequest item : request.getItems()) {
            Product product = productRepository.findByIdAndIsActiveTrue(item.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Active product not found with id: " + item.getProductId()));
            PriceHistory price = priceHistoryService
                    .lookupApproved(product.getId(), request.getWarehouseId(), request.getDocumentDate())
                    .orElseThrow(() -> PriceHistoryException.missingPrice(product.getId().toString()));
            plans.add(new ItemPlan(product, item.getRequestedQty(), value(price.getSellingPrice()),
                    value(price.getCostPrice())));
        }
        return plans;
    }

    private void validateCredit(Dealer dealer, BigDecimal orderValue) {
        if (dealer.getCreditStatus() == CreditStatus.CREDIT_HOLD) {
            throw creditHold("Dealer is on credit hold");
        }
        BigDecimal currentBalance = dealer.getCurrentBalance() == null ? ZERO : dealer.getCurrentBalance();
        BigDecimal creditLimit = dealer.getCreditLimit() == null ? ZERO : dealer.getCreditLimit();
        if (currentBalance.add(orderValue).compareTo(creditLimit) > 0) {
            throw creditHold("Dealer credit limit exceeded");
        }
        int overdueDays = systemConfigService.getIntValue("CREDIT_HOLD_OVERDUE_DAYS", DEFAULT_CREDIT_HOLD_OVERDUE_DAYS);
        LocalDate overdueThreshold = LocalDate.now().minusDays(overdueDays);
        boolean hasOverdue = invoiceRepository.existsByDealerIdAndStatusInAndDueDateBefore(
                dealer.getId(), List.of(InvoiceStatus.UNPAID, InvoiceStatus.PARTIALLY_PAID), overdueThreshold);
        if (hasOverdue) {
            throw creditHold("Dealer has invoice overdue more than " + overdueDays + " days");
        }
    }

    private OutboundDeliveryException creditHold(String message) {
        return new OutboundDeliveryException("CREDIT_HOLD", HttpStatus.UNPROCESSABLE_ENTITY, message);
    }

    private void validateAvailability(Warehouse warehouse, Map<Long, BigDecimal> requestedByProduct) {
        Map<Long, BigDecimal> insufficient = new LinkedHashMap<>();
        for (Map.Entry<Long, BigDecimal> entry : requestedByProduct.entrySet()) {
            BigDecimal available = availableQty(warehouse.getId(), entry.getKey());
            if (available.compareTo(entry.getValue()) <= 0) {
                insufficient.put(entry.getKey(), available);
            }
        }
        if (!insufficient.isEmpty()) {
            throw new OutboundDeliveryException("INSUFFICIENT_STOCK",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Insufficient stock in selected warehouse",
                    Map.of("availableByProduct", insufficient,
                            "suggestedWarehouses", stockSuggestions(insufficient.keySet(), requestedByProduct)));
        }
    }

    private BigDecimal availableQty(Long warehouseId, Long productId) {
        BigDecimal inventoryAvailable = inventoryRepository.sumValidAvailableQty(warehouseId, productId);
        BigDecimal plannerReserved = reservationRepository
                .findWithWarehouseAndProductByWarehouseIdAndProductId(warehouseId, productId)
                .map(WarehouseProductReservation::getReservedQty)
                .orElse(ZERO);
        return value(inventoryAvailable).subtract(value(plannerReserved));
    }

    private List<Map<String, Object>> stockSuggestions(Set<Long> productIds, Map<Long, BigDecimal> requestedByProduct) {
        return warehouseRepository.findByIsActive(true).stream()
                .map(warehouse -> suggestionForWarehouse(warehouse, productIds, requestedByProduct))
                .filter(suggestion -> !suggestion.isEmpty())
                .toList();
    }

    private Map<String, Object> suggestionForWarehouse(Warehouse warehouse,
            Set<Long> productIds,
            Map<Long, BigDecimal> requestedByProduct) {
        Map<Long, BigDecimal> availableByProduct = new LinkedHashMap<>();
        for (Long productId : productIds) {
            BigDecimal available = availableQty(warehouse.getId(), productId);
            if (available.compareTo(value(requestedByProduct.get(productId))) >= 0) {
                availableByProduct.put(productId, available);
            }
        }
        if (availableByProduct.isEmpty()) {
            return Map.of();
        }
        return PartnerAuditUtil.values(
                "warehouseId", warehouse.getId(),
                "warehouseCode", warehouse.getCode(),
                "availableByProduct", availableByProduct);
    }

    private List<Map<String, Object>> reserveWarehouseProducts(Warehouse warehouse,
            Map<Long, BigDecimal> requestedByProduct,
            OffsetDateTime now) {
        List<Map<String, Object>> deltas = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : requestedByProduct.entrySet()) {
            WarehouseProductReservation reservation = reservationRepository
                    .findWithWarehouseAndProductByWarehouseIdAndProductIdForUpdate(warehouse.getId(), entry.getKey())
                    .orElseGet(() -> newReservation(warehouse, entry.getKey(), now));
            BigDecimal before = value(reservation.getReservedQty());
            reservation.setReservedQty(before.add(entry.getValue()));
            reservation.setUpdatedAt(now);
            saveReservationWithConflictHandling(reservation);
            deltas.add(delta(warehouse.getId(), entry.getKey(), before, reservation.getReservedQty()));
        }
        return deltas;
    }

    private WarehouseProductReservation newReservation(Warehouse warehouse, Long productId, OffsetDateTime now) {
        WarehouseProductReservation reservation = new WarehouseProductReservation();
        reservation.setWarehouse(warehouse);
        reservation.setProduct(reference(Product.class, productId));
        reservation.setReservedQty(ZERO);
        reservation.setCreatedAt(now);
        reservation.setUpdatedAt(now);
        return reservation;
    }

    private List<Map<String, Object>> releaseReservations(DeliveryOrder order,
            List<DeliveryOrderItem> orderItems,
            List<DeliveryOrderItemAllocation> orderAllocations,
            OffsetDateTime now) {
        Map<Long, BigDecimal> concreteReservedByItemId = orderAllocations.stream()
                .collect(Collectors.groupingBy(
                        allocation -> allocation.getDeliveryOrderItem().getId(),
                        Collectors.mapping(DeliveryOrderItemAllocation::getPlannedQty,
                                Collectors.reducing(ZERO, this::valueAdd))));
        List<Map<String, Object>> deltas = new ArrayList<>();
        if (!orderAllocations.isEmpty()) {
            releaseConcreteAllocations(orderAllocations, now);
            orderAllocations.forEach(allocation -> {
                allocation.setStatus(AllocationStatus.CANCELLED);
                allocation.setUpdatedAt(now);
            });
            allocationRepository.saveAll(orderAllocations);
        }
        for (DeliveryOrderItem item : orderItems) {
            BigDecimal plannerReserved = value(item.getReservedQty()).subtract(
                    value(concreteReservedByItemId.get(item.getId())));
            if (plannerReserved.compareTo(ZERO) > 0) {
                deltas.add(releaseWarehouseProduct(order.getWarehouse().getId(), item.getProduct().getId(),
                        plannerReserved, now));
            }
            item.setReservedQty(ZERO);
            item.setPlannedQty(ZERO);
            item.setPickedQty(ZERO);
            item.setQcPassQty(ZERO);
            item.setQcFailQty(ZERO);
            item.setBatch(null);
            item.setLocation(null);
            item.setZone(null);
            deliveryOrderItemRepository.save(item);
        }
        return deltas;
    }

    private void releaseConcreteAllocations(List<DeliveryOrderItemAllocation> allocations, OffsetDateTime now) {
        for (DeliveryOrderItemAllocation allocation : allocations) {
            Inventory inventory = allocation.getInventory();
            inventory.setReservedQty(subtractOrThrow(value(inventory.getReservedQty()), value(allocation.getPlannedQty()),
                    "INVENTORY_VERSION_CONFLICT",
                    "Concrete inventory reservation release would make reserved quantity negative"));
            inventory.setUpdatedAt(now);
            saveInventoryWithConflictHandling(inventory);
        }
    }

    private Map<String, Object> releaseWarehouseProduct(Long warehouseId,
            Long productId,
            BigDecimal releaseQty,
            OffsetDateTime now) {
        WarehouseProductReservation reservation = reservationRepository
                .findWithWarehouseAndProductByWarehouseIdAndProductIdForUpdate(warehouseId, productId)
                .orElseThrow(() -> new OutboundDeliveryException("RESERVATION_NOT_FOUND",
                        HttpStatus.CONFLICT,
                        "Warehouse product reservation not found for cancellation"));
        BigDecimal before = value(reservation.getReservedQty());
        BigDecimal after = subtractOrThrow(before, releaseQty,
                "INVENTORY_VERSION_CONFLICT",
                "Reservation release would make reserved quantity negative");
        reservation.setReservedQty(after);
        reservation.setUpdatedAt(now);
        saveReservationWithConflictHandling(reservation);
        return delta(warehouseId, productId, before, after);
    }

    private DeliveryOrderItem toEntity(ItemPlan plan, DeliveryOrder order) {
        DeliveryOrderItem item = new DeliveryOrderItem();
        item.setDeliveryOrder(order);
        item.setProduct(plan.product());
        item.setRequestedQty(plan.requestedQty());
        item.setReservedQty(plan.requestedQty());
        item.setPlannedQty(ZERO);
        item.setPickedQty(ZERO);
        item.setQcPassQty(ZERO);
        item.setQcFailQty(ZERO);
        item.setIssuedQty(ZERO);
        item.setUnitPrice(plan.unitPrice());
        item.setUnitCost(plan.unitCost());
        return item;
    }

    private void requireRole(User actor, UserRole role, String message) {
        if (actor == null || actor.getRole() != role) {
            throw new OutboundDeliveryException("WAREHOUSE_SCOPE_FORBIDDEN", HttpStatus.FORBIDDEN, message);
        }
    }

    private void requireWarehouseScope(User actor, Long warehouseId) {
        if (actor.getRole() == UserRole.ADMIN || actor.getRole() == UserRole.CEO) {
            return;
        }
        boolean assigned = assignmentRepository.findWarehouseIdsByUserId(actor.getId()).contains(warehouseId);
        if (!assigned) {
            throw new OutboundDeliveryException("WAREHOUSE_SCOPE_FORBIDDEN",
                    HttpStatus.FORBIDDEN,
                    "User is not assigned to warehouse: " + warehouseId);
        }
    }

    private boolean isWarehouseScopedReadRole(User actor) {
        if (actor == null || actor.getRole() == null) {
            return false;
        }
        return actor.getRole() == UserRole.STOREKEEPER
                || actor.getRole() == UserRole.WAREHOUSE_MANAGER
                || actor.getRole() == UserRole.WAREHOUSE_STAFF
                || actor.getRole() == UserRole.DISPATCHER;
    }

    private Warehouse activeWarehouse(Long warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + warehouseId));
        if (!Boolean.TRUE.equals(warehouse.getIsActive())) {
            throw new OutboundDeliveryException("WAREHOUSE_INACTIVE",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Warehouse is inactive");
        }
        return warehouse;
    }

    private List<ResolvedAllocationSelection> resolvePickingSelections(DeliveryOrder order,
            List<DeliveryOrderItem> items,
            List<DeliveryOrderItemAllocation> existingAllocations,
            List<DeliveryOrderAllocationRequest> requests,
            User actor) {
        Map<Long, DeliveryOrderItem> itemsById = items.stream()
                .collect(Collectors.toMap(DeliveryOrderItem::getId, Function.identity()));
        List<Inventory> inventories = inventoryRepository.findByIdInWithLock(requests.stream()
                .map(DeliveryOrderAllocationRequest::getInventoryId)
                .distinct()
                .toList());
        Map<Long, Inventory> inventoryById = inventories.stream()
                .collect(Collectors.toMap(Inventory::getId, Function.identity()));
        Set<AllocationSlotKey> requestKeys = new java.util.HashSet<>();
        List<ResolvedAllocationSelection> selections = new ArrayList<>();
        for (DeliveryOrderAllocationRequest request : requests) {
            DeliveryOrderItem item = itemsById.get(request.getDoItemId());
            if (item == null) {
                throw new OutboundDeliveryException("DELIVERY_ORDER_ITEM_NOT_FOUND",
                        HttpStatus.NOT_FOUND,
                        "Delivery Order item not found: " + request.getDoItemId());
            }
            Inventory inventory = inventoryById.get(request.getInventoryId());
            if (inventory == null) {
                throw new ResourceNotFoundException("Inventory not found with id: " + request.getInventoryId());
            }
            WarehouseLocation zone = resolveZone(inventory, request.getZoneId());
            AllocationSlotKey key = new AllocationSlotKey(item.getId(), inventory.getId(),
                    request.getBatchId(), request.getLocationId(), zone.getId());
            if (!requestKeys.add(key)) {
                throw new OutboundDeliveryException("DUPLICATE_ALLOCATION",
                        HttpStatus.BAD_REQUEST,
                        "Duplicate allocation rows are not allowed for the same item/inventory/location");
            }
            BigDecimal existingQty = existingAllocations.stream()
                    .filter(allocation -> AllocationSlotKey.from(item.getId(), allocation).equals(key))
                    .map(DeliveryOrderItemAllocation::getPlannedQty)
                    .findFirst()
                    .orElse(ZERO);
            validateConcreteInventorySelection(order, item, inventory, zone, request, existingQty);
            selections.add(new ResolvedAllocationSelection(item, inventory, zone, request.getPlannedQty(), actor));
        }
        return selections;
    }

    private List<DeliveryOrderAllocationRequest> buildAutoFifoPickingPlanRequests(DeliveryOrder order,
            List<DeliveryOrderItem> items) {
        List<DeliveryOrderAllocationRequest> requests = new ArrayList<>();
        for (DeliveryOrderItem item : items) {
            BigDecimal remainingQty = value(item.getRequestedQty());
            List<Inventory> candidates = inventoryRepository.findValidFifoCandidates(
                    order.getWarehouse().getId(), item.getProduct().getId());
            for (Inventory candidate : candidates) {
                if (remainingQty.compareTo(ZERO) <= 0) {
                    break;
                }
                BigDecimal availableQty = value(candidate.getTotalQty()).subtract(value(candidate.getReservedQty()));
                if (availableQty.compareTo(ZERO) <= 0) {
                    continue;
                }
                BigDecimal plannedQty = remainingQty.min(availableQty);
                DeliveryOrderAllocationRequest request = new DeliveryOrderAllocationRequest();
                request.setDoItemId(item.getId());
                request.setInventoryId(candidate.getId());
                request.setBatchId(candidate.getBatch().getId());
                request.setLocationId(candidate.getLocation().getId());
                request.setZoneId(candidate.getLocation().getParent() == null ? null
                        : candidate.getLocation().getParent().getId());
                request.setPlannedQty(plannedQty);
                requests.add(request);
                remainingQty = remainingQty.subtract(plannedQty);
            }
            if (remainingQty.compareTo(ZERO) > 0) {
                throw new OutboundDeliveryException("INSUFFICIENT_STOCK",
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "Insufficient FIFO inventory to auto-build the picking plan for product "
                                + item.getProduct().getId());
            }
        }
        return requests;
    }

    /**
     * Resolves the ZONE for a given inventory row and validates it matches the
     * requested zoneId.
     * If the BIN has no parent (flat warehouse without explicit zones), the BIN
     * itself is treated
     * as the zone, and a null/absent zoneId from the request is accepted.
     */
    private WarehouseLocation resolveZone(Inventory inventory, Long zoneId) {
        WarehouseLocation parent = inventory.getLocation().getParent();
        if (parent == null) {
            // Flat structure: BIN has no parent zone — accept null or the location's own id
            // as zoneId
            if (zoneId != null && !zoneId.equals(inventory.getLocation().getId())) {
                throw new OutboundDeliveryException("INVENTORY_ROW_INVALID",
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "Inventory row does not match the requested zone");
            }
            // Return the BIN itself as the zone so downstream code has a non-null location
            return inventory.getLocation();
        }
        if (zoneId != null && !parent.getId().equals(zoneId)) {
            throw new OutboundDeliveryException("INVENTORY_ROW_INVALID",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Inventory row does not match the requested zone");
        }
        return parent;
    }

    private void validateConcreteInventorySelection(DeliveryOrder order,
            DeliveryOrderItem item,
            Inventory inventory,
            WarehouseLocation zone,
            DeliveryOrderAllocationRequest request,
            BigDecimal existingQty) {
        if (!inventory.getWarehouse().getId().equals(order.getWarehouse().getId())
                || inventory.getWarehouse().getType() == WarehouseType.IN_TRANSIT) {
            throw new OutboundDeliveryException("INVENTORY_ROW_INVALID",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Inventory row does not belong to the delivery order warehouse");
        }
        if (!inventory.getProduct().getId().equals(item.getProduct().getId())
                || !inventory.getBatch().getId().equals(request.getBatchId())
                || !inventory.getLocation().getId().equals(request.getLocationId())
                || inventory.getLocation().getType() != LocationType.BIN
                || !Boolean.TRUE.equals(inventory.getLocation().getIsActive())
                || Boolean.TRUE.equals(inventory.getLocation().getIsLocked())
                || !zone.getWarehouse().getId().equals(order.getWarehouse().getId())
                || !Boolean.TRUE.equals(zone.getIsActive())
                || Boolean.TRUE.equals(inventory.getLocation().getIsQuarantine())) {
            throw new OutboundDeliveryException("INVENTORY_ROW_INVALID",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Inventory row does not match the requested product/batch/bin");
        }
        BigDecimal availableQty = value(inventory.getTotalQty()).subtract(value(inventory.getReservedQty()));
        if (availableQty.add(value(existingQty)).compareTo(value(request.getPlannedQty())) < 0) {
            throw new OutboundDeliveryException("INVENTORY_ROW_INVALID",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Inventory row does not have enough available quantity");
        }
    }

    private void validateFifoSelections(DeliveryOrder order,
            List<DeliveryOrderItem> items,
            List<DeliveryOrderItemAllocation> existingAllocations,
            List<ResolvedAllocationSelection> selections) {
        Map<Long, Map<Long, BigDecimal>> selectedByItem = selections.stream()
                .collect(Collectors.groupingBy(selection -> selection.item().getId(),
                        Collectors.toMap(selection -> selection.inventory().getId(),
                                ResolvedAllocationSelection::plannedQty, BigDecimal::add)));
        for (DeliveryOrderItem item : items) {
            BigDecimal remaining = value(item.getRequestedQty());
            Map<Long, BigDecimal> selected = selectedByItem.getOrDefault(item.getId(), Map.of());
            Map<Long, BigDecimal> existingByInventory = existingAllocations.stream()
                    .filter(allocation -> allocation.getDeliveryOrderItem().getId().equals(item.getId()))
                    .collect(Collectors.toMap(allocation -> allocation.getInventory().getId(),
                            DeliveryOrderItemAllocation::getPlannedQty, BigDecimal::add));
            Map<LocalDate, List<Inventory>> candidatesByDate = inventoryRepository.findFifoRowsForPlanning(
                    order.getWarehouse().getId(), item.getProduct().getId()).stream()
                    .collect(Collectors.groupingBy(candidate -> Optional.ofNullable(
                                    candidate.getBatch().getReceivedDate()).orElse(LocalDate.MAX),
                            LinkedHashMap::new, Collectors.toList()));
            for (List<Inventory> sameDateCandidates : candidatesByDate.values()) {
                BigDecimal availableOnDate = sameDateCandidates.stream()
                        .map(candidate -> value(candidate.getTotalQty())
                                .subtract(value(candidate.getReservedQty()))
                                .add(value(existingByInventory.get(candidate.getId()))))
                        .reduce(ZERO, this::valueAdd);
                BigDecimal selectedOnDate = sameDateCandidates.stream()
                        .map(candidate -> value(selected.get(candidate.getId())))
                        .reduce(ZERO, this::valueAdd);
                BigDecimal expectedOnDate = remaining.min(availableOnDate);
                if (selectedOnDate.compareTo(expectedOnDate) != 0) {
                    throw new OutboundDeliveryException("FIFO_VIOLATION", HttpStatus.UNPROCESSABLE_ENTITY,
                            "Picking allocations must consume available inventory in FIFO order");
                }
                remaining = remaining.subtract(expectedOnDate);
                if (remaining.compareTo(ZERO) <= 0) {
                    break;
                }
            }
        }
    }

    private void validateRequestedItemTotals(List<DeliveryOrderItem> items,
            List<ResolvedAllocationSelection> selections) {
        Map<Long, BigDecimal> totalsByItemId = selections.stream()
                .collect(Collectors.groupingBy(selection -> selection.item().getId(),
                        Collectors.mapping(ResolvedAllocationSelection::plannedQty,
                                Collectors.reducing(ZERO, this::valueAdd))));
        for (DeliveryOrderItem item : items) {
            BigDecimal requested = value(item.getRequestedQty());
            BigDecimal planned = value(totalsByItemId.get(item.getId()));
            if (requested.compareTo(planned) != 0) {
                throw new OutboundDeliveryException("PICKING_PLAN_QTY_MISMATCH",
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "Planned quantity must equal requested quantity for each delivery order item");
            }
        }
    }

    private List<Map<String, Object>> transferPlannerReservations(DeliveryOrder order,
            List<ResolvedAllocationSelection> selections,
            OffsetDateTime now) {
        Map<Long, BigDecimal> qtyByProductId = selections.stream()
                .collect(Collectors.groupingBy(selection -> selection.item().getProduct().getId(),
                        Collectors.mapping(ResolvedAllocationSelection::plannedQty,
                                Collectors.reducing(ZERO, this::valueAdd))));
        List<Map<String, Object>> deltas = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : qtyByProductId.entrySet()) {
            WarehouseProductReservation reservation = reservationRepository
                    .findWithWarehouseAndProductByWarehouseIdAndProductIdForUpdate(order.getWarehouse().getId(),
                            entry.getKey())
                    .orElseThrow(() -> new OutboundDeliveryException("RESERVATION_NOT_FOUND",
                            HttpStatus.CONFLICT,
                            "Planner reservation not found for warehouse/product"));
            BigDecimal before = value(reservation.getReservedQty());
            BigDecimal after = subtractOrThrow(before, entry.getValue(),
                    "INVENTORY_VERSION_CONFLICT",
                    "Planner reservation transfer would make reserved quantity negative");
            reservation.setReservedQty(after);
            reservation.setUpdatedAt(now);
            saveReservationWithConflictHandling(reservation);
            deltas.add(delta(order.getWarehouse().getId(), entry.getKey(), before, after));
        }
        return deltas;
    }

    private List<DeliveryOrderItemAllocation> createInitialAllocations(List<ResolvedAllocationSelection> selections,
            OffsetDateTime now,
            User actor) {
        List<DeliveryOrderItemAllocation> created = new ArrayList<>();
        for (ResolvedAllocationSelection selection : selections) {
            Inventory inventory = selection.inventory();
            BigDecimal newReservedQty = value(inventory.getReservedQty()).add(selection.plannedQty());
            if (value(inventory.getTotalQty()).compareTo(newReservedQty) < 0) {
                throw new OutboundDeliveryException("INVENTORY_ROW_INVALID",
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "Concrete inventory reservation would exceed total quantity");
            }
            inventory.setReservedQty(newReservedQty);
            inventory.setUpdatedAt(now);
            saveInventoryWithConflictHandling(inventory);

            DeliveryOrderItemAllocation allocation = DeliveryOrderItemAllocation.builder()
                    .deliveryOrderItem(selection.item())
                    .inventory(inventory)
                    .batch(inventory.getBatch())
                    .location(inventory.getLocation())
                    .zone(selection.zone())
                    .plannedQty(selection.plannedQty())
                    .pickedQty(ZERO)
                    .replacement(false)
                    .status(AllocationStatus.ACTIVE)
                    .createdBy(actor)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            created.add(allocationRepository.save(allocation));
        }
        return created.stream()
                .sorted(Comparator.comparing(DeliveryOrderItemAllocation::getId))
                .toList();
    }

    private List<DeliveryOrderItemAllocation> reviseAllocations(DeliveryOrder order,
            List<DeliveryOrderItemAllocation> existingAllocations,
            List<ResolvedAllocationSelection> requestedSelections,
            List<DeliveryOrderReturnToBinRequest> returnRequests,
            OffsetDateTime now,
            User actor) {
        Map<Long, DeliveryOrderReturnToBinRequest> returnRequestByAllocationId = Optional.ofNullable(returnRequests)
                .orElse(List.of())
                .stream()
                .collect(Collectors.toMap(DeliveryOrderReturnToBinRequest::getAllocationId, Function.identity(),
                        (left, right) -> {
                            throw new OutboundDeliveryException("DUPLICATE_RETURN_RECORD", HttpStatus.BAD_REQUEST,
                                    "Duplicate return-to-bin records are not allowed");
                        }));
        Set<Long> consumedReturnRequestIds = new java.util.HashSet<>();
        Map<AllocationSlotKey, ResolvedAllocationSelection> requestedByKey = requestedSelections.stream()
                .collect(Collectors.toMap(ResolvedAllocationSelection::key, Function.identity()));

        List<DeliveryOrderItemAllocation> finalAllocations = new ArrayList<>();
        for (DeliveryOrderItemAllocation existing : existingAllocations) {
            AllocationSlotKey key = AllocationSlotKey.from(existing.getDeliveryOrderItem().getId(), existing);
            ResolvedAllocationSelection requested = requestedByKey.remove(key);
            BigDecimal requestedQty = requested == null ? ZERO : requested.plannedQty();
            BigDecimal currentQty = value(existing.getPlannedQty());
            BigDecimal delta = requestedQty.subtract(currentQty);
            if (delta.compareTo(ZERO) < 0) {
                BigDecimal reductionQty = delta.abs();
                BigDecimal unpickedQty = currentQty.subtract(value(existing.getPickedQty())).max(ZERO);
                BigDecimal reservationReleaseQty = reductionQty.min(unpickedQty);
                BigDecimal pickedReturnQty = reductionQty.subtract(reservationReleaseQty);
                if (pickedReturnQty.compareTo(ZERO) > 0) {
                    DeliveryOrderReturnToBinRequest returnRequest = returnRequestByAllocationId.get(existing.getId());
                    if (returnRequest == null) {
                        throw new OutboundDeliveryException("PICKED_GOODS_RETURN_REQUIRED",
                                HttpStatus.UNPROCESSABLE_ENTITY,
                                "Changing a picked allocation requires return-to-bin records");
                    }
                    consumedReturnRequestIds.add(existing.getId());
                    processReturnToBin(order, existing, pickedReturnQty, returnRequest, now, actor);
                }
                Inventory inventory = existing.getInventory();
                inventory.setReservedQty(subtractOrThrow(value(inventory.getReservedQty()), reservationReleaseQty,
                        "INVENTORY_VERSION_CONFLICT",
                        "Concrete inventory reservation release would make reserved quantity negative"));
                inventory.setUpdatedAt(now);
                saveInventoryWithConflictHandling(inventory);
            } else if (delta.compareTo(ZERO) > 0) {
                Inventory inventory = existing.getInventory();
                BigDecimal newReservedQty = value(inventory.getReservedQty()).add(delta);
                if (value(inventory.getTotalQty()).compareTo(newReservedQty) < 0) {
                    throw new OutboundDeliveryException("INVENTORY_ROW_INVALID",
                            HttpStatus.UNPROCESSABLE_ENTITY,
                            "Concrete inventory reservation would exceed total quantity");
                }
                inventory.setReservedQty(newReservedQty);
                inventory.setUpdatedAt(now);
                saveInventoryWithConflictHandling(inventory);
            }
            if (requestedQty.compareTo(ZERO) > 0) {
                existing.setPlannedQty(requestedQty);
                existing.setUpdatedAt(now);
                finalAllocations.add(allocationRepository.save(existing));
            } else {
                existing.setStatus(AllocationStatus.CANCELLED);
                existing.setUpdatedAt(now);
                allocationRepository.save(existing);
            }
        }

        for (ResolvedAllocationSelection requested : requestedByKey.values()) {
            finalAllocations.addAll(createInitialAllocations(List.of(requested), now, actor));
        }
        if (!consumedReturnRequestIds.equals(returnRequestByAllocationId.keySet())) {
            throw new OutboundDeliveryException("RETURN_RECORD_NOT_APPLICABLE", HttpStatus.BAD_REQUEST,
                    "Return-to-bin records must reference allocations being reduced or removed");
        }
        return finalAllocations.stream()
                .sorted(Comparator.comparing(DeliveryOrderItemAllocation::getId))
                .toList();
    }

    private void processReturnToBin(DeliveryOrder order,
            DeliveryOrderItemAllocation existing,
            BigDecimal reductionQty,
            DeliveryOrderReturnToBinRequest request,
            OffsetDateTime now,
            User actor) {
        if (value(request.getReturnedQty()).compareTo(reductionQty) != 0) {
            throw new OutboundDeliveryException("PICKED_GOODS_RETURN_REQUIRED",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Returned quantity must equal the reduced picked allocation quantity");
        }
        WarehouseLocation sourceLocation = resolveWarehouseLocation(order, request.getSourceLocationId(), false,
                "source");
        Inventory sourceInventory = inventoryRepository.findConcreteReservationRowForUpdate(
                order.getWarehouse().getId(),
                existing.getDeliveryOrderItem().getProduct().getId(),
                existing.getBatch().getId(),
                sourceLocation.getId())
                .orElseThrow(() -> new OutboundDeliveryException("INVENTORY_ROW_INVALID",
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "Return source location does not contain the picked inventory row"));
        List<OutboundQcRecord> qcRows = outboundQcRecordRepository.findByAllocationIdIn(List.of(existing.getId()));
        boolean matchesRecordedStaging = qcRows.stream()
                .anyMatch(row -> row.getStagingLocation() != null
                        && row.getStagingLocation().getId().equals(request.getSourceLocationId()));
        if (!matchesRecordedStaging) {
            throw new OutboundDeliveryException("INVENTORY_ROW_INVALID", HttpStatus.UNPROCESSABLE_ENTITY,
                    "Return source location does not match the recorded staging location");
        }
        Inventory originalInventory = existing.getInventory();
        if (sourceInventory.getId().equals(originalInventory.getId())) {
            throw new OutboundDeliveryException("INVENTORY_ROW_INVALID",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Return source location must differ from the original bin");
        }

        sourceInventory.setTotalQty(subtractOrThrow(value(sourceInventory.getTotalQty()), request.getReturnedQty(),
                "INVENTORY_ROW_INVALID", "Return source inventory does not have enough quantity"));
        sourceInventory.setReservedQty(subtractOrThrow(value(sourceInventory.getReservedQty()), request.getReturnedQty(),
                "INVENTORY_ROW_INVALID", "Return source inventory does not have enough reserved quantity"));
        sourceInventory.setUpdatedAt(now);
        saveInventoryWithConflictHandling(sourceInventory);

        originalInventory.setTotalQty(value(originalInventory.getTotalQty()).add(request.getReturnedQty()));
        originalInventory.setUpdatedAt(now);
        saveInventoryWithConflictHandling(originalInventory);

        DeliveryOrderItemReturnToBinRecord record = DeliveryOrderItemReturnToBinRecord.builder()
                .deliveryOrderItem(existing.getDeliveryOrderItem())
                .allocation(existing)
                .product(existing.getDeliveryOrderItem().getProduct())
                .batch(existing.getBatch())
                .originalLocation(existing.getLocation())
                .originalZone(existing.getZone())
                .sourceLocation(sourceLocation)
                .returnedQty(request.getReturnedQty())
                .reason(request.getReason())
                .createdBy(actor)
                .createdAt(now)
                .build();
        returnToBinRecordRepository.save(record);

        auditUtil.logChange(actor, AuditAction.PICKED_GOODS_RETURN_TO_BIN, "DELIVERY_ORDER", order.getId(),
                order.getDoNumber(), Map.of(), PartnerAuditUtil.values(
                        "allocationId", existing.getId(),
                        "productId", existing.getDeliveryOrderItem().getProduct().getId(),
                        "returnedQty", request.getReturnedQty(),
                        "sourceLocationId", request.getSourceLocationId(),
                        "originalLocationId", existing.getLocation().getId(),
                        "originalZoneId", existing.getZone() != null ? existing.getZone().getId() : null));
    }

    private void validateReplacementInventory(DeliveryOrder order,
            DeliveryOrderItem item,
            Inventory replacementInventory,
            DeliveryOrderReplacementAllocationRequest request) {
        if (!replacementInventory.getWarehouse().getId().equals(order.getWarehouse().getId())
                || replacementInventory.getWarehouse().getType() == WarehouseType.IN_TRANSIT) {
            throw new OutboundDeliveryException("INVENTORY_ROW_INVALID",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Replacement inventory does not belong to the delivery order warehouse");
        }
        WarehouseLocation location = replacementInventory.getLocation();
        WarehouseLocation zone = location.getParent() == null ? location : location.getParent();
        if (!replacementInventory.getProduct().getId().equals(item.getProduct().getId())
                || !replacementInventory.getBatch().getId().equals(request.getReplacementBatchId())
                || !location.getId().equals(request.getReplacementLocationId())
                || (request.getReplacementZoneId() != null && !zone.getId().equals(request.getReplacementZoneId()))
                || location.getType() != LocationType.BIN
                || !Boolean.TRUE.equals(location.getIsActive())
                || Boolean.TRUE.equals(location.getIsLocked())
                || Boolean.TRUE.equals(location.getIsQuarantine())
                || !zone.getWarehouse().getId().equals(order.getWarehouse().getId())) {
            throw new OutboundDeliveryException("INVENTORY_ROW_INVALID",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Replacement inventory row is invalid");
        }
    }

    private void validatePickQcRequest(DeliveryOrder order,
            List<DeliveryOrderItem> orderItems,
            List<DeliveryOrderPickQcRowRequest> results,
            List<DeliveryOrderItemAllocation> activeAllocations,
            Map<Long, DeliveryOrderItemAllocation> allocationsById,
            Map<Long, OutboundQcRecord> qcByAllocationId) {
        Map<Long, DeliveryOrderItemAllocation> activeAllocationById = activeAllocations.stream()
                .collect(Collectors.toMap(DeliveryOrderItemAllocation::getId, Function.identity()));
        Set<Long> seenAllocationIds = new java.util.HashSet<>();
        Map<Long, BigDecimal> pickedByItemId = new LinkedHashMap<>();
        Map<Long, BigDecimal> passByItemId = new LinkedHashMap<>();
        for (DeliveryOrderPickQcRowRequest row : results) {
            DeliveryOrderItemAllocation activeAllocation = activeAllocationById.get(row.getAllocationId());
            if (activeAllocation == null) {
                if (qcByAllocationId.containsKey(row.getAllocationId())) {
                    throw new OutboundDeliveryException("QC_RESULT_ALREADY_RECORDED",
                            HttpStatus.UNPROCESSABLE_ENTITY,
                            "Pick/QC result was already recorded for allocation " + row.getAllocationId());
                }
                throw new OutboundDeliveryException("INVENTORY_ROW_INVALID",
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "Request contains an allocation outside the active pick/QC set");
            }
            if (!seenAllocationIds.add(row.getAllocationId())) {
                throw new OutboundDeliveryException("PICK_QC_RESULT_INVALID",
                        HttpStatus.BAD_REQUEST,
                        "Duplicate pick/QC rows are not allowed for the same allocation");
            }
            if (!activeAllocation.getDeliveryOrderItem().getId().equals(row.getDoItemId())
                    || !activeAllocation.getBatch().getId().equals(row.getBatchId())
                    || !activeAllocation.getLocation().getId().equals(row.getLocationId())
                    || !activeAllocation.getZone().getId().equals(row.getZoneId())) {
                throw new OutboundDeliveryException("INVENTORY_ROW_INVALID",
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "Pick/QC row does not match the planned allocation");
            }
            if (value(row.getPickedQty()).compareTo(value(row.getQcPassQty()).add(value(row.getQcFailQty()))) != 0) {
                throw new OutboundDeliveryException("PICK_QC_RESULT_INVALID",
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "pickedQty must equal qcPassQty + qcFailQty");
            }
            if (value(row.getPickedQty()).compareTo(value(activeAllocation.getPlannedQty())) != 0) {
                throw new OutboundDeliveryException("PICK_QC_RESULT_INVALID",
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "Each active allocation must be fully picked and QC checked in one submission");
            }
            if (value(row.getQcFailQty()).compareTo(ZERO) > 0
                    && (row.getQcFailReason() == null || row.getQcFailReason().isBlank())) {
                throw new OutboundDeliveryException("PICK_QC_RESULT_INVALID",
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "qcFailReason is required when qcFailQty is greater than zero");
            }
            if (row.getStagingLocationId() == null) {
                throw new OutboundDeliveryException("INVENTORY_ROW_INVALID",
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "stagingLocationId is required for each pick/QC row");
            }
            if (value(row.getQcFailQty()).compareTo(ZERO) > 0 && row.getQuarantineLocationId() == null) {
                throw new OutboundDeliveryException("INVENTORY_ROW_INVALID",
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "quarantineLocationId is required when qcFailQty is greater than zero");
            }
            pickedByItemId.merge(row.getDoItemId(), value(row.getPickedQty()), BigDecimal::add);
            passByItemId.merge(row.getDoItemId(), value(row.getQcPassQty()), BigDecimal::add);
        }
        if (seenAllocationIds.size() != activeAllocations.size()) {
            throw new OutboundDeliveryException("PICK_QC_RESULT_INVALID",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Pick/QC submission must include every active planned allocation exactly once");
        }
        Map<Long, BigDecimal> activePlannedByItemId = activeAllocations.stream()
                .collect(Collectors.groupingBy(allocation -> allocation.getDeliveryOrderItem().getId(),
                        Collectors.mapping(DeliveryOrderItemAllocation::getPlannedQty,
                                Collectors.reducing(ZERO, this::valueAdd))));
        for (DeliveryOrderItem item : orderItems) {
            BigDecimal pickedQty = value(pickedByItemId.get(item.getId()));
            BigDecimal activePlannedQty = value(activePlannedByItemId.get(item.getId()));
            if (pickedQty.compareTo(activePlannedQty) != 0) {
                throw new OutboundDeliveryException("PICK_QC_RESULT_INVALID",
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "Total picked quantity must equal the active planned quantity for each delivery order item");
            }
            BigDecimal cumulativePass = value(item.getQcPassQty()).add(value(passByItemId.get(item.getId())));
            if (cumulativePass.compareTo(value(item.getRequestedQty())) > 0
                    && cumulativePass.subtract(value(item.getRequestedQty())).compareTo(BigDecimal.ONE) > 0) {
                throw new OutboundDeliveryException("PICK_QC_RESULT_INVALID",
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "Cumulative QC-passed quantity cannot exceed requested quantity");
            }
        }
    }

    private WarehouseLocation resolveWarehouseLocation(DeliveryOrder order,
            Long locationId,
            boolean quarantineRequired,
            String label) {
        WarehouseLocation location = entityManager.find(WarehouseLocation.class, locationId);
        if (location == null) {
            throw new ResourceNotFoundException("Warehouse location not found with id: " + locationId);
        }
        if (!location.getWarehouse().getId().equals(order.getWarehouse().getId())
                || !Boolean.TRUE.equals(location.getIsActive())
                || Boolean.TRUE.equals(location.getIsLocked())
                || location.getType() != LocationType.BIN) {
            throw new OutboundDeliveryException("INVENTORY_ROW_INVALID",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Invalid " + label + " location for the delivery order warehouse");
        }
        if (quarantineRequired != Boolean.TRUE.equals(location.getIsQuarantine())) {
            throw new OutboundDeliveryException("INVENTORY_ROW_INVALID",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Location does not match the required " + label + " rules");
        }
        if (location.getParent() != null
                && !location.getParent().getWarehouse().getId().equals(order.getWarehouse().getId())) {
            throw new OutboundDeliveryException("INVENTORY_ROW_INVALID",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Location zone does not belong to the delivery order warehouse");
        }
        return location;
    }

    private Inventory loadOrCreateInventoryRow(DeliveryOrder order,
            Product product,
            Batch batch,
            WarehouseLocation location,
            Inventory sourceInventory,
            boolean quarantineRow,
            OffsetDateTime now) {
        if (quarantineRow != Boolean.TRUE.equals(location.getIsQuarantine())) {
            throw new OutboundDeliveryException("INVENTORY_ROW_INVALID",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Inventory row location does not match the required quarantine rule");
        }
        Optional<Inventory> existingRow = inventoryRepository.findConcreteReservationRowForUpdate(
                order.getWarehouse().getId(), product.getId(), batch.getId(), location.getId());
        if (existingRow.isPresent()) {
            return existingRow.get();
        }
        Inventory inventory = new Inventory();
        inventory.setWarehouse(order.getWarehouse());
        inventory.setProduct(product);
        inventory.setBatch(batch);
        inventory.setLocation(location);
        inventory.setTotalQty(ZERO);
        inventory.setReservedQty(ZERO);
        inventory.setCostPrice(sourceInventory.getCostPrice());
        inventory.setUpdatedAt(now);
        return saveInventoryWithConflictHandling(inventory);
    }

    private String buildRequestHash(DeliveryOrderPickQcResultRequest request) {
        String payload = Optional.ofNullable(request.getResults()).orElse(List.of()).stream()
                .sorted(Comparator.comparing(DeliveryOrderPickQcRowRequest::getAllocationId))
                .map(row -> String.join("|",
                        String.valueOf(row.getDoItemId()),
                        String.valueOf(row.getAllocationId()),
                        String.valueOf(row.getBatchId()),
                        String.valueOf(row.getLocationId()),
                        String.valueOf(row.getZoneId()),
                        String.valueOf(row.getPickedQty()),
                        String.valueOf(row.getQcPassQty()),
                        String.valueOf(row.getQcFailQty()),
                        String.valueOf(blankToNull(row.getQcFailReason())),
                        String.valueOf(row.getStagingLocationId()),
                        String.valueOf(row.getQuarantineLocationId()),
                        String.valueOf(blankToNull(row.getNotes()))))
                .collect(Collectors.joining(";"));
        return digest(blankToNull(request.getIdempotencyKey()) + "::" + payload);
    }

    private String digest(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte current : bytes) {
                builder.append(String.format("%02x", current));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String generateAdjustmentNumber() {
        return "ADJ-QC-" + System.currentTimeMillis();
    }

    private void refreshItemSummaries(List<DeliveryOrderItem> items, List<DeliveryOrderItemAllocation> allocations) {
        Map<Long, List<DeliveryOrderItemAllocation>> allocationsByItemId = allocations.stream()
                .collect(Collectors.groupingBy(allocation -> allocation.getDeliveryOrderItem().getId()));
        for (DeliveryOrderItem item : items) {
            List<DeliveryOrderItemAllocation> itemAllocations = allocationsByItemId.getOrDefault(item.getId(),
                    List.of());
            BigDecimal plannedQty = itemAllocations.stream()
                    .map(DeliveryOrderItemAllocation::getPlannedQty)
                    .reduce(ZERO, this::valueAdd);
            item.setPlannedQty(plannedQty);
            if (itemAllocations.size() == 1) {
                DeliveryOrderItemAllocation allocation = itemAllocations.get(0);
                item.setBatch(allocation.getBatch());
                item.setLocation(allocation.getLocation());
                item.setZone(allocation.getZone());
            } else {
                item.setBatch(null);
                item.setLocation(null);
                item.setZone(null);
            }
        }
    }

    private Map<String, Object> delta(Long warehouseId,
            Long productId,
            BigDecimal before,
            BigDecimal after) {
        return PartnerAuditUtil.values(
                "warehouseId", warehouseId,
                "productId", productId,
                "beforeReservedQty", before,
                "afterReservedQty", after,
                "delta", after.subtract(before));
    }

    private boolean hasQcOrPickedRecord(DeliveryOrderItemAllocation allocation) {
        return outboundQcRecordRepository.existsByAllocationId(allocation.getId())
                || value(allocation.getPickedQty()).compareTo(ZERO) > 0;
    }

    private Inventory saveInventoryWithConflictHandling(Inventory inventory) {
        try {
            return inventoryRepository.save(inventory);
        } catch (OptimisticLockingFailureException ex) {
            throw new OutboundDeliveryException("INVENTORY_VERSION_CONFLICT",
                    HttpStatus.CONFLICT,
                    "Inventory row was updated by another transaction");
        }
    }

    private WarehouseProductReservation saveReservationWithConflictHandling(WarehouseProductReservation reservation) {
        try {
            return reservationRepository.save(reservation);
        } catch (OptimisticLockingFailureException ex) {
            throw new OutboundDeliveryException("INVENTORY_VERSION_CONFLICT",
                    HttpStatus.CONFLICT,
                    "Warehouse product reservation was updated by another transaction");
        }
    }

    private <T> T reference(Class<T> type, Long id) {
        return entityManager.getReference(type, id);
    }

    private String generateDoNumber() {
        String prefix = "DO-" + OffsetDateTime.now().toLocalDate().toString().replace("-", "");
        String candidate;
        int sequence = 1;
        do {
            candidate = prefix + "-" + String.format("%04d", sequence++);
        } while (deliveryOrderRepository.existsByDoNumber(candidate));
        return candidate;
    }

    private Map<String, Object> snapshot(DeliveryOrder order) {
        return snapshot(order, null, List.of(), List.of(), List.of());
    }

    private Map<String, Object> snapshot(DeliveryOrder order,
            BigDecimal orderValue,
            List<Map<String, Object>> reservationDeltas,
            List<DeliveryOrderItem> items,
            List<DeliveryOrderItemAllocation> allocations) {
        Map<String, Object> values = new LinkedHashMap<>(PartnerAuditUtil.values(
                "doNumber", order.getDoNumber(),
                "dealerId", order.getDealer() == null ? null : order.getDealer().getId(),
                "warehouseId", order.getWarehouse() == null ? null : order.getWarehouse().getId(),
                "type", order.getType(),
                "expectedDeliveryDate", order.getExpectedDeliveryDate(),
                "status", order.getStatus(),
                "cancelReason", order.getCancelReason(),
                "rejectionReason", order.getRejectionReason(),
                "documentDate", order.getDocumentDate(),
                "notes", order.getNotes()));
        if (orderValue != null) {
            values.put("orderValue", orderValue);
        }
        if (reservationDeltas != null && !reservationDeltas.isEmpty()) {
            values.put("reservationDeltas", reservationDeltas);
        }
        if (items != null && !items.isEmpty()) {
            values.put("items", items.stream().map(item -> PartnerAuditUtil.values(
                    "itemId", item.getId(),
                    "productId", item.getProduct().getId(),
                    "requestedQty", item.getRequestedQty(),
                    "reservedQty", item.getReservedQty(),
                    "plannedQty", item.getPlannedQty(),
                    "pickedQty", item.getPickedQty(),
                    "qcPassQty", item.getQcPassQty(),
                    "qcFailQty", item.getQcFailQty(),
                    "batchId", item.getBatch() == null ? null : item.getBatch().getId(),
                    "locationId", item.getLocation() == null ? null : item.getLocation().getId(),
                    "zoneId", item.getZone() == null ? null : item.getZone().getId())).toList());
        }
        if (allocations != null && !allocations.isEmpty()) {
            values.put("allocations", allocations.stream().map(allocation -> PartnerAuditUtil.values(
                    "allocationId", allocation.getId(),
                    "deliveryOrderItemId", allocation.getDeliveryOrderItem().getId(),
                    "inventoryId", allocation.getInventory().getId(),
                    "batchId", allocation.getBatch().getId(),
                    "locationId", allocation.getLocation().getId(),
                    "zoneId", allocation.getZone() != null ? allocation.getZone().getId() : null,
                    "plannedQty", allocation.getPlannedQty(),
                    "pickedQty", allocation.getPickedQty(),
                    "replacement", allocation.getReplacement())).toList());
        }
        return values;
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private BigDecimal valueAdd(BigDecimal left, BigDecimal right) {
        return value(left).add(value(right));
    }

    private BigDecimal subtractOrThrow(BigDecimal base,
            BigDecimal subtract,
            String code,
            String message) {
        BigDecimal result = value(base).subtract(value(subtract));
        if (result.compareTo(ZERO) < 0) {
            throw new OutboundDeliveryException(code, HttpStatus.CONFLICT, message);
        }
        return result;
    }

    private String allocationKey(Long doItemId, Long inventoryId) {
        return doItemId + ":" + inventoryId;
    }

    private record ItemPlan(Product product, BigDecimal requestedQty, BigDecimal unitPrice, BigDecimal unitCost) {
        BigDecimal lineAmount() {
            return requestedQty.multiply(unitPrice);
        }
    }

    private record ResolvedAllocationSelection(DeliveryOrderItem item,
            Inventory inventory,
            WarehouseLocation zone,
            BigDecimal plannedQty,
            User actor) {
        AllocationSlotKey key() {
            return new AllocationSlotKey(item.getId(), inventory.getId(),
                    inventory.getBatch().getId(), inventory.getLocation().getId(), zone.getId());
        }
    }

    private record AllocationSlotKey(Long deliveryOrderItemId,
            Long inventoryId,
            Long batchId,
            Long locationId,
            Long zoneId) {
        static AllocationSlotKey from(Long itemId, DeliveryOrderItemAllocation allocation) {
            return new AllocationSlotKey(itemId,
                    allocation.getInventory().getId(),
                    allocation.getBatch().getId(),
                    allocation.getLocation().getId(),
                    allocation.getZone().getId());
        }
    }
}
