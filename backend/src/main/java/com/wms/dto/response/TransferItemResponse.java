package com.wms.dto.response;

import com.wms.entity.TransferItem;
import java.math.BigDecimal;

public record TransferItemResponse(
        Long id,
        Long productId,
        String productSku,
        String productName,
        Long sourceLocationId,
        Long destinationLocationId,
        BigDecimal plannedQty,
        BigDecimal sentQty,
        BigDecimal workerReceivedQty,
        BigDecimal receivedQty,
        BigDecimal qcPassedQty,
        BigDecimal qcFailedQty,
        BigDecimal varianceQty,
        String issueReason,
        String checkerNote,
        String qcFailureReason) {

    public static TransferItemResponse from(TransferItem item) {
        return new TransferItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getSku(),
                item.getProduct().getName(),
                item.getSourceLocation() == null ? null : item.getSourceLocation().getId(),
                item.getDestinationLocation() == null ? null : item.getDestinationLocation().getId(),
                item.getPlannedQty(),
                item.getSentQty(),
                item.getWorkerReceivedQty(),
                item.getReceivedQty(),
                item.getQcPassedQty(),
                item.getQcFailedQty(),
                item.getVarianceQty(),
                item.getIssueReason(),
                item.getCheckerNote(),
                item.getQcFailureReason());
    }
}
