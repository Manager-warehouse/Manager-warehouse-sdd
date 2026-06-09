package com.wms.enums;

public enum DeliveryStatus {
    PENDING,
    IN_TRANSIT,
    // OTP verified or delivery otherwise confirmed.
    DELIVERED,
    RETURNED
}
