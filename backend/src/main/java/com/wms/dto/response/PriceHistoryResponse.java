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
