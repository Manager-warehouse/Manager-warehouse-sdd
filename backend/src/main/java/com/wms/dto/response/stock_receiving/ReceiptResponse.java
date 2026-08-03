package com.wms.dto.response.stock_receiving;


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

    @JsonProperty("supplier_name")
    private String supplierName;

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

    private String notes;

    private List<ReceiptItemResponse> items;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("approved_at")
    private OffsetDateTime approvedAt;

    @JsonProperty("pre_receive_approved_at")
    private OffsetDateTime preReceiveApprovedAt;

    @JsonProperty("pre_receive_rejection_reason")
    private String preReceiveRejectionReason;

    @JsonProperty("storekeeper_reviewed_at")
    private OffsetDateTime storekeeperReviewedAt;

    @JsonProperty("recount_reason")
    private String recountReason;

    @JsonProperty("rejection_reason")
    private String rejectionReason;

    @JsonProperty("cancellation_reason")
    private String cancellationReason;

    private Integer version;

    @JsonProperty("credit_note_generated")
    private Boolean creditNoteGenerated;

    @JsonProperty("credit_note_id")
    private Long creditNoteId;

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

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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

    public OffsetDateTime getPreReceiveApprovedAt() {
        return preReceiveApprovedAt;
    }

    public void setPreReceiveApprovedAt(OffsetDateTime preReceiveApprovedAt) {
        this.preReceiveApprovedAt = preReceiveApprovedAt;
    }

    public String getPreReceiveRejectionReason() {
        return preReceiveRejectionReason;
    }

    public void setPreReceiveRejectionReason(String preReceiveRejectionReason) {
        this.preReceiveRejectionReason = preReceiveRejectionReason;
    }

    public OffsetDateTime getStorekeeperReviewedAt() {
        return storekeeperReviewedAt;
    }

    public void setStorekeeperReviewedAt(OffsetDateTime storekeeperReviewedAt) {
        this.storekeeperReviewedAt = storekeeperReviewedAt;
    }

    public String getRecountReason() {
        return recountReason;
    }

    public void setRecountReason(String recountReason) {
        this.recountReason = recountReason;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getCancellationReason() {
        return cancellationReason != null ? cancellationReason : rejectionReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
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

    public Long getCreditNoteId() {
        return creditNoteId;
    }

    public void setCreditNoteId(Long creditNoteId) {
        this.creditNoteId = creditNoteId;
    }
}
