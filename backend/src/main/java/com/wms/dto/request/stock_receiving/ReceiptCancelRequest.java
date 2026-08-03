package com.wms.dto.request.stock_receiving;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReceiptCancelRequest {

    @NotNull(message = "EXPECTED_VERSION_REQUIRED")
    private Integer expectedVersion;

    @NotBlank(message = "REASON_REQUIRED")
    @Size(max = 2000, message = "REASON_TOO_LONG")
    private String reason;
}
