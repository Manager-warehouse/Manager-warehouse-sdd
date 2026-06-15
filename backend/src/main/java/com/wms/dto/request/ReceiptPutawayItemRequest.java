package com.wms.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReceiptPutawayItemRequest {

    @NotNull
    private Long receiptItemId;

    @NotNull
    private Long locationId;
}
