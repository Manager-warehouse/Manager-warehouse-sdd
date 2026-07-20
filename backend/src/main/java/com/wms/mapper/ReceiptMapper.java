package com.wms.mapper;

import com.wms.dto.response.ReceiptItemResponse;
import com.wms.dto.response.ReceiptResponse;
import com.wms.entity.Receipt;
import com.wms.entity.ReceiptItem;
import com.wms.enums.QcSamplingMethod;
import com.wms.enums.QcResult;
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
        response.setDealerId(receipt.getDealer() != null ? receipt.getDealer().getId() : null);
        response.setDealerName(receipt.getDealer() != null ? receipt.getDealer().getName() : null);
        response.setDeliveryOrderId(receipt.getDeliveryOrder() != null ? receipt.getDeliveryOrder().getId() : null);
        response.setSourceOrderCode(receipt.getSourceOrderCode());
        response.setWarehouseId(receipt.getWarehouse().getId());
        response.setSourceReference(receipt.getSourceOrderCode());
        response.setSourceChannel(receipt.getSourceChannel());
        response.setDocumentDate(receipt.getDocumentDate());
        response.setCreatedAt(receipt.getCreatedAt());
        response.setApprovedAt(receipt.getApprovedAt());
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

        // Calculate QC passed and failed quantities based on rules
        Integer passedQty = 0;
        Integer failedQty = 0;
        if (item.getQcResult() != null) {
            Integer samplePassed = item.getSamplePassedQty() != null ? item.getSamplePassedQty() : 0;
            Integer sampleFailed = item.getSampleFailedQty() != null ? item.getSampleFailedQty() : 0;
            Integer actual = item.getActualQty() != null ? item.getActualQty() : 0;

            if (item.getQcSamplingMethod() == QcSamplingMethod.FULL_INSPECTION) {
                passedQty = samplePassed;
                failedQty = sampleFailed;
            } else if (item.getQcSamplingMethod() == QcSamplingMethod.RANDOM_SAMPLE) {
                if (item.getQcResult() == QcResult.PASSED) {
                    passedQty = actual;
                    failedQty = 0;
                } else if (item.getQcResult() == QcResult.FAILED) {
                    passedQty = 0;
                    failedQty = actual;
                }
            }
        }
        response.setQcPassedQty(passedQty);
        response.setQcFailedQty(failedQty);

        return response;
    }
}
