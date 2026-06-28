package com.wms.dto.response;

import com.wms.enums.DeliveryStatus;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class DeliveryAttemptResponse {
    private Long deliveryId;
    private Integer attemptNumber;
    private DeliveryStatus status;
    private String podImageUrl;
    private String podSignatureUrl;
    private OffsetDateTime podTimestamp;
    private OffsetDateTime otpVerifiedAt;
    private String failureReason;
    private OffsetDateTime dispatchedAt;
    private OffsetDateTime deliveredAt;
}
