package com.wms.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record TransferReceiveCheckRequest(
        @NotEmpty List<@Valid TransferReceiveCheckItemRequest> items) {
}
