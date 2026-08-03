package com.wms.exception;

import org.springframework.http.HttpStatus;

public class OtpDeliveryFailedException extends OutboundDeliveryException {

    public OtpDeliveryFailedException(String message) {
        super("OTP_DELIVERY_FAILED", HttpStatus.BAD_GATEWAY, message);
    }
}
