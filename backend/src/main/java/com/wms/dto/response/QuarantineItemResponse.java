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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuarantineItemResponse {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("product_sku")
    private String productSku;

    @JsonProperty("product_name")
    private String productName;

    @JsonProperty("qc_failed_qty")
    private Integer qcFailedQty;

    @JsonProperty("qc_failure_reason")
    private String qcFailureReason;

    @JsonProperty("receipt_number")
    private String receiptNumber;

    @JsonProperty("supplier_id")
    private Long supplierId;

    @JsonProperty("total_value")
    private BigDecimal totalValue;

    @JsonProperty("unit")
    private String unit;

    @JsonProperty("receipt_id")
    private Long receiptId;

    @JsonProperty("receipt_version")
    private Integer receiptVersion;

    @JsonProperty("origin_type")
    @Builder.Default
    private String originType = "RECEIPT";

    @JsonProperty("quarantine_record_id")
    private Long quarantineRecordId;
}
