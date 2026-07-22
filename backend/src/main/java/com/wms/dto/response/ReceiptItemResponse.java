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
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public class ReceiptItemResponse {

    @JsonProperty("receipt_item_id")
    private Long receiptItemId;

    @JsonProperty("product_id")
    private Long productId;

    @JsonProperty("expected_qty")
    private Integer expectedQty;

    @JsonProperty("actual_qty")
    private Integer actualQty;

    @JsonProperty("over_received_qty")
    private Integer overReceivedQty;

    @JsonProperty("unit_cost")
    private BigDecimal unitCost;

    @JsonProperty("product_name")
    private String productName;

    @JsonProperty("product_sku")
    private String productSku;

    @JsonProperty("qc_passed_qty")
    private Integer qcPassedQty;

    @JsonProperty("qc_failed_qty")
    private Integer qcFailedQty;



    @JsonProperty("location_id")
    private Long locationId;

    public Long getReceiptItemId() {
        return receiptItemId;
    }

    public void setReceiptItemId(Long receiptItemId) {
        this.receiptItemId = receiptItemId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getExpectedQty() {
        return expectedQty;
    }

    public void setExpectedQty(Integer expectedQty) {
        this.expectedQty = expectedQty;
    }

    public Integer getActualQty() {
        return actualQty;
    }

    public void setActualQty(Integer actualQty) {
        this.actualQty = actualQty;
    }

    public Integer getOverReceivedQty() {
        return overReceivedQty;
    }

    public void setOverReceivedQty(Integer overReceivedQty) {
        this.overReceivedQty = overReceivedQty;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductSku() {
        return productSku;
    }

    public void setProductSku(String productSku) {
        this.productSku = productSku;
    }

    public Integer getQcPassedQty() {
        return qcPassedQty;
    }

    public void setQcPassedQty(Integer qcPassedQty) {
        this.qcPassedQty = qcPassedQty;
    }

    public Integer getQcFailedQty() {
        return qcFailedQty;
    }

    public void setQcFailedQty(Integer qcFailedQty) {
        this.qcFailedQty = qcFailedQty;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }
}

