package com.wms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductRequest {

    @NotBlank
    @Size(max = 50)
    private String sku;

    @NotBlank
    @Size(max = 255)
    private String name;

    @NotBlank
    @Size(max = 30)
    private String unit;

    private Integer unitPerPack;

    private String description;

    private BigDecimal weightKg;

    private BigDecimal volumeM3;

    private BigDecimal reorderPoint;
}
