package com.wms.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Builder
public class PriceHistoryResponse {

    private Long id;
    private Long productId;
    private String productSku;
    private String productName;
    private LocalDate effectiveDate;
    private LocalDate endDate;
    private BigDecimal costPrice;
    private BigDecimal sellingPrice;
    private String status;
    private String notes;
    private UserRef createdBy;
    private OffsetDateTime createdAt;
    private UserRef approvedBy;
    private OffsetDateTime approvedAt;
    private UserRef cancelledBy;
    private OffsetDateTime cancelledAt;

    /** Populated only for ACCOUNTANT_MANAGER detail view. */
    private PreviousApprovedRef previousApproved;

    @Getter
    @Builder
    public static class UserRef {
        private Long id;
        private String fullName;
    }

    @Getter
    @Builder
    public static class PreviousApprovedRef {
        private Long id;
        private LocalDate effectiveDate;
        private LocalDate endDate;
        private BigDecimal costPrice;
        private BigDecimal sellingPrice;
        private BigDecimal costPriceDelta;
        private BigDecimal costPriceDeltaPct;
        private BigDecimal sellingPriceDelta;
        private BigDecimal sellingPriceDeltaPct;
    }
}
