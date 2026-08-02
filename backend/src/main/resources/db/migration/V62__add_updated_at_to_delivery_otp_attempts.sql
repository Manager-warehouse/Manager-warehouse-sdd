ALTER TABLE delivery_otp_attempts
ADD COLUMN
IF NOT EXISTS updated_at TIMESTAMPTZ;

UPDATE delivery_otp_attempts
SET updated_at = COALESCE(created_at, NOW())
WHERE updated_at IS NULL;

ALTER TABLE delivery_otp_attempts
ALTER COLUMN updated_at
SET
DEFAULT NOW
();

ALTER TABLE delivery_otp_attempts
ALTER COLUMN updated_at
SET
NOT NULL;