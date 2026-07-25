-- V33__supplier_invoices_receipt_id_unique.sql
-- SupplierInvoiceServiceImpl.createSupplierInvoice() only guards against a duplicate
-- invoice with a check-then-act read (findByReceiptId().isPresent()), which is not atomic:
-- two concurrent POST /supplier-invoices requests for the same receiptId (double-click,
-- retry) can both pass that check before either commits, silently creating two
-- supplier_invoices rows for one receipt and double-counting what's owed to the supplier.
-- Unlike invoices.do_id (unique since V15), receipt_id here had no DB-level constraint at
-- all. Add one so the race is caught atomically; the service layer translates the resulting
-- DataIntegrityViolationException into the existing SUPPLIER_INVOICE_ALREADY_EXISTS (409).
ALTER TABLE supplier_invoices ADD CONSTRAINT uq_supplier_invoices_receipt_id UNIQUE (receipt_id);
