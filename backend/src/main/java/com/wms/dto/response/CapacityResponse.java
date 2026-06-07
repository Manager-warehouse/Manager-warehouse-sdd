package com.wms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CapacityResponse {
    private BigDecimal capacityM3;
    private BigDecimal capacityKg;
    private BigDecimal usedVolumeM3;
    private BigDecimal usedWeightKg;
    private BigDecimal availableVolumeM3;
    private BigDecimal availableWeightKg;
}
