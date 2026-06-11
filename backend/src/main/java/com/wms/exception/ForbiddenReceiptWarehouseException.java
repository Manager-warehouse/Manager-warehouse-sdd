package com.wms.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a Trưởng kho attempts to approve/reject/create RTV for a receipt
 * belonging to a warehouse not assigned to them.
 * Maps to HTTP 403 FORBIDDEN.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenReceiptWarehouseException extends RuntimeException {
    public ForbiddenReceiptWarehouseException(Long receiptId, Long warehouseId) {
        super("FORBIDDEN_RECEIPT_WAREHOUSE: User is not assigned to warehouse " + warehouseId
              + " for receipt " + receiptId);
    }
}
