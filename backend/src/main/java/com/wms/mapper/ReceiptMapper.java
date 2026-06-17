package com.wms.mapper;

import com.wms.dto.response.ReceiptItemResponse;
import com.wms.dto.response.ReceiptResponse;
import com.wms.entity.Receipt;
import com.wms.entity.ReceiptItem;
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
        response.setWarehouseId(receipt.getWarehouse().getId());
        response.setSourceReference(receipt.getSourceOrderCode());
        response.setSourceChannel(receipt.getSourceChannel());
        response.setDocumentDate(receipt.getDocumentDate());
        response.setCreatedAt(receipt.getCreatedAt());
        response.setVersion(receipt.getVersion());
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
        if (item.getProduct() != null) {
            response.setProductName(item.getProduct().getName());
            response.setProductSku(item.getProduct().getSku());
            response.setHasSerial(item.getProduct().getHasSerial());
        }
        return response;
    }
}

