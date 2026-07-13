package com.wms.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record TransferReturnRequest(
    @NotBlank(message = "RETURN_REASON_REQUIRED")
    String reason,

    List<WrongSkuItemRequest> wrongSkuItems
) {
    public TransferReturnRequest(String reason) {
        this(reason, java.util.Collections.emptyList());
    }
}
