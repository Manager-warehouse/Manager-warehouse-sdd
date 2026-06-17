package com.wms.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class DisposalCreateRequest {

    @NotNull(message = "WAREHOUSE_ID_REQUIRED")
    private Long warehouseId;

    @NotNull(message = "PRODUCT_ID_REQUIRED")
    private Long productId;

    @NotNull(message = "BATCH_ID_REQUIRED")
    private Long batchId;

    @NotNull(message = "LOCATION_ID_REQUIRED")
    private Long locationId;

    @NotNull(message = "QUANTITY_REQUIRED")
    @DecimalMin(value = "0.01", message = "QUANTITY_MUST_BE_POSITIVE")
    private BigDecimal quantity;

    @NotBlank(message = "CAUSE_REQUIRED")
    @Size(max = 2000, message = "CAUSE_TOO_LONG")
    private String cause;

    @Size(max = 500, message = "IMAGE_URL_TOO_LONG")
    private String imageUrl;

    private LocalDate documentDate;
}
