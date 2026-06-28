package com.wms.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wms.entity.StockTake;
import com.wms.enums.ApprovalLevel;
import com.wms.enums.StockTakeStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public class StockTakeResponse {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("stock_take_number")
    private String stockTakeNumber;

    @JsonProperty("warehouse_id")
    private Long warehouseId;

    @JsonProperty("warehouse_name")
    private String warehouseName;

    @JsonProperty("conducted_by_id")
    private Long conductedById;

    @JsonProperty("conducted_by_name")
    private String conductedByName;

    @JsonProperty("approved_by_id")
    private Long approvedById;

    @JsonProperty("approved_by_name")
    private String approvedByName;

    @JsonProperty("approved_at")
    private OffsetDateTime approvedAt;

    @JsonProperty("status")
    private StockTakeStatus status;

    @JsonProperty("approval_level")
    private ApprovalLevel approvalLevel;

    @JsonProperty("is_employee_fault")
    private Boolean isEmployeeFault;

    @JsonProperty("rejection_reason")
    private String rejectionReason;

    @JsonProperty("total_variance_value")
    private BigDecimal totalVarianceValue;

    @JsonProperty("stock_take_date")
    private LocalDate stockTakeDate;

    @JsonProperty("document_date")
    private LocalDate documentDate;

    @JsonProperty("accounting_period_id")
    private Long accountingPeriodId;

    @JsonProperty("accounting_period_name")
    private String accountingPeriodName;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;

    @JsonProperty("items")
    private List<StockTakeItemResponse> items;

    public static StockTakeResponse from(StockTake st, List<StockTakeItemResponse> items) {
        StockTakeResponse r = new StockTakeResponse();
        r.id = st.getId();
        r.stockTakeNumber = st.getStockTakeNumber();
        r.warehouseId = st.getWarehouse().getId();
        r.warehouseName = st.getWarehouse().getName();
        r.conductedById = st.getConductedBy().getId();
        r.conductedByName = st.getConductedBy().getFullName();
        if (st.getApprovedBy() != null) {
            r.approvedById = st.getApprovedBy().getId();
            r.approvedByName = st.getApprovedBy().getFullName();
        }
        r.approvedAt = st.getApprovedAt();
        r.status = st.getStatus();
        r.approvalLevel = st.getApprovalLevel();
        r.isEmployeeFault = st.getIsEmployeeFault();
        r.rejectionReason = st.getRejectionReason();
        r.totalVarianceValue = st.getTotalVarianceValue();
        r.stockTakeDate = st.getStockTakeDate();
        r.documentDate = st.getDocumentDate();
        if (st.getAccountingPeriod() != null) {
            r.accountingPeriodId = st.getAccountingPeriod().getId();
            r.accountingPeriodName = st.getAccountingPeriod().getPeriodName();
        }
        r.createdAt = st.getCreatedAt();
        r.updatedAt = st.getUpdatedAt();
        r.items = items;
        return r;
    }

    // Getters
    public Long getId() { return id; }
    public String getStockTakeNumber() { return stockTakeNumber; }
    public Long getWarehouseId() { return warehouseId; }
    public String getWarehouseName() { return warehouseName; }
    public Long getConductedById() { return conductedById; }
    public String getConductedByName() { return conductedByName; }
    public Long getApprovedById() { return approvedById; }
    public String getApprovedByName() { return approvedByName; }
    public OffsetDateTime getApprovedAt() { return approvedAt; }
    public StockTakeStatus getStatus() { return status; }
    public ApprovalLevel getApprovalLevel() { return approvalLevel; }
    public Boolean getIsEmployeeFault() { return isEmployeeFault; }
    public String getRejectionReason() { return rejectionReason; }
    public BigDecimal getTotalVarianceValue() { return totalVarianceValue; }
    public LocalDate getStockTakeDate() { return stockTakeDate; }
    public LocalDate getDocumentDate() { return documentDate; }
    public Long getAccountingPeriodId() { return accountingPeriodId; }
    public String getAccountingPeriodName() { return accountingPeriodName; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public List<StockTakeItemResponse> getItems() { return items; }
}
