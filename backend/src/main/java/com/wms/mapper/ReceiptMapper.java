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
import com.wms.dto.response.ReceiptItemResponse;
import com.wms.dto.response.ReceiptResponse;
import com.wms.entity.stock_receiving.Receipt;
import com.wms.entity.stock_receiving.ReceiptItem;
import com.wms.enums.stock_receiving.QcSamplingMethod;
import com.wms.enums.stock_receiving.QcResult;
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
