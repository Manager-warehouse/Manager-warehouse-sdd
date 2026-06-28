package com.wms.dto.response;

import com.wms.enums.DeliveryOtpStatus;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class DeliveryOtpResponse {
    private Long deliveryId;
    private String recipientEmail;
    private DeliveryOtpStatus status;
    private OffsetDateTime expiresAt;
    private Integer attemptCount;
}
