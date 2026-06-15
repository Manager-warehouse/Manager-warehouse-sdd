package com.wms.enums;

public enum AuditAction {
    // Auth
    LOGIN,
    LOGOUT,

    // Generic CRUD
    CREATE,
    UPDATE,
    STATUS_CHANGE,
    APPROVE,
    REJECT,
    CANCEL,
    SOFT_DELETE,
    ASSIGN,
    UNASSIGN,

    // Receipt lifecycle (spec 003)
    RECEIPT_CREATE,
    RECEIPT_RECEIVE,
    RECEIPT_QC_SUBMIT,
    RECEIPT_QC_CONFIRM,
    RECEIPT_APPROVE,
    RECEIPT_REJECT,
    RECEIPT_PUTAWAY_COMPLETE,

    // Quarantine / RTV (spec 003 US-WMS-04)
    QUARANTINE_RTV_CREATE,
    QUARANTINE_RTV_CONFIRM,

    // Inventory snapshot (attached to every inventory-affecting transition)
    INVENTORY_UPDATE
}
