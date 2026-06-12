package com.wms.enums;

public enum QcSamplingMethod {
    // Supplier has fewer than 5 previous APPROVED receipts.
    FULL_INSPECTION,
    // Supplier has at least 5 previous APPROVED receipts.
    RANDOM_SAMPLE
}
