package com.wms.service.impl;

import com.wms.dto.request.DeliveryOrderCreateRequest;
import com.wms.dto.request.DeliveryOrderItemCreateRequest;
import com.wms.dto.request.DeliveryOrderUpdateRequest;
import com.wms.dto.response.DeliveryOrderResponse;
import com.wms.entity.Batch;
import com.wms.entity.Dealer;
import com.wms.entity.DeliveryOrder;
import com.wms.entity.DeliveryOrderItem;
import com.wms.entity.PriceHistory;
import com.wms.entity.Product;
import com.wms.entity.User;
import com.wms.entity.Warehouse;
import com.wms.entity.WarehouseLocation;
import com.wms.enums.AuditAction;
import com.wms.enums.DeliveryOrderStatus;
import com.wms.exception.PriceHistoryException;
import com.wms.exception.ResourceNotFoundException;
import com.wms.mapper.DeliveryOrderMapper;
import com.wms.repository.DeliveryOrderItemRepository;
import com.wms.repository.DeliveryOrderRepository;
import com.wms.repository.DealerRepository;
import com.wms.service.DeliveryOrderService;
import com.wms.service.PartnerEligibilityService;
import com.wms.service.PriceHistoryService;
import com.wms.util.PartnerAuditUtil;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliveryOrderServiceImpl implements DeliveryOrderService {

    private final DeliveryOrderRepository deliveryOrderRepository;
    private final DeliveryOrderItemRepository deliveryOrderItemRepository;
    private final DealerRepository dealerRepository;
    private final PartnerEligibilityService partnerEligibilityService;
    private final DeliveryOrderMapper deliveryOrderMapper;
    private final PartnerAuditUtil auditUtil;
    private final EntityManager entityManager;
    private final PriceHistoryService priceHistoryService;

    public DeliveryOrderServiceImpl(DeliveryOrderRepository deliveryOrderRepository,
                                    DeliveryOrderItemRepository deliveryOrderItemRepository,
                                    DealerRepository dealerRepository,
                                    PartnerEligibilityService partnerEligibilityService,
                                    DeliveryOrderMapper deliveryOrderMapper,
                                    PartnerAuditUtil auditUtil,
                                    EntityManager entityManager,
                                    PriceHistoryService priceHistoryService) {
        this.deliveryOrderRepository = deliveryOrderRepository;
        this.deliveryOrderItemRepository = deliveryOrderItemRepository;
        this.dealerRepository = dealerRepository;
        this.partnerEligibilityService = partnerEligibilityService;
        this.deliveryOrderMapper = deliveryOrderMapper;
        this.auditUtil = auditUtil;
        this.entityManager = entityManager;
        this.priceHistoryService = priceHistoryService;
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
        partnerEligibilityService.ensureDealerActive(request.getDealerId());
        Dealer dealer = dealerRepository.findById(request.getDealerId())
                .orElseThrow(() -> new ResourceNotFoundException("Dealer not found with id: " + request.getDealerId()));

        OffsetDateTime now = OffsetDateTime.now();
        DeliveryOrder order = new DeliveryOrder();
        order.setDoNumber(generateDoNumber());
        order.setDealer(dealer);
        order.setWarehouse(reference(Warehouse.class, request.getWarehouseId()));
        order.setType(request.getType());
        order.setExpectedDeliveryDate(request.getExpectedDeliveryDate());
        order.setStatus(DeliveryOrderStatus.NEW);
        order.setCreatedBy(actor);
        order.setDocumentDate(request.getDocumentDate());
        order.setNotes(request.getNotes());
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        LocalDate today = now.toLocalDate();
        Long warehouseId = request.getWarehouseId();
        // Validate all lines have an approved price for this warehouse before creating anything
        List<Long> missingPrice = request.getItems().stream()
                .map(i -> i.getProductId())
                .distinct()
                .filter(pid -> priceHistoryService.lookupApproved(pid, warehouseId, today).isEmpty())
                .toList();
        if (!missingPrice.isEmpty()) {
            throw PriceHistoryException.missingPrice(missingPrice.toString());
        }

        DeliveryOrder saved = deliveryOrderRepository.save(order);
        List<DeliveryOrderItem> savedItems = request.getItems().stream()
                .map(item -> {
                    PriceHistory price = priceHistoryService.lookupApproved(item.getProductId(), warehouseId, today).get();
                    return toEntity(item, saved, price);
                })
                .map(deliveryOrderItemRepository::save)
                .toList();
        auditUtil.logChange(actor, AuditAction.CREATE, "DELIVERY_ORDER", saved.getId(), saved.getDoNumber(),
                Map.of(), snapshot(saved));
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
    public void cancelDeliveryOrder(Long id, User actor) {
        DeliveryOrder order = findOrder(id);
        Map<String, Object> before = snapshot(order);
        order.setStatus(DeliveryOrderStatus.CANCELLED);
        order.setCancelReason(order.getCancelReason() == null ? "Cancelled by user" : order.getCancelReason());
        order.setUpdatedAt(OffsetDateTime.now());
        DeliveryOrder saved = deliveryOrderRepository.save(order);
        auditUtil.logChange(actor, AuditAction.CANCEL, "DELIVERY_ORDER", saved.getId(), saved.getDoNumber(),
                before, snapshot(saved));
    }

    private DeliveryOrder findOrder(Long id) {
        return deliveryOrderRepository.findWithDealerAndWarehouseById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery order not found with id: " + id));
    }

    private List<DeliveryOrderItem> items(Long orderId) {
        return deliveryOrderItemRepository.findByDeliveryOrderId(orderId);
    }

    private DeliveryOrderItem toEntity(DeliveryOrderItemCreateRequest request, DeliveryOrder order,
                                       PriceHistory price) {
        DeliveryOrderItem item = new DeliveryOrderItem();
        item.setDeliveryOrder(order);
        item.setProduct(reference(Product.class, request.getProductId()));
        if (request.getBatchId() != null) {
            item.setBatch(reference(Batch.class, request.getBatchId()));
        }
        if (request.getLocationId() != null) {
            item.setLocation(reference(WarehouseLocation.class, request.getLocationId()));
        }
        item.setRequestedQty(request.getRequestedQty());
        item.setReservedQty(BigDecimal.ZERO);
        item.setIssuedQty(BigDecimal.ZERO);
        // Snapshot from price_history, not from request (spec 007)
        item.setUnitPrice(price.getSellingPrice());
        item.setUnitCost(price.getCostPrice());
        return item;
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
        return PartnerAuditUtil.values(
                "doNumber", order.getDoNumber(),
                "dealerId", order.getDealer() == null ? null : order.getDealer().getId(),
                "warehouseId", order.getWarehouse() == null ? null : order.getWarehouse().getId(),
                "type", order.getType(),
                "expectedDeliveryDate", order.getExpectedDeliveryDate(),
                "status", order.getStatus(),
                "cancelReason", order.getCancelReason(),
                "documentDate", order.getDocumentDate(),
                "notes", order.getNotes());
    }
}
