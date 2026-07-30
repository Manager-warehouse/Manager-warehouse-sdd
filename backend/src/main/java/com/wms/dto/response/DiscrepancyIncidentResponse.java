package com.wms.dto.response;

import com.wms.entity.access_control.User;
import com.wms.entity.product_catalog.Product;
import com.wms.entity.warehouse_location.Warehouse;
import com.wms.entity.warehouse_transfer.DiscrepancyIncident;
import com.wms.entity.warehouse_transfer.InterWarehouseTransfer;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record DiscrepancyIncidentResponse(
        Long id,
        Long transferId,
        String transferNumber,
        Long sourceWarehouseId,
        String sourceWarehouseCode,
        Long destinationWarehouseId,
        String destinationWarehouseCode,
        Long productId,
        String productSku,
        String productName,
        String incidentType,
        BigDecimal quantity,
        String status,
        String resolutionNote,
        Long resolvedById,
        String resolvedByName,
        OffsetDateTime resolvedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static DiscrepancyIncidentResponse from(DiscrepancyIncident incident) {
        InterWarehouseTransfer transfer = incident.getTransfer();
        Product product = incident.getProduct();
        Warehouse source = transfer.getSourceWarehouse();
        Warehouse destination = transfer.getDestinationWarehouse();
        User resolvedBy = incident.getResolvedBy();

        return new DiscrepancyIncidentResponse(
                incident.getId(),
                transfer.getId(),
                transfer.getTransferNumber(),
                source.getId(),
                source.getCode(),
                destination.getId(),
                destination.getCode(),
                product.getId(),
                product.getSku(),
                product.getName(),
                incident.getIncidentType(),
                incident.getQuantity(),
                incident.getStatus(),
                incident.getResolutionNote(),
                resolvedBy == null ? null : resolvedBy.getId(),
                resolvedBy == null ? null : resolvedBy.getFullName(),
                incident.getResolvedAt(),
                incident.getCreatedAt(),
                incident.getUpdatedAt()
        );
    }
}
