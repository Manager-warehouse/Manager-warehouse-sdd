package com.wms.dto.response;


import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
public class StockAlertResponse {
    private Long id;
    private Long warehouseId;
    private String warehouseName;
    private Long productId;
    private String productSku;
    private String productName;
    private BigDecimal currentQty;
    private BigDecimal reorderPoint;
    private String alertType;
    private Boolean isResolved;
    private OffsetDateTime resolvedAt;
    private OffsetDateTime createdAt;
}
