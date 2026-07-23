-- V23__supplier_invoices.sql
-- Support for Supplier Invoicing & Accounts Payable (US-WMS-28)

ALTER TABLE suppliers ADD COLUMN IF NOT EXISTS current_balance NUMERIC(18,2) NOT NULL DEFAULT 0.00;

CREATE TABLE IF NOT EXISTS supplier_invoices (
    id BIGSERIAL PRIMARY KEY,
    invoice_number VARCHAR(50) UNIQUE NOT NULL,
    supplier_invoice_number VARCHAR(100) NOT NULL,
    receipt_id BIGINT NOT NULL REFERENCES receipts(id),
    supplier_id BIGINT NOT NULL REFERENCES suppliers(id),
    total_amount NUMERIC(18,2) NOT NULL CHECK (total_amount >= 0),
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UNPAID' CHECK (status IN ('UNPAID', 'PARTIALLY_PAID', 'PAID')),
    document_date DATE NOT NULL,
    accounting_period_id BIGINT NOT NULL REFERENCES accounting_periods(id),
    created_by BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS supplier_payments (
    id BIGSERIAL PRIMARY KEY,
    payment_number VARCHAR(50) UNIQUE NOT NULL,
    supplier_id BIGINT NOT NULL REFERENCES suppliers(id),
    supplier_invoice_id BIGINT NOT NULL REFERENCES supplier_invoices(id),
    amount NUMERIC(18,2) NOT NULL CHECK (amount > 0),
    payment_date DATE NOT NULL,
    payment_method VARCHAR(30) NOT NULL CHECK (payment_method IN ('BANK_TRANSFER', 'CASH')),
    document_date DATE NOT NULL,
    accounting_period_id BIGINT NOT NULL REFERENCES accounting_periods(id),
    created_by BIGINT NOT NULL REFERENCES users(id),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS supplier_billing_notifications (
    id BIGSERIAL PRIMARY KEY,
    receipt_id BIGINT NOT NULL REFERENCES receipts(id),
    receipt_number VARCHAR(50) NOT NULL,
    supplier_id BIGINT NOT NULL REFERENCES suppliers(id),
    supplier_name VARCHAR(255) NOT NULL,
    warehouse_id BIGINT NOT NULL REFERENCES warehouses(id),
    completed_at TIMESTAMPTZ NOT NULL,
    total_amount_estimate NUMERIC(18,2) NOT NULL DEFAULT 0.00,
    invoice_status VARCHAR(30) NOT NULL DEFAULT 'NOT_INVOICED' CHECK (invoice_status IN ('NOT_INVOICED', 'INVOICED')),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'READ', 'ARCHIVED')),
    recipient_role VARCHAR(50) NOT NULL DEFAULT 'ACCOUNTANT',
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_supplier_invoices_supplier_id ON supplier_invoices(supplier_id);
CREATE INDEX IF NOT EXISTS idx_supplier_invoices_period_id ON supplier_invoices(accounting_period_id);
CREATE INDEX IF NOT EXISTS idx_supplier_payments_supplier_id ON supplier_payments(supplier_id);
CREATE INDEX IF NOT EXISTS idx_supplier_payments_invoice_id ON supplier_payments(supplier_invoice_id);
CREATE INDEX IF NOT EXISTS idx_supplier_billing_notif_status ON supplier_billing_notifications(status, invoice_status);
