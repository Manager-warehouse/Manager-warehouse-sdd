package com.wms.mapper;

import com.wms.dto.response.DeliveryOrderItemResponse;
import com.wms.dto.response.DeliveryOrderResponse;
import com.wms.entity.DeliveryOrder;
import com.wms.entity.DeliveryOrderItem;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DeliveryOrderMapper {
    public DeliveryOrderResponse toResponse(DeliveryOrder order, List<DeliveryOrderItem> items) {
        return DeliveryOrderResponse.builder()
                .id(order.getId())
                .doNumber(order.getDoNumber())
                .dealerId(order.getDealer().getId())
                .warehouseId(order.getWarehouse().getId())
                .type(order.getType())
                .expectedDeliveryDate(order.getExpectedDeliveryDate())
                .status(order.getStatus())
                .cancelReason(order.getCancelReason())
                .documentDate(order.getDocumentDate())
                .notes(order.getNotes())
                .items(items.stream().map(this::toItemResponse).toList())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    public DeliveryOrderItemResponse toItemResponse(DeliveryOrderItem item) {
        return DeliveryOrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .batchId(item.getBatch() == null ? null : item.getBatch().getId())
                .locationId(item.getLocation() == null ? null : item.getLocation().getId())
                .requestedQty(item.getRequestedQty())
                .reservedQty(item.getReservedQty())
                .issuedQty(item.getIssuedQty())
                .unitPrice(item.getUnitPrice())
                .build();
    }
}
