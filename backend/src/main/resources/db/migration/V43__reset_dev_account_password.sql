-- Reset password for dev/test accounts only (not team member accounts)
-- Password: Password@123 (bcrypt cost 12)
-- Hash verified in V13__seed_test_user.sql

-- Reset test@wms.com (pure dev seed account)
UPDATE users
SET password_hash = '$2a$12$R.S/mD2l7rYt.L6iXw8CKe1Z28XgL.K01fXW7P3S4t3kXyFvT8O5K'
WHERE email = 'test@wms.com';
