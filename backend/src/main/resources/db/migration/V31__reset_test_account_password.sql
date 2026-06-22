-- Reset password for dev-only test account: test@wms.com
-- Password value: Password@123 (bcrypt cost 12, from V13 seed)
-- This only touches the pure test account, not any team member accounts.

UPDATE users
SET password_hash = '$2a$12$R.S/mD2l7rYt.L6iXw8CKe1Z28XgL.K01fXW7P3S4t3kXyFvT8O5K'
WHERE email = 'test@wms.com';
