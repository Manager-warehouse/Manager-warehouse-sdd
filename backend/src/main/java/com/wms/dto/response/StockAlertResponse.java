package com.wms.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

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
