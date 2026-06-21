package com.wms.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record InterWarehouseTransferReceiveCheckRequest(
        @NotEmpty List<@Valid InterWarehouseTransferReceiveCheckItemRequest> items) {}
