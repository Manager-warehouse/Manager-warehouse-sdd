package com.wms.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DisposalResponse {
    private Long adjustmentId;
    private String adjustmentNumber;
    private boolean autoApproved;
    private String message;
}
