package com.wms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountingPeriodCreateRequest {

    // Format YYYY-MM; start/end dates are derived server-side (whole calendar month)
    // to avoid mismatched or overlapping ranges being entered by hand.
    @NotBlank(message = "Period name is required")
    @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])", message = "Period name must be in YYYY-MM format")
    private String periodName;

    private String notes;
}
