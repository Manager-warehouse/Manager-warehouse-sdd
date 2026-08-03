package com.wms.mapper;


import com.wms.dto.response.DeliveryOrderItemResponse;
import com.wms.dto.response.DeliveryOrderResponse;
import com.wms.entity.order_fulfillment.DeliveryOrder;
import com.wms.entity.order_fulfillment.DeliveryOrderItem;
import com.wms.entity.order_fulfillment.DeliveryOrderItemAllocation;
import com.wms.entity.order_fulfillment.DeliveryOrderWarehouseApproval;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class DeliveryOrderMapper {
        public DeliveryOrderResponse toResponse(DeliveryOrder order, List<DeliveryOrderItem> items) {
                return toResponse(order, items, List.of());
        }

        public DeliveryOrderResponse toResponse(DeliveryOrder order,
                        List<DeliveryOrderItem> items,
                        List<DeliveryOrderItemAllocation> allocations) {
                return toResponse(order, items, allocations, Map.of(), null);
        }

        public DeliveryOrderResponse toResponse(DeliveryOrder order,
                        List<DeliveryOrderItem> items,
                        List<DeliveryOrderItemAllocation> allocations,
                        Map<Long, AllocationQcSummary> qcSummaryByAllocationId) {
                return toResponse(order, items, allocations, qcSummaryByAllocationId, null);
        }

        public DeliveryOrderResponse toResponse(DeliveryOrder order,
                        List<DeliveryOrderItem> items,
                        List<DeliveryOrderItemAllocation> allocations,
                        Map<Long, AllocationQcSummary> qcSummaryByAllocationId,
                        DeliveryOrderWarehouseApproval warehouseApproval) {
                Map<Long, List<DeliveryOrderItemAllocation>> allocationsByItemId = allocations.stream()
                                .collect(Collectors
                                                .groupingBy(allocation -> allocation.getDeliveryOrderItem().getId()));
                BigDecimal totalPickedQty = items.stream()
                                .map(DeliveryOrderItem::getPickedQty)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal totalQcPassQty = items.stream()
                                .map(DeliveryOrderItem::getQcPassQty)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal totalQcFailQty = items.stream()
                                .map(DeliveryOrderItem::getQcFailQty)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                String pickingPlanSavedByName = null;
                if (allocations != null) {
                        for (DeliveryOrderItemAllocation allocation : allocations) {
                                if (allocation.getCreatedBy() != null) {
                                        pickingPlanSavedByName = allocation.getCreatedBy().getFullName();
                                        break;
                                }
                        }
                }
                return DeliveryOrderResponse.builder()
                                .id(order.getId())
                                .doNumber(order.getDoNumber())
                                .dealerId(order.getDealer().getId())
                                .warehouseId(order.getWarehouse().getId())
                                .type(order.getType())
                                .expectedDeliveryDate(order.getExpectedDeliveryDate())
                                .status(order.getStatus())
                                .cancelReason(order.getCancelReason())
                                .rejectionReason(order.getRejectionReason())
                                .totalPickedQty(totalPickedQty)
                                .totalQcPassQty(totalQcPassQty)
                                .totalQcFailQty(totalQcFailQty)
                                .documentDate(order.getDocumentDate())
                                .notes(order.getNotes())
                                .items(items.stream()
                                                .map(item -> toItemResponse(item,
                                                                allocationsByItemId.getOrDefault(item.getId(),
                                                                                List.of()),
                                                                qcSummaryByAllocationId))
                                                .toList())
                                .createdAt(order.getCreatedAt())
                                .updatedAt(order.getUpdatedAt())
                                .createdByName(order.getCreatedBy() != null ? order.getCreatedBy().getFullName() : null)
                                .pickingPlanSavedByName(pickingPlanSavedByName)
                                .qcByName(order.getQcBy() != null ? order.getQcBy().getFullName() : null)
                                .approvedByName(warehouseApproval != null && warehouseApproval.getApprover() != null
                                                ? warehouseApproval.getApprover().getFullName()
                                                : null)
                                .warehouseApprovedAt(warehouseApproval != null ? warehouseApproval.getApprovedAt() : null)
                                .build();
        }

        public DeliveryOrderItemResponse toItemResponse(DeliveryOrderItem item) {
                return toItemResponse(item, List.of());
        }

        public DeliveryOrderItemResponse toItemResponse(DeliveryOrderItem item,
                        List<DeliveryOrderItemAllocation> allocations) {
                return toItemResponse(item, allocations, Map.of());
        }

        public DeliveryOrderItemResponse toItemResponse(DeliveryOrderItem item,
                        List<DeliveryOrderItemAllocation> allocations,
                        Map<Long, AllocationQcSummary> qcSummaryByAllocationId) {
                return DeliveryOrderItemResponse.builder()
                                .id(item.getId())
                                .productId(item.getProduct().getId())
                                .productName(item.getProduct() == null ? null : item.getProduct().getName())
                                .sku(item.getProduct() == null ? null : item.getProduct().getSku())
                                .batchId(item.getBatch() == null ? null : item.getBatch().getId())
                                .locationId(item.getLocation() == null ? null : item.getLocation().getId())
                                .zoneId(item.getZone() == null ? null : item.getZone().getId())
                                .requestedQty(item.getRequestedQty())
                                .reservedQty(item.getReservedQty())
                                .plannedQty(item.getPlannedQty())
                                .pickedQty(item.getPickedQty())
                                .qcPassQty(item.getQcPassQty())
                                .qcFailQty(item.getQcFailQty())
                                .issuedQty(item.getIssuedQty())
                                .unitPrice(item.getUnitPrice())
                                .allocations(allocations.stream()
                                                .map(allocation -> com.wms.dto.response.DeliveryOrderAllocationResponse
                                                                .builder()
                                                                .allocationId(allocation.getId())
                                                                .inventoryId(allocation.getInventory().getId())
                                                                .batchId(allocation.getBatch().getId())
                                                                .locationId(allocation.getLocation().getId())
                                                                .zoneId(allocation.getZone() == null ? null
                                                                                : allocation.getZone().getId())
                                                                .plannedQty(allocation.getPlannedQty())
                                                                .pickedQty(allocation.getPickedQty())
                                                                .qcPassQty(qcSummaryByAllocationId
                                                                                .getOrDefault(allocation.getId(),
                                                                                                AllocationQcSummary.EMPTY)
                                                                                .qcPassQty())
                                                                .qcFailQty(qcSummaryByAllocationId
                                                                                .getOrDefault(allocation.getId(),
                                                                                                AllocationQcSummary.EMPTY)
                                                                                .qcFailQty())
                                                                .qcFailReason(qcSummaryByAllocationId
                                                                                .getOrDefault(allocation.getId(),
                                                                                                AllocationQcSummary.EMPTY)
                                                                                .qcFailReason())
                                                                .stagingLocationId(qcSummaryByAllocationId
                                                                                .getOrDefault(allocation.getId(),
                                                                                                AllocationQcSummary.EMPTY)
                                                                                .stagingLocationId())
                                                                .quarantineLocationId(qcSummaryByAllocationId
                                                                                .getOrDefault(allocation.getId(),
                                                                                                AllocationQcSummary.EMPTY)
                                                                                .quarantineLocationId())
                                                                .qcCompleted(qcSummaryByAllocationId
                                                                                .getOrDefault(allocation.getId(),
                                                                                                AllocationQcSummary.EMPTY)
                                                                                .completed())
                                                                .replacement(Boolean.TRUE
                                                                                .equals(allocation.getReplacement()))
                                                                .build())
                                                .toList())
                                .build();
        }

        public record AllocationQcSummary(BigDecimal qcPassQty, BigDecimal qcFailQty, String qcFailReason,
                        Long stagingLocationId, Long quarantineLocationId, boolean completed) {
                public static final AllocationQcSummary EMPTY = new AllocationQcSummary(BigDecimal.ZERO, BigDecimal.ZERO,
                                null, null, null, false);
        }
}
