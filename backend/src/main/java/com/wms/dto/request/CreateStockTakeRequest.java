package com.wms.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = false)
public class CreateStockTakeRequest {

    @NotNull(message = "warehouse_id is required")
    @JsonProperty("warehouse_id")
    private Long warehouseId;

    @NotNull(message = "stock_take_date is required")
    @JsonProperty("stock_take_date")
    private LocalDate stockTakeDate;

    @NotNull(message = "document_date is required")
    @JsonProperty("document_date")
    private LocalDate documentDate;

    @NotNull(message = "accounting_period_id is required")
    @JsonProperty("accounting_period_id")
    private Long accountingPeriodId;

    @JsonProperty("notes")
    private String notes;

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }

    public LocalDate getStockTakeDate() { return stockTakeDate; }
    public void setStockTakeDate(LocalDate stockTakeDate) { this.stockTakeDate = stockTakeDate; }

    public LocalDate getDocumentDate() { return documentDate; }
    public void setDocumentDate(LocalDate documentDate) { this.documentDate = documentDate; }

    public Long getAccountingPeriodId() { return accountingPeriodId; }
    public void setAccountingPeriodId(Long accountingPeriodId) { this.accountingPeriodId = accountingPeriodId; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
