package com.wms.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wms.entity.StockTake;
import com.wms.enums.ApprovalLevel;
import com.wms.enums.StockTakeStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public class StockTakeSummaryResponse {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("stock_take_number")
    private String stockTakeNumber;

    @JsonProperty("warehouse_id")
    private Long warehouseId;

    @JsonProperty("warehouse_name")
    private String warehouseName;

    @JsonProperty("conducted_by_name")
    private String conductedByName;

    @JsonProperty("status")
    private StockTakeStatus status;

    @JsonProperty("approval_level")
    private ApprovalLevel approvalLevel;

    @JsonProperty("is_employee_fault")
    private Boolean isEmployeeFault;

    @JsonProperty("total_variance_value")
    private BigDecimal totalVarianceValue;

    @JsonProperty("stock_take_date")
    private LocalDate stockTakeDate;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    public static StockTakeSummaryResponse from(StockTake st) {
        StockTakeSummaryResponse r = new StockTakeSummaryResponse();
        r.id = st.getId();
        r.stockTakeNumber = st.getStockTakeNumber();
        r.warehouseId = st.getWarehouse().getId();
        r.warehouseName = st.getWarehouse().getName();
        r.conductedByName = st.getConductedBy().getFullName();
        r.status = st.getStatus();
        r.approvalLevel = st.getApprovalLevel();
        r.isEmployeeFault = st.getIsEmployeeFault();
        r.totalVarianceValue = st.getTotalVarianceValue();
        r.stockTakeDate = st.getStockTakeDate();
        r.createdAt = st.getCreatedAt();
        return r;
    }

    // Getters
    public Long getId() { return id; }
    public String getStockTakeNumber() { return stockTakeNumber; }
    public Long getWarehouseId() { return warehouseId; }
    public String getWarehouseName() { return warehouseName; }
    public String getConductedByName() { return conductedByName; }
    public StockTakeStatus getStatus() { return status; }
    public ApprovalLevel getApprovalLevel() { return approvalLevel; }
    public Boolean getIsEmployeeFault() { return isEmployeeFault; }
    public BigDecimal getTotalVarianceValue() { return totalVarianceValue; }
    public LocalDate getStockTakeDate() { return stockTakeDate; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
