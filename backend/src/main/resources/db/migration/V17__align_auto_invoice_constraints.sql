-- Ensure one auto-created invoice per Delivery Order.

CREATE UNIQUE INDEX IF NOT EXISTS ux_invoices_do_id
    ON invoices(do_id);
