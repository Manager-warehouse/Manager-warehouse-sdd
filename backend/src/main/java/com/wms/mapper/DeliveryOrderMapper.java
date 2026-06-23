package com.wms.mapper;

import com.wms.dto.response.DeliveryOrderItemResponse;
import com.wms.dto.response.DeliveryOrderResponse;
import com.wms.entity.DeliveryOrder;
import com.wms.entity.DeliveryOrderItemAllocation;
import com.wms.entity.DeliveryOrderItem;
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
                                                                                List.of())))
                                                .toList())
                                .createdAt(order.getCreatedAt())
                                .updatedAt(order.getUpdatedAt())
                                .build();
        }

        public DeliveryOrderItemResponse toItemResponse(DeliveryOrderItem item) {
                return toItemResponse(item, List.of());
        }

        public DeliveryOrderItemResponse toItemResponse(DeliveryOrderItem item,
                        List<DeliveryOrderItemAllocation> allocations) {
                return DeliveryOrderItemResponse.builder()
                                .id(item.getId())
                                .productId(item.getProduct().getId())
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
                                                                .zoneId(allocation.getZone().getId())
                                                                .plannedQty(allocation.getPlannedQty())
                                                                .pickedQty(allocation.getPickedQty())
                                                                .replacement(Boolean.TRUE
                                                                                .equals(allocation.getReplacement()))
                                                                .build())
                                                .toList())
                                .build();
        }
}
