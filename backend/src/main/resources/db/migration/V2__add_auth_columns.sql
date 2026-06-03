-- Add refresh token and OTP columns to users table for auth feature
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS refresh_token_hash     VARCHAR(255),
    ADD COLUMN IF NOT EXISTS refresh_token_expires_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS otp_hash               VARCHAR(255),
    ADD COLUMN IF NOT EXISTS otp_expires_at         TIMESTAMPTZ;
