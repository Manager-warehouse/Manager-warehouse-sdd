package com.wms.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public class ReceiptResponse {

    private Long id;

    @JsonProperty("receipt_number")
    private String receiptNumber;

    private String type;
    private String status;

    @JsonProperty("supplier_id")
    private Long supplierId;

    @JsonProperty("dealer_id")
    private Long dealerId;

    @JsonProperty("dealer_name")
    private String dealerName;

    @JsonProperty("delivery_order_id")
    private Long deliveryOrderId;

    @JsonProperty("source_order_code")
    private String sourceOrderCode;

    @JsonProperty("warehouse_id")
    private Long warehouseId;

    @JsonProperty("source_reference")
    private String sourceReference;

    @JsonProperty("source_channel")
    private String sourceChannel;

    @JsonProperty("document_date")
    private LocalDate documentDate;

    private List<ReceiptItemResponse> items;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("approved_at")
    private OffsetDateTime approvedAt;

    private Integer version;

    @JsonProperty("credit_note_generated")
    private Boolean creditNoteGenerated;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public Long getDealerId() {
        return dealerId;
    }

    public void setDealerId(Long dealerId) {
        this.dealerId = dealerId;
    }

    public String getDealerName() {
        return dealerName;
    }

    public void setDealerName(String dealerName) {
        this.dealerName = dealerName;
    }

    public Long getDeliveryOrderId() {
        return deliveryOrderId;
    }

    public void setDeliveryOrderId(Long deliveryOrderId) {
        this.deliveryOrderId = deliveryOrderId;
    }

    public String getSourceOrderCode() {
        return sourceOrderCode;
    }

    public void setSourceOrderCode(String sourceOrderCode) {
        this.sourceOrderCode = sourceOrderCode;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getSourceReference() {
        return sourceReference;
    }

    public void setSourceReference(String sourceReference) {
        this.sourceReference = sourceReference;
    }

    public String getSourceChannel() {
        return sourceChannel;
    }

    public void setSourceChannel(String sourceChannel) {
        this.sourceChannel = sourceChannel;
    }

    public LocalDate getDocumentDate() {
        return documentDate;
    }

    public void setDocumentDate(LocalDate documentDate) {
        this.documentDate = documentDate;
    }

    public List<ReceiptItemResponse> getItems() {
        return items;
    }

    public void setItems(List<ReceiptItemResponse> items) {
        this.items = items;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(OffsetDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Boolean getCreditNoteGenerated() {
        return creditNoteGenerated;
    }

    public void setCreditNoteGenerated(Boolean creditNoteGenerated) {
        this.creditNoteGenerated = creditNoteGenerated;
    }
}
