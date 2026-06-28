package com.wms.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = false)
public class StockTakeCountRequest {

    @Valid
    @NotEmpty(message = "items must not be empty")
    @JsonProperty("items")
    private List<StockTakeCountItemRequest> items;

    public List<StockTakeCountItemRequest> getItems() { return items; }
    public void setItems(List<StockTakeCountItemRequest> items) { this.items = items; }
}
