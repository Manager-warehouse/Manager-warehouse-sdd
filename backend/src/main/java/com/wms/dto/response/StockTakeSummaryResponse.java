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
