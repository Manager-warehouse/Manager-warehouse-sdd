package com.wms.enums;

public enum AuditAction {
    LOGIN,
    LOGOUT,
    CREATE,
    UPDATE,
    STATUS_CHANGE,
    APPROVE,
    REJECT,
    CANCEL,
    SOFT_DELETE,
    ASSIGN,
    UNASSIGN,
    // Inbound receipt lifecycle actions (US-WMS-05)
    RECEIPT_APPROVE,
    RECEIPT_REJECT,
    RECEIPT_RETURN_CONFIRM,
    RECEIPT_PUTAWAY_COMPLETE,
    // Quarantine RTV actions (US-WMS-04)
    QUARANTINE_RTV_CREATE,
    QUARANTINE_RTV_CONFIRM,
    // Inventory mutation audit
    INVENTORY_UPDATE
}
