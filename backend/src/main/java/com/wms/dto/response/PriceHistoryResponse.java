package com.wms.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Builder
public class PriceHistoryResponse {

    private Long id;

    @JsonProperty("product_id")
    private Long productId;

    @JsonProperty("product_sku")
    private String productSku;

    @JsonProperty("product_name")
    private String productName;

    @JsonProperty("warehouse_id")
    private Long warehouseId;

    @JsonProperty("warehouse_name")
    private String warehouseName;

    @JsonProperty("warehouse_code")
    private String warehouseCode;

    @JsonProperty("effective_date")
    private LocalDate effectiveDate;

    @JsonProperty("cost_price")
    private BigDecimal costPrice;

    @JsonProperty("selling_price")
    private BigDecimal sellingPrice;

    private String status;
    private String notes;

    @JsonProperty("created_by")
    private UserRef createdBy;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("approved_by")
    private UserRef approvedBy;

    @JsonProperty("approved_at")
    private OffsetDateTime approvedAt;

    @JsonProperty("cancelled_by")
    private UserRef cancelledBy;

    @JsonProperty("cancelled_at")
    private OffsetDateTime cancelledAt;

    @JsonProperty("previous_approved")
    private PreviousApprovedRef previousApproved;

    @Getter
    @Builder
    public static class UserRef {
        private Long id;

        @JsonProperty("full_name")
        private String fullName;
    }

    @Getter
    @Builder
    public static class PreviousApprovedRef {
        private Long id;

        @JsonProperty("effective_date")
        private LocalDate effectiveDate;

        @JsonProperty("cost_price")
        private BigDecimal costPrice;

        @JsonProperty("selling_price")
        private BigDecimal sellingPrice;

        @JsonProperty("cost_price_delta")
        private BigDecimal costPriceDelta;

        @JsonProperty("cost_price_delta_pct")
        private BigDecimal costPriceDeltaPct;

        @JsonProperty("selling_price_delta")
        private BigDecimal sellingPriceDelta;

        @JsonProperty("selling_price_delta_pct")
        private BigDecimal sellingPriceDeltaPct;
    }
}
