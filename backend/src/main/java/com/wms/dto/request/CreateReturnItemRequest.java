package com.wms.dto.request;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
