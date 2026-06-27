package com.wms.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateReturnItemRequest {
    @NotNull(message = "PRODUCT_REQUIRED")
    private Long productId;

    @NotNull(message = "QTY_REQUIRED")
    @Min(value = 1, message = "QTY_MIN_1")
    private Integer expectedQty;
}
