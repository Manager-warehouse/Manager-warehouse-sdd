package com.wms.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.wms.config.jackson.StrictIntegerDeserializer;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviseReceiptItemRequest {

    @NotNull(message = "RECEIPT_ITEM_ID_REQUIRED")
    @JsonProperty("receiptItemId")
    @JsonAlias("receipt_item_id")
    private Long receiptItemId;

    @NotNull(message = "PRODUCT_ID_REQUIRED")
    @JsonProperty("productId")
    @JsonAlias("product_id")
    private Long productId;

    @NotNull(message = "EXPECTED_QTY_REQUIRED")
    @Min(value = 1, message = "EXPECTED_QTY_POSITIVE")
    @JsonProperty("expectedQty")
    @JsonAlias("expected_qty")
    @JsonDeserialize(using = StrictIntegerDeserializer.class)
    private Integer expectedQty;

    @JsonProperty("unitCost")
    @JsonAlias("unit_cost")
    private BigDecimal unitCost;
}
