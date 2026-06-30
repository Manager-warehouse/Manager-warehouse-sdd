package com.wms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InterWarehouseTransferRejectRequest {
    @NotBlank
    @Size(max = 1000)
    private String rejectionReason;
}
