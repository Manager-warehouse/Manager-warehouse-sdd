package com.wms.dto.request;

import com.wms.enums.BatchGrade;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReceiptApproveItemRequest {

    @NotNull
    private Long receiptItemId;

    @NotNull
    private Long locationId;

    @NotNull
    private BatchGrade grade;

    private BigDecimal unitCost;
}
