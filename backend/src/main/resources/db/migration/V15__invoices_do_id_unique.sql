-- Prevents duplicate invoices for the same delivery order. AutoInvoiceServiceImpl relies on
-- catching a unique-constraint violation to stay idempotent when confirmDelivery() is retried
-- (e.g. driver app resubmits after a timeout), but until now invoices.do_id had no such
-- constraint, so a race could create two Invoice rows and double-count dealer.current_balance.
ALTER TABLE invoices
    ADD CONSTRAINT uq_invoices_do_id UNIQUE (do_id);
