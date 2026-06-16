package com.wms.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wms.enums.CreditStatus;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditAgingReportResponse {

    @JsonProperty("dealer_id")
    private Long dealerId;

    @JsonProperty("dealer_code")
    private String dealerCode;

    @JsonProperty("dealer_name")
    private String dealerName;

    @JsonProperty("credit_limit")
    private BigDecimal creditLimit;

    @JsonProperty("current_balance")
    private BigDecimal currentBalance;

    @JsonProperty("credit_status")
    private CreditStatus creditStatus;

    @JsonProperty("in_term_amount")
    private BigDecimal inTermAmount;

    @JsonProperty("overdue_1_to_30")
    private BigDecimal overdue1To30;

    @JsonProperty("overdue_31_to_60")
    private BigDecimal overdue31To60;

    @JsonProperty("overdue_61_to_90")
    private BigDecimal overdue61To90;

    @JsonProperty("overdue_over_90")
    private BigDecimal overdueOver90;

    @JsonProperty("risk_level")
    private String riskLevel;
}
