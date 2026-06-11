package com.wms.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a second RTV (Return To Vendor) is attempted for a receipt that already has
 * a pending or confirmed RETURN_TO_VENDOR adjustment.
 * Maps to HTTP 409 CONFLICT.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class RtvAlreadyExistsException extends RuntimeException {
    public RtvAlreadyExistsException(Long receiptId) {
        super("RTV_ALREADY_EXISTS: A pending or confirmed Return-To-Vendor adjustment already exists for receipt "
              + receiptId + ". Duplicate RTV creation is not allowed.");
    }
}
