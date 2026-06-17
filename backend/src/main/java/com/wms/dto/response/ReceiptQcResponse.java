package com.wms.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wms.enums.ReceiptStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ReceiptQcResponse {

    @JsonProperty("receipt_id")
    private Long receiptId;

    @JsonProperty("receipt_number")
    private String receiptNumber;

    @JsonProperty("status")
    private ReceiptStatus status;

    @JsonProperty("items")
    private List<ReceiptItemQcResponse> items;
}
