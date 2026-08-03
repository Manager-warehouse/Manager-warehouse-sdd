package com.wms.dto.response;


import com.wms.entity.warehouse_transfer.InterWarehouseTransferItem;
import java.math.BigDecimal;

public record InterWarehouseTransferItemResponse(
        Long id,
        Long productId,
        String productSku,
        String productName,
        Long sourceLocationId,
        Long destinationLocationId,
        BigDecimal plannedQty,
        BigDecimal loadedQty,
        BigDecimal sentQty,
        BigDecimal workerReceivedQty,
        BigDecimal receivedQty,
        BigDecimal qcPassedQty,
        BigDecimal qcFailedQty,
        BigDecimal varianceQty,
        String issueReason,
        String checkerNote,
        String qcFailureReason,
        String uomUnitSnapshot,
        Integer uomPackRateSnapshot,
        BigDecimal unitWeightSnapshot,
        BigDecimal unitVolumeSnapshot) {

    public static InterWarehouseTransferItemResponse from(InterWarehouseTransferItem item) {
        return new InterWarehouseTransferItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getSku(),
                item.getProduct().getName(),
                item.getSourceLocation() == null ? null : item.getSourceLocation().getId(),
                item.getDestinationLocation() == null ? null : item.getDestinationLocation().getId(),
                item.getPlannedQty(),
                item.getLoadedQty(),
                item.getSentQty(),
                item.getWorkerReceivedQty(),
                item.getReceivedQty(),
                item.getQcPassedQty(),
                item.getQcFailedQty(),
                item.getVarianceQty(),
                item.getIssueReason(),
                item.getCheckerNote(),
                item.getQcFailureReason(),
                item.getUomUnitSnapshot() != null ? item.getUomUnitSnapshot() : item.getProduct().getUnit(),
                item.getUomPackRateSnapshot() != null ? item.getUomPackRateSnapshot() : (item.getProduct().getUnitPerPack() != null ? item.getProduct().getUnitPerPack() : 1),
                item.getUnitWeightSnapshot() != null ? item.getUnitWeightSnapshot() : item.getProduct().getWeightKg(),
                item.getUnitVolumeSnapshot() != null ? item.getUnitVolumeSnapshot() : item.getProduct().getVolumeM3());
    }
}

