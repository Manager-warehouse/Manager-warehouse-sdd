package com.wms.mapper;


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
import com.wms.dto.response.DeliveryOrderItemResponse;
import com.wms.dto.response.DeliveryOrderResponse;
import com.wms.entity.order_fulfillment.DeliveryOrder;
import com.wms.entity.order_fulfillment.DeliveryOrderItemAllocation;
import com.wms.entity.order_fulfillment.DeliveryOrderItem;
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
