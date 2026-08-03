package com.wms.dto.request;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record InterWarehouseTransferReceiveCountRequest(
        @NotEmpty(message = "RECEIVE_COUNT_ITEMS_REQUIRED")
        List<@Valid InterWarehouseTransferReceiveCountItemRequest> items) {}
