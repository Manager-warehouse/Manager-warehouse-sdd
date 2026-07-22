-- Seed document sequences for all operational modules (Receipt, StockTake, Invoice, Payment)
INSERT INTO document_sequences (sequence_key, next_value)
VALUES 
    ('RECEIPT', 1),
    ('ST', 1),
    ('INVOICE', 1),
    ('PAYMENT', 1)
ON CONFLICT (sequence_key) DO NOTHING;
