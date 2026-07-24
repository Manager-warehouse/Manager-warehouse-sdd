package com.wms.enums.billing_payment;

// Which financial document a Correction Voucher (US-WMS-29) targets. Deliberately
// narrower than Adjustment.referenceType in general (which is a free-text column
// shared with STOCK_TAKE/TRANSFER/RECEIPT_ITEM references) - only these four
// document types can have a period-closed correction posted against them.
public enum CorrectionVoucherReferenceType {
    INVOICE,
    PAYMENT_RECEIPT,
    SUPPLIER_INVOICE,
    SUPPLIER_PAYMENT
}
