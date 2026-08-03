package com.wms.entity.order_fulfillment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class DeliveryOtpAttemptTest {

    @Test
    void initializeTimestamps_populatesRequiredAuditColumns() {
        DeliveryOtpAttempt otp = new DeliveryOtpAttempt();

        otp.initializeTimestamps();

        assertThat(otp.getCreatedAt()).isNotNull();
        assertThat(otp.getUpdatedAt()).isNotNull();
    }

    @Test
    void refreshUpdatedAt_replacesPreviousTimestamp() {
        DeliveryOtpAttempt otp = new DeliveryOtpAttempt();
        OffsetDateTime previous = OffsetDateTime.now().minusMinutes(1);
        otp.setUpdatedAt(previous);

        otp.refreshUpdatedAt();

        assertThat(otp.getUpdatedAt()).isAfter(previous);
    }
}
