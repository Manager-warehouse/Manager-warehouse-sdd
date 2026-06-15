ALTER TABLE delivery_order_items
    DROP COLUMN IF EXISTS serial_number;

ALTER TABLE deliveries
    DROP COLUMN IF EXISTS otp_recipient_email,
    DROP COLUMN IF EXISTS otp_sent_at;

ALTER TABLE deliveries
    ADD COLUMN IF NOT EXISTS otp_verified_at TIMESTAMPTZ;

CREATE TABLE IF NOT EXISTS delivery_otp_attempts (
    id              BIGSERIAL    PRIMARY KEY,
    delivery_id     BIGINT       NOT NULL REFERENCES deliveries(id),
    otp_hash        VARCHAR(255) NOT NULL,
    recipient_email VARCHAR(255) NOT NULL,
    expires_at      TIMESTAMPTZ  NOT NULL,
    consumed_at     TIMESTAMPTZ,
    attempt_count   INTEGER      NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_delivery_otp_attempts_active
    ON delivery_otp_attempts(delivery_id, expires_at)
    WHERE consumed_at IS NULL;
