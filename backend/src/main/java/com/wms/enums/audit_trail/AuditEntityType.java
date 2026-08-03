package com.wms.enums.audit_trail;


/** Enum loại đối tượng được ghi audit log (USER, SYSTEM_CONFIG, RECEIPT, ...) (Spec 001). */
public enum AuditEntityType {
    RECEIPT,
    ISSUE,
    TRANSFER,
    ADJUSTMENT,
    STOCKTAKE,
    DELIVERY_ORDER,
    DELIVERY,
    DELIVERY_OTP_ATTEMPT,
    DEALER,
    SUPPLIER,
    BATCH,
    INVENTORY,
    RETURN,
    SCRAP_DISPOSAL,
    TRIP,
    INVOICE,
    RECEIPT_ITEM
}
