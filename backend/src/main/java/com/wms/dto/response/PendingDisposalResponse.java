package com.wms.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Builder
public class PendingDisposalResponse {
    private Long id;
    private String adjustmentNumber;
    private Long warehouseId;
    private String warehouseName;
    private Long productId;
    private String productSku;
    private String productName;
    private BigDecimal failedQty;
    private BigDecimal totalValue;
    private String cause;
    private String reportedByName;
    private LocalDate documentDate;
    private OffsetDateTime createdAt;
}
