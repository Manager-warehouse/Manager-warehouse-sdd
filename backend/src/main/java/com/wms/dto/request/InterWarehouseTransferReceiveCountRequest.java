package com.wms.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record InterWarehouseTransferReceiveCountRequest(
        @NotEmpty List<@Valid InterWarehouseTransferReceiveCountItemRequest> items) {}
