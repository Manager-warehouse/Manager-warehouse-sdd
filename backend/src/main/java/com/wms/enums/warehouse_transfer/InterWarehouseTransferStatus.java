package com.wms.enums.warehouse_transfer;


public enum InterWarehouseTransferStatus {
    NEW,
    APPROVED,
    REJECTED,
    IN_TRANSIT,
    PUTAWAY_PENDING_APPROVAL,
    COMPLETED,
    COMPLETED_WITH_DISCREPANCY,
    CANCELLED,
    QUARANTINED
}
