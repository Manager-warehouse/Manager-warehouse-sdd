package com.wms.dto.request.stock_receiving;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class ReceiveQcReceiptRequest {

    @NotNull
    @JsonAlias("expected_version")
    private Integer expectedVersion;

    @Valid
    @NotEmpty
    private List<ReceiveQcReceiptItemRequest> items;

    public Integer getExpectedVersion() {
        return expectedVersion;
    }

    public void setExpectedVersion(Integer expectedVersion) {
        this.expectedVersion = expectedVersion;
    }

    public List<ReceiveQcReceiptItemRequest> getItems() {
        return items;
    }

    public void setItems(List<ReceiveQcReceiptItemRequest> items) {
        this.items = items;
    }
}
