package com.wms.dto.request;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class ReceiveReceiptRequest {

    @Valid
    @NotEmpty
    private List<ReceiveReceiptItemRequest> items;

    private Integer expectedVersion;

    public List<ReceiveReceiptItemRequest> getItems() {
        return items;
    }

    public void setItems(List<ReceiveReceiptItemRequest> items) {
        this.items = items;
    }

    public Integer getExpectedVersion() {
        return expectedVersion;
    }

    public void setExpectedVersion(Integer expectedVersion) {
        this.expectedVersion = expectedVersion;
    }
}
