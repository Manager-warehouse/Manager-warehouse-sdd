package com.wms.exception;

import com.wms.enums.ReceiptStatus;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when approve/reject is attempted on a receipt that is already in a final decision state
 * (APPROVED, RETURN_TO_SUPPLIER_PENDING, RETURNED_TO_SUPPLIER).
 * Maps to HTTP 409 CONFLICT.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class ReceiptAlreadyDecidedException extends RuntimeException {
    public ReceiptAlreadyDecidedException(Long receiptId, ReceiptStatus currentStatus) {
        super("RECEIPT_ALREADY_DECIDED: Receipt " + receiptId
              + " is already in status " + currentStatus
              + " and cannot be approved or rejected again.");
    }
}
