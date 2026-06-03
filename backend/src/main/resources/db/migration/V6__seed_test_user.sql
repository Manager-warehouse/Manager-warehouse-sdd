INSERT INTO users (code, full_name, email, phone, password_hash, role, is_active)
VALUES ('U001', 'Test User', 'test@wms.com', '0123456789', '$2a$12$R.S/mD2l7rYt.L6iXw8CKe1Z28XgL.K01fXW7P3S4t3kXyFvT8O5K', 'ADMIN', true)
ON CONFLICT (email) DO NOTHING;
