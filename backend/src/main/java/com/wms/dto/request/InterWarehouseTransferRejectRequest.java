package com.wms.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InterWarehouseTransferRejectRequest {
    @NotBlank(message = "REJECTION_REASON_REQUIRED")
    @Size(max = 1000, message = "REASON_TOO_LONG")
    private String rejectionReason;
}
