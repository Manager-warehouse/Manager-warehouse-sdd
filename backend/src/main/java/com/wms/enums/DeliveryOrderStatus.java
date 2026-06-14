package com.wms.enums;

public enum DeliveryOrderStatus {
    NEW,
    PICKING,
    PENDING_WAREHOUSE_APPROVAL,
    READY_TO_SHIP,
    IN_TRANSIT,
    DELIVERED,
    RETURNED,
    CANCELLED
}
