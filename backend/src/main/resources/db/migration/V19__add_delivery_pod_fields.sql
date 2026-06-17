-- Add missing POD fields to deliveries table to match the JPA Entity.
ALTER TABLE deliveries
    ADD COLUMN IF NOT EXISTS pod_image_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS pod_signature_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS pod_timestamp TIMESTAMPTZ;
