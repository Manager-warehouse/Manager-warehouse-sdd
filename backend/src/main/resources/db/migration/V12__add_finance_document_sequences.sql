-- Seed document sequences for invoices and payments (Spec 008)
INSERT INTO document_sequences (sequence_key, next_value)
VALUES ('INVOICE', 1), ('PAYMENT', 1)
ON CONFLICT (sequence_key) DO NOTHING;
