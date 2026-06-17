package com.wms.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class StockTakeCountItemRequest {

    @NotNull(message = "item_id is required")
    @JsonProperty("item_id")
    private Long itemId;

    @NotNull(message = "actual_qty is required")
    @DecimalMin(value = "0", message = "actual_qty must be >= 0")
    @JsonProperty("actual_qty")
    private BigDecimal actualQty;

    @JsonProperty("is_employee_fault")
    private Boolean isEmployeeFault = false;

    @JsonProperty("notes")
    private String notes;

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public BigDecimal getActualQty() { return actualQty; }
    public void setActualQty(BigDecimal actualQty) { this.actualQty = actualQty; }

    public Boolean getIsEmployeeFault() { return isEmployeeFault; }
    public void setIsEmployeeFault(Boolean isEmployeeFault) { this.isEmployeeFault = isEmployeeFault; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
