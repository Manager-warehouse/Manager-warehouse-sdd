package com.wms.dto.request;


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
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class WarehouseLocationRequest {

    @NotNull(message = "WAREHOUSE_ID_REQUIRED")
    private Long warehouseId;

    @NotBlank(message = "CODE_REQUIRED")
    @Size(max = 50, message = "CODE_TOO_LONG")
    @Pattern(regexp = "^[a-zA-Z0-9_.\\-]+$", message = "INVALID_CODE_FORMAT")
    private String code;

    @NotBlank(message = "TYPE_REQUIRED")
    @Pattern(regexp = "^(ZONE|BIN)$", message = "INVALID_LOCATION_TYPE")
    private String type;

    private Long parentId;

    @DecimalMin(value = "0.001", message = "CAPACITY_MUST_BE_POSITIVE")
    private BigDecimal capacityM3;

    @DecimalMin(value = "0.01", message = "CAPACITY_MUST_BE_POSITIVE")
    private BigDecimal capacityKg;

    private Boolean isQuarantine;

    private Boolean isStaging;
}
