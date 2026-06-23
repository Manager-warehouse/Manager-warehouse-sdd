package com.wms.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wms.entity.StockTakeItem;

import java.math.BigDecimal;

public class StockTakeItemResponse {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("product_id")
    private Long productId;

    @JsonProperty("product_sku")
    private String productSku;

    @JsonProperty("product_name")
    private String productName;

    @JsonProperty("batch_id")
    private Long batchId;

    @JsonProperty("batch_number")
    private String batchNumber;

    @JsonProperty("location_id")
    private Long locationId;

    @JsonProperty("location_code")
    private String locationCode;

    @JsonProperty("system_qty")
    private BigDecimal systemQty;

    @JsonProperty("actual_qty")
    private BigDecimal actualQty;

    @JsonProperty("variance_qty")
    private BigDecimal varianceQty;

    @JsonProperty("variance_value")
    private BigDecimal varianceValue;

    @JsonProperty("notes")
    private String notes;

    public static StockTakeItemResponse from(StockTakeItem item) {
        StockTakeItemResponse r = new StockTakeItemResponse();
        r.id = item.getId();
        r.productId = item.getProduct().getId();
        r.productSku = item.getProduct().getSku();
        r.productName = item.getProduct().getName();
        r.batchId = item.getBatch().getId();
        r.batchNumber = item.getBatch().getBatchNumber();
        r.locationId = item.getLocation().getId();
        r.locationCode = item.getLocation().getCode();
        r.systemQty = item.getSystemQty();
        r.actualQty = item.getActualQty();
        r.varianceQty = item.getVarianceQty();
        r.varianceValue = item.getVarianceValue();
        r.notes = item.getNotes();
        return r;
    }

    // Getters
    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public String getProductSku() { return productSku; }
    public String getProductName() { return productName; }
    public Long getBatchId() { return batchId; }
    public String getBatchNumber() { return batchNumber; }
    public Long getLocationId() { return locationId; }
    public String getLocationCode() { return locationCode; }
    public BigDecimal getSystemQty() { return systemQty; }
    public BigDecimal getActualQty() { return actualQty; }
    public BigDecimal getVarianceQty() { return varianceQty; }
    public BigDecimal getVarianceValue() { return varianceValue; }
    public String getNotes() { return notes; }
}
