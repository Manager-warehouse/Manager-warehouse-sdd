package com.wms.dto.request;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * DTO request nhập số đếm thực tế — chứa danh sách StockTakeCountItemRequest.
 * Dùng bởi: PUT /api/v1/stocktakes/{id}/count (StockTakeController.recordCount)
 * → StockTakeService.recordCount()
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public class StockTakeCountRequest {

    @Valid
    @NotEmpty(message = "items must not be empty")
    @JsonProperty("items")
    private List<StockTakeCountItemRequest> items;

    public List<StockTakeCountItemRequest> getItems() { return items; }
    public void setItems(List<StockTakeCountItemRequest> items) { this.items = items; }
}
