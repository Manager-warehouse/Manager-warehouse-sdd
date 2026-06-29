package com.wms.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryValuationResponse {
    private OffsetDateTime generatedAt;
    private Filters filters;
    private Summary summary;
    private List<ValuationRecord> records;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Filters {
        private Long warehouseId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Summary {
        private Integer totalItems;
        private BigDecimal totalQty;
        private BigDecimal totalValuation;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ValuationRecord {
        private Long warehouseId;
        private String warehouseName;
        private Long productId;
        private String productSku;
        private String productName;
        private String batchNumber;
        private BigDecimal totalQty;
        private BigDecimal unitCost;
        private BigDecimal valuationAmount;
    }
}
