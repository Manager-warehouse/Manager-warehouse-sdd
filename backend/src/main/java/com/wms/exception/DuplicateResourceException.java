package com.wms.exception;


/** Exception tài nguyên trùng lặp (email đã tồn tại, mã NV trùng) — trả HTTP 409 (Spec 001). */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
