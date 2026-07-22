package com.wms.dto.response;


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
        BigDecimal sentQty,
        BigDecimal workerReceivedQty,
        BigDecimal receivedQty,
        BigDecimal qcPassedQty,
        BigDecimal qcFailedQty,
        BigDecimal varianceQty,
        String issueReason,
        String checkerNote,
        String qcFailureReason) {

    public static InterWarehouseTransferItemResponse from(InterWarehouseTransferItem item) {
        return new InterWarehouseTransferItemResponse(
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
