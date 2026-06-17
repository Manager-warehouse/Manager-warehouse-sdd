package com.wms.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransferRejectRequest {

    @NotBlank
    private String rejectionReason;
}
