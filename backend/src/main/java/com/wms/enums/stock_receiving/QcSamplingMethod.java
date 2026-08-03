package com.wms.enums.stock_receiving;


import com.wms.entity.supplier_management.Supplier;

public enum QcSamplingMethod {
    // Supplier has fewer than 5 previous APPROVED receipts.
    FULL_INSPECTION,
    // Supplier has at least 5 previous APPROVED receipts.
    RANDOM_SAMPLE
}
