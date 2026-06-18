package com.wms.service.impl;

import com.wms.dto.request.DeliveryOrderCancelRequest;
import com.wms.dto.request.DeliveryOrderCreateRequest;
import com.wms.dto.request.DeliveryOrderItemCreateRequest;
import com.wms.dto.request.DeliveryOrderUpdateRequest;
import com.wms.dto.response.DeliveryOrderResponse;
import com.wms.entity.Dealer;
import com.wms.entity.DeliveryOrder;
import com.wms.entity.DeliveryOrderItem;
import com.wms.entity.Inventory;
import com.wms.entity.PriceHistory;
import com.wms.entity.Product;
import com.wms.entity.User;
import com.wms.entity.Warehouse;
import com.wms.entity.WarehouseProductReservation;
import com.wms.enums.AuditAction;
import com.wms.enums.CreditStatus;
import com.wms.enums.DeliveryOrderStatus;
import com.wms.enums.InvoiceStatus;
import com.wms.enums.PriceHistoryStatus;
import com.wms.enums.UserRole;
import com.wms.exception.OutboundDeliveryException;
import com.wms.exception.ResourceNotFoundException;
import com.wms.mapper.DeliveryOrderMapper;
import com.wms.repository.DealerRepository;
import com.wms.repository.DeliveryOrderItemRepository;
import com.wms.repository.DeliveryOrderRepository;
import com.wms.repository.InventoryRepository;
import com.wms.repository.InvoiceRepository;
import com.wms.repository.PriceHistoryRepository;
import com.wms.repository.ProductRepository;
import com.wms.repository.UserWarehouseAssignmentRepository;
import com.wms.repository.WarehouseProductReservationRepository;
import com.wms.repository.WarehouseRepository;
import com.wms.service.DeliveryOrderService;
import com.wms.service.PartnerEligibilityService;
import com.wms.util.PartnerAuditUtil;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliveryOrderServiceImpl implements DeliveryOrderService {

    private static final Set<DeliveryOrderStatus> CANCELLABLE_STATUSES = EnumSet.of(
            DeliveryOrderStatus.NEW,
            DeliveryOrderStatus.WAITING_PICKING,
            DeliveryOrderStatus.QC_PENDING_APPROVAL,
            DeliveryOrderStatus.QC_COMPLETED);

    private final DeliveryOrderRepository deliveryOrderRepository;
    private final DeliveryOrderItemRepository deliveryOrderItemRepository;
    private final DealerRepository dealerRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final InvoiceRepository invoiceRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final WarehouseProductReservationRepository reservationRepository;
    private final UserWarehouseAssignmentRepository assignmentRepository;
    private final PartnerEligibilityService partnerEligibilityService;
    private final DeliveryOrderMapper deliveryOrderMapper;
    private final PartnerAuditUtil auditUtil;
    private final EntityManager entityManager;

    public DeliveryOrderServiceImpl(DeliveryOrderRepository deliveryOrderRepository,
                                    DeliveryOrderItemRepository deliveryOrderItemRepository,
                                    DealerRepository dealerRepository,
                                    WarehouseRepository warehouseRepository,
                                    ProductRepository productRepository,
                                    InventoryRepository inventoryRepository,
                                    InvoiceRepository invoiceRepository,
                                    PriceHistoryRepository priceHistoryRepository,
                                    WarehouseProductReservationRepository reservationRepository,
                                    UserWarehouseAssignmentRepository assignmentRepository,
                                    PartnerEligibilityService partnerEligibilityService,
                                    DeliveryOrderMapper deliveryOrderMapper,
                                    PartnerAuditUtil auditUtil,
                                    EntityManager entityManager) {
        this.deliveryOrderRepository = deliveryOrderRepository;
        this.deliveryOrderItemRepository = deliveryOrderItemRepository;
        this.dealerRepository = dealerRepository;
        this.warehouseRepository = warehouseRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.invoiceRepository = invoiceRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.reservationRepository = reservationRepository;
        this.assignmentRepository = assignmentRepository;
        this.partnerEligibilityService = partnerEligibilityService;
        this.deliveryOrderMapper = deliveryOrderMapper;
        this.auditUtil = auditUtil;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryOrderResponse> getAllDeliveryOrders() {
        return deliveryOrderRepository.findAll().stream()
                .map(order -> deliveryOrderMapper.toResponse(order, items(order.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryOrderResponse getDeliveryOrderById(Long id) {
        DeliveryOrder order = findOrder(id);
        return deliveryOrderMapper.toResponse(order, items(order.getId()));
    }

    @Override
    @Transactional
    public DeliveryOrderResponse createDeliveryOrder(DeliveryOrderCreateRequest request, User actor) {
        requireRole(actor, UserRole.PLANNER, "Only Planner can create Delivery Orders");
        Warehouse warehouse = activeWarehouse(request.getWarehouseId());
        requireWarehouseScope(actor, warehouse.getId());
        partnerEligibilityService.ensureDealerActive(request.getDealerId());
        Dealer dealer = dealerRepository.findById(request.getDealerId())
                .orElseThrow(() -> new ResourceNotFoundException("Dealer not found with id: " + request.getDealerId()));

        List<ItemPlan> itemPlans = buildItemPlans(request);
        BigDecimal orderValue = itemPlans.stream()
                .map(ItemPlan::lineAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        validateCredit(dealer, orderValue);

        Map<Long, BigDecimal> requestedByProduct = itemPlans.stream()
                .collect(Collectors.toMap(plan -> plan.product().getId(), ItemPlan::requestedQty));
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
                Map.of(), snapshot(saved, orderValue, reservationDeltas));
        return deliveryOrderMapper.toResponse(saved, savedItems);
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
        return deliveryOrderMapper.toResponse(saved, items(saved.getId()));
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
        Map<String, Object> before = snapshot(order);
        OffsetDateTime now = OffsetDateTime.now();
        List<Map<String, Object>> releasedDeltas = releaseReservations(order, orderItems, now);

        order.setStatus(DeliveryOrderStatus.CANCELLED);
        order.setCancelReason(request.getCancelReason());
        order.setUpdatedAt(now);
        DeliveryOrder saved = deliveryOrderRepository.save(order);

        auditUtil.logChange(actor, AuditAction.CANCEL, "DELIVERY_ORDER", saved.getId(), saved.getDoNumber(),
                before, snapshot(saved, null, releasedDeltas));
        return deliveryOrderMapper.toResponse(saved, orderItems);
    }

    private DeliveryOrder findOrder(Long id) {
        return deliveryOrderRepository.findWithDealerAndWarehouseById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery order not found with id: " + id));
    }

    private List<DeliveryOrderItem> items(Long orderId) {
        return deliveryOrderItemRepository.findByDeliveryOrderId(orderId);
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
        LocalDate priceDate = request.getDocumentDate();
        for (DeliveryOrderItemCreateRequest item : request.getItems()) {
            Product product = productRepository.findByIdAndIsActiveTrue(item.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Active product not found with id: " + item.getProductId()));
            PriceHistory price = priceHistoryRepository.findEffectivePrices(
                            product.getId(), PriceHistoryStatus.APPROVED, priceDate)
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new OutboundDeliveryException("PRICE_NOT_FOUND",
                            HttpStatus.UNPROCESSABLE_ENTITY,
                            "No approved selling price found for product " + product.getId()));
            plans.add(new ItemPlan(product, item.getRequestedQty(), price.getSellingPrice()));
        }
        return plans;
    }

    private void validateCredit(Dealer dealer, BigDecimal orderValue) {
        if (dealer.getCreditStatus() == CreditStatus.CREDIT_HOLD) {
            throw creditHold("Dealer is on credit hold");
        }
        BigDecimal currentBalance = dealer.getCurrentBalance() == null ? BigDecimal.ZERO : dealer.getCurrentBalance();
        BigDecimal creditLimit = dealer.getCreditLimit() == null ? BigDecimal.ZERO : dealer.getCreditLimit();
        if (currentBalance.add(orderValue).compareTo(creditLimit) > 0) {
            throw creditHold("Dealer credit limit exceeded");
        }
        LocalDate overdueThreshold = LocalDate.now().minusDays(30);
        boolean hasOverdue = invoiceRepository.existsByDealerIdAndStatusInAndDueDateBefore(
                dealer.getId(), List.of(InvoiceStatus.UNPAID, InvoiceStatus.PARTIALLY_PAID), overdueThreshold);
        if (hasOverdue) {
            throw creditHold("Dealer has invoice overdue more than 30 days");
        }
    }

    private OutboundDeliveryException creditHold(String message) {
        return new OutboundDeliveryException("CREDIT_HOLD", HttpStatus.UNPROCESSABLE_ENTITY, message);
    }

    private void validateAvailability(Warehouse warehouse, Map<Long, BigDecimal> requestedByProduct) {
        Map<Long, BigDecimal> insufficient = new LinkedHashMap<>();
        for (Map.Entry<Long, BigDecimal> entry : requestedByProduct.entrySet()) {
            BigDecimal available = availableQty(warehouse.getId(), entry.getKey());
            if (available.compareTo(entry.getValue()) < 0) {
                insufficient.put(entry.getKey(), available);
            }
        }
        if (!insufficient.isEmpty()) {
            throw new OutboundDeliveryException("INSUFFICIENT_STOCK",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Insufficient stock in selected warehouse",
                    Map.of("availableByProduct", insufficient, "suggestedWarehouses",
                            stockSuggestions(insufficient.keySet(), requestedByProduct)));
        }
    }

    private BigDecimal availableQty(Long warehouseId, Long productId) {
        BigDecimal inventoryAvailable = inventoryRepository.sumValidAvailableQty(warehouseId, productId);
        BigDecimal plannerReserved = reservationRepository.findByWarehouseIdAndProductId(warehouseId, productId)
                .map(WarehouseProductReservation::getReservedQty)
                .orElse(BigDecimal.ZERO);
        return inventoryAvailable.subtract(plannerReserved);
    }

    private List<Map<String, Object>> stockSuggestions(Set<Long> productIds, Map<Long, BigDecimal> requestedByProduct) {
        return warehouseRepository.findByIsActive(true).stream()
                .map(warehouse -> suggestionForWarehouse(warehouse, productIds, requestedByProduct))
                .filter(suggestion -> Boolean.TRUE.equals(suggestion.get("hasEnoughStock")))
                .toList();
    }

    private Map<String, Object> suggestionForWarehouse(Warehouse warehouse,
                                                       Set<Long> productIds,
                                                       Map<Long, BigDecimal> requestedByProduct) {
        Map<Long, BigDecimal> availableByProduct = new LinkedHashMap<>();
        boolean hasEnoughStock = true;
        for (Long productId : productIds) {
            BigDecimal available = availableQty(warehouse.getId(), productId);
            availableByProduct.put(productId, available);
            if (available.compareTo(requestedByProduct.get(productId)) < 0) {
                hasEnoughStock = false;
            }
        }
        return PartnerAuditUtil.values(
                "warehouseId", warehouse.getId(),
                "warehouseCode", warehouse.getCode(),
                "availableByProduct", availableByProduct,
                "hasEnoughStock", hasEnoughStock);
    }

    private List<Map<String, Object>> reserveWarehouseProducts(Warehouse warehouse,
                                                               Map<Long, BigDecimal> requestedByProduct,
                                                               OffsetDateTime now) {
        List<Map<String, Object>> deltas = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : requestedByProduct.entrySet()) {
            WarehouseProductReservation reservation = reservationRepository
                    .findByWarehouseIdAndProductId(warehouse.getId(), entry.getKey())
                    .orElseGet(() -> newReservation(warehouse, entry.getKey(), now));
            BigDecimal before = reservation.getReservedQty();
            reservation.setReservedQty(before.add(entry.getValue()));
            reservation.setUpdatedAt(now);
            reservationRepository.save(reservation);
            deltas.add(delta(warehouse.getId(), entry.getKey(), before, reservation.getReservedQty()));
        }
        return deltas;
    }

    private WarehouseProductReservation newReservation(Warehouse warehouse, Long productId, OffsetDateTime now) {
        WarehouseProductReservation reservation = new WarehouseProductReservation();
        reservation.setWarehouse(warehouse);
        reservation.setProduct(reference(Product.class, productId));
        reservation.setReservedQty(BigDecimal.ZERO);
        reservation.setCreatedAt(now);
        reservation.setUpdatedAt(now);
        return reservation;
    }

    private List<Map<String, Object>> releaseReservations(DeliveryOrder order,
                                                          List<DeliveryOrderItem> orderItems,
                                                          OffsetDateTime now) {
        List<Map<String, Object>> deltas = new ArrayList<>();
        for (DeliveryOrderItem item : orderItems) {
            BigDecimal releaseQty = item.getReservedQty() == null ? BigDecimal.ZERO : item.getReservedQty();
            if (releaseQty.compareTo(BigDecimal.ZERO) > 0) {
                deltas.add(releaseWarehouseProduct(order.getWarehouse().getId(), item.getProduct().getId(),
                        releaseQty, now));
                releaseConcreteInventory(order, item, releaseQty, now);
                item.setReservedQty(BigDecimal.ZERO);
                deliveryOrderItemRepository.save(item);
            }
        }
        return deltas;
    }

    private Map<String, Object> releaseWarehouseProduct(Long warehouseId,
                                                        Long productId,
                                                        BigDecimal releaseQty,
                                                        OffsetDateTime now) {
        WarehouseProductReservation reservation = reservationRepository
                .findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseThrow(() -> new OutboundDeliveryException("RESERVATION_NOT_FOUND",
                        HttpStatus.CONFLICT,
                        "Warehouse product reservation not found for cancellation"));
        BigDecimal before = reservation.getReservedQty();
        BigDecimal after = before.subtract(releaseQty);
        if (after.compareTo(BigDecimal.ZERO) < 0) {
            throw new OutboundDeliveryException("INVENTORY_VERSION_CONFLICT",
                    HttpStatus.CONFLICT,
                    "Reservation release would make reserved quantity negative");
        }
        reservation.setReservedQty(after);
        reservation.setUpdatedAt(now);
        reservationRepository.save(reservation);
        return delta(warehouseId, productId, before, after);
    }

    private void releaseConcreteInventory(DeliveryOrder order,
                                          DeliveryOrderItem item,
                                          BigDecimal releaseQty,
                                          OffsetDateTime now) {
        if (item.getBatch() == null || item.getLocation() == null) {
            return;
        }
        List<Inventory> inventories = inventoryRepository.findConcreteReservationRows(
                order.getWarehouse().getId(),
                item.getProduct().getId(),
                item.getBatch().getId(),
                item.getLocation().getId());
        for (Inventory inventory : inventories) {
            BigDecimal after = inventory.getReservedQty().subtract(releaseQty);
            if (after.compareTo(BigDecimal.ZERO) < 0) {
                throw new OutboundDeliveryException("INVENTORY_VERSION_CONFLICT",
                        HttpStatus.CONFLICT,
                        "Concrete inventory reservation release would make reserved quantity negative");
            }
            inventory.setReservedQty(after);
            inventory.setUpdatedAt(now);
            inventoryRepository.save(inventory);
        }
    }

    private DeliveryOrderItem toEntity(ItemPlan plan, DeliveryOrder order) {
        DeliveryOrderItem item = new DeliveryOrderItem();
        item.setDeliveryOrder(order);
        item.setProduct(plan.product());
        item.setRequestedQty(plan.requestedQty());
        item.setReservedQty(plan.requestedQty());
        item.setIssuedQty(BigDecimal.ZERO);
        item.setUnitPrice(plan.unitPrice());
        return item;
    }

    private void requireRole(User actor, UserRole role, String message) {
        if (actor == null || actor.getRole() != role) {
            throw new OutboundDeliveryException("WAREHOUSE_SCOPE_FORBIDDEN", HttpStatus.FORBIDDEN, message);
        }
    }

    private void requireWarehouseScope(User actor, Long warehouseId) {
        boolean assigned = assignmentRepository.findWarehouseIdsByUserId(actor.getId()).contains(warehouseId);
        if (!assigned) {
            throw new OutboundDeliveryException("WAREHOUSE_SCOPE_FORBIDDEN",
                    HttpStatus.FORBIDDEN,
                    "User is not assigned to warehouse: " + warehouseId);
        }
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
        return snapshot(order, null, List.of());
    }

    private Map<String, Object> snapshot(DeliveryOrder order,
                                         BigDecimal orderValue,
                                         List<Map<String, Object>> reservationDeltas) {
        Map<String, Object> values = new LinkedHashMap<>(PartnerAuditUtil.values(
                "doNumber", order.getDoNumber(),
                "dealerId", order.getDealer() == null ? null : order.getDealer().getId(),
                "warehouseId", order.getWarehouse() == null ? null : order.getWarehouse().getId(),
                "type", order.getType(),
                "expectedDeliveryDate", order.getExpectedDeliveryDate(),
                "status", order.getStatus(),
                "cancelReason", order.getCancelReason(),
                "documentDate", order.getDocumentDate(),
                "notes", order.getNotes()));
        if (orderValue != null) {
            values.put("orderValue", orderValue);
        }
        if (reservationDeltas != null && !reservationDeltas.isEmpty()) {
            values.put("reservationDeltas", reservationDeltas);
        }
        return values;
    }

    private record ItemPlan(Product product, BigDecimal requestedQty, BigDecimal unitPrice) {
        BigDecimal lineAmount() {
            return requestedQty.multiply(unitPrice);
        }
    }
}
