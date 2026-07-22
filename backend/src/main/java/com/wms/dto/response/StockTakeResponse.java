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
import com.wms.entity.stock_counting.StockTake;
import com.wms.enums.order_fulfillment.ApprovalLevel;
import com.wms.enums.stock_counting.StockTakeStatus;

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
