package com.wms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PreReceiveApprovalRequest {

    @NotNull(message = "EXPECTED_VERSION_REQUIRED")
    private Integer expectedVersion;

    @NotBlank(message = "PRE_RECEIVE_DECISION_REQUIRED")
    @Pattern(regexp = "APPROVE|REJECT", message = "PRE_RECEIVE_DECISION_INVALID")
    private String decision;

    @Size(max = 2000, message = "REASON_TOO_LONG")
    private String reason;
}
