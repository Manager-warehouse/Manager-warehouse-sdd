package com.wms.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReceiptQcRequest {

    /**
     * SUBMIT: WAREHOUSE_STAFF ghi kết quả mẫu QC.
     * CONFIRM: STOREKEEPER kết luận kết quả QC (pass/fail).
     */
    @NotBlank
    @Pattern(regexp = "SUBMIT|CONFIRM")
    private String action;

    // Only for action=SUBMIT
    @Valid
    private List<ReceiptQcItemRequest> items;

    // Only for action=CONFIRM: PASSED or FAILED
    @Pattern(regexp = "PASSED|FAILED")
    private String decision;
}
