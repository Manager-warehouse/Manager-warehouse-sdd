package com.wms.mapper;


import com.wms.dto.response.ReceiptItemResponse;
import com.wms.dto.response.ReceiptResponse;
import com.wms.entity.stock_control.Batch;
import com.wms.entity.stock_receiving.Receipt;
import com.wms.entity.stock_receiving.ReceiptItem;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ReceiptMapper {

    public ReceiptResponse toResponse(Receipt receipt, List<ReceiptItem> items) {
        ReceiptResponse response = new ReceiptResponse();
        response.setId(receipt.getId());
        response.setReceiptNumber(receipt.getReceiptNumber());
        response.setType(receipt.getType().name());
        response.setStatus(receipt.getStatus().name());
        response.setSupplierId(receipt.getSupplier() != null ? receipt.getSupplier().getId() : null);
        response.setSupplierName(receipt.getSupplier() != null ? receipt.getSupplier().getCompanyName() : null);
        response.setDealerId(receipt.getDealer() != null ? receipt.getDealer().getId() : null);
        response.setDealerName(receipt.getDealer() != null ? receipt.getDealer().getName() : null);
        response.setDeliveryOrderId(receipt.getDeliveryOrder() != null ? receipt.getDeliveryOrder().getId() : null);
        response.setSourceOrderCode(receipt.getSourceOrderCode());
        response.setWarehouseId(receipt.getWarehouse().getId());
        response.setSourceReference(receipt.getSourceOrderCode());
        response.setSourceChannel(receipt.getSourceChannel());
        response.setDocumentDate(receipt.getDocumentDate());
        response.setNotes(receipt.getNotes());
        response.setCreatedAt(receipt.getCreatedAt());
        response.setApprovedAt(receipt.getApprovedAt());
        response.setPreReceiveApprovedAt(receipt.getPreReceiveApprovedAt());
        response.setPreReceiveRejectionReason(receipt.getPreReceiveRejectionReason());
        response.setStorekeeperReviewedAt(receipt.getStorekeeperReviewedAt());
        response.setRecountReason(receipt.getRecountReason());
        response.setVersion(receipt.getVersion());
        response.setCreditNoteGenerated(false);
        response.setItems(items.stream().map(this::toItemResponse).toList());
        return response;
    }

    private ReceiptItemResponse toItemResponse(ReceiptItem item) {
        ReceiptItemResponse response = new ReceiptItemResponse();
        response.setReceiptItemId(item.getId());
        response.setProductId(item.getProduct().getId());
        response.setExpectedQty(item.getExpectedQty());
        response.setActualQty(item.getActualQty());
        response.setOverReceivedQty(item.getOverReceivedQty());
        response.setUnitCost(item.getUnitCost());
        response.setProductName(item.getProduct().getName());
        response.setProductSku(item.getProduct().getSku());
        response.setLocationId(item.getLocation() != null ? item.getLocation().getId() : null);
        response.setBatchId(item.getBatch() != null ? item.getBatch().getId() : null);
        response.setBatchCode(item.getBatch() != null ? readableBatchCode(item.getBatch()) : null);

        response.setQcPassedQty(item.getQualityPassedQty() != null ? item.getQualityPassedQty() : 0);
        response.setQcFailedQty(item.getQualityFailedQty() != null ? item.getQualityFailedQty() : 0);
        response.setQcResult(item.getQcResult() != null ? item.getQcResult().name() : null);
        response.setQcFailureReason(item.getQcFailureReason());
        response.setApprovedQty(item.getApprovedQty());
        response.setQuarantineReadyQty(item.getQuarantineReadyQty());
        response.setQuarantineQty(item.getQuarantineQty());

        return response;
    }

    private String readableBatchCode(Batch batch) {
        return batch.getBatchCode() != null ? batch.getBatchCode() : batch.getBatchNumber();
    }
}
