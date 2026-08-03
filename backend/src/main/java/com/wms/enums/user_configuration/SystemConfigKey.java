package com.wms.enums.user_configuration;


/** Enum khóa cấu hình hệ thống (DEFAULT_CREDIT_LIMIT, MONTHLY_CLOSING_DAY, ...) (Spec 001). */
public enum SystemConfigKey {
    DEFAULT_CREDIT_LIMIT,
    DEFAULT_PAYMENT_TERM_DAYS,
    CREDIT_HOLD_OVERDUE_DAYS,
    CREDIT_UNLOCK_BUFFER_PCT,
    MONTHLY_CLOSING_DAY,
    MIN_INVENTORY_WARNING_THRESHOLD
}
