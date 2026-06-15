package com.wms.dto.response;

import com.wms.enums.ReceiptStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ReceiptQcResponse {

    private Long receiptId;
    private String receiptNumber;
    private ReceiptStatus status;
    private List<ReceiptItemQcResponse> items;
}
