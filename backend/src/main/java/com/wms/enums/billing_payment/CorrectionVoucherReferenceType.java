package com.wms.enums.billing_payment;

// Which financial document a Correction Voucher (US-WMS-29) targets. Deliberately
// narrower than Adjustment.referenceType in general (which is a free-text column
// shared with STOCK_TAKE/TRANSFER/RECEIPT_ITEM references) - only these five
// document types have no UPDATE/DELETE endpoint of their own and therefore rely on
// this mechanism to fix a wrong amount after the fact.
public enum CorrectionVoucherReferenceType {
    INVOICE,
    PAYMENT_RECEIPT,
    SUPPLIER_INVOICE,
    SUPPLIER_PAYMENT,
    CREDIT_NOTE
}
