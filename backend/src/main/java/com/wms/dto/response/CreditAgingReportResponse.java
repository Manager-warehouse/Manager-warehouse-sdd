package com.wms.dto.response;


import com.wms.entity.access_control.*;
import com.wms.entity.audit_trail.*;
import com.wms.entity.billing_payment.*;
import com.wms.entity.dealer_management.*;
import com.wms.entity.document_numbering.*;
import com.wms.entity.driver_management.*;
import com.wms.entity.fleet_management.*;
import com.wms.entity.notification_delivery.*;
import com.wms.entity.order_fulfillment.*;
import com.wms.entity.price_management.*;
import com.wms.entity.product_catalog.*;
import com.wms.entity.stock_control.*;
import com.wms.entity.stock_counting.*;
import com.wms.entity.stock_receiving.*;
import com.wms.entity.supplier_management.*;
import com.wms.entity.user_configuration.*;
import com.wms.entity.warehouse_location.*;
import com.wms.entity.warehouse_transfer.*;
import com.wms.enums.access_control.*;
import com.wms.enums.audit_trail.*;
import com.wms.enums.billing_payment.*;
import com.wms.enums.dealer_management.*;
import com.wms.enums.driver_management.*;
import com.wms.enums.fleet_management.*;
import com.wms.enums.notification_delivery.*;
import com.wms.enums.order_fulfillment.*;
import com.wms.enums.price_management.*;
import com.wms.enums.stock_control.*;
import com.wms.enums.stock_counting.*;
import com.wms.enums.stock_receiving.*;
import com.wms.enums.supplier_management.*;
import com.wms.enums.user_configuration.*;
import com.wms.enums.warehouse_location.*;
import com.wms.enums.warehouse_transfer.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wms.enums.dealer_management.CreditStatus;
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
