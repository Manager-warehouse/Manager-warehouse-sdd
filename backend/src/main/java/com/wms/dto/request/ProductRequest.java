package com.wms.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
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

    @Positive
    private Integer unitPerPack;

    @Size(max = 1000)
    private String description;

    @PositiveOrZero
    private BigDecimal weightKg;

    @PositiveOrZero
    private BigDecimal volumeM3;

    @PositiveOrZero
    private BigDecimal reorderPoint;
}
