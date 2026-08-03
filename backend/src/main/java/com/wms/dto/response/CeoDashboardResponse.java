package com.wms.dto.response;


import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
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
public class CeoDashboardResponse {
    private OffsetDateTime asOfTime;
    private Kpis kpis;
    private List<DebtorInfo> topDebtors;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Kpis {
        private BigDecimal totalInventoryValue;
        private PAndL pAndL;
        private BigDecimal qcFailureRate;
        private BigDecimal onTimeDeliveryRate;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PAndL {
        private String period;
        private BigDecimal revenue;
        private BigDecimal cogs;
        private BigDecimal operatingCosts;
        private BigDecimal netProfit;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DebtorInfo {
        private Long dealerId;
        private String dealerName;
        private BigDecimal overdueAmount;
        private Integer maxOverdueDays;
    }
}
