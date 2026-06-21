package com.wms.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InterWarehouseTransferRejectRequest {
    private String rejectionReason;
}
